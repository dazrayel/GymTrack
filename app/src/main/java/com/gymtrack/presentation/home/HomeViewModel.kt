package com.gymtrack.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.domain.model.CompletedSetRecord
import com.gymtrack.domain.model.DashboardPeriod
import com.gymtrack.domain.model.PeriodDashboardStats
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionStatus
import com.gymtrack.domain.model.dailyVolumeTrend
import com.gymtrack.domain.model.dashboardPeriodStats
import com.gymtrack.domain.model.evaluateAchievements
import com.gymtrack.domain.model.historicalPersonalRecords
import com.gymtrack.domain.model.periodBounds
import com.gymtrack.domain.model.recommendNextWorkout
import com.gymtrack.domain.model.trainedDayCount
import com.gymtrack.domain.repository.WorkoutRepository
import com.gymtrack.domain.repository.WorkoutSessionRepository
import com.gymtrack.domain.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(DashboardPeriod.WEEK)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeDashboard()
        observeInProgressSession()
    }

    fun selectPeriod(period: DashboardPeriod) {
        selectedPeriod.value = period
    }

    private fun observeDashboard() {
        viewModelScope.launch {
            combine(
                workoutRepository.getAll(),
                workoutRepository.observeWorkoutIdsWithExercises(),
                sessionRepository.observeCompletedSessions(),
                sessionRepository.observeCompletedSetHistory(),
                selectedPeriod,
            ) { workouts, executableIds, sessions, sets, period ->
                HomeDashboardEmission(
                    workouts = workouts,
                    executableWorkoutIds = executableIds,
                    sessions = sessions,
                    sets = sets,
                    period = period,
                )
            }
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message)
                    }
                }
                .collect { emission ->
                    val nowMillis = timeProvider.nowMillis()
                    val zoneId = ZoneId.systemDefault()
                    val bounds = periodBounds(emission.period, nowMillis, zoneId)
                    val emptyHistory = emission.sessions.isEmpty()
                    val nextWorkout = recommendNextWorkout(
                        workouts = emission.workouts,
                        sessions = emission.sessions.map { it.toCompletedSession() },
                        workoutIdsWithExercises = emission.executableWorkoutIds,
                    )
                    val achievements = evaluateAchievements(
                        sessions = emission.sessions,
                        completedSets = emission.sets,
                        zoneId = zoneId,
                    )
                    _uiState.update {
                        it.copy(
                            recentWorkout = emission.sessions.firstOrNull(),
                            nextWorkout = nextWorkout,
                            selectedPeriod = emission.period,
                            periodStats = if (emptyHistory) {
                                PeriodDashboardStats.Empty
                            } else {
                                dashboardPeriodStats(emission.sessions, emission.sets, bounds)
                            },
                            trainedDayCount = if (emptyHistory) {
                                0
                            } else {
                                trainedDayCount(emission.sessions, bounds, zoneId)
                            },
                            dailyVolumeTrend = if (emptyHistory) {
                                emptyList()
                            } else {
                                dailyVolumeTrend(emission.sessions, nowMillis, zoneId)
                            },
                            records = if (emptyHistory) {
                                emptyList()
                            } else {
                                historicalPersonalRecords(emission.sets)
                            },
                            unlockedAchievementCount = achievements.count { it.unlocked },
                            totalAchievementCount = achievements.size,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
        }
    }

    private fun observeInProgressSession() {
        viewModelScope.launch {
            sessionRepository.observeInProgress()
                .catch { e ->
                    _uiState.update {
                        it.copy(error = e.message)
                    }
                }
                .collect { session ->
                    _uiState.update { it.copy(inProgressSession = session) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

private data class HomeDashboardEmission(
    val workouts: List<Workout>,
    val executableWorkoutIds: Set<Long>,
    val sessions: List<WorkoutHistoryItem>,
    val sets: List<CompletedSetRecord>,
    val period: DashboardPeriod,
)

private fun WorkoutHistoryItem.toCompletedSession(): WorkoutSession = WorkoutSession(
    id = sessionId,
    workoutId = workoutId,
    workoutName = workoutName,
    startedAtMillis = startedAtMillis,
    endedAtMillis = endedAtMillis,
    status = WorkoutSessionStatus.COMPLETED,
)

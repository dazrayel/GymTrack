package com.gymtrack.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.domain.model.AchievementCatalog
import com.gymtrack.domain.model.CompletedSetRecord
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionStatus
import com.gymtrack.domain.model.WeeklyWorkoutPlan
import com.gymtrack.domain.model.absoluteMetricChange
import com.gymtrack.domain.model.bestAvailablePerformance
import com.gymtrack.domain.model.dashboardPeriodTotals
import com.gymtrack.domain.model.evaluateAchievements
import com.gymtrack.domain.model.historicalPersonalRecords
import com.gymtrack.domain.model.isoWeekBounds
import com.gymtrack.domain.model.percentMetricChange
import com.gymtrack.domain.model.previousIsoWeekBounds
import com.gymtrack.domain.model.previousRollingDayBounds
import com.gymtrack.domain.model.recommendNextWorkout
import com.gymtrack.domain.model.rollingDayBounds
import com.gymtrack.domain.model.trainedDayCount
import com.gymtrack.domain.repository.GoogleIdentityRepository
import com.gymtrack.domain.repository.WeeklyPlanRepository
import com.gymtrack.domain.repository.WorkoutRepository
import com.gymtrack.domain.repository.WorkoutSessionRepository
import com.gymtrack.domain.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val weeklyPlanRepository: WeeklyPlanRepository,
    private val googleIdentityRepository: GoogleIdentityRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeDashboard()
        observeIdentity()
    }

    private fun observeIdentity() {
        viewModelScope.launch {
            googleIdentityRepository.currentUser.collect { user ->
                _uiState.update { it.copy(displayName = user?.displayName) }
            }
        }
    }

    private fun observeDashboard() {
        viewModelScope.launch {
            combine(
                workoutRepository.getAll(),
                workoutRepository.observeWorkoutIdsWithExercises(),
                sessionRepository.observeCompletedSessions(),
                sessionRepository.observeCompletedSetHistory(),
                weeklyPlanRepository.observePlans(),
            ) { workouts, executableIds, sessions, sets, plans ->
                DashboardEmission(
                    workouts = workouts,
                    executableWorkoutIds = executableIds,
                    sessions = sessions,
                    sets = sets,
                    plans = plans,
                )
            }
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message)
                    }
                }
                .collect { emission ->
                    _uiState.update { current ->
                        buildState(
                            currentDisplayName = current.displayName,
                            emission = emission,
                        )
                    }
                }
        }
    }

    private fun buildState(
        currentDisplayName: String?,
        emission: DashboardEmission,
    ): DashboardUiState {
        val nowMillis = timeProvider.nowMillis()
        val zoneId = ZoneId.systemDefault()
        val emptyHistory = emission.sessions.isEmpty()

        if (emptyHistory) {
            return DashboardUiState(
                displayName = currentDisplayName,
                isLoading = false,
                isEmpty = true,
                error = null,
                totalAchievementCount = AchievementCatalog.size,
                nextWorkout = recommendNextWorkout(
                    workouts = emission.workouts,
                    sessions = emptyList(),
                    workoutIdsWithExercises = emission.executableWorkoutIds,
                ),
                hasWeeklyPlan = emission.plans.isNotEmpty(),
                todayPlanWorkoutName = planNameForOffset(emission.plans, zoneId, nowMillis, 0),
                tomorrowPlanWorkoutName = planNameForOffset(emission.plans, zoneId, nowMillis, 1),
            )
        }

        val weekBounds = isoWeekBounds(nowMillis, zoneId)
        val previousWeekBounds = previousIsoWeekBounds(nowMillis, zoneId)
        val weekTotals = dashboardPeriodTotals(emission.sessions, weekBounds)
        val previousWeekTotals = dashboardPeriodTotals(emission.sessions, previousWeekBounds)
        val hasPreviousWeekData = !previousWeekTotals.isEmpty

        val last30 = rollingDayBounds(nowMillis, zoneId, dayCount = 30)
        val previous30 = previousRollingDayBounds(nowMillis, zoneId, dayCount = 30)
        val volumeLast30 = dashboardPeriodTotals(emission.sessions, last30).volume
        val previousVolume30 = dashboardPeriodTotals(emission.sessions, previous30)
        val hasPreviousVolumePeriod = !previousVolume30.isEmpty

        val achievements = evaluateAchievements(
            sessions = emission.sessions,
            completedSets = emission.sets,
            zoneId = zoneId,
        )
        val records = historicalPersonalRecords(emission.sets)

        return DashboardUiState(
            displayName = currentDisplayName,
            isLoading = false,
            isEmpty = false,
            error = null,
            weeklySessionCount = weekTotals.sessionCount,
            weeklySetCount = weekTotals.completedSetCount,
            weeklyVolume = weekTotals.volume,
            hasPreviousWeekData = hasPreviousWeekData,
            sessionChange = if (hasPreviousWeekData) {
                absoluteMetricChange(weekTotals.sessionCount, previousWeekTotals.sessionCount)
            } else {
                null
            },
            setChange = if (hasPreviousWeekData) {
                percentMetricChange(weekTotals.completedSetCount, previousWeekTotals.completedSetCount)
            } else {
                null
            },
            volumeChange = if (hasPreviousWeekData) {
                percentMetricChange(weekTotals.volume, previousWeekTotals.volume)
            } else {
                null
            },
            trainedDaysLast30 = trainedDayCount(emission.sessions, last30, zoneId),
            volumeLast30 = volumeLast30,
            hasPreviousVolumePeriod = hasPreviousVolumePeriod,
            volumePeriodChange = if (hasPreviousVolumePeriod) {
                percentMetricChange(volumeLast30, previousVolume30.volume)
            } else {
                null
            },
            highlightRecord = bestAvailablePerformance(records),
            unlockedAchievementCount = achievements.count { it.unlocked },
            totalAchievementCount = achievements.size,
            nextWorkout = recommendNextWorkout(
                workouts = emission.workouts,
                sessions = emission.sessions.map { it.toCompletedSession() },
                workoutIdsWithExercises = emission.executableWorkoutIds,
            ),
            hasWeeklyPlan = emission.plans.isNotEmpty(),
            todayPlanWorkoutName = planNameForOffset(emission.plans, zoneId, nowMillis, 0),
            tomorrowPlanWorkoutName = planNameForOffset(emission.plans, zoneId, nowMillis, 1),
        )
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

private data class DashboardEmission(
    val workouts: List<Workout>,
    val executableWorkoutIds: Set<Long>,
    val sessions: List<WorkoutHistoryItem>,
    val sets: List<CompletedSetRecord>,
    val plans: List<WeeklyWorkoutPlan>,
)

private fun planNameForOffset(
    plans: List<WeeklyWorkoutPlan>,
    zoneId: ZoneId,
    nowMillis: Long,
    dayOffset: Long,
): String? {
    if (plans.isEmpty()) return null
    val day = java.time.Instant.ofEpochMilli(nowMillis)
        .atZone(zoneId)
        .toLocalDate()
        .plusDays(dayOffset)
        .dayOfWeek
    return plans.firstOrNull { it.dayOfWeek == day }?.workoutName
}

private fun WorkoutHistoryItem.toCompletedSession(): WorkoutSession = WorkoutSession(
    id = sessionId,
    workoutId = workoutId,
    workoutName = workoutName,
    startedAtMillis = startedAtMillis,
    endedAtMillis = endedAtMillis,
    status = WorkoutSessionStatus.COMPLETED,
)

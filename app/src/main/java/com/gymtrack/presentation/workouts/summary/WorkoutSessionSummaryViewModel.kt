package com.gymtrack.presentation.workouts.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.domain.model.CompletedSetRecord
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSet
import com.gymtrack.domain.model.XpEvent
import com.gymtrack.domain.model.evaluateAchievements
import com.gymtrack.domain.repository.GamificationRepository
import com.gymtrack.domain.repository.WorkoutSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutSessionSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val gamificationRepository: GamificationRepository,
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(WorkoutSessionSummaryUiState(sessionId = sessionId))
    val uiState: StateFlow<WorkoutSessionSummaryUiState> = _uiState.asStateFlow()

    init {
        observeSummary()
    }

    private fun observeSummary() {
        viewModelScope.launch {
            combine(
                workoutSessionRepository.observeSession(sessionId),
                observeExercisesWithSets(),
                workoutSessionRepository.observeCompletedSessions(),
                workoutSessionRepository.observeCompletedSetHistory(),
                gamificationRepository.observeXpEvents(),
            ) { session, exercisesWithSets, sessions, history, xpEvents ->
                SummaryEmission(
                    session = session,
                    exercisesWithSets = exercisesWithSets,
                    completedSessions = sessions,
                    history = history,
                    xpEvents = xpEvents,
                )
            }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { emission ->
                    if (emission.session == null) {
                        _uiState.update {
                            it.copy(
                                session = null,
                                exercises = emptyList(),
                                setsByExerciseId = emptyMap(),
                                completedSetHistory = emptyList(),
                                xpEvents = emptyList(),
                                achievementsWithSession = emptyList(),
                                achievementsWithoutSession = emptyList(),
                                isLoading = false,
                                sessionNotFound = true,
                            )
                        }
                    } else {
                        val (exercises, setsByExerciseId) = emission.exercisesWithSets
                        val zoneId = ZoneId.systemDefault()
                        val withSession = evaluateAchievements(
                            sessions = emission.completedSessions,
                            completedSets = emission.history,
                            zoneId = zoneId,
                        )
                        val withoutSession = evaluateAchievements(
                            sessions = emission.completedSessions.filter { it.sessionId != sessionId },
                            completedSets = emission.history.filter { it.sessionId != sessionId },
                            zoneId = zoneId,
                        )
                        _uiState.update {
                            it.copy(
                                session = emission.session,
                                exercises = exercises,
                                setsByExerciseId = setsByExerciseId,
                                completedSetHistory = emission.history,
                                xpEvents = emission.xpEvents,
                                achievementsWithSession = withSession,
                                achievementsWithoutSession = withoutSession,
                                isLoading = false,
                                sessionNotFound = false,
                            )
                        }
                    }
                }
        }
    }

    private fun observeExercisesWithSets() =
        workoutSessionRepository.observeSessionExercises(sessionId)
            .flatMapLatest { exercises ->
                val sorted = exercises.sortedBy { it.position }
                if (sorted.isEmpty()) {
                    flowOf(sorted to emptyMap<Long, List<WorkoutSet>>())
                } else {
                    combine(sorted.map { exercise ->
                        workoutSessionRepository.observeSets(exercise.id)
                    }) { setLists ->
                        val setsByExerciseId = sorted.mapIndexed { index, exercise ->
                            exercise.id to setLists[index]
                        }.toMap()
                        sorted to setsByExerciseId
                    }
                }
            }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private data class SummaryEmission(
        val session: WorkoutSession?,
        val exercisesWithSets: Pair<List<WorkoutSessionExercise>, Map<Long, List<WorkoutSet>>>,
        val completedSessions: List<WorkoutHistoryItem>,
        val history: List<CompletedSetRecord>,
        val xpEvents: List<XpEvent>,
    )
}

package com.gymtrack.presentation.workouts.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.domain.model.WorkoutSet
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
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutSessionSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutSessionRepository: WorkoutSessionRepository,
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
                workoutSessionRepository.observeCompletedSetHistory(),
            ) { session, exercisesWithSets, history ->
                Triple(session, exercisesWithSets, history)
            }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { (session, exercisesWithSets, history) ->
                    if (session == null) {
                        _uiState.update {
                            it.copy(
                                session = null,
                                exercises = emptyList(),
                                setsByExerciseId = emptyMap(),
                                completedSetHistory = emptyList(),
                                isLoading = false,
                                sessionNotFound = true,
                            )
                        }
                    } else {
                        val (exercises, setsByExerciseId) = exercisesWithSets
                        _uiState.update {
                            it.copy(
                                session = session,
                                exercises = exercises,
                                setsByExerciseId = setsByExerciseId,
                                completedSetHistory = history,
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
}

package com.gymtrack.presentation.stats

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.domain.model.exercisePerformanceHistory
import com.gymtrack.domain.model.personalRecords
import com.gymtrack.domain.model.sessionExerciseStats
import com.gymtrack.domain.repository.WorkoutSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@HiltViewModel
class ExerciseStatsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: WorkoutSessionRepository,
) : ViewModel() {

    private val exerciseName: String = URLDecoder.decode(
        checkNotNull(savedStateHandle["exerciseName"]),
        StandardCharsets.UTF_8,
    )

    private val _uiState = MutableStateFlow(ExerciseStatsUiState(exerciseName = exerciseName))
    val uiState: StateFlow<ExerciseStatsUiState> = _uiState.asStateFlow()

    init {
        observeStats()
    }

    private fun observeStats() {
        viewModelScope.launch {
            sessionRepository.observeCompletedSetHistory()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { records ->
                    val history = exercisePerformanceHistory(records, exerciseName)
                    val personalRecord = personalRecords(
                        sessionExerciseStats(records.filter { it.exerciseName == exerciseName }),
                    )
                    _uiState.update {
                        it.copy(
                            exerciseName = exerciseName,
                            performanceHistory = history,
                            personalRecord = personalRecord,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

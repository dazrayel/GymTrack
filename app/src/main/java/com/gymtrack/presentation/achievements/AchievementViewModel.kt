package com.gymtrack.presentation.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.domain.model.evaluateAchievements
import com.gymtrack.domain.repository.WorkoutSessionRepository
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
class AchievementViewModel @Inject constructor(
    private val sessionRepository: WorkoutSessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AchievementUiState())
    val uiState: StateFlow<AchievementUiState> = _uiState.asStateFlow()

    init {
        observeAchievements()
    }

    private fun observeAchievements() {
        viewModelScope.launch {
            combine(
                sessionRepository.observeCompletedSessions(),
                sessionRepository.observeCompletedSetHistory(),
            ) { sessions, sets ->
                sessions to sets
            }
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message)
                    }
                }
                .collect { (sessions, sets) ->
                    val achievements = evaluateAchievements(
                        sessions = sessions,
                        completedSets = sets,
                        zoneId = ZoneId.systemDefault(),
                    )
                    _uiState.update {
                        it.copy(
                            achievements = achievements,
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

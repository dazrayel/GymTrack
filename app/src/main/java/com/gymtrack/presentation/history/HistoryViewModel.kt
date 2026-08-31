package com.gymtrack.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.repository.WorkoutSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepository: WorkoutSessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.observeCompletedSessions()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { items ->
                    _uiState.update {
                        it.copy(items = items, isLoading = false, error = null)
                    }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun showDeleteConfirmation(item: WorkoutHistoryItem) {
        _uiState.update { it.copy(sessionToDelete = item) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(sessionToDelete = null) }
    }

    fun confirmDelete() {
        val item = _uiState.value.sessionToDelete ?: return
        viewModelScope.launch {
            try {
                sessionRepository.deleteCompletedSession(item.sessionId)
                _uiState.update { it.copy(sessionToDelete = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(sessionToDelete = null, error = e.message) }
            }
        }
    }
}

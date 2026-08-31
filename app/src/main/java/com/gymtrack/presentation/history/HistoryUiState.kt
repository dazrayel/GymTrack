package com.gymtrack.presentation.history

import com.gymtrack.domain.model.WorkoutHistoryItem

data class HistoryUiState(
    val items: List<WorkoutHistoryItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val sessionToDelete: WorkoutHistoryItem? = null,
)

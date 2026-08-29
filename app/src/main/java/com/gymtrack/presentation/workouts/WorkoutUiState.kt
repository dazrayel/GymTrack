package com.gymtrack.presentation.workouts

import com.gymtrack.domain.model.Workout

data class WorkoutUiState(
    val workouts: List<Workout> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val showAddEditDialog: Boolean = false,
    val workoutToEdit: Workout? = null,
    val showDeleteConfirmation: Boolean = false,
    val workoutToDelete: Workout? = null,
)

package com.gymtrack.presentation.exercises

import com.gymtrack.domain.exercise.CatalogDemoOption
import com.gymtrack.domain.model.Exercise

data class ExerciseUiState(
    val exercises: List<Exercise> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val showAddEditDialog: Boolean = false,
    val exerciseToEdit: Exercise? = null,
    val showDeleteConfirmation: Boolean = false,
    val exerciseToDelete: Exercise? = null,
    val showDemoPicker: Boolean = false,
    val catalogDemoOptions: List<CatalogDemoOption> = emptyList(),
)

package com.gymtrack.presentation.workouts.detail

import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutExerciseDetail

data class WorkoutDetailUiState(
    val workout: Workout? = null,
    val exercises: List<WorkoutExerciseDetail> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,

    // Editar nome/descrição do treino
    val showEditWorkoutDialog: Boolean = false,

    // Fluxo de adição: picker de catálogo → configuração de parâmetros
    val showExercisePicker: Boolean = false,
    val exerciseToConfigure: WorkoutExerciseDetail? = null,

    // Exclusão de exercício do treino
    val showDeleteConfirmation: Boolean = false,
    val exerciseToDelete: WorkoutExerciseDetail? = null,

    /** Consumable one-shot navigation to workout execution. */
    val sessionStartedEvent: Long? = null,

    /** Set when start is blocked by an in-progress session of another workout. */
    val inProgressConflict: InProgressConflictUiState? = null,
)

data class InProgressConflictUiState(
    val sessionId: Long,
    val workoutName: String,
)

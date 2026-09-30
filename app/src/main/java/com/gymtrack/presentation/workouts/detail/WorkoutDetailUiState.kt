package com.gymtrack.presentation.workouts.detail

import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutBlock
import com.gymtrack.domain.model.WorkoutBlockDetail
import com.gymtrack.domain.model.WorkoutBlockType
import com.gymtrack.domain.model.WorkoutExerciseDetail

data class WorkoutDetailUiState(
    val workout: Workout? = null,
    val blocks: List<WorkoutBlockDetail> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,

    val showEditWorkoutDialog: Boolean = false,

    /** Chooser: normal / bi-set / tri-set */
    val showAddTypeDialog: Boolean = false,
    val pendingBlockType: WorkoutBlockType? = null,

    /** Multi-slot builder for BI_SET / TRI_SET before persist. */
    val blockDraftSlots: List<Exercise?> = emptyList(),
    val blockDraftSlotIndex: Int? = null,
    val showBlockBuilder: Boolean = false,

    val showExercisePicker: Boolean = false,
    val exerciseToConfigure: WorkoutExerciseDetail? = null,
    val configureBlock: WorkoutBlock? = null,
    val configureRounds: Int = 3,
    val configureRestSeconds: Int = 60,

    val showDeleteConfirmation: Boolean = false,
    val blockToDelete: WorkoutBlockDetail? = null,

    val sessionStartedEvent: Long? = null,
    val inProgressConflict: InProgressConflictUiState? = null,
)

data class InProgressConflictUiState(
    val sessionId: Long,
    val workoutName: String,
)

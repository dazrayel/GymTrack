package com.gymtrack.presentation.workouts.execution

import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSet

data class WorkoutExecutionUiState(
    val sessionId: Long,
    val session: WorkoutSession? = null,
    val exercises: List<WorkoutSessionExercise> = emptyList(),
    val setsByExerciseId: Map<Long, List<WorkoutSet>> = emptyMap(),
    val currentExerciseIndex: Int = 0,
    val currentSetIndex: Int = 0,
    val isWorkoutComplete: Boolean = false,
    val phase: WorkoutExecutionPhase = WorkoutExecutionPhase.WORKING,
    val restEndsAtMillis: Long? = null,
    val restPausedRemainingMillis: Long? = null,
    val restSessionExerciseId: Long? = null,
    val restAfterSetIndex: Int? = null,
    val restRemainingMillis: Long = 0L,
    val elapsedMillis: Long = 0L,
    val completedSets: Int = 0,
    val plannedSets: Int = 0,
    val completedExercises: Int = 0,
    val totalExercises: Int = 0,
    val progressPercent: Int = 0,
    val showFinishConfirmation: Boolean = false,
    val sessionFinishedEvent: Long? = null,
    val repsInput: String = "",
    val weightInput: String = "",
    val repsError: Int? = null,
    val weightError: Int? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val isRestPaused: Boolean
        get() = restPausedRemainingMillis != null

    val restExercise: WorkoutSessionExercise?
        get() = exercises.firstOrNull { it.id == restSessionExerciseId }

    val currentExercise: WorkoutSessionExercise?
        get() = if (isWorkoutComplete && phase != WorkoutExecutionPhase.RESTING) {
            null
        } else {
            exercises.getOrNull(currentExerciseIndex)
        }

    val currentExerciseSets: List<WorkoutSet>
        get() = currentExercise?.let { setsByExerciseId[it.id].orEmpty() }.orEmpty()
}

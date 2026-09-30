package com.gymtrack.presentation.workouts.execution

import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSessionExerciseStatus
import com.gymtrack.domain.model.WorkoutSet

data class WorkoutExecutionUiState(
    val sessionId: Long,
    val session: WorkoutSession? = null,
    val exercises: List<WorkoutSessionExercise> = emptyList(),
    val setsByExerciseId: Map<Long, List<WorkoutSet>> = emptyMap(),
    /** Library exercise for the current session exercise (media / external identity). */
    val mediaExercise: Exercise? = null,
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
    val finishHasPendingExercises: Boolean = false,
    val showSkipConfirmation: Boolean = false,
    val sessionFinishedEvent: Long? = null,
    /** One-shot event: rest timer hit zero for this endsAt millis (beep once). */
    val restBeepEvent: Long? = null,
    val repsInput: String = "",
    val weightInput: String = "",
    /** Session exercise id that [repsInput]/[weightInput] currently belong to. */
    val inputsExerciseId: Long? = null,
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
        get() {
            if (isWorkoutComplete && phase != WorkoutExecutionPhase.RESTING) return null
            if (currentExerciseIndex !in exercises.indices) return null
            return exercises[currentExerciseIndex]
        }

    val pendingSkippedExercises: List<WorkoutSessionExercise>
        get() {
            val currentId = currentExercise?.id
            return exercises.filter { exercise ->
                exercise.id != currentId &&
                    exercise.status == WorkoutSessionExerciseStatus.SKIPPED &&
                    (setsByExerciseId[exercise.id]?.size ?: 0) < exercise.plannedSets
            }
        }

    val sessionExerciseRows: List<SessionExerciseRow>
        get() {
            val currentId = currentExercise?.id
            return exercises.map { exercise ->
                val completedSets = setsByExerciseId[exercise.id]?.size ?: 0
                val isComplete = completedSets >= exercise.plannedSets
                SessionExerciseRow(
                    exercise = exercise,
                    completedSets = completedSets,
                    isCurrent = exercise.id == currentId,
                    isComplete = isComplete,
                )
            }
        }

    val hasIncompleteExercises: Boolean
        get() = exercises.any { exercise ->
            (setsByExerciseId[exercise.id]?.size ?: 0) < exercise.plannedSets
        }

    val currentExerciseSets: List<WorkoutSet>
        get() = currentExercise?.let { setsByExerciseId[it.id].orEmpty() }.orEmpty()
}

data class SessionExerciseRow(
    val exercise: WorkoutSessionExercise,
    val completedSets: Int,
    val isCurrent: Boolean,
    val isComplete: Boolean,
) {
    val canSelectNow: Boolean
        get() = !isComplete && !isCurrent

    val listStatus: SessionExerciseListStatus
        get() = when {
            isComplete -> SessionExerciseListStatus.COMPLETED
            exercise.status == WorkoutSessionExerciseStatus.SKIPPED ->
                SessionExerciseListStatus.SKIPPED
            isCurrent || exercise.status == WorkoutSessionExerciseStatus.IN_PROGRESS ->
                SessionExerciseListStatus.IN_PROGRESS
            else -> SessionExerciseListStatus.PENDING
        }
}

enum class SessionExerciseListStatus {
    PENDING,
    IN_PROGRESS,
    SKIPPED,
    COMPLETED,
}

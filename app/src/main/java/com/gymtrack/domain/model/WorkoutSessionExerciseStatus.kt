package com.gymtrack.domain.model

enum class WorkoutSessionExerciseStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED,
}

fun parseWorkoutSessionExerciseStatus(raw: String): WorkoutSessionExerciseStatus =
    runCatching { WorkoutSessionExerciseStatus.valueOf(raw) }
        .getOrDefault(WorkoutSessionExerciseStatus.PENDING)

fun isSessionExerciseComplete(
    exercise: WorkoutSessionExercise,
    completedSetCount: Int,
): Boolean = completedSetCount >= exercise.plannedSets

fun areAllSessionExercisesComplete(
    exercises: List<WorkoutSessionExercise>,
    setsByExerciseId: Map<Long, List<WorkoutSet>>,
): Boolean = exercises.all { exercise ->
    isSessionExerciseComplete(exercise, setsByExerciseId[exercise.id]?.size ?: 0)
}

fun resolveCurrentExerciseIndex(
    exercises: List<WorkoutSessionExercise>,
    setsByExerciseId: Map<Long, List<WorkoutSet>>,
): Int {
    fun completed(exercise: WorkoutSessionExercise): Boolean =
        isSessionExerciseComplete(exercise, setsByExerciseId[exercise.id]?.size ?: 0)

    val inProgress = exercises.indexOfFirst { exercise ->
        exercise.status == WorkoutSessionExerciseStatus.IN_PROGRESS && !completed(exercise)
    }
    if (inProgress >= 0) return inProgress

    return exercises.indexOfFirst { exercise ->
        !completed(exercise) && exercise.status != WorkoutSessionExerciseStatus.SKIPPED
    }
}

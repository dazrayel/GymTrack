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

/**
 * Picks the next exercise respecting block rounds:
 * within a bi/tri-set, finish the current round across all slots before starting the next round.
 */
fun resolveCurrentExerciseIndex(
    exercises: List<WorkoutSessionExercise>,
    setsByExerciseId: Map<Long, List<WorkoutSet>>,
): Int {
    fun completedCount(exercise: WorkoutSessionExercise): Int =
        setsByExerciseId[exercise.id]?.size ?: 0

    fun isComplete(exercise: WorkoutSessionExercise): Boolean =
        isSessionExerciseComplete(exercise, completedCount(exercise))

    val inProgress = exercises.indexOfFirst { exercise ->
        exercise.status == WorkoutSessionExerciseStatus.IN_PROGRESS && !isComplete(exercise)
    }
    if (inProgress >= 0) {
        // Still honour explicit IN_PROGRESS, but prefer same-round ordering inside its block.
        val current = exercises[inProgress]
        val block = exercises
            .filter {
                it.blockPosition == current.blockPosition &&
                    it.status != WorkoutSessionExerciseStatus.SKIPPED
            }
            .sortedBy { it.positionInBlock }
        val round = block.minOfOrNull { completedCount(it).coerceAtMost(it.plannedSets) } ?: 0
        val sameRound = block.firstOrNull { completedCount(it) == round && !isComplete(it) }
        if (sameRound != null) {
            return exercises.indexOfFirst { it.id == sameRound.id }
        }
        return inProgress
    }

    val blockPositions = exercises
        .filter { it.status != WorkoutSessionExerciseStatus.SKIPPED }
        .map { it.blockPosition }
        .distinct()
        .sorted()

    for (blockPosition in blockPositions) {
        val block = exercises
            .filter {
                it.blockPosition == blockPosition &&
                    it.status != WorkoutSessionExerciseStatus.SKIPPED
            }
            .sortedBy { it.positionInBlock }
        if (block.isEmpty()) continue
        if (block.all { isComplete(it) }) continue

        val currentRound = block.minOf { completedCount(it).coerceAtMost(it.plannedSets) }
        val next = block.firstOrNull { completedCount(it) == currentRound }
            ?: block.firstOrNull { !isComplete(it) }
            ?: continue
        return exercises.indexOfFirst { it.id == next.id }
    }
    return -1
}

/**
 * Rest starts only after every active (non-skipped) exercise in the block has finished the
 * round that was just completed. There is no rest between slots of the same bi-set/tri-set round.
 *
 * SINGLE keeps resting after every completed set (including the last), matching prior behaviour.
 * BI_SET / TRI_SET rest only between rounds — not after the final round of the block.
 */
fun shouldRestAfterCompletingSet(
    completedExercise: WorkoutSessionExercise,
    completedSetCountAfter: Int,
    exercises: List<WorkoutSessionExercise>,
    setsByExerciseId: Map<Long, List<WorkoutSet>>,
): Boolean {
    if (completedExercise.restSeconds <= 0) return false
    val block = exercises.filter {
        it.blockPosition == completedExercise.blockPosition &&
            it.status != WorkoutSessionExerciseStatus.SKIPPED
    }
    if (block.isEmpty()) return false
    val roundComplete = block.all { exercise ->
        val count = if (exercise.id == completedExercise.id) {
            completedSetCountAfter
        } else {
            setsByExerciseId[exercise.id]?.size ?: 0
        }
        count >= completedSetCountAfter
    }
    if (!roundComplete) return false
    if (completedExercise.blockType == WorkoutBlockType.SINGLE) return true
    // Last round of a multi-exercise block: advance straight to the next block.
    return completedSetCountAfter < completedExercise.plannedSets
}

fun blockExercises(
    exercises: List<WorkoutSessionExercise>,
    blockPosition: Int,
): List<WorkoutSessionExercise> =
    exercises
        .filter { it.blockPosition == blockPosition }
        .sortedBy { it.positionInBlock }

fun currentRoundIndex(
    exercise: WorkoutSessionExercise,
    setsByExerciseId: Map<Long, List<WorkoutSet>>,
): Int = (setsByExerciseId[exercise.id]?.size ?: 0).coerceAtMost(exercise.plannedSets)

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
 *
 * An [WorkoutSessionExerciseStatus.IN_PROGRESS] mark (e.g. from Exercícios) allows intentional
 * out-of-order work on another block, but within the selected block round order is always
 * honoured — selecting partner B must not skip A's owed set in the same BI/TRI round.
 */
fun resolveCurrentExerciseIndex(
    exercises: List<WorkoutSessionExercise>,
    setsByExerciseId: Map<Long, List<WorkoutSet>>,
): Int {
    fun completedCount(exercise: WorkoutSessionExercise): Int =
        setsByExerciseId[exercise.id]?.size ?: 0

    fun isComplete(exercise: WorkoutSessionExercise): Boolean =
        isSessionExerciseComplete(exercise, completedCount(exercise))

    fun nextInBlock(block: List<WorkoutSessionExercise>): WorkoutSessionExercise? {
        if (block.isEmpty() || block.all { isComplete(it) }) return null
        val currentRound = block.minOf { completedCount(it).coerceAtMost(it.plannedSets) }
        return block.firstOrNull { completedCount(it) == currentRound && !isComplete(it) }
            ?: block.firstOrNull { !isComplete(it) }
    }

    val inProgress = exercises.indexOfFirst { exercise ->
        exercise.status == WorkoutSessionExerciseStatus.IN_PROGRESS && !isComplete(exercise)
    }
    if (inProgress >= 0) {
        val current = exercises[inProgress]
        val block = exercises
            .filter {
                it.blockPosition == current.blockPosition &&
                    it.status != WorkoutSessionExerciseStatus.SKIPPED
            }
            .sortedBy { it.positionInBlock }
        // Honour round order inside the selected block — selecting BI/TRI partner B must not
        // skip A's owed set in the same round. Out-of-order selection of another block is OK.
        val sameRound = nextInBlock(block)
        if (sameRound != null) {
            return exercises.indexOfFirst { it.id == sameRound.id }
        }
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
        val next = nextInBlock(block) ?: continue
        return exercises.indexOfFirst { it.id == next.id }
    }
    return -1
}

/**
 * Rest starts only after every active (non-skipped) exercise in the block has finished the
 * round that was just completed. There is no rest between slots of the same bi-set/tri-set round.
 *
 * Rest runs when there is still executable work after this completion (more sets/rounds in the
 * block, or another incomplete block). No rest after the last set of the last remaining exercise.
 *
 * Completing the last set of exercise A must not start rest while exercise B in the same block
 * still owes that same round — regardless of which exercise is marked IN_PROGRESS in the UI.
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
    val roundComplete = isBlockRoundComplete(
        block = block,
        completedExerciseId = completedExercise.id,
        completedSetCountAfter = completedSetCountAfter,
        setsByExerciseId = setsByExerciseId,
    )
    if (!roundComplete) return false
    return hasExecutableWorkRemaining(
        completedExercise = completedExercise,
        completedSetCountAfter = completedSetCountAfter,
        exercises = exercises,
        setsByExerciseId = setsByExerciseId,
    )
}

/**
 * True when every active exercise in [block] has at least [completedSetCountAfter] sets
 * (projecting the set that was just completed onto [completedExerciseId]).
 */
fun isBlockRoundComplete(
    block: List<WorkoutSessionExercise>,
    completedExerciseId: Long,
    completedSetCountAfter: Int,
    setsByExerciseId: Map<Long, List<WorkoutSet>>,
): Boolean {
    if (block.isEmpty() || completedSetCountAfter <= 0) return false
    return block.all { exercise ->
        val count = if (exercise.id == completedExerciseId) {
            completedSetCountAfter
        } else {
            setsByExerciseId[exercise.id]?.size ?: 0
        }
        count >= completedSetCountAfter
    }
}

/**
 * True when, after projecting [completedSetCountAfter] on [completedExercise], any non-skipped
 * exercise still needs sets.
 */
fun hasExecutableWorkRemaining(
    completedExercise: WorkoutSessionExercise,
    completedSetCountAfter: Int,
    exercises: List<WorkoutSessionExercise>,
    setsByExerciseId: Map<Long, List<WorkoutSet>>,
): Boolean {
    val active = exercises.filter { it.status != WorkoutSessionExerciseStatus.SKIPPED }
    return active.any { exercise ->
        val count = if (exercise.id == completedExercise.id) {
            completedSetCountAfter
        } else {
            setsByExerciseId[exercise.id]?.size ?: 0
        }
        count < exercise.plannedSets
    }
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

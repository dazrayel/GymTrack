package com.gymtrack.domain.model

import kotlin.math.abs

/** Default load bump (kg) when suggesting [ProgressionAction.INCREASE_WEIGHT]. */
const val WEIGHT_PROGRESSION_STEP_KG = 2.5

enum class ProgressionAction {
    NO_HISTORY,
    INCREASE_REPS,
    INCREASE_WEIGHT,
    MAINTAIN,
}

/**
 * One performed set from the last completed session for a single exercise.
 * Callers must not mix sessions or exercises in the same list.
 */
data class ProgressionPerformedSet(
    val setIndex: Int,
    val reps: Int,
    val weight: Double,
)

/**
 * Pure double-progression recommendation from planned targets vs last session sets.
 * Does not persist or mutate workout templates.
 */
data class ProgressionSuggestion(
    val action: ProgressionAction,
    val plannedWeight: Double,
    val suggestedWeight: Double?,
    val minRepetitions: Int,
    val maxRepetitions: Int,
    val plannedSets: Int,
    val weightStepKg: Double = WEIGHT_PROGRESSION_STEP_KG,
    val lastSessionSets: List<ProgressionPerformedSet> = emptyList(),
)

/**
 * Evaluates classic double progression for one exercise.
 *
 * - Empty [lastSessionSets] → [ProgressionAction.NO_HISTORY]
 * - All planned sets at planned load with reps ≥ max → increase weight (if load > 0) or reps (if load == 0)
 * - All planned sets at planned load with reps ≥ min but not all at max → increase reps
 * - Otherwise → maintain (incomplete, below min, mismatched load, invalid input)
 */
fun evaluateLoadProgression(
    plannedWeight: Double,
    minRepetitions: Int,
    maxRepetitions: Int,
    plannedSets: Int,
    lastSessionSets: List<ProgressionPerformedSet>,
): ProgressionSuggestion {
    val sortedSets = lastSessionSets.sortedWith(
        compareBy<ProgressionPerformedSet> { it.setIndex }.thenBy { it.reps },
    )
    fun result(
        action: ProgressionAction,
        suggestedWeight: Double? = null,
    ) = ProgressionSuggestion(
        action = action,
        plannedWeight = plannedWeight,
        suggestedWeight = suggestedWeight,
        minRepetitions = minRepetitions,
        maxRepetitions = maxRepetitions,
        plannedSets = plannedSets,
        weightStepKg = WEIGHT_PROGRESSION_STEP_KG,
        lastSessionSets = sortedSets,
    )

    if (sortedSets.isEmpty()) {
        return result(ProgressionAction.NO_HISTORY)
    }
    if (plannedSets <= 0 || minRepetitions <= 0 || maxRepetitions < minRepetitions || plannedWeight < 0.0) {
        return result(ProgressionAction.MAINTAIN)
    }
    if (sortedSets.size < plannedSets) {
        return result(ProgressionAction.MAINTAIN)
    }

    val evaluated = sortedSets.take(plannedSets)
    if (evaluated.any { !weightsMatch(it.weight, plannedWeight) }) {
        return result(ProgressionAction.MAINTAIN)
    }
    if (evaluated.any { it.reps < minRepetitions }) {
        return result(ProgressionAction.MAINTAIN)
    }

    val allAtOrAboveMax = evaluated.all { it.reps >= maxRepetitions }
    return if (allAtOrAboveMax && plannedWeight > 0.0) {
        result(
            action = ProgressionAction.INCREASE_WEIGHT,
            suggestedWeight = plannedWeight + WEIGHT_PROGRESSION_STEP_KG,
        )
    } else {
        result(ProgressionAction.INCREASE_REPS)
    }
}

/**
 * Picks sets from the latest COMPLETED session that includes [exerciseName],
 * excluding [excludeSessionId] (typically the in-progress session).
 * Does not mix sessions. Indices within the session are assigned in list order
 * (history rows for one session share the same [CompletedSetRecord.occurredAtMillis]).
 */
fun lastCompletedSessionSetsForExercise(
    history: List<CompletedSetRecord>,
    exerciseName: String,
    excludeSessionId: Long? = null,
): List<ProgressionPerformedSet> {
    val relevant = history.filter { record ->
        record.exerciseName == exerciseName &&
            (excludeSessionId == null || record.sessionId != excludeSessionId)
    }
    if (relevant.isEmpty()) return emptyList()

    val lastSessionId = relevant
        .maxWith(
            compareBy<CompletedSetRecord> { it.occurredAtMillis }.thenBy { it.sessionId },
        )
        .sessionId

    return relevant
        .filter { it.sessionId == lastSessionId }
        .mapIndexed { index, record ->
            ProgressionPerformedSet(
                setIndex = index,
                reps = record.reps,
                weight = record.weight,
            )
        }
}

/** Rep sequence for UI, e.g. `12/12/12`, ordered by [ProgressionPerformedSet.setIndex]. */
fun formatProgressionRepsSequence(sets: List<ProgressionPerformedSet>): String =
    sets.sortedBy { it.setIndex }.joinToString("/") { it.reps.toString() }

private fun weightsMatch(executed: Double, planned: Double): Boolean =
    abs(executed - planned) < 0.001

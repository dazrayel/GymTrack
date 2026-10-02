package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadProgressionTest {

    @Test
    fun noHistory_returnsNoHistoryWithoutSuggestedWeight() {
        val suggestion = evaluate(
            plannedWeight = 80.0,
            min = 8,
            max = 12,
            plannedSets = 3,
            sets = emptyList(),
        )

        assertEquals(ProgressionAction.NO_HISTORY, suggestion.action)
        assertNull(suggestion.suggestedWeight)
        assertEquals(80.0, suggestion.plannedWeight, 0.001)
        assertEquals(8, suggestion.minRepetitions)
        assertEquals(12, suggestion.maxRepetitions)
        assertEquals(3, suggestion.plannedSets)
        assertTrue(suggestion.lastSessionSets.isEmpty())
    }

    @Test
    fun allSetsAtMaxOnPlannedLoad_increasesWeightByStep() {
        val suggestion = evaluate(
            plannedWeight = 80.0,
            min = 8,
            max = 12,
            plannedSets = 3,
            sets = listOf(
                set(0, reps = 12, weight = 80.0),
                set(1, reps = 12, weight = 80.0),
                set(2, reps = 12, weight = 80.0),
            ),
        )

        assertEquals(ProgressionAction.INCREASE_WEIGHT, suggestion.action)
        assertEquals(82.5, suggestion.suggestedWeight!!, 0.001)
        assertEquals(WEIGHT_PROGRESSION_STEP_KG, suggestion.weightStepKg, 0.001)
        assertEquals(3, suggestion.lastSessionSets.size)
    }

    @Test
    fun midRangeRepsOnPlannedLoad_increasesReps() {
        val suggestion = evaluate(
            plannedWeight = 80.0,
            min = 8,
            max = 12,
            plannedSets = 3,
            sets = listOf(
                set(0, reps = 8, weight = 80.0),
                set(1, reps = 9, weight = 80.0),
                set(2, reps = 9, weight = 80.0),
            ),
        )

        assertEquals(ProgressionAction.INCREASE_REPS, suggestion.action)
        assertNull(suggestion.suggestedWeight)
        assertEquals(80.0, suggestion.plannedWeight, 0.001)
    }

    @Test
    fun setBelowMinimum_maintains() {
        val suggestion = evaluate(
            plannedWeight = 80.0,
            min = 8,
            max = 12,
            plannedSets = 3,
            sets = listOf(
                set(0, reps = 8, weight = 80.0),
                set(1, reps = 8, weight = 80.0),
                set(2, reps = 7, weight = 80.0),
            ),
        )

        assertEquals(ProgressionAction.MAINTAIN, suggestion.action)
        assertNull(suggestion.suggestedWeight)
    }

    @Test
    fun fewerSetsThanPlanned_maintains() {
        val suggestion = evaluate(
            plannedWeight = 80.0,
            min = 8,
            max = 12,
            plannedSets = 3,
            sets = listOf(
                set(0, reps = 12, weight = 80.0),
                set(1, reps = 12, weight = 80.0),
            ),
        )

        assertEquals(ProgressionAction.MAINTAIN, suggestion.action)
        assertNull(suggestion.suggestedWeight)
    }

    @Test
    fun zeroPlannedWeight_atMaxReps_increasesRepsNotWeight() {
        val suggestion = evaluate(
            plannedWeight = 0.0,
            min = 10,
            max = 15,
            plannedSets = 3,
            sets = listOf(
                set(0, reps = 15, weight = 0.0),
                set(1, reps = 15, weight = 0.0),
                set(2, reps = 15, weight = 0.0),
            ),
        )

        assertEquals(ProgressionAction.INCREASE_REPS, suggestion.action)
        assertNull(suggestion.suggestedWeight)
        assertEquals(0.0, suggestion.plannedWeight, 0.001)
    }

    @Test
    fun zeroPlannedWeight_midRange_increasesReps() {
        val suggestion = evaluate(
            plannedWeight = 0.0,
            min = 10,
            max = 15,
            plannedSets = 3,
            sets = listOf(
                set(0, reps = 10, weight = 0.0),
                set(1, reps = 12, weight = 0.0),
                set(2, reps = 11, weight = 0.0),
            ),
        )

        assertEquals(ProgressionAction.INCREASE_REPS, suggestion.action)
        assertNull(suggestion.suggestedWeight)
    }

    @Test
    fun minEqualsMax_allHitTarget_increasesWeight() {
        val suggestion = evaluate(
            plannedWeight = 50.0,
            min = 10,
            max = 10,
            plannedSets = 3,
            sets = listOf(
                set(0, reps = 10, weight = 50.0),
                set(1, reps = 10, weight = 50.0),
                set(2, reps = 10, weight = 50.0),
            ),
        )

        assertEquals(ProgressionAction.INCREASE_WEIGHT, suggestion.action)
        assertEquals(52.5, suggestion.suggestedWeight!!, 0.001)
    }

    @Test
    fun executedWeightDifferentFromPlanned_maintainsEvenAtMaxReps() {
        val suggestion = evaluate(
            plannedWeight = 80.0,
            min = 8,
            max = 12,
            plannedSets = 3,
            sets = listOf(
                set(0, reps = 12, weight = 75.0),
                set(1, reps = 12, weight = 75.0),
                set(2, reps = 12, weight = 75.0),
            ),
        )

        assertEquals(ProgressionAction.MAINTAIN, suggestion.action)
        assertNull(suggestion.suggestedWeight)
    }

    @Test
    fun plannedSetsOne_atMax_increasesWeight() {
        val suggestion = evaluate(
            plannedWeight = 60.0,
            min = 8,
            max = 12,
            plannedSets = 1,
            sets = listOf(set(0, reps = 12, weight = 60.0)),
        )

        assertEquals(ProgressionAction.INCREASE_WEIGHT, suggestion.action)
        assertEquals(62.5, suggestion.suggestedWeight!!, 0.001)
    }

    @Test
    fun plannedSetsOne_belowMin_maintains() {
        val suggestion = evaluate(
            plannedWeight = 60.0,
            min = 8,
            max = 12,
            plannedSets = 1,
            sets = listOf(set(0, reps = 6, weight = 60.0)),
        )

        assertEquals(ProgressionAction.MAINTAIN, suggestion.action)
    }

    @Test
    fun setsFromDifferentSessions_areNotMixedByCallerContract() {
        // Caller must pass only one session. Evaluating session A alone yields INCREASE_WEIGHT;
        // mixing A+B would still only take first plannedSets by setIndex — callers must filter.
        val sessionA = listOf(
            set(0, reps = 12, weight = 80.0),
            set(1, reps = 12, weight = 80.0),
            set(2, reps = 12, weight = 80.0),
        )
        val sessionB = listOf(
            set(0, reps = 7, weight = 80.0),
            set(1, reps = 7, weight = 80.0),
            set(2, reps = 7, weight = 80.0),
        )

        assertEquals(
            ProgressionAction.INCREASE_WEIGHT,
            evaluate(80.0, 8, 12, 3, sessionA).action,
        )
        assertEquals(
            ProgressionAction.MAINTAIN,
            evaluate(80.0, 8, 12, 3, sessionB).action,
        )
    }

    @Test
    fun setIndexOutOfOrder_stillEvaluatesCorrectly() {
        val suggestion = evaluate(
            plannedWeight = 80.0,
            min = 8,
            max = 12,
            plannedSets = 3,
            sets = listOf(
                set(2, reps = 12, weight = 80.0),
                set(0, reps = 12, weight = 80.0),
                set(1, reps = 12, weight = 80.0),
            ),
        )

        assertEquals(ProgressionAction.INCREASE_WEIGHT, suggestion.action)
        assertEquals(listOf(0, 1, 2), suggestion.lastSessionSets.map { it.setIndex })
    }

    @Test
    fun biSet_evaluatedPerExerciseIndependently() {
        val exerciseA = evaluate(
            plannedWeight = 80.0,
            min = 8,
            max = 12,
            plannedSets = 3,
            sets = listOf(
                set(0, reps = 12, weight = 80.0),
                set(1, reps = 12, weight = 80.0),
                set(2, reps = 12, weight = 80.0),
            ),
        )
        val exerciseB = evaluate(
            plannedWeight = 40.0,
            min = 8,
            max = 12,
            plannedSets = 3,
            sets = listOf(
                set(0, reps = 8, weight = 40.0),
                set(1, reps = 9, weight = 40.0),
                set(2, reps = 9, weight = 40.0),
            ),
        )

        assertEquals(ProgressionAction.INCREASE_WEIGHT, exerciseA.action)
        assertEquals(82.5, exerciseA.suggestedWeight!!, 0.001)
        assertEquals(ProgressionAction.INCREASE_REPS, exerciseB.action)
        assertNull(exerciseB.suggestedWeight)
    }

    @Test
    fun triSet_evaluatedPerExerciseIndependently() {
        val curl = evaluate(
            plannedWeight = 20.0,
            min = 10,
            max = 12,
            plannedSets = 3,
            sets = listOf(
                set(0, reps = 12, weight = 20.0),
                set(1, reps = 12, weight = 20.0),
                set(2, reps = 12, weight = 20.0),
            ),
        )
        val kickback = evaluate(
            plannedWeight = 12.5,
            min = 10,
            max = 15,
            plannedSets = 3,
            sets = listOf(
                set(0, reps = 10, weight = 12.5),
                set(1, reps = 11, weight = 12.5),
                set(2, reps = 10, weight = 12.5),
            ),
        )
        val press = evaluate(
            plannedWeight = 30.0,
            min = 8,
            max = 10,
            plannedSets = 3,
            sets = listOf(
                set(0, reps = 8, weight = 30.0),
                set(1, reps = 7, weight = 30.0),
                set(2, reps = 8, weight = 30.0),
            ),
        )

        assertEquals(ProgressionAction.INCREASE_WEIGHT, curl.action)
        assertEquals(22.5, curl.suggestedWeight!!, 0.001)
        assertEquals(ProgressionAction.INCREASE_REPS, kickback.action)
        assertEquals(ProgressionAction.MAINTAIN, press.action)
    }

    @Test
    fun positivePlannedWeight_usesTwoPointFiveStep() {
        val suggestion = evaluate(
            plannedWeight = 100.0,
            min = 5,
            max = 5,
            plannedSets = 2,
            sets = listOf(
                set(0, reps = 5, weight = 100.0),
                set(1, reps = 5, weight = 100.0),
            ),
        )

        assertEquals(ProgressionAction.INCREASE_WEIGHT, suggestion.action)
        assertEquals(2.5, suggestion.weightStepKg, 0.001)
        assertEquals(102.5, suggestion.suggestedWeight!!, 0.001)
    }

    @Test
    fun repsAboveMax_stillAllowIncreaseWeight() {
        val suggestion = evaluate(
            plannedWeight = 80.0,
            min = 8,
            max = 12,
            plannedSets = 3,
            sets = listOf(
                set(0, reps = 15, weight = 80.0),
                set(1, reps = 14, weight = 80.0),
                set(2, reps = 13, weight = 80.0),
            ),
        )

        assertEquals(ProgressionAction.INCREASE_WEIGHT, suggestion.action)
        assertEquals(82.5, suggestion.suggestedWeight!!, 0.001)
    }

    @Test
    fun insufficientData_invalidPlannedSets_maintains() {
        val suggestion = evaluate(
            plannedWeight = 80.0,
            min = 8,
            max = 12,
            plannedSets = 0,
            sets = listOf(set(0, reps = 12, weight = 80.0)),
        )

        assertEquals(ProgressionAction.MAINTAIN, suggestion.action)
        assertNull(suggestion.suggestedWeight)
    }

    @Test
    fun insufficientData_invalidRepRange_maintains() {
        val suggestion = evaluate(
            plannedWeight = 80.0,
            min = 12,
            max = 8,
            plannedSets = 3,
            sets = listOf(
                set(0, reps = 12, weight = 80.0),
                set(1, reps = 12, weight = 80.0),
                set(2, reps = 12, weight = 80.0),
            ),
        )

        assertEquals(ProgressionAction.MAINTAIN, suggestion.action)
    }

    @Test
    fun extraSetsBeyondPlanned_onlyFirstPlannedCountMatter() {
        val suggestion = evaluate(
            plannedWeight = 80.0,
            min = 8,
            max = 12,
            plannedSets = 3,
            sets = listOf(
                set(0, reps = 12, weight = 80.0),
                set(1, reps = 12, weight = 80.0),
                set(2, reps = 12, weight = 80.0),
                set(3, reps = 5, weight = 80.0),
            ),
        )

        assertEquals(ProgressionAction.INCREASE_WEIGHT, suggestion.action)
        assertEquals(82.5, suggestion.suggestedWeight!!, 0.001)
    }

    @Test
    fun oneMismatchedLoadAmongSets_maintains() {
        val suggestion = evaluate(
            plannedWeight = 80.0,
            min = 8,
            max = 12,
            plannedSets = 3,
            sets = listOf(
                set(0, reps = 12, weight = 80.0),
                set(1, reps = 12, weight = 82.5),
                set(2, reps = 12, weight = 80.0),
            ),
        )

        assertEquals(ProgressionAction.MAINTAIN, suggestion.action)
    }

    @Test
    fun formatProgressionRepsSequence_joinsOrderedReps() {
        val text = formatProgressionRepsSequence(
            listOf(
                ProgressionPerformedSet(2, 12, 80.0),
                ProgressionPerformedSet(0, 8, 80.0),
                ProgressionPerformedSet(1, 9, 80.0),
            ),
        )
        assertEquals("8/9/12", text)
    }

    private fun evaluate(
        plannedWeight: Double,
        min: Int,
        max: Int,
        plannedSets: Int,
        sets: List<ProgressionPerformedSet>,
    ): ProgressionSuggestion = evaluateLoadProgression(
        plannedWeight = plannedWeight,
        minRepetitions = min,
        maxRepetitions = max,
        plannedSets = plannedSets,
        lastSessionSets = sets,
    )

    private fun set(setIndex: Int, reps: Int, weight: Double) = ProgressionPerformedSet(
        setIndex = setIndex,
        reps = reps,
        weight = weight,
    )
}

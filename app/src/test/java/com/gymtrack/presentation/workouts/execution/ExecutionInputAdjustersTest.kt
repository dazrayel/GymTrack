package com.gymtrack.presentation.workouts.execution

import org.junit.Assert.assertEquals
import org.junit.Test

class ExecutionInputAdjustersTest {

    @Test
    fun weight_incrementsByTwoPointFive() {
        assertEquals("42.5", adjustWeightInput("40", WEIGHT_ADJUST_STEP_KG))
        assertEquals("45", adjustWeightInput("42.5", WEIGHT_ADJUST_STEP_KG))
    }

    @Test
    fun weight_decrementsByTwoPointFive() {
        assertEquals("42.5", adjustWeightInput("45", -WEIGHT_ADJUST_STEP_KG))
        assertEquals("40", adjustWeightInput("42.5", -WEIGHT_ADJUST_STEP_KG))
    }

    @Test
    fun weight_doesNotGoBelowZero() {
        assertEquals("0", adjustWeightInput("2", -WEIGHT_ADJUST_STEP_KG))
        assertEquals("0", adjustWeightInput("0", -WEIGHT_ADJUST_STEP_KG))
    }

    @Test
    fun weight_emptyOrInvalidStartsFromZero() {
        assertEquals("2.5", adjustWeightInput("", WEIGHT_ADJUST_STEP_KG))
        assertEquals("2.5", adjustWeightInput("abc", WEIGHT_ADJUST_STEP_KG))
    }

    @Test
    fun weight_avoidsFloatingPointDriftAcrossSteps() {
        var value = "40"
        repeat(4) { value = adjustWeightInput(value, WEIGHT_ADJUST_STEP_KG) }
        assertEquals("50", value)
        repeat(4) { value = adjustWeightInput(value, -WEIGHT_ADJUST_STEP_KG) }
        assertEquals("40", value)
    }

    @Test
    fun reps_incrementsByOne() {
        assertEquals("11", adjustRepsInput("10", 1))
    }

    @Test
    fun reps_decrementsByOne() {
        assertEquals("10", adjustRepsInput("11", -1))
    }

    @Test
    fun reps_doesNotGoBelowOne() {
        assertEquals("1", adjustRepsInput("1", -1))
        assertEquals("1", adjustRepsInput("0", -1))
    }

    @Test
    fun reps_emptyOrInvalidStartsFromZeroThenClamps() {
        assertEquals("1", adjustRepsInput("", 1))
        assertEquals("1", adjustRepsInput("x", 1))
    }
}

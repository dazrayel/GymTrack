package com.gymtrack.presentation.workouts.execution

import org.junit.Assert.assertEquals
import org.junit.Test

class RestRemainingFractionTest {

    @Test
    fun fullDuration_isCompletelyFilled() {
        assertEquals(1f, restRemainingFraction(90_000L, 90_000L))
    }

    @Test
    fun halfRemaining_isHalfFilled() {
        assertEquals(0.5f, restRemainingFraction(45_000L, 90_000L))
    }

    @Test
    fun zeroRemaining_isEmpty() {
        assertEquals(0f, restRemainingFraction(0L, 90_000L))
    }

    @Test
    fun invalidDuration_isEmpty() {
        assertEquals(0f, restRemainingFraction(30_000L, 0L))
    }
}

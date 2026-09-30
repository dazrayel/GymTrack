package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class WorkoutBlockTypeTest {

    // ─── requiredExerciseCount ────────────────────────────────────────────────

    @Test
    fun single_requiresExactlyOneExercise() {
        assertEquals(1, WorkoutBlockType.SINGLE.requiredExerciseCount)
    }

    @Test
    fun biSet_requiresExactlyTwoExercises() {
        assertEquals(2, WorkoutBlockType.BI_SET.requiredExerciseCount)
    }

    @Test
    fun triSet_requiresExactlyThreeExercises() {
        assertEquals(3, WorkoutBlockType.TRI_SET.requiredExerciseCount)
    }

    // ─── validateExerciseCount — success paths ────────────────────────────────

    @Test
    fun single_validateWith1_doesNotThrow() {
        WorkoutBlockType.SINGLE.validateExerciseCount(1)
    }

    @Test
    fun biSet_validateWith2_doesNotThrow() {
        WorkoutBlockType.BI_SET.validateExerciseCount(2)
    }

    @Test
    fun triSet_validateWith3_doesNotThrow() {
        WorkoutBlockType.TRI_SET.validateExerciseCount(3)
    }

    // ─── validateExerciseCount — failure paths ────────────────────────────────

    @Test
    fun single_validateWith0_throws() {
        try {
            WorkoutBlockType.SINGLE.validateExerciseCount(0)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun single_validateWith2_throws() {
        try {
            WorkoutBlockType.SINGLE.validateExerciseCount(2)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun biSet_validateWith1_throws() {
        try {
            WorkoutBlockType.BI_SET.validateExerciseCount(1)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun biSet_validateWith3_throws() {
        try {
            WorkoutBlockType.BI_SET.validateExerciseCount(3)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun triSet_validateWith2_throws() {
        try {
            WorkoutBlockType.TRI_SET.validateExerciseCount(2)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun triSet_validateWith4_throws() {
        try {
            WorkoutBlockType.TRI_SET.validateExerciseCount(4)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }
}

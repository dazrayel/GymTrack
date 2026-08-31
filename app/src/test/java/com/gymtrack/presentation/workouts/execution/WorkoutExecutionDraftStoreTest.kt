package com.gymtrack.presentation.workouts.execution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WorkoutExecutionDraftStoreTest {

    private lateinit var store: WorkoutExecutionDraftStore

    @Before
    fun setUp() {
        store = WorkoutExecutionDraftStore()
    }

    @Test
    fun put_thenGet_returnsSameDraft() {
        store.put(sessionId = 1L, exerciseId = 10L, repsInput = "12", weightInput = "80")

        assertEquals(
            ExerciseInputDraft(repsInput = "12", weightInput = "80"),
            store.get(sessionId = 1L, exerciseId = 10L),
        )
    }

    @Test
    fun draftsForDifferentExercises_doNotMix() {
        store.put(sessionId = 1L, exerciseId = 10L, repsInput = "8", weightInput = "60")
        store.put(sessionId = 1L, exerciseId = 20L, repsInput = "10", weightInput = "14.5")

        assertEquals("8", store.get(1L, 10L)?.repsInput)
        assertEquals("60", store.get(1L, 10L)?.weightInput)
        assertEquals("10", store.get(1L, 20L)?.repsInput)
        assertEquals("14.5", store.get(1L, 20L)?.weightInput)
    }

    @Test
    fun draftsForDifferentSessions_doNotMix() {
        store.put(sessionId = 1L, exerciseId = 10L, repsInput = "8", weightInput = "40")
        store.put(sessionId = 2L, exerciseId = 10L, repsInput = "12", weightInput = "90")

        assertEquals(ExerciseInputDraft("8", "40"), store.get(1L, 10L))
        assertEquals(ExerciseInputDraft("12", "90"), store.get(2L, 10L))
    }

    @Test
    fun clear_removesOnlyThatExerciseDraft() {
        store.put(sessionId = 1L, exerciseId = 10L, repsInput = "8", weightInput = "60")
        store.put(sessionId = 1L, exerciseId = 20L, repsInput = "10", weightInput = "14")

        store.clear(sessionId = 1L, exerciseId = 10L)

        assertNull(store.get(1L, 10L))
        assertEquals(ExerciseInputDraft("10", "14"), store.get(1L, 20L))
    }

    @Test
    fun clearSession_removesAllDraftsForThatSessionOnly() {
        store.put(sessionId = 1L, exerciseId = 10L, repsInput = "8", weightInput = "60")
        store.put(sessionId = 1L, exerciseId = 20L, repsInput = "10", weightInput = "14")
        store.put(sessionId = 2L, exerciseId = 10L, repsInput = "12", weightInput = "90")

        store.clearSession(1L)

        assertNull(store.get(1L, 10L))
        assertNull(store.get(1L, 20L))
        assertEquals(ExerciseInputDraft("12", "90"), store.get(2L, 10L))
    }
}

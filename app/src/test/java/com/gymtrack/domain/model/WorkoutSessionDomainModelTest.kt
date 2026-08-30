package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutSessionDomainModelTest {

    @Test
    fun session_defaultId_isZero() {
        val session = WorkoutSession(
            workoutName = "Push Day",
            startedAtMillis = 1L,
            status = WorkoutSessionStatus.IN_PROGRESS,
        )
        assertEquals(0L, session.id)
        assertNull(session.endedAtMillis)
        assertEquals("", session.workoutDescription)
        assertNull(session.restEndsAtMillis)
        assertNull(session.restPausedRemainingMillis)
        assertNull(session.restSessionExerciseId)
        assertNull(session.restAfterSetIndex)
    }

    @Test
    fun session_fieldsAreStoredCorrectly() {
        val session = WorkoutSession(
            id = 9,
            workoutId = 3,
            workoutName = "Push Day",
            workoutDescription = "Chest",
            startedAtMillis = 100,
            endedAtMillis = 200,
            status = WorkoutSessionStatus.COMPLETED,
        )
        assertEquals(9L, session.id)
        assertEquals(3L, session.workoutId)
        assertEquals(WorkoutSessionStatus.COMPLETED, session.status)
        assertEquals(200L, session.endedAtMillis)
    }

    @Test
    fun sessionExercise_snapshotFieldsAreStored() {
        val item = WorkoutSessionExercise(
            id = 1,
            sessionId = 2,
            exerciseId = 3,
            position = 1,
            exerciseName = "Squat",
            muscleGroup = "Legs",
            equipmentType = "Barbell",
            plannedSets = 4,
            minRepetitions = 6,
            maxRepetitions = 10,
            plannedWeight = 80.0,
            restSeconds = 120,
            notes = "pause",
        )
        assertEquals("Squat", item.exerciseName)
        assertEquals(4, item.plannedSets)
        assertEquals(80.0, item.plannedWeight, 0.001)
        assertEquals("pause", item.notes)
    }

    @Test
    fun workoutSet_fieldsAreStored() {
        val set = WorkoutSet(
            id = 1,
            sessionExerciseId = 2,
            setIndex = 0,
            reps = 10,
            weight = 60.5,
            completedAtMillis = 9L,
        )
        assertEquals(0, set.setIndex)
        assertEquals(10, set.reps)
        assertEquals(60.5, set.weight, 0.001)
    }

    @Test
    fun status_enumNamesMatchStorageConstants() {
        assertEquals("IN_PROGRESS", WorkoutSessionStatus.IN_PROGRESS.name)
        assertEquals("COMPLETED", WorkoutSessionStatus.COMPLETED.name)
        assertEquals(
            WorkoutSessionStatus.IN_PROGRESS,
            WorkoutSessionStatus.valueOf("IN_PROGRESS"),
        )
    }
}

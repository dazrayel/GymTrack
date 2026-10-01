package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutHistoryItemTest {

    @Test
    fun fieldsAreStoredCorrectly() {
        val item = WorkoutHistoryItem(
            sessionId = 12L,
            workoutId = 4L,
            workoutName = "Push Day",
            startedAtMillis = 1_000L,
            endedAtMillis = 61_000L,
            durationMillis = elapsedMillis(
                WorkoutSession(
                    id = 12L,
                    workoutId = 4L,
                    workoutName = "Push Day",
                    startedAtMillis = 1_000L,
                    endedAtMillis = 61_000L,
                    status = WorkoutSessionStatus.COMPLETED,
                ),
                nowMillis = 61_000L,
            ),
            volume = 480.0,
            exerciseCount = 2,
            completedSetCount = 1,
            plannedSetCount = 7,
        )
        assertEquals(12L, item.sessionId)
        assertEquals(4L, item.workoutId)
        assertEquals("Push Day", item.workoutName)
        assertEquals(60_000L, item.durationMillis)
        assertEquals(480.0, item.volume, 0.001)
        assertEquals(2, item.exerciseCount)
        assertEquals(1, item.completedSetCount)
        assertEquals(7, item.plannedSetCount)
    }

    @Test
    fun workoutId_defaultsToNull() {
        val item = WorkoutHistoryItem(
            sessionId = 1L,
            workoutName = "Orphan",
            startedAtMillis = 1L,
            endedAtMillis = 2L,
            durationMillis = 1L,
            volume = 0.0,
            exerciseCount = 0,
            completedSetCount = 0,
            plannedSetCount = 0,
        )
        assertEquals(null, item.workoutId)
    }
}

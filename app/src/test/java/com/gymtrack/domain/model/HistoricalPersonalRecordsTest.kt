package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoricalPersonalRecordsTest {

    @Test
    fun emptyRecords_returnsEmptyList() {
        assertTrue(historicalPersonalRecords(emptyList()).isEmpty())
    }

    @Test
    fun singleExercise_returnsOneRecord() {
        val records = historicalPersonalRecords(
            listOf(rec(sessionId = 1L, name = "Supino", reps = 8, weight = 80.0)),
        )
        assertEquals(1, records.size)
        assertEquals("Supino", records.single().exerciseName)
        assertEquals(80.0, records.single().bestWeight, 0.001)
        assertEquals(8, records.single().bestReps)
        assertEquals(640.0, records.single().bestVolume, 0.001)
    }

    @Test
    fun twoExercises_areNotMixed() {
        val records = historicalPersonalRecords(
            listOf(
                rec(1L, "Supino", 8, 80.0),
                rec(1L, "Agachamento", 5, 120.0),
            ),
        )
        assertEquals(2, records.size)
        assertEquals(80.0, records.single { it.exerciseName == "Supino" }.bestWeight, 0.001)
        assertEquals(120.0, records.single { it.exerciseName == "Agachamento" }.bestWeight, 0.001)
    }

    @Test
    fun sortsByExerciseNameAscending() {
        val records = historicalPersonalRecords(
            listOf(
                rec(1L, "Supino", 8, 80.0),
                rec(1L, "Crucifixo", 12, 25.0),
                rec(1L, "Agachamento", 5, 100.0),
            ),
        )
        assertEquals(listOf("Agachamento", "Crucifixo", "Supino"), records.map { it.exerciseName })
    }

    @Test
    fun twoSessionsSameExercise_collapseToOneRecord() {
        val records = historicalPersonalRecords(
            listOf(
                rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 80.0),
                rec(sessionId = 2L, occurredAt = 2_000L, name = "Supino", reps = 8, weight = 85.0),
            ),
        )
        assertEquals(1, records.size)
        assertEquals(85.0, records.single().bestWeight, 0.001)
        assertEquals(2L, records.single().bestWeightSessionId)
    }

    @Test
    fun usesSnapshotExerciseName() {
        val records = historicalPersonalRecords(
            listOf(rec(1L, "Supino Snapshot", 10, 60.0)),
        )
        assertEquals("Supino Snapshot", records.single().exerciseName)
    }

    @Test
    fun zeroWeight_isPreserved() {
        val records = historicalPersonalRecords(
            listOf(rec(1L, "Abdominal", 20, 0.0)),
        )
        assertEquals(0.0, records.single().bestWeight, 0.001)
        assertEquals(20, records.single().bestReps)
        assertEquals(0.0, records.single().bestVolume, 0.001)
    }

    @Test
    fun multipleSets_volumeIsSumOfRepsTimesWeight() {
        val records = historicalPersonalRecords(
            listOf(
                rec(sessionId = 1L, name = "Supino", reps = 8, weight = 80.0),
                rec(sessionId = 1L, name = "Supino", reps = 6, weight = 80.0),
            ),
        )
        assertEquals(1_120.0, records.single().bestVolume, 0.001)
        assertEquals(8, records.single().bestReps)
        assertEquals(80.0, records.single().bestWeight, 0.001)
    }

    @Test
    fun delegatesTieBreakToPersonalRecords() {
        val records = historicalPersonalRecords(
            listOf(
                rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 100.0),
                rec(sessionId = 2L, occurredAt = 2_000L, name = "Supino", reps = 10, weight = 100.0),
            ),
        )
        assertEquals(10, records.single().bestWeightReps)
        assertEquals(2L, records.single().bestWeightSessionId)
    }

    private fun rec(
        sessionId: Long,
        name: String,
        reps: Int,
        weight: Double,
        occurredAt: Long = sessionId * 1_000L,
    ) = CompletedSetRecord(
        sessionId = sessionId,
        occurredAtMillis = occurredAt,
        exerciseName = name,
        reps = reps,
        weight = weight,
    )
}

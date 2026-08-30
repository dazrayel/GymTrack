package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseProgressTest {

    @Test
    fun bestWeight_picksHighestLoad() {
        val progress = progress(
            currentSessionId = 2L,
            records = listOf(
                rec(1L, 1_000L, reps = 10, weight = 80.0),
                rec(2L, 2_000L, reps = 8, weight = 100.0),
            ),
        )
        assertEquals(100.0, progress.historicalBest!!.bestWeight, 0.001)
        assertEquals(2L, progress.historicalBest!!.bestWeightSessionId)
    }

    @Test
    fun bestReps_picksHighestReps() {
        val progress = progress(
            currentSessionId = 2L,
            records = listOf(
                rec(1L, 1_000L, reps = 8, weight = 100.0),
                rec(2L, 2_000L, reps = 12, weight = 80.0),
            ),
        )
        assertEquals(12, progress.historicalBest!!.bestReps)
        assertEquals(2L, progress.historicalBest!!.bestRepsSessionId)
    }

    @Test
    fun bestVolume_picksHighestSessionVolume() {
        val progress = progress(
            currentSessionId = 2L,
            records = listOf(
                rec(1L, 1_000L, reps = 8, weight = 80.0),
                rec(1L, 1_000L, reps = 8, weight = 80.0),
                rec(2L, 2_000L, reps = 10, weight = 80.0),
            ),
        )
        assertEquals(1_280.0, progress.historicalBest!!.bestVolume, 0.001)
        assertEquals(1L, progress.historicalBest!!.bestVolumeSessionId)
    }

    @Test
    fun exerciseWithoutSets_hasNoSessionMarks() {
        val progress = exerciseProgress(
            exerciseName = "Supino",
            currentSessionId = 1L,
            currentOccurredAtMillis = 1_000L,
            history = emptyList(),
        )
        assertNull(progress.sessionBestWeight)
        assertNull(progress.historicalBest)
        assertNull(progress.previousBest)
        assertNull(progress.weightDelta)
    }

    @Test
    fun zeroWeight_isValidBestLoad() {
        val progress = progress(
            currentSessionId = 1L,
            records = listOf(rec(1L, 1_000L, reps = 15, weight = 0.0)),
        )
        assertEquals(0.0, progress.historicalBest!!.bestWeight, 0.001)
        assertEquals(0.0, progress.sessionVolume!!, 0.001)
        assertEquals(15, progress.historicalBest!!.bestReps)
    }

    @Test
    fun tiedWeight_prefersHigherReps() {
        val progress = progress(
            currentSessionId = 2L,
            records = listOf(
                rec(1L, 1_000L, reps = 8, weight = 100.0),
                rec(2L, 2_000L, reps = 10, weight = 100.0),
            ),
        )
        assertEquals(10, progress.historicalBest!!.bestWeightReps)
        assertEquals(2L, progress.historicalBest!!.bestWeightSessionId)
    }

    @Test
    fun tiedReps_prefersHigherWeight() {
        val progress = progress(
            currentSessionId = 2L,
            records = listOf(
                rec(1L, 1_000L, reps = 10, weight = 80.0),
                rec(2L, 2_000L, reps = 10, weight = 90.0),
            ),
        )
        assertEquals(90.0, progress.historicalBest!!.bestRepsWeight, 0.001)
        assertEquals(2L, progress.historicalBest!!.bestRepsSessionId)
    }

    @Test
    fun tiedVolume_prefersMoreRecentSession() {
        val progress = progress(
            currentSessionId = 2L,
            records = listOf(
                rec(1L, 1_000L, reps = 8, weight = 80.0),
                rec(2L, 2_000L, reps = 8, weight = 80.0),
            ),
        )
        assertEquals(2L, progress.historicalBest!!.bestVolumeSessionId)
    }

    @Test
    fun multipleSessions_isolateExerciseBySnapshotName() {
        val progress = exerciseProgress(
            exerciseName = "Supino",
            currentSessionId = 2L,
            currentOccurredAtMillis = 2_000L,
            history = listOf(
                rec(1L, 1_000L, name = "Supino", reps = 8, weight = 80.0),
                rec(1L, 1_000L, name = "Agachamento", reps = 5, weight = 140.0),
                rec(2L, 2_000L, name = "Supino", reps = 8, weight = 85.0),
            ),
        )
        assertEquals(85.0, progress.historicalBest!!.bestWeight, 0.001)
        assertEquals(80.0, progress.previousBest!!.bestWeight, 0.001)
    }

    @Test
    fun inProgressRecords_areIgnoredWhenNotInHistory() {
        val progress = progress(
            currentSessionId = 1L,
            records = listOf(rec(1L, 1_000L, reps = 8, weight = 80.0)),
        )
        assertEquals(80.0, progress.historicalBest!!.bestWeight, 0.001)
        assertEquals(1, sessionExerciseStats(listOf(rec(1L, 1_000L, reps = 8, weight = 80.0))).size)
    }

    @Test
    fun partialCompletedSession_usesOnlyPerformedSets() {
        val stats = sessionExerciseStats(
            listOf(rec(1L, 1_000L, reps = 8, weight = 80.0)),
        ).single()
        assertEquals(640.0, stats.volume, 0.001)
        assertEquals(8, stats.bestReps)
        assertEquals(80.0, stats.bestWeight, 0.001)
    }

    @Test
    fun snapshotName_identifiesExercise() {
        val progress = exerciseProgress(
            exerciseName = "Supino Snapshot",
            currentSessionId = 1L,
            currentOccurredAtMillis = 1_000L,
            history = listOf(rec(1L, 1_000L, name = "Supino Snapshot", reps = 8, weight = 60.0)),
        )
        assertEquals("Supino Snapshot", progress.historicalBest!!.exerciseName)
    }

    @Test
    fun nullExerciseId_isIrrelevantBecauseGroupingUsesName() {
        val progress = progress(
            currentSessionId = 2L,
            records = listOf(
                rec(1L, 1_000L, name = "Supino", reps = 8, weight = 80.0),
                rec(2L, 2_000L, name = "Supino", reps = 8, weight = 82.5),
            ),
        )
        assertEquals(82.5, progress.historicalBest!!.bestWeight, 0.001)
        assertEquals(80.0, progress.previousBest!!.bestWeight, 0.001)
    }

    @Test
    fun previousSession_isTheMostRecentEarlierSession() {
        val progress = progress(
            currentSessionId = 3L,
            currentOccurredAt = 3_000L,
            records = listOf(
                rec(1L, 1_000L, reps = 8, weight = 70.0),
                rec(2L, 2_000L, reps = 8, weight = 75.0),
                rec(3L, 3_000L, reps = 8, weight = 80.0),
            ),
        )
        assertEquals(2L, progress.previousSession!!.sessionId)
        assertEquals(75.0, progress.previousSession!!.bestWeight, 0.001)
        assertEquals(75.0, progress.previousBest!!.bestWeight, 0.001)
    }

    @Test
    fun firstSession_hasNoPreviousComparison() {
        val progress = progress(
            currentSessionId = 1L,
            records = listOf(rec(1L, 1_000L, reps = 8, weight = 80.0)),
        )
        assertNull(progress.previousBest)
        assertNull(progress.previousSession)
        assertNull(progress.weightDelta)
        assertNull(progress.repsDelta)
        assertNull(progress.volumeDelta)
        assertEquals(80.0, progress.historicalBest!!.bestWeight, 0.001)
    }

    @Test
    fun increasedWeight_producesPositiveWeightDelta() {
        val progress = progress(
            currentSessionId = 2L,
            records = listOf(
                rec(1L, 1_000L, reps = 8, weight = 80.0),
                rec(2L, 2_000L, reps = 8, weight = 82.5),
            ),
        )
        assertEquals(2.5, progress.weightDelta!!, 0.001)
        assertEquals("+2,5 kg", formatSignedKg(progress.weightDelta!!))
    }

    @Test
    fun increasedReps_producesPositiveRepsDelta() {
        val progress = progress(
            currentSessionId = 2L,
            records = listOf(
                rec(1L, 1_000L, reps = 8, weight = 80.0),
                rec(2L, 2_000L, reps = 10, weight = 80.0),
            ),
        )
        assertEquals(2, progress.repsDelta)
        assertEquals("+2", formatSignedInt(progress.repsDelta!!))
    }

    @Test
    fun increasedVolume_producesPositiveVolumeDelta() {
        val progress = progress(
            currentSessionId = 2L,
            records = listOf(
                rec(1L, 1_000L, reps = 8, weight = 80.0),
                rec(2L, 2_000L, reps = 9, weight = 80.0),
            ),
        )
        assertEquals(80.0, progress.volumeDelta!!, 0.001)
        assertEquals("+80 kg", formatSignedKg(progress.volumeDelta!!))
    }

    @Test
    fun currentSession_isNotComparedAgainstItself() {
        val progress = progress(
            currentSessionId = 1L,
            records = listOf(
                rec(1L, 1_000L, reps = 8, weight = 80.0),
                rec(1L, 1_000L, reps = 8, weight = 85.0),
            ),
        )
        assertNull(progress.previousBest)
        assertEquals(1L, progress.historicalBest!!.bestWeightSessionId)
        assertTrue(progress.weightDelta == null)
    }

    @Test
    fun futureSession_isNotUsedAsPreviousBest() {
        val progress = progress(
            currentSessionId = 1L,
            currentOccurredAt = 1_000L,
            records = listOf(
                rec(1L, 1_000L, reps = 8, weight = 80.0),
                rec(2L, 2_000L, reps = 8, weight = 100.0),
            ),
        )
        assertNull(progress.previousBest)
        assertEquals(100.0, progress.historicalBest!!.bestWeight, 0.001)
        assertEquals(80.0, progress.sessionBestWeight!!, 0.001)
    }

    private fun progress(
        currentSessionId: Long,
        currentOccurredAt: Long = currentSessionId * 1_000L,
        records: List<CompletedSetRecord>,
    ) = exerciseProgress(
        exerciseName = "Supino",
        currentSessionId = currentSessionId,
        currentOccurredAtMillis = currentOccurredAt,
        history = records,
    )

    private fun rec(
        sessionId: Long,
        occurredAt: Long,
        name: String = "Supino",
        reps: Int,
        weight: Double,
    ) = CompletedSetRecord(
        sessionId = sessionId,
        occurredAtMillis = occurredAt,
        exerciseName = name,
        reps = reps,
        weight = weight,
    )
}

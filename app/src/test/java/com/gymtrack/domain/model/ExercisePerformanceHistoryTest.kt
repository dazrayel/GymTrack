package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExercisePerformanceHistoryTest {

    @Test
    fun emptyRecords_returnsEmptyList() {
        assertTrue(exercisePerformanceHistory(emptyList(), "Supino").isEmpty())
    }

    @Test
    fun emptyNameMatch_returnsEmptyList() {
        val records = listOf(rec(sessionId = 1L, name = "Agachamento", reps = 5, weight = 100.0))
        assertTrue(exercisePerformanceHistory(records, "Supino").isEmpty())
    }

    @Test
    fun singleSession_returnsOnePoint() {
        val points = exercisePerformanceHistory(
            listOf(rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 80.0)),
            "Supino",
        )
        assertEquals(1, points.size)
        val point = points.single()
        assertEquals(1L, point.sessionId)
        assertEquals("Supino", point.exerciseName)
        assertEquals(1_000L, point.occurredAtMillis)
        assertEquals(80.0, point.bestWeight, 0.001)
        assertEquals(8, point.bestReps)
        assertEquals(640.0, point.volume, 0.001)
    }

    @Test
    fun multipleSessions_sameExercise_onePointEach() {
        val points = exercisePerformanceHistory(
            listOf(
                rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 80.0),
                rec(sessionId = 2L, occurredAt = 2_000L, name = "Supino", reps = 6, weight = 90.0),
                rec(sessionId = 3L, occurredAt = 3_000L, name = "Supino", reps = 10, weight = 85.0),
            ),
            "Supino",
        )
        assertEquals(3, points.size)
        assertEquals(listOf(1L, 2L, 3L), points.map { it.sessionId })
    }

    @Test
    fun sessions_areSortedChronologically() {
        val points = exercisePerformanceHistory(
            listOf(
                rec(sessionId = 3L, occurredAt = 3_000L, name = "Supino", reps = 8, weight = 80.0),
                rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 70.0),
                rec(sessionId = 2L, occurredAt = 2_000L, name = "Supino", reps = 8, weight = 75.0),
            ),
            "Supino",
        )
        assertEquals(listOf(1_000L, 2_000L, 3_000L), points.map { it.occurredAtMillis })
        assertEquals(listOf(1L, 2L, 3L), points.map { it.sessionId })
    }

    @Test
    fun multipleSetsInSameSession_collapseToOnePoint() {
        val points = exercisePerformanceHistory(
            listOf(
                rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 80.0),
                rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 6, weight = 85.0),
                rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 10, weight = 70.0),
            ),
            "Supino",
        )
        assertEquals(1, points.size)
        assertEquals(1L, points.single().sessionId)
    }

    @Test
    fun volume_isSumOfRepsTimesWeight() {
        val points = exercisePerformanceHistory(
            listOf(
                rec(sessionId = 1L, name = "Supino", reps = 8, weight = 80.0),
                rec(sessionId = 1L, name = "Supino", reps = 6, weight = 80.0),
            ),
            "Supino",
        )
        assertEquals(1_120.0, points.single().volume, 0.001)
    }

    @Test
    fun bestWeight_usesSessionExerciseStatsRule() {
        val sets = listOf(
            rec(sessionId = 1L, name = "Supino", reps = 10, weight = 80.0),
            rec(sessionId = 1L, name = "Supino", reps = 6, weight = 90.0),
            rec(sessionId = 1L, name = "Supino", reps = 8, weight = 90.0),
        )
        val stats = sessionExerciseStats(sets).single()
        val point = exercisePerformanceHistory(sets, "Supino").single()
        assertEquals(90.0, point.bestWeight, 0.001)
        assertEquals(stats.bestWeight, point.bestWeight, 0.001)
        assertEquals(8, stats.repsAtBestWeight)
    }

    @Test
    fun bestReps_usesSessionExerciseStatsRule() {
        val sets = listOf(
            rec(sessionId = 1L, name = "Supino", reps = 12, weight = 70.0),
            rec(sessionId = 1L, name = "Supino", reps = 8, weight = 90.0),
            rec(sessionId = 1L, name = "Supino", reps = 12, weight = 80.0),
        )
        val stats = sessionExerciseStats(sets).single()
        val point = exercisePerformanceHistory(sets, "Supino").single()
        assertEquals(12, point.bestReps)
        assertEquals(stats.bestReps, point.bestReps)
        assertEquals(80.0, stats.weightAtBestReps, 0.001)
    }

    @Test
    fun zeroWeight_isValid() {
        val point = exercisePerformanceHistory(
            listOf(rec(sessionId = 1L, name = "Abdominal", reps = 20, weight = 0.0)),
            "Abdominal",
        ).single()
        assertEquals(0.0, point.bestWeight, 0.001)
        assertEquals(20, point.bestReps)
        assertEquals(0.0, point.volume, 0.001)
    }

    @Test
    fun differentExerciseNames_areNotMixed() {
        val records = listOf(
            rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 80.0),
            rec(sessionId = 1L, occurredAt = 1_000L, name = "Agachamento", reps = 5, weight = 140.0),
        )
        val bench = exercisePerformanceHistory(records, "Supino")
        val squat = exercisePerformanceHistory(records, "Agachamento")
        assertEquals(listOf("Supino"), bench.map { it.exerciseName })
        assertEquals(80.0, bench.single().bestWeight, 0.001)
        assertEquals(listOf("Agachamento"), squat.map { it.exerciseName })
        assertEquals(140.0, squat.single().bestWeight, 0.001)
    }

    @Test
    fun filter_keepsOnlyRequestedExerciseName() {
        val records = listOf(
            rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 80.0),
            rec(sessionId = 2L, occurredAt = 2_000L, name = "Crucifixo", reps = 12, weight = 25.0),
            rec(sessionId = 3L, occurredAt = 3_000L, name = "Supino", reps = 8, weight = 85.0),
        )
        val points = exercisePerformanceHistory(records, "Supino")
        assertEquals(2, points.size)
        assertTrue(points.all { it.exerciseName == "Supino" })
        assertEquals(listOf(1L, 3L), points.map { it.sessionId })
    }

    @Test
    fun identityIsExerciseName_completedSetRecordHasNoExerciseId() {
        val records = listOf(
            rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 80.0),
            rec(sessionId = 2L, occurredAt = 2_000L, name = "Supino", reps = 8, weight = 82.5),
        )
        val points = exercisePerformanceHistory(records, "Supino")
        assertEquals(2, points.size)
        assertTrue(points.all { it.exerciseName == "Supino" })
        assertEquals(82.5, points.last().bestWeight, 0.001)
    }

    @Test
    fun differentSnapshots_remainSeparate() {
        val records = listOf(
            rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 80.0),
            rec(sessionId = 2L, occurredAt = 2_000L, name = "Supino Snapshot", reps = 8, weight = 100.0),
        )
        assertEquals(1, exercisePerformanceHistory(records, "Supino").size)
        assertEquals(80.0, exercisePerformanceHistory(records, "Supino").single().bestWeight, 0.001)
        assertEquals(1, exercisePerformanceHistory(records, "Supino Snapshot").size)
        assertEquals(
            100.0,
            exercisePerformanceHistory(records, "Supino Snapshot").single().bestWeight,
            0.001,
        )
    }

    @Test
    fun equalTimestamps_sortBySessionId() {
        val points = exercisePerformanceHistory(
            listOf(
                rec(sessionId = 20L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 90.0),
                rec(sessionId = 10L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 80.0),
            ),
            "Supino",
        )
        assertEquals(listOf(10L, 20L), points.map { it.sessionId })
    }

    @Test
    fun partialSession_usesOnlyPerformedSets() {
        val point = exercisePerformanceHistory(
            listOf(rec(sessionId = 1L, name = "Supino", reps = 8, weight = 80.0)),
            "Supino",
        ).single()
        assertEquals(640.0, point.volume, 0.001)
        assertEquals(8, point.bestReps)
        assertEquals(80.0, point.bestWeight, 0.001)
    }

    @Test
    fun otherExercises_doNotAppearInResult() {
        val points = exercisePerformanceHistory(
            listOf(
                rec(sessionId = 1L, name = "Supino", reps = 8, weight = 80.0),
                rec(sessionId = 1L, name = "Remada", reps = 10, weight = 60.0),
                rec(sessionId = 2L, name = "Agachamento", reps = 5, weight = 140.0),
            ),
            "Supino",
        )
        assertEquals(1, points.size)
        assertEquals("Supino", points.single().exerciseName)
        assertEquals(80.0, points.single().bestWeight, 0.001)
    }

    @Test
    fun sortOrder_doesNotDependOnInputOrder() {
        val a = rec(sessionId = 3L, occurredAt = 3_000L, name = "Supino", reps = 8, weight = 80.0)
        val b = rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 70.0)
        val c = rec(sessionId = 2L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 75.0)
        val expected = listOf(1L, 2L, 3L)
        assertEquals(expected, exercisePerformanceHistory(listOf(a, b, c), "Supino").map { it.sessionId })
        assertEquals(expected, exercisePerformanceHistory(listOf(c, a, b), "Supino").map { it.sessionId })
        assertEquals(expected, exercisePerformanceHistory(listOf(b, c, a), "Supino").map { it.sessionId })
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

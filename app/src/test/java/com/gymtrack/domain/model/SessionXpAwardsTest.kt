package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class SessionXpAwardsTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val now = local("2026-08-26T12:00:00") // Wednesday

    @Test
    fun completedSession_alwaysAwardsWorkoutCompleted() {
        val events = buildSessionXpEvents(baseInput())
        assertTrue(events.any { it.type == XpEventType.WORKOUT_COMPLETED && it.amount == 100 })
        assertEquals(
            XpSourceKeys.workoutCompleted(10L),
            events.single { it.type == XpEventType.WORKOUT_COMPLETED }.sourceKey,
        )
    }

    @Test
    fun plannedWorkout_awardsBonusWhenIdsMatch() {
        val events = buildSessionXpEvents(
            baseInput(workoutId = 5L, plannedWorkoutIdForDay = 5L),
        )
        assertTrue(events.any { it.type == XpEventType.PLANNED_WORKOUT_COMPLETED && it.amount == 25 })
    }

    @Test
    fun unplannedWorkout_doesNotAwardPlannedBonus() {
        val events = buildSessionXpEvents(
            baseInput(workoutId = 5L, plannedWorkoutIdForDay = 9L),
        )
        assertFalse(events.any { it.type == XpEventType.PLANNED_WORKOUT_COMPLETED })
    }

    @Test
    fun nullPlan_doesNotAwardPlannedBonus() {
        val events = buildSessionXpEvents(
            baseInput(workoutId = 5L, plannedWorkoutIdForDay = null),
        )
        assertFalse(events.any { it.type == XpEventType.PLANNED_WORKOUT_COMPLETED })
    }

    @Test
    fun firstWorkoutOfWeek_whenNoOthersInWeek() {
        val events = buildSessionXpEvents(
            baseInput(otherCompletedSessionsInIsoWeek = emptyList()),
        )
        val first = events.single { it.type == XpEventType.FIRST_WORKOUT_OF_WEEK }
        assertEquals(25, first.amount)
        assertEquals(
            XpSourceKeys.firstWorkoutOfWeek(isoWeekKey(now, zone)),
            first.sourceKey,
        )
    }

    @Test
    fun firstWorkoutOfWeek_skippedWhenAnotherExists() {
        val events = buildSessionXpEvents(
            baseInput(otherCompletedSessionsInIsoWeek = listOf(99L)),
        )
        assertFalse(events.any { it.type == XpEventType.FIRST_WORKOUT_OF_WEEK })
    }

    @Test
    fun increaseWeight_awardsProgressionWeight() {
        val exercise = SessionXpExerciseInput(
            exerciseName = "Supino",
            plannedWeight = 60.0,
            minRepetitions = 8,
            maxRepetitions = 12,
            plannedSets = 3,
            performedSets = listOf(
                ProgressionPerformedSet(0, 12, 60.0),
                ProgressionPerformedSet(1, 12, 60.0),
                ProgressionPerformedSet(2, 12, 60.0),
            ),
        )
        val events = buildSessionXpEvents(baseInput(exercises = listOf(exercise)))
        assertTrue(events.any { it.type == XpEventType.PROGRESSION_WEIGHT && it.amount == 30 })
        assertFalse(events.any { it.type == XpEventType.PROGRESSION_REPS })
    }

    @Test
    fun increaseReps_awardsProgressionReps() {
        val exercise = SessionXpExerciseInput(
            exerciseName = "Supino",
            plannedWeight = 60.0,
            minRepetitions = 8,
            maxRepetitions = 12,
            plannedSets = 3,
            performedSets = listOf(
                ProgressionPerformedSet(0, 10, 60.0),
                ProgressionPerformedSet(1, 10, 60.0),
                ProgressionPerformedSet(2, 10, 60.0),
            ),
        )
        val events = buildSessionXpEvents(baseInput(exercises = listOf(exercise)))
        assertTrue(events.any { it.type == XpEventType.PROGRESSION_REPS && it.amount == 20 })
        assertFalse(events.any { it.type == XpEventType.PROGRESSION_WEIGHT })
    }

    @Test
    fun maintain_doesNotAwardProgression() {
        val exercise = SessionXpExerciseInput(
            exerciseName = "Supino",
            plannedWeight = 60.0,
            minRepetitions = 8,
            maxRepetitions = 12,
            plannedSets = 3,
            performedSets = listOf(
                ProgressionPerformedSet(0, 5, 60.0),
                ProgressionPerformedSet(1, 5, 60.0),
                ProgressionPerformedSet(2, 5, 60.0),
            ),
        )
        val events = buildSessionXpEvents(baseInput(exercises = listOf(exercise)))
        assertFalse(events.any { it.type == XpEventType.PROGRESSION_WEIGHT })
        assertFalse(events.any { it.type == XpEventType.PROGRESSION_REPS })
    }

    @Test
    fun noHistory_doesNotAwardProgression() {
        val exercise = SessionXpExerciseInput(
            exerciseName = "Supino",
            plannedWeight = 60.0,
            minRepetitions = 8,
            maxRepetitions = 12,
            plannedSets = 3,
            performedSets = emptyList(),
        )
        val events = buildSessionXpEvents(baseInput(exercises = listOf(exercise)))
        assertFalse(events.any { it.type == XpEventType.PROGRESSION_WEIGHT })
        assertFalse(events.any { it.type == XpEventType.PROGRESSION_REPS })
    }

    @Test
    fun personalRecord_awardsWhenImprovingWeight() {
        val history = listOf(
            CompletedSetRecord(1L, local("2026-08-20T10:00:00"), "Supino", 8, 50.0),
            CompletedSetRecord(10L, now, "Supino", 8, 60.0),
        )
        val exercise = SessionXpExerciseInput(
            exerciseName = "Supino",
            plannedWeight = 60.0,
            minRepetitions = 8,
            maxRepetitions = 12,
            plannedSets = 1,
            performedSets = listOf(ProgressionPerformedSet(0, 8, 60.0)),
        )
        val events = buildSessionXpEvents(
            baseInput(
                sessionId = 10L,
                exercises = listOf(exercise),
                completedSetHistory = history,
                otherCompletedSessionsInIsoWeek = listOf(1L),
            ),
        )
        assertTrue(events.any { it.type == XpEventType.PERSONAL_RECORD && it.amount == 50 })
    }

    @Test
    fun personalRecord_notAwardedWhenNoImprovement() {
        val history = listOf(
            CompletedSetRecord(1L, local("2026-08-20T10:00:00"), "Supino", 8, 80.0),
            CompletedSetRecord(10L, now, "Supino", 8, 60.0),
        )
        val exercise = SessionXpExerciseInput(
            exerciseName = "Supino",
            plannedWeight = 60.0,
            minRepetitions = 8,
            maxRepetitions = 12,
            plannedSets = 1,
            performedSets = listOf(ProgressionPerformedSet(0, 8, 60.0)),
        )
        val events = buildSessionXpEvents(
            baseInput(
                sessionId = 10L,
                exercises = listOf(exercise),
                completedSetHistory = history,
                otherCompletedSessionsInIsoWeek = listOf(1L),
            ),
        )
        assertFalse(events.any { it.type == XpEventType.PERSONAL_RECORD })
    }

    @Test
    fun biSet_singleWorkoutCompletedEvent() {
        val exercises = listOf(
            SessionXpExerciseInput(
                exerciseName = "A",
                plannedWeight = 40.0,
                minRepetitions = 8,
                maxRepetitions = 10,
                plannedSets = 1,
                performedSets = listOf(ProgressionPerformedSet(0, 5, 40.0)),
            ),
            SessionXpExerciseInput(
                exerciseName = "B",
                plannedWeight = 40.0,
                minRepetitions = 8,
                maxRepetitions = 10,
                plannedSets = 1,
                performedSets = listOf(ProgressionPerformedSet(0, 5, 40.0)),
            ),
        )
        val events = buildSessionXpEvents(baseInput(exercises = exercises))
        assertEquals(1, events.count { it.type == XpEventType.WORKOUT_COMPLETED })
    }

    @Test
    fun sourceKeys_areDeterministicForIdempotency() {
        val a = buildSessionXpEvents(baseInput())
        val b = buildSessionXpEvents(baseInput())
        assertEquals(a.map { it.sourceKey }.toSet(), b.map { it.sourceKey }.toSet())
    }

    private fun baseInput(
        sessionId: Long = 10L,
        workoutId: Long? = 1L,
        exercises: List<SessionXpExerciseInput> = emptyList(),
        completedSetHistory: List<CompletedSetRecord> = emptyList(),
        otherCompletedSessionsInIsoWeek: List<Long> = emptyList(),
        plannedWorkoutIdForDay: Long? = null,
    ) = SessionXpAwardInput(
        sessionId = sessionId,
        workoutId = workoutId,
        occurredAtMillis = now,
        exercises = exercises,
        completedSetHistory = completedSetHistory,
        otherCompletedSessionsInIsoWeek = otherCompletedSessionsInIsoWeek,
        plannedWorkoutIdForDay = plannedWorkoutIdForDay,
        zoneId = zone,
        createdAtMillis = now,
    )

    private fun local(dateTime: String): Long =
        java.time.LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()
}

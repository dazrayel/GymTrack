package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class AchievementEvaluatorTest {

    private val zone: ZoneId = ZoneId.of("America/Sao_Paulo")

    @Test
    fun zeroSessions_allAchievementsLocked() {
        val statuses = evaluateAchievements(emptyList(), emptyList(), zone)
        assertEquals(AchievementCatalog.size, statuses.size)
        statuses.forEach { status ->
            assertFalse(status.achievementId, status.unlocked)
            assertEquals(status.achievementId, 0L, status.current)
        }
    }

    @Test
    fun exactlyOneSession_unlocksFirstWorkoutOnlyAmongWorkoutTiers() {
        val sessions = sessions(count = 1)
        val sets = setsForSessions(sessions, setsPerSession = 1)
        val statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "FIRST_WORKOUT", unlocked = true, current = 1, target = 1)
        assertStatus(statuses, "WORKOUTS_10", unlocked = false, current = 1, target = 10)
    }

    @Test
    fun exactlyTenSessions_unlocksWorkouts10() {
        val sessions = sessions(count = 10)
        val sets = setsForSessions(sessions, setsPerSession = 1)
        val statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "WORKOUTS_10", unlocked = true, current = 10, target = 10)
        assertStatus(statuses, "WORKOUTS_25", unlocked = false, current = 10, target = 25)
    }

    @Test
    fun exactlyTwentyFiveSessions_unlocksWorkouts25() {
        val sessions = sessions(count = 25)
        val sets = setsForSessions(sessions, setsPerSession = 1)
        val statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "WORKOUTS_25", unlocked = true, current = 25, target = 25)
    }

    @Test
    fun exactlyFiftySessions_unlocksWorkouts50() {
        val sessions = sessions(count = 50)
        val sets = setsForSessions(sessions, setsPerSession = 1)
        val statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "WORKOUTS_50", unlocked = true, current = 50, target = 50)
    }

    @Test
    fun sevenSessions_partialProgressForWorkouts10() {
        val sessions = sessions(count = 7)
        val sets = setsForSessions(sessions, setsPerSession = 1)
        val statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "WORKOUTS_10", unlocked = false, current = 7, target = 10)
    }

    @Test
    fun fifteenSessions_capsCurrentAtTargetForWorkouts10() {
        val sessions = sessions(count = 15)
        val sets = setsForSessions(sessions, setsPerSession = 1)
        val statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "WORKOUTS_10", unlocked = true, current = 10, target = 10)
    }

    @Test
    fun volumeBelowLimit_notUnlocked() {
        val sessions = listOf(session(sessionId = 1L, volume = 9_999.0))
        val sets = listOf(set(sessionId = 1L, reps = 10, weight = 999.9))
        val statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "VOLUME_10000", unlocked = false, current = 9_999, target = 10_000)
    }

    @Test
    fun volumeExactlyAtLimit_unlocks() {
        val sessions = listOf(session(sessionId = 1L, volume = 10_000.0))
        val sets = listOf(set(sessionId = 1L, reps = 10, weight = 1_000.0))
        val statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "VOLUME_10000", unlocked = true, current = 10_000, target = 10_000)
    }

    @Test
    fun volumeAboveLimit_capsCurrentAtTarget() {
        val sessions = listOf(session(sessionId = 1L, volume = 12_000.0))
        val sets = listOf(set(sessionId = 1L, reps = 12, weight = 1_000.0))
        val statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "VOLUME_10000", unlocked = true, current = 10_000, target = 10_000)
    }

    @Test
    fun volumePartialProgress_7450() {
        val sessions = listOf(session(sessionId = 1L, volume = 7_450.0))
        val sets = listOf(set(sessionId = 1L, reps = 10, weight = 745.0))
        val statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "VOLUME_10000", unlocked = false, current = 7_450, target = 10_000)
    }

    @Test
    fun setCountAtLimits() {
        val sessions = sessions(count = 1)
        val sets = (1..100).map { set(sessionId = 1L, setIndex = it) }
        var statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "SETS_100", unlocked = true, current = 100, target = 100)
        assertStatus(statuses, "SETS_500", unlocked = false, current = 100, target = 500)

        val sets500 = (1..500).map { set(sessionId = 1L, setIndex = it) }
        statuses = evaluateAchievements(sessions, sets500, zone)
        assertStatus(statuses, "SETS_500", unlocked = true, current = 500, target = 500)

        val sets1000 = (1..1_000).map { set(sessionId = 1L, setIndex = it) }
        statuses = evaluateAchievements(sessions, sets1000, zone)
        assertStatus(statuses, "SETS_1000", unlocked = true, current = 1_000, target = 1_000)
    }

    @Test
    fun distinctExercisesAtLimits() {
        val sessions = sessions(count = 1)
        val names5 = (1..5).map { "Exercício $it" }
        var statuses = evaluateAchievements(sessions, setsForNames(sessions, names5), zone)
        assertStatus(statuses, "EXERCISES_5", unlocked = true, current = 5, target = 5)
        assertStatus(statuses, "EXERCISES_10", unlocked = false, current = 5, target = 10)

        val names3 = (1..3).map { "Exercício $it" }
        statuses = evaluateAchievements(sessions, setsForNames(sessions, names3), zone)
        assertStatus(statuses, "EXERCISES_5", unlocked = false, current = 3, target = 5)
    }

    @Test
    fun noPersonalRecords_prAchievementsLocked() {
        val statuses = evaluateAchievements(emptyList(), emptyList(), zone)
        assertStatus(statuses, "PR_FIRST", unlocked = false, current = 0, target = 1)
        assertStatus(statuses, "PR_EXERCISES_5", unlocked = false, current = 0, target = 5)
    }

    @Test
    fun onePersonalRecord_unlocksPrFirst() {
        val sessions = sessions(count = 1)
        val sets = listOf(set(sessionId = 1L, name = "Supino", reps = 8, weight = 80.0))
        val statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "PR_FIRST", unlocked = true, current = 1, target = 1)
        assertStatus(statuses, "PR_EXERCISES_5", unlocked = false, current = 1, target = 5)
    }

    @Test
    fun fivePersonalRecords_unlocksPrExercises5() {
        val sessions = sessions(count = 1)
        val names = (1..5).map { "Exercício $it" }
        val sets = setsForNames(sessions, names)
        val statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "PR_EXERCISES_5", unlocked = true, current = 5, target = 5)
    }

    @Test
    fun tenPersonalRecords_unlocksPrExercises10() {
        val sessions = sessions(count = 1)
        val names = (1..10).map { "Exercício $it" }
        val sets = setsForNames(sessions, names)
        val statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "PR_EXERCISES_10", unlocked = true, current = 10, target = 10)
    }

    @Test
    fun trainedDaysAtLimits() {
        val dayStarts = (0 until 5).map { offset ->
            local("2026-08-0${offset + 1}T10:00:00")
        }
        val sessions = dayStarts.mapIndexed { index, millis ->
            session(sessionId = index + 1L, occurredAt = millis)
        }
        var statuses = evaluateAchievements(sessions, setsForSessions(sessions, 1), zone)
        assertStatus(statuses, "TRAINED_DAYS_5", unlocked = true, current = 5, target = 5)
        assertStatus(statuses, "TRAINED_DAYS_20", unlocked = false, current = 5, target = 20)

        val twentyDays = (0 until 20).map { offset ->
            session(sessionId = offset + 1L, occurredAt = local("2026-07-${(offset % 28 + 1).toString().padStart(2, '0')}T10:00:00"))
        }
        statuses = evaluateAchievements(twentyDays, setsForSessions(twentyDays, 1), zone)
        assertStatus(statuses, "TRAINED_DAYS_20", unlocked = true, current = 20, target = 20)
    }

    @Test
    fun twoSessionsSameDay_countAsOneTrainedDay() {
        val day = local("2026-08-10T08:00:00")
        val evening = local("2026-08-10T20:00:00")
        val sessions = listOf(
            session(sessionId = 1L, occurredAt = day),
            session(sessionId = 2L, occurredAt = evening),
        )
        val statuses = evaluateAchievements(sessions, setsForSessions(sessions, 1), zone)
        assertStatus(statuses, "TRAINED_DAYS_5", unlocked = false, current = 1, target = 5)
    }

    @Test
    fun sessionsOnDifferentDays_increaseTrainedDayCount() {
        val sessions = listOf(
            session(sessionId = 1L, occurredAt = local("2026-08-10T08:00:00")),
            session(sessionId = 2L, occurredAt = local("2026-08-11T08:00:00")),
        )
        val statuses = evaluateAchievements(sessions, setsForSessions(sessions, 1), zone)
        assertStatus(statuses, "TRAINED_DAYS_5", unlocked = false, current = 2, target = 5)
    }

    @Test
    fun biTriSets_countAsNormalSetsAndDistinctNames() {
        val sessions = sessions(count = 1)
        val sets = listOf(
            set(sessionId = 1L, name = "Supino", reps = 8, weight = 60.0),
            set(sessionId = 1L, name = "Crucifixo", reps = 12, weight = 20.0),
            set(sessionId = 1L, name = "Tríceps", reps = 10, weight = 25.0),
        )
        val statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "SETS_100", unlocked = false, current = 3, target = 100)
        assertStatus(statuses, "EXERCISES_5", unlocked = false, current = 3, target = 5)
        assertStatus(statuses, "PR_EXERCISES_5", unlocked = false, current = 3, target = 5)
    }

    @Test
    fun zeroWeight_validForSetsAndPrReps() {
        val sessions = listOf(session(sessionId = 1L, volume = 0.0))
        val sets = listOf(set(sessionId = 1L, name = "Abdominal", reps = 20, weight = 0.0))
        val statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "SETS_100", unlocked = false, current = 1, target = 100)
        assertStatus(statuses, "PR_FIRST", unlocked = true, current = 1, target = 1)
        assertStatus(statuses, "VOLUME_10000", unlocked = false, current = 0, target = 10_000)
    }

    @Test
    fun substitutedExercise_keepsDistinctHistoricalNames() {
        val sessions = sessions(count = 2)
        val sets = listOf(
            set(sessionId = 1L, name = "Supino Reto", reps = 8, weight = 80.0),
            set(sessionId = 2L, name = "Supino Inclinado", reps = 8, weight = 70.0),
        )
        val statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "EXERCISES_5", unlocked = false, current = 2, target = 5)
        assertStatus(statuses, "PR_EXERCISES_5", unlocked = false, current = 2, target = 5)
    }

    @Test
    fun removedSession_recalculatesAndCanRevokeUnlock() {
        val sessions = sessions(count = 10)
        val sets = setsForSessions(sessions, setsPerSession = 1)
        var statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "WORKOUTS_10", unlocked = true, current = 10, target = 10)

        val afterDelete = sessions.dropLast(1)
        val setsAfterDelete = sets.filter { it.sessionId != 10L }
        statuses = evaluateAchievements(afterDelete, setsAfterDelete, zone)
        assertStatus(statuses, "WORKOUTS_10", unlocked = false, current = 9, target = 10)
    }

    @Test
    fun catalogOrderAndIds_matchMvp() {
        val ids = AchievementCatalog.map { it.id }
        assertEquals(
            listOf(
                "FIRST_WORKOUT",
                "WORKOUTS_10",
                "WORKOUTS_25",
                "WORKOUTS_50",
                "VOLUME_10000",
                "VOLUME_50000",
                "VOLUME_100000",
                "SETS_100",
                "SETS_500",
                "SETS_1000",
                "EXERCISES_5",
                "EXERCISES_10",
                "EXERCISES_25",
                "PR_FIRST",
                "PR_EXERCISES_5",
                "PR_EXERCISES_10",
                "TRAINED_DAYS_5",
                "TRAINED_DAYS_20",
            ),
            ids,
        )
        val statuses = evaluateAchievements(emptyList(), emptyList(), zone)
        assertEquals(ids, statuses.map { it.achievementId })
    }

    private fun assertStatus(
        statuses: List<AchievementStatus>,
        id: String,
        unlocked: Boolean,
        current: Long,
        target: Long,
    ) {
        val status = statuses.single { it.achievementId == id }
        assertEquals(id, unlocked, status.unlocked)
        assertEquals(id, current, status.current)
        assertEquals(id, target, status.target)
    }

    private fun sessions(count: Int): List<WorkoutHistoryItem> =
        (1..count).map { session(sessionId = it.toLong(), occurredAt = it * 86_400_000L) }

    private fun setsForSessions(
        sessions: List<WorkoutHistoryItem>,
        setsPerSession: Int,
    ): List<CompletedSetRecord> =
        sessions.flatMap { item ->
            (1..setsPerSession).map { index ->
                set(sessionId = item.sessionId, setIndex = index, occurredAt = item.startedAtMillis)
            }
        }

    private fun setsForNames(
        sessions: List<WorkoutHistoryItem>,
        names: List<String>,
    ): List<CompletedSetRecord> {
        val sessionId = sessions.first().sessionId
        val occurredAt = sessions.first().startedAtMillis
        return names.mapIndexed { index, name ->
            set(sessionId = sessionId, setIndex = index, name = name, occurredAt = occurredAt)
        }
    }

    private fun session(
        sessionId: Long,
        occurredAt: Long = sessionId * 1_000L,
        volume: Double = 100.0,
    ) = WorkoutHistoryItem(
        sessionId = sessionId,
        workoutName = "Treino",
        startedAtMillis = occurredAt,
        endedAtMillis = occurredAt,
        durationMillis = 1_000L,
        volume = volume,
        exerciseCount = 1,
        completedSetCount = 1,
        plannedSetCount = 1,
    )

    private fun set(
        sessionId: Long,
        setIndex: Int = 1,
        name: String = "Supino",
        reps: Int = 8,
        weight: Double = 10.0,
        occurredAt: Long = sessionId * 1_000L,
    ) = CompletedSetRecord(
        sessionId = sessionId,
        occurredAtMillis = occurredAt,
        exerciseName = name,
        reps = reps,
        weight = weight,
    )

    private fun local(dateTime: String): Long =
        LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()
}

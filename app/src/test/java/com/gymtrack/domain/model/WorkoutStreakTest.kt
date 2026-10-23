package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class WorkoutStreakTest {

    private val zone: ZoneId = ZoneId.of("America/Sao_Paulo")

    @Test
    fun emptyHistory_isZero() {
        val now = local("2026-08-26T12:00:00")
        assertEquals(0, workoutStreakDays(emptyList(), now, zone))
    }

    @Test
    fun trainedToday_countsOne() {
        val now = local("2026-08-26T18:00:00")
        val session = item(1L, local("2026-08-26T10:00:00"))
        assertEquals(1, workoutStreakDays(listOf(session), now, zone))
    }

    @Test
    fun lastWorkoutYesterday_streakRemainsValid() {
        val now = local("2026-08-26T12:00:00")
        val session = item(1L, local("2026-08-25T10:00:00"))
        assertEquals(1, workoutStreakDays(listOf(session), now, zone))
    }

    @Test
    fun lastWorkoutTwoDaysAgo_streakRemainsValid() {
        val now = local("2026-08-26T12:00:00")
        val session = item(1L, local("2026-08-24T10:00:00"))
        assertEquals(1, workoutStreakDays(listOf(session), now, zone))
    }

    @Test
    fun lastWorkoutThreeDaysAgo_streakLost() {
        val now = local("2026-08-26T12:00:00")
        val session = item(1L, local("2026-08-23T10:00:00"))
        assertEquals(0, workoutStreakDays(listOf(session), now, zone))
    }

    @Test
    fun lastWorkoutFourDaysAgo_streakLost() {
        val now = local("2026-08-26T12:00:00")
        val session = item(1L, local("2026-08-22T10:00:00"))
        assertEquals(0, workoutStreakDays(listOf(session), now, zone))
    }

    @Test
    fun monThuSun_withTwoDayRestGaps_streakIsThree() {
        // Mon→Thu gap=3 (Tue,Wed idle), Thu→Sun gap=3 (Fri,Sat idle) — both allowed.
        val now = local("2026-08-30T12:00:00") // Sunday
        val sessions = listOf(
            item(1L, local("2026-08-24T10:00:00")), // Mon
            item(2L, local("2026-08-27T10:00:00")), // Thu
            item(3L, local("2026-08-30T10:00:00")), // Sun
        )
        assertEquals(3, workoutStreakDays(sessions, now, zone))
    }

    @Test
    fun gapOfThreeCompleteIdleDays_breaksStreak() {
        // Mon→Fri: Tue,Wed,Thu idle = 3 complete days; ChronoUnit gap=4 → break.
        val now = local("2026-08-28T12:00:00") // Friday
        val sessions = listOf(
            item(1L, local("2026-08-24T10:00:00")), // Mon
            item(2L, local("2026-08-28T10:00:00")), // Fri
        )
        assertEquals(1, workoutStreakDays(sessions, now, zone))
    }

    @Test
    fun returnAfterLostStreak_startsNewSequence() {
        val now = local("2026-08-26T12:00:00")
        val sessions = listOf(
            item(1L, local("2026-08-18T10:00:00")), // far past
            item(2L, local("2026-08-26T09:00:00")), // today
        )
        assertEquals(1, workoutStreakDays(sessions, now, zone))
    }

    @Test
    fun multipleSessionsSameDay_countAsOneDay() {
        val now = local("2026-08-26T20:00:00")
        val sessions = listOf(
            item(1L, local("2026-08-26T08:00:00")),
            item(2L, local("2026-08-26T18:00:00")),
            item(3L, local("2026-08-25T10:00:00")),
        )
        assertEquals(2, workoutStreakDays(sessions, now, zone))
    }

    @Test
    fun usesEndedAtWhenPresent_forLocalDay() {
        val now = local("2026-08-31T12:00:00")
        val session = item(
            sessionId = 1L,
            startedAt = local("2026-08-30T22:00:00"),
            endedAt = local("2026-08-31T01:00:00"),
        )
        assertEquals(1, workoutStreakDays(listOf(session), now, zone))
    }

    @Test
    fun crossesMonthBoundary_withAllowedGap() {
        val now = local("2026-09-02T12:00:00")
        val sessions = listOf(
            item(1L, local("2026-08-31T10:00:00")),
            item(2L, local("2026-09-02T10:00:00")),
        )
        assertEquals(2, workoutStreakDays(sessions, now, zone))
    }

    @Test
    fun crossesYearBoundary_withAllowedGap() {
        val now = local("2027-01-02T12:00:00")
        val sessions = listOf(
            item(1L, local("2026-12-31T10:00:00")),
            item(2L, local("2027-01-02T10:00:00")),
        )
        assertEquals(2, workoutStreakDays(sessions, now, zone))
    }

    @Test
    fun consecutiveCalendarDays_countFully() {
        val now = local("2026-08-26T12:00:00")
        val sessions = listOf(
            item(1L, local("2026-08-24T10:00:00")),
            item(2L, local("2026-08-25T10:00:00")),
            item(3L, local("2026-08-26T10:00:00")),
        )
        assertEquals(3, workoutStreakDays(sessions, now, zone))
    }

    @Test
    fun olderSegmentBrokenByLongGap_isExcluded() {
        val now = local("2026-08-26T12:00:00")
        val sessions = listOf(
            item(1L, local("2026-08-10T10:00:00")),
            item(2L, local("2026-08-11T10:00:00")),
            item(3L, local("2026-08-25T10:00:00")),
            item(4L, local("2026-08-26T10:00:00")),
        )
        assertEquals(2, workoutStreakDays(sessions, now, zone))
    }

    private fun local(dateTime: String): Long =
        LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()

    private fun item(
        sessionId: Long,
        occurredAt: Long? = null,
        startedAt: Long? = null,
        endedAt: Long? = occurredAt,
    ) = WorkoutHistoryItem(
        sessionId = sessionId,
        workoutName = "Treino",
        startedAtMillis = startedAt ?: occurredAt ?: 0L,
        endedAtMillis = endedAt,
        durationMillis = 1_000L,
        volume = 100.0,
        exerciseCount = 1,
        completedSetCount = 1,
        plannedSetCount = 1,
    )
}

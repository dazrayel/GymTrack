package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DashboardStatsTest {

    private val zone: ZoneId = ZoneId.of("America/Sao_Paulo")

    @Test
    fun monday_belongsToCurrentWeek() {
        val monday = local("2026-08-24T08:00:00")
        val bounds = isoWeekBounds(monday, zone)
        assertTrue(bounds.contains(monday))
        assertEquals(local("2026-08-24T00:00:00"), bounds.startMillis)
        assertEquals(local("2026-08-31T00:00:00"), bounds.endMillis)
    }

    @Test
    fun sunday_belongsToCurrentWeek() {
        val bounds = isoWeekBounds(local("2026-08-30T21:00:00"), zone)
        assertTrue(bounds.contains(local("2026-08-30T23:59:00")))
    }

    @Test
    fun sundayToMonday_changesPeriod() {
        val sundayBounds = isoWeekBounds(local("2026-08-30T12:00:00"), zone)
        val mondayBounds = isoWeekBounds(local("2026-08-31T12:00:00"), zone)
        assertFalse(sundayBounds.contains(local("2026-08-31T00:00:00")))
        assertTrue(mondayBounds.contains(local("2026-08-31T00:00:00")))
        assertEquals(local("2026-08-31T00:00:00"), mondayBounds.startMillis)
    }

    @Test
    fun sessionExactlyAtWeekStart_belongs() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val session = item(sessionId = 1L, occurredAt = bounds.startMillis, volume = 100.0)
        val stats = dashboardPeriodStats(listOf(session), emptyList(), bounds)
        assertEquals(1, stats.sessionCount)
    }

    @Test
    fun sessionExactlyAtWeekEnd_doesNotBelong() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val session = item(sessionId = 1L, occurredAt = bounds.endMillis, volume = 100.0)
        val stats = dashboardPeriodStats(listOf(session), emptyList(), bounds)
        assertEquals(0, stats.sessionCount)
        assertTrue(stats.isEmpty)
    }

    @Test
    fun sessionEndingMondayAfterSundayStart_belongsToNewWeek() {
        val newWeek = isoWeekBounds(local("2026-08-31T12:00:00"), zone)
        val session = item(
            sessionId = 1L,
            startedAt = local("2026-08-30T23:00:00"),
            endedAt = local("2026-08-31T00:30:00"),
            volume = 80.0,
        )
        val stats = dashboardPeriodStats(listOf(session), emptyList(), newWeek)
        assertEquals(1, stats.sessionCount)
        val previousWeek = isoWeekBounds(local("2026-08-30T12:00:00"), zone)
        assertEquals(0, dashboardPeriodStats(listOf(session), emptyList(), previousWeek).sessionCount)
    }

    @Test
    fun inProgressSession_isNotInCompletedInput() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val completed = item(sessionId = 2L, occurredAt = local("2026-08-26T10:00:00"), volume = 50.0)
        val stats = dashboardPeriodStats(listOf(completed), emptyList(), bounds)
        assertEquals(1, stats.sessionCount)
        assertEquals(setOf(2L), listOf(completed).map { it.sessionId }.toSet())
    }

    @Test
    fun completedSession_isCounted() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val session = item(sessionId = 3L, occurredAt = local("2026-08-25T18:00:00"), volume = 200.0)
        assertEquals(1, dashboardPeriodStats(listOf(session), emptyList(), bounds).sessionCount)
    }

    @Test
    fun partialCompletedSession_isCounted() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val session = item(
            sessionId = 4L,
            occurredAt = local("2026-08-25T18:00:00"),
            volume = 320.0,
            completedSetCount = 1,
            plannedSetCount = 4,
        )
        val set = CompletedSetRecord(
            sessionId = 4L,
            occurredAtMillis = local("2026-08-25T18:00:00"),
            exerciseName = "Supino",
            reps = 8,
            weight = 40.0,
        )
        val stats = dashboardPeriodStats(listOf(session), listOf(set), bounds)
        assertEquals(1, stats.sessionCount)
        assertEquals(1, stats.distinctExerciseCount)
        assertEquals(320.0, stats.volume, 0.001)
    }

    @Test
    fun endedAt_takesPriorityOverStartedAt() {
        val bounds = isoWeekBounds(local("2026-08-31T12:00:00"), zone)
        val session = item(
            sessionId = 5L,
            startedAt = local("2026-08-30T22:00:00"),
            endedAt = local("2026-08-31T01:00:00"),
            volume = 10.0,
        )
        assertEquals(local("2026-08-31T01:00:00"), occurredAtMillis(session))
        assertEquals(1, dashboardPeriodStats(listOf(session), emptyList(), bounds).sessionCount)
    }

    @Test
    fun startedAt_isFallbackWhenEndedAtIsNull() {
        val started = local("2026-08-26T09:00:00")
        val session = WorkoutHistoryItem(
            sessionId = 6L,
            workoutName = "A",
            startedAtMillis = started,
            endedAtMillis = null,
            durationMillis = 1_000L,
            volume = 10.0,
            exerciseCount = 1,
            completedSetCount = 1,
            plannedSetCount = 1,
        )
        assertEquals(started, occurredAtMillis(session))
        val bounds = isoWeekBounds(started, zone)
        assertEquals(1, dashboardPeriodStats(listOf(session), emptyList(), bounds).sessionCount)
    }

    @Test
    fun weeklyVolume_sumsSessionVolumes() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val a = item(sessionId = 1L, occurredAt = local("2026-08-24T10:00:00"), volume = 100.0)
        val b = item(sessionId = 2L, occurredAt = local("2026-08-25T10:00:00"), volume = 250.5)
        assertEquals(350.5, dashboardPeriodStats(listOf(a, b), emptyList(), bounds).volume, 0.001)
    }

    @Test
    fun weeklyDuration_sumsSessionDurations() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val a = item(sessionId = 1L, occurredAt = local("2026-08-24T10:00:00"), duration = 60_000L)
        val b = item(sessionId = 2L, occurredAt = local("2026-08-25T10:00:00"), duration = 120_000L)
        assertEquals(180_000L, dashboardPeriodStats(listOf(a, b), emptyList(), bounds).durationMillis)
    }

    @Test
    fun weeklySessionCount() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val inWeek = item(sessionId = 1L, occurredAt = local("2026-08-24T10:00:00"))
        val alsoInWeek = item(sessionId = 2L, occurredAt = local("2026-08-30T10:00:00"))
        val outside = item(sessionId = 3L, occurredAt = local("2026-08-23T10:00:00"))
        assertEquals(
            2,
            dashboardPeriodStats(listOf(inWeek, alsoInWeek, outside), emptyList(), bounds).sessionCount,
        )
    }

    @Test
    fun distinctExercises_useSnapshotName() {
        val occurred = local("2026-08-25T10:00:00")
        val bounds = isoWeekBounds(occurred, zone)
        val session = item(sessionId = 1L, occurredAt = occurred)
        val sets = listOf(
            CompletedSetRecord(1L, occurred, "Supino Snapshot", 8, 60.0),
            CompletedSetRecord(1L, occurred, "Agachamento", 5, 100.0),
        )
        assertEquals(2, dashboardPeriodStats(listOf(session), sets, bounds).distinctExerciseCount)
    }

    @Test
    fun sameExerciseAcrossSessions_countsOnce() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val first = item(sessionId = 1L, occurredAt = local("2026-08-24T10:00:00"))
        val second = item(sessionId = 2L, occurredAt = local("2026-08-25T10:00:00"))
        val sets = listOf(
            CompletedSetRecord(1L, local("2026-08-24T10:00:00"), "Supino", 8, 60.0),
            CompletedSetRecord(2L, local("2026-08-25T10:00:00"), "Supino", 8, 62.5),
        )
        assertEquals(1, dashboardPeriodStats(listOf(first, second), sets, bounds).distinctExerciseCount)
    }

    @Test
    fun differentExerciseNames_countSeparately() {
        val occurred = local("2026-08-25T10:00:00")
        val bounds = isoWeekBounds(occurred, zone)
        val session = item(sessionId = 1L, occurredAt = occurred)
        val sets = listOf(
            CompletedSetRecord(1L, occurred, "Supino", 8, 60.0),
            CompletedSetRecord(1L, occurred, "Crucifixo", 10, 20.0),
        )
        assertEquals(2, dashboardPeriodStats(listOf(session), sets, bounds).distinctExerciseCount)
    }

    @Test
    fun zeroWeight_isValidForDistinctExercise() {
        val occurred = local("2026-08-25T10:00:00")
        val bounds = isoWeekBounds(occurred, zone)
        val session = item(sessionId = 1L, occurredAt = occurred, volume = 0.0)
        val set = CompletedSetRecord(1L, occurred, "Abdominal", 15, 0.0)
        val stats = dashboardPeriodStats(listOf(session), listOf(set), bounds)
        assertEquals(1, stats.distinctExerciseCount)
        assertEquals(0.0, stats.volume, 0.001)
    }

    @Test
    fun periodWithoutSessions_returnsEmptyStats() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val outside = item(sessionId = 9L, occurredAt = local("2026-08-01T10:00:00"), volume = 999.0)
        val stats = dashboardPeriodStats(listOf(outside), emptyList(), bounds)
        assertTrue(stats.isEmpty)
        assertEquals(0, stats.sessionCount)
        assertEquals(0.0, stats.volume, 0.001)
        assertEquals(0L, stats.durationMillis)
        assertEquals(0, stats.distinctExerciseCount)
    }

    @Test
    fun trainedDayCount_zeroSessions() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        assertEquals(0, trainedDayCount(emptyList(), bounds, zone))
    }

    @Test
    fun trainedDayCount_oneSession_isOneDay() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val session = item(sessionId = 1L, occurredAt = local("2026-08-25T10:00:00"))
        assertEquals(1, trainedDayCount(listOf(session), bounds, zone))
    }

    @Test
    fun trainedDayCount_twoSessionsSameDay_countAsOne() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val a = item(sessionId = 1L, occurredAt = local("2026-08-25T08:00:00"))
        val b = item(sessionId = 2L, occurredAt = local("2026-08-25T20:00:00"))
        assertEquals(1, trainedDayCount(listOf(a, b), bounds, zone))
    }

    @Test
    fun trainedDayCount_twoDifferentDays_countAsTwo() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val a = item(sessionId = 1L, occurredAt = local("2026-08-24T10:00:00"))
        val b = item(sessionId = 2L, occurredAt = local("2026-08-25T10:00:00"))
        assertEquals(2, trainedDayCount(listOf(a, b), bounds, zone))
    }

    @Test
    fun trainedDayCount_sessionOutsideWeek_isIgnored() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val outside = item(sessionId = 1L, occurredAt = local("2026-08-23T10:00:00"))
        assertEquals(0, trainedDayCount(listOf(outside), bounds, zone))
    }

    @Test
    fun trainedDayCount_periodStart_belongs() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val session = item(sessionId = 1L, occurredAt = bounds.startMillis)
        assertEquals(1, trainedDayCount(listOf(session), bounds, zone))
    }

    @Test
    fun trainedDayCount_periodEnd_doesNotBelong() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val session = item(sessionId = 1L, occurredAt = bounds.endMillis)
        assertEquals(0, trainedDayCount(listOf(session), bounds, zone))
    }

    @Test
    fun trainedDayCount_midnight_isStartOfThatLocalDay() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val midnight = local("2026-08-24T00:00:00")
        val session = item(sessionId = 1L, occurredAt = midnight)
        assertEquals(1, trainedDayCount(listOf(session), bounds, zone))
        assertEquals(midnight, localDateStartMillis(midnight, zone))
    }

    @Test
    fun trainedDayCount_usesEndedAtWhenPresent() {
        val bounds = isoWeekBounds(local("2026-08-31T12:00:00"), zone)
        val session = item(
            sessionId = 1L,
            startedAt = local("2026-08-30T22:00:00"),
            endedAt = local("2026-08-31T01:00:00"),
        )
        assertEquals(1, trainedDayCount(listOf(session), bounds, zone))
    }

    @Test
    fun trainedDayCount_fallsBackToStartedAt() {
        val started = local("2026-08-26T09:00:00")
        val session = item(sessionId = 1L, startedAt = started, endedAt = null)
        val bounds = isoWeekBounds(started, zone)
        assertEquals(1, trainedDayCount(listOf(session), bounds, zone))
    }

    @Test
    fun trainedDayCount_usesSaoPauloZone() {
        val now = local("2026-08-26T12:00:00")
        val bounds = isoWeekBounds(now, zone)
        val session = item(sessionId = 1L, occurredAt = local("2026-08-25T23:30:00"))
        assertEquals(1, trainedDayCount(listOf(session), bounds, zone))
    }

    @Test
    fun trainedDayCount_sameInstant_differsBetweenUtcAndSaoPaulo() {
        val now = java.time.Instant.parse("2026-08-31T12:00:00Z").toEpochMilli()
        val sessionAt = java.time.Instant.parse("2026-08-31T02:00:00Z").toEpochMilli()
        val utc = java.time.ZoneOffset.UTC
        val session = item(sessionId = 1L, occurredAt = sessionAt)
        assertEquals(0, trainedDayCount(listOf(session), isoWeekBounds(now, zone), zone))
        assertEquals(1, trainedDayCount(listOf(session), isoWeekBounds(now, utc), utc))
    }

    @Test
    fun trainedDayCount_countsCompletedInputOnly() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val completed = item(sessionId = 2L, occurredAt = local("2026-08-25T10:00:00"))
        assertEquals(1, trainedDayCount(listOf(completed), bounds, zone))
    }

    @Test
    fun trainedDayCount_partialCompletedSession_counts() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val partial = item(
            sessionId = 4L,
            occurredAt = local("2026-08-25T18:00:00"),
            volume = 320.0,
            completedSetCount = 1,
            plannedSetCount = 4,
        )
        assertEquals(1, trainedDayCount(listOf(partial), bounds, zone))
    }

    @Test
    fun trainedDayCount_zeroVolumeSession_stillCountsDay() {
        val bounds = isoWeekBounds(local("2026-08-26T12:00:00"), zone)
        val session = item(sessionId = 1L, occurredAt = local("2026-08-25T10:00:00"), volume = 0.0)
        assertEquals(1, trainedDayCount(listOf(session), bounds, zone))
    }

    @Test
    fun isoMonthBounds_firstDayOfMonth_isStart() {
        val bounds = isoMonthBounds(local("2026-08-15T12:00:00"), zone)
        assertEquals(local("2026-08-01T00:00:00"), bounds.startMillis)
        assertTrue(bounds.contains(local("2026-08-01T00:00:00")))
    }

    @Test
    fun isoMonthBounds_lastDayOfMonth_belongs() {
        val bounds = isoMonthBounds(local("2026-08-15T12:00:00"), zone)
        assertTrue(bounds.contains(local("2026-08-31T23:59:00")))
        assertEquals(local("2026-09-01T00:00:00"), bounds.endMillis)
    }

    @Test
    fun isoMonthBounds_nextMonthStart_isExcluded() {
        val august = isoMonthBounds(local("2026-08-31T18:00:00"), zone)
        assertFalse(august.contains(local("2026-09-01T00:00:00")))
        val september = isoMonthBounds(local("2026-09-01T00:00:00"), zone)
        assertTrue(september.contains(local("2026-09-01T00:00:00")))
        assertEquals(local("2026-09-01T00:00:00"), september.startMillis)
        assertEquals(local("2026-10-01T00:00:00"), september.endMillis)
    }

    @Test
    fun isoMonthBounds_januaryToFebruary() {
        val bounds = isoMonthBounds(local("2026-01-20T08:00:00"), zone)
        assertEquals(local("2026-01-01T00:00:00"), bounds.startMillis)
        assertEquals(local("2026-02-01T00:00:00"), bounds.endMillis)
        assertTrue(bounds.contains(local("2026-01-31T23:00:00")))
        assertFalse(bounds.contains(local("2026-02-01T00:00:00")))
    }

    @Test
    fun isoMonthBounds_decemberToJanuary() {
        val bounds = isoMonthBounds(local("2026-12-15T08:00:00"), zone)
        assertEquals(local("2026-12-01T00:00:00"), bounds.startMillis)
        assertEquals(local("2027-01-01T00:00:00"), bounds.endMillis)
        assertTrue(bounds.contains(local("2026-12-31T23:00:00")))
        assertFalse(bounds.contains(local("2027-01-01T00:00:00")))
    }

    @Test
    fun isoMonthBounds_usesLocalMidnightNotUtc() {
        val now = local("2026-08-15T12:00:00")
        val utc = java.time.ZoneOffset.UTC
        val localBounds = isoMonthBounds(now, zone)
        val utcBounds = isoMonthBounds(now, utc)
        assertEquals(local("2026-08-01T00:00:00"), localBounds.startMillis)
        assertTrue(localBounds.startMillis != utcBounds.startMillis)
    }

    @Test
    fun isoMonthBounds_dstUsesAtStartOfDay() {
        val ny = ZoneId.of("America/New_York")
        val now = LocalDateTime.parse("2026-03-15T12:00:00").atZone(ny).toInstant().toEpochMilli()
        val bounds = isoMonthBounds(now, ny)
        val expectedStart = java.time.LocalDate.parse("2026-03-01").atStartOfDay(ny).toInstant().toEpochMilli()
        val expectedEnd = java.time.LocalDate.parse("2026-04-01").atStartOfDay(ny).toInstant().toEpochMilli()
        assertEquals(expectedStart, bounds.startMillis)
        assertEquals(expectedEnd, bounds.endMillis)
        val naiveThirtyOneDays = 31L * 24 * 60 * 60 * 1000
        assertTrue(bounds.endMillis - bounds.startMillis != naiveThirtyOneDays)
    }

    @Test
    fun periodBounds_all_isNull() {
        assertEquals(null, periodBounds(DashboardPeriod.ALL, local("2026-08-15T12:00:00"), zone))
        assertEquals(
            isoWeekBounds(local("2026-08-26T12:00:00"), zone),
            periodBounds(DashboardPeriod.WEEK, local("2026-08-26T12:00:00"), zone),
        )
        assertEquals(
            isoMonthBounds(local("2026-08-15T12:00:00"), zone),
            periodBounds(DashboardPeriod.MONTH, local("2026-08-15T12:00:00"), zone),
        )
    }

    @Test
    fun dashboardPeriodStats_all_includesEverySession() {
        val inWeek = item(sessionId = 1L, occurredAt = local("2026-08-26T10:00:00"), volume = 10.0)
        val inMonthOutsideWeek = item(sessionId = 2L, occurredAt = local("2026-08-10T10:00:00"), volume = 20.0)
        val outsideMonth = item(sessionId = 3L, occurredAt = local("2026-07-01T10:00:00"), volume = 30.0)
        val sessions = listOf(inWeek, inMonthOutsideWeek, outsideMonth)
        val all = dashboardPeriodStats(sessions, emptyList(), bounds = null)
        assertEquals(3, all.sessionCount)
        assertEquals(60.0, all.volume, 0.001)
        val week = dashboardPeriodStats(
            sessions,
            emptyList(),
            isoWeekBounds(local("2026-08-26T12:00:00"), zone),
        )
        assertEquals(1, week.sessionCount)
        val month = dashboardPeriodStats(
            sessions,
            emptyList(),
            isoMonthBounds(local("2026-08-26T12:00:00"), zone),
        )
        assertEquals(2, month.sessionCount)
        assertEquals(30.0, month.volume, 0.001)
    }

    @Test
    fun dashboardPeriodStats_all_emptySessions() {
        val stats = dashboardPeriodStats(emptyList(), emptyList(), bounds = null)
        assertTrue(stats.isEmpty)
        assertEquals(0, stats.sessionCount)
    }

    @Test
    fun dashboardPeriodStats_all_oneSession() {
        val session = item(sessionId = 1L, occurredAt = local("2026-01-01T10:00:00"), volume = 40.0)
        assertEquals(1, dashboardPeriodStats(listOf(session), emptyList(), null).sessionCount)
        assertEquals(40.0, dashboardPeriodStats(listOf(session), emptyList(), null).volume, 0.001)
    }

    @Test
    fun dashboardPeriodStats_monthStartBelongs_endExcluded() {
        val bounds = isoMonthBounds(local("2026-08-15T12:00:00"), zone)
        assertEquals(
            1,
            dashboardPeriodStats(
                listOf(item(1L, occurredAt = bounds.startMillis, volume = 5.0)),
                emptyList(),
                bounds,
            ).sessionCount,
        )
        assertEquals(
            0,
            dashboardPeriodStats(
                listOf(item(2L, occurredAt = bounds.endMillis, volume = 5.0)),
                emptyList(),
                bounds,
            ).sessionCount,
        )
    }

    @Test
    fun trainedDayCount_month_ignoresOtherMonths() {
        val bounds = isoMonthBounds(local("2026-08-15T12:00:00"), zone)
        val inMonth = item(sessionId = 1L, occurredAt = local("2026-08-02T10:00:00"))
        val alsoInMonthSameDay = item(sessionId = 2L, occurredAt = local("2026-08-02T20:00:00"))
        val otherDay = item(sessionId = 3L, occurredAt = local("2026-08-31T10:00:00"))
        val july = item(sessionId = 4L, occurredAt = local("2026-07-31T10:00:00"))
        assertEquals(2, trainedDayCount(listOf(inMonth, alsoInMonthSameDay, otherDay, july), bounds, zone))
    }

    @Test
    fun trainedDayCount_all_countsDistinctLocalDays() {
        val a = item(sessionId = 1L, occurredAt = local("2026-08-02T08:00:00"))
        val b = item(sessionId = 2L, occurredAt = local("2026-08-02T20:00:00"))
        val c = item(sessionId = 3L, occurredAt = local("2026-07-01T10:00:00"))
        assertEquals(2, trainedDayCount(listOf(a, b, c), bounds = null, zone))
        assertEquals(0, trainedDayCount(emptyList(), bounds = null, zone))
    }

    @Test
    fun trainedDayCount_monthBoundary_endedAt() {
        val august = isoMonthBounds(local("2026-08-15T12:00:00"), zone)
        val crossing = item(
            sessionId = 1L,
            startedAt = local("2026-08-31T23:00:00"),
            endedAt = local("2026-09-01T00:30:00"),
        )
        assertEquals(0, trainedDayCount(listOf(crossing), august, zone))
        val september = isoMonthBounds(local("2026-09-01T12:00:00"), zone)
        assertEquals(1, trainedDayCount(listOf(crossing), september, zone))
    }

    private fun local(dateTime: String): Long =
        LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()

    private fun item(
        sessionId: Long,
        occurredAt: Long? = null,
        startedAt: Long? = null,
        endedAt: Long? = occurredAt,
        volume: Double = 100.0,
        duration: Long = 1_000L,
        completedSetCount: Int = 1,
        plannedSetCount: Int = 1,
    ) = WorkoutHistoryItem(
        sessionId = sessionId,
        workoutName = "Treino",
        startedAtMillis = startedAt ?: occurredAt ?: 0L,
        endedAtMillis = endedAt,
        durationMillis = duration,
        volume = volume,
        exerciseCount = 1,
        completedSetCount = completedSetCount,
        plannedSetCount = plannedSetCount,
    )
}

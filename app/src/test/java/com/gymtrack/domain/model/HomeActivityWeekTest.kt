package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class HomeActivityWeekTest {

    private val zone: ZoneId = ZoneId.of("America/Sao_Paulo")

    @Test
    fun alwaysReturnsExactlySevenDays() {
        val now = local("2026-08-26T12:00:00")
        assertEquals(7, homeActivityWeek(emptyList(), now, zone).size)
        assertEquals(7, homeActivityWeek(listOf(item(1L, now)), now, zone).size)
    }

    @Test
    fun displayOrderIsMondayThroughSunday() {
        val now = local("2026-08-26T12:00:00")
        val days = homeActivityWeek(emptyList(), now, zone)
        assertEquals(
            listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY,
            ),
            days.map { it.dayOfWeek },
        )
    }

    @Test
    fun todayIsMarkedCorrectly() {
        val now = local("2026-08-26T12:00:00") // Wednesday
        val days = homeActivityWeek(emptyList(), now, zone)
        assertEquals(1, days.count { it.isToday })
        val today = days.single { it.isToday }
        assertEquals(DayOfWeek.WEDNESDAY, today.dayOfWeek)
        assertEquals(local("2026-08-26T00:00:00"), today.dayStartMillis)
    }

    @Test
    fun completedSessionMarksDayAsTrained() {
        val now = local("2026-08-26T12:00:00")
        val session = item(1L, local("2026-08-25T10:00:00"))
        val days = homeActivityWeek(listOf(session), now, zone)
        val tuesday = days.single { it.dayOfWeek == DayOfWeek.TUESDAY }
        assertTrue(tuesday.trained)
        assertEquals(6, days.count { !it.trained })
    }

    @Test
    fun dayWithoutSessionIsNotTrained() {
        val now = local("2026-08-26T12:00:00")
        val days = homeActivityWeek(emptyList(), now, zone)
        assertTrue(days.none { it.trained })
    }

    @Test
    fun multipleSessionsSameDayStillOneTrainedDay() {
        val now = local("2026-08-26T12:00:00")
        val a = item(1L, local("2026-08-24T08:00:00"))
        val b = item(2L, local("2026-08-24T18:00:00"))
        val days = homeActivityWeek(listOf(a, b), now, zone)
        assertEquals(1, days.count { it.trained })
        assertTrue(days.single { it.dayOfWeek == DayOfWeek.MONDAY }.trained)
    }

    @Test
    fun sessionsOutsideLastSevenDaysAreIgnored() {
        val now = local("2026-08-26T12:00:00")
        val old = item(1L, local("2026-08-19T10:00:00"))
        val days = homeActivityWeek(listOf(old), now, zone)
        assertTrue(days.none { it.trained })
    }

    @Test
    fun todaySessionIsIncluded() {
        val now = local("2026-08-26T12:00:00")
        val today = item(1L, local("2026-08-26T10:00:00"))
        val days = homeActivityWeek(listOf(today), now, zone)
        val wednesday = days.single { it.isToday }
        assertTrue(wednesday.trained)
        assertEquals(DayOfWeek.WEDNESDAY, wednesday.dayOfWeek)
    }

    @Test
    fun weekBoundary_sundayToMondayMapsCorrectly() {
        // Monday 2026-08-24 → last 7 days include previous Tuesday..Sunday + Monday
        val monday = local("2026-08-24T12:00:00")
        val previousSunday = item(1L, local("2026-08-23T10:00:00"))
        val days = homeActivityWeek(listOf(previousSunday), monday, zone)
        assertEquals(DayOfWeek.MONDAY, days.single { it.isToday }.dayOfWeek)
        assertTrue(days.single { it.dayOfWeek == DayOfWeek.SUNDAY }.trained)
        assertFalse(days.single { it.dayOfWeek == DayOfWeek.MONDAY }.trained)
        assertEquals(local("2026-08-23T00:00:00"), days.single { it.dayOfWeek == DayOfWeek.SUNDAY }.dayStartMillis)
        assertEquals(local("2026-08-24T00:00:00"), days.single { it.dayOfWeek == DayOfWeek.MONDAY }.dayStartMillis)
    }

    @Test
    fun monthBoundary_mapsDaysAcrossMonths() {
        val now = local("2026-09-02T12:00:00") // Wednesday
        val august = item(1L, local("2026-08-28T10:00:00")) // Friday previous month
        val days = homeActivityWeek(listOf(august), now, zone)
        assertTrue(days.single { it.dayOfWeek == DayOfWeek.FRIDAY }.trained)
        assertEquals(local("2026-08-28T00:00:00"), days.single { it.dayOfWeek == DayOfWeek.FRIDAY }.dayStartMillis)
        assertEquals(local("2026-09-02T00:00:00"), days.single { it.isToday }.dayStartMillis)
    }

    @Test
    fun yearBoundary_mapsDaysAcrossYears() {
        val now = local("2026-01-02T12:00:00") // Friday
        val newYearEve = item(1L, local("2025-12-31T10:00:00")) // Wednesday
        val days = homeActivityWeek(listOf(newYearEve), now, zone)
        assertTrue(days.single { it.dayOfWeek == DayOfWeek.WEDNESDAY }.trained)
        assertEquals(local("2025-12-31T00:00:00"), days.single { it.dayOfWeek == DayOfWeek.WEDNESDAY }.dayStartMillis)
        assertEquals(local("2026-01-02T00:00:00"), days.single { it.isToday }.dayStartMillis)
    }

    @Test
    fun timezone_usesLocalDayBoundariesLikeRestOfApp() {
        val zoneSp = ZoneId.of("America/Sao_Paulo")
        val utc = ZoneOffset.UTC
        val instant = Instant.parse("2026-08-26T02:30:00Z").toEpochMilli()
        val session = item(1L, instant)

        val spDays = homeActivityWeek(listOf(session), instant, zoneSp)
        val utcDays = homeActivityWeek(listOf(session), instant, utc)

        // SP (UTC-3): 2026-08-25 23:30 → Tuesday local
        assertTrue(spDays.single { it.dayOfWeek == DayOfWeek.TUESDAY }.trained)
        // UTC: 2026-08-26 02:30 → Wednesday
        assertTrue(utcDays.single { it.dayOfWeek == DayOfWeek.WEDNESDAY }.trained)
    }

    @Test
    fun periodIsTodayPlusSixPreviousDays() {
        val now = local("2026-08-26T12:00:00") // Wednesday
        val days = homeActivityWeek(emptyList(), now, zone)
        val byDow = days.associateBy { it.dayOfWeek }
        assertEquals(local("2026-08-24T00:00:00"), byDow.getValue(DayOfWeek.MONDAY).dayStartMillis)
        assertEquals(local("2026-08-25T00:00:00"), byDow.getValue(DayOfWeek.TUESDAY).dayStartMillis)
        assertEquals(local("2026-08-26T00:00:00"), byDow.getValue(DayOfWeek.WEDNESDAY).dayStartMillis)
        assertEquals(local("2026-08-20T00:00:00"), byDow.getValue(DayOfWeek.THURSDAY).dayStartMillis)
        assertEquals(local("2026-08-21T00:00:00"), byDow.getValue(DayOfWeek.FRIDAY).dayStartMillis)
        assertEquals(local("2026-08-22T00:00:00"), byDow.getValue(DayOfWeek.SATURDAY).dayStartMillis)
        assertEquals(local("2026-08-23T00:00:00"), byDow.getValue(DayOfWeek.SUNDAY).dayStartMillis)
    }

    private fun local(dateTime: String): Long =
        LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()

    private fun item(sessionId: Long, occurredAt: Long) = WorkoutHistoryItem(
        sessionId = sessionId,
        workoutName = "Treino",
        startedAtMillis = occurredAt,
        endedAtMillis = occurredAt,
        durationMillis = 60_000L,
        volume = 100.0,
        exerciseCount = 1,
        completedSetCount = 1,
        plannedSetCount = 1,
    )
}

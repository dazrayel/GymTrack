package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

class WorkoutCalendarTest {

    private val zone: ZoneId = ZoneId.of("America/Sao_Paulo")
    private val utc: ZoneId = ZoneId.of("UTC")

    @Test
    fun completedSession_appearsOnCorrectLocalDay() {
        val session = item(sessionId = 1L, occurredAt = local("2026-10-05T18:30:00"))
        val byDay = groupSessionsByLocalDay(listOf(session), zone)

        assertEquals(setOf(LocalDate.of(2026, 10, 5)), byDay.keys)
        assertEquals(listOf(1L), byDay.getValue(LocalDate.of(2026, 10, 5)).map { it.sessionId })
    }

    @Test
    fun lateEveningSession_staysOnSameLocalDay() {
        val session = item(sessionId = 1L, occurredAt = local("2026-10-05T23:30:00"))
        val byDay = groupSessionsByLocalDay(listOf(session), zone)

        assertEquals(setOf(LocalDate.of(2026, 10, 5)), byDay.keys)
        assertFalse(byDay.containsKey(LocalDate.of(2026, 10, 6)))
    }

    @Test
    fun usesEndedAtForLocalDay_notStartedAt() {
        val session = item(
            sessionId = 1L,
            startedAt = local("2026-09-30T23:30:00"),
            endedAt = local("2026-10-01T00:15:00"),
        )
        val byDay = groupSessionsByLocalDay(listOf(session), zone)

        assertEquals(setOf(LocalDate.of(2026, 10, 1)), byDay.keys)
    }

    @Test
    fun fallsBackToStartedAtWhenEndedAtNull() {
        val session = item(
            sessionId = 1L,
            startedAt = local("2026-10-05T10:00:00"),
            endedAt = null,
        )
        val byDay = groupSessionsByLocalDay(listOf(session), zone)

        assertEquals(setOf(LocalDate.of(2026, 10, 5)), byDay.keys)
    }

    @Test
    fun sameInstant_differsBetweenUtcAndSaoPaulo() {
        // 2026-10-06 02:00 UTC == 2026-10-05 23:00 Sao Paulo
        val occurredAt = LocalDateTime.parse("2026-10-06T02:00:00")
            .atZone(utc)
            .toInstant()
            .toEpochMilli()
        val session = item(sessionId = 1L, occurredAt = occurredAt)

        val spDays = groupSessionsByLocalDay(listOf(session), zone).keys
        val utcDays = groupSessionsByLocalDay(listOf(session), utc).keys

        assertEquals(setOf(LocalDate.of(2026, 10, 5)), spDays)
        assertEquals(setOf(LocalDate.of(2026, 10, 6)), utcDays)
    }

    @Test
    fun twoSessionsSameDay_groupTogetherNewestFirst() {
        val morning = item(sessionId = 1L, occurredAt = local("2026-10-05T08:00:00"))
        val evening = item(sessionId = 2L, occurredAt = local("2026-10-05T20:00:00"))
        val byDay = groupSessionsByLocalDay(listOf(morning, evening), zone)

        assertEquals(1, byDay.size)
        assertEquals(listOf(2L, 1L), byDay.getValue(LocalDate.of(2026, 10, 5)).map { it.sessionId })
    }

    @Test
    fun threeSessionsSameDay_allListed() {
        val a = item(sessionId = 1L, occurredAt = local("2026-10-05T07:00:00"))
        val b = item(sessionId = 2L, occurredAt = local("2026-10-05T12:00:00"))
        val c = item(sessionId = 3L, occurredAt = local("2026-10-05T19:00:00"))
        val sessions = groupSessionsByLocalDay(listOf(a, b, c), zone)
            .getValue(LocalDate.of(2026, 10, 5))

        assertEquals(listOf(3L, 2L, 1L), sessions.map { it.sessionId })
    }

    @Test
    fun monthSummary_countsSessionsAndDistinctDays() {
        val day5a = item(sessionId = 1L, occurredAt = local("2026-10-05T08:00:00"))
        val day5b = item(sessionId = 2L, occurredAt = local("2026-10-05T20:00:00"))
        val day8 = item(sessionId = 3L, occurredAt = local("2026-10-08T10:00:00"))
        val september = item(sessionId = 4L, occurredAt = local("2026-09-30T10:00:00"))
        val byDay = groupSessionsByLocalDay(listOf(day5a, day5b, day8, september), zone)
        val summary = calendarMonthSummary(YearMonth.of(2026, 10), byDay)

        assertEquals(3, summary.sessionCount)
        assertEquals(2, summary.trainedDayCount)
    }

    @Test
    fun monthWithoutSessions_summaryIsZero() {
        val session = item(sessionId = 1L, occurredAt = local("2026-09-15T10:00:00"))
        val byDay = groupSessionsByLocalDay(listOf(session), zone)
        val summary = calendarMonthSummary(YearMonth.of(2026, 10), byDay)

        assertEquals(0, summary.sessionCount)
        assertEquals(0, summary.trainedDayCount)
    }

    @Test
    fun sessionOnFirstAndLastDayOfMonth_included() {
        val first = item(sessionId = 1L, occurredAt = local("2026-10-01T00:00:00"))
        val last = item(sessionId = 2L, occurredAt = local("2026-10-31T23:59:00"))
        val byDay = groupSessionsByLocalDay(listOf(first, last), zone)
        val cells = buildCalendarMonthCells(
            yearMonth = YearMonth.of(2026, 10),
            sessionsByDay = byDay,
            today = LocalDate.of(2026, 10, 15),
            firstDayOfWeek = DayOfWeek.SUNDAY,
        )

        val trained = cells.filter { it.hasWorkouts }.map { it.date }
        assertTrue(trained.contains(LocalDate.of(2026, 10, 1)))
        assertTrue(trained.contains(LocalDate.of(2026, 10, 31)))
        assertEquals(2, trained.size)
    }

    @Test
    fun buildGrid_marksTodayAndPadding() {
        val cells = buildCalendarMonthCells(
            yearMonth = YearMonth.of(2026, 10),
            sessionsByDay = emptyMap(),
            today = LocalDate.of(2026, 10, 5),
            firstDayOfWeek = DayOfWeek.SUNDAY,
        )

        assertTrue(cells.size % 7 == 0)
        assertTrue(cells.any { it.date == null })
        val todayCell = cells.first { it.date == LocalDate.of(2026, 10, 5) }
        assertTrue(todayCell.isToday)
        assertTrue(todayCell.isCurrentMonth)
        assertFalse(todayCell.hasWorkouts)
    }

    @Test
    fun sessionsForLocalDate_emptyWhenNoSelectionOrNoWorkouts() {
        val session = item(sessionId = 1L, occurredAt = local("2026-10-05T10:00:00"))
        val byDay = groupSessionsByLocalDay(listOf(session), zone)

        assertTrue(sessionsForLocalDate(null, byDay).isEmpty())
        assertTrue(sessionsForLocalDate(LocalDate.of(2026, 10, 6), byDay).isEmpty())
        assertEquals(1, sessionsForLocalDate(LocalDate.of(2026, 10, 5), byDay).size)
    }

    @Test
    fun navigation_blocksFutureMonths() {
        val current = YearMonth.of(2026, 10)
        assertTrue(canNavigateToNextMonth(YearMonth.of(2026, 9), current))
        assertFalse(canNavigateToNextMonth(current, current))
        assertFalse(canNavigateToNextMonth(YearMonth.of(2026, 11), current))
    }

    @Test
    fun navigation_previousStopsAtEarliestSessionMonth() {
        val current = YearMonth.of(2026, 10)
        val earliest = YearMonth.of(2026, 8)
        assertTrue(canNavigateToPreviousMonth(YearMonth.of(2026, 9), earliest, current))
        assertFalse(canNavigateToPreviousMonth(earliest, earliest, current))
    }

    @Test
    fun navigation_withoutHistory_allowsTwoYearsBack() {
        val current = YearMonth.of(2026, 10)
        assertTrue(canNavigateToPreviousMonth(YearMonth.of(2025, 1), null, current))
        assertFalse(canNavigateToPreviousMonth(YearMonth.of(2024, 10), null, current))
    }

    @Test
    fun earliestSessionYearMonth_returnsMinimum() {
        val a = item(sessionId = 1L, occurredAt = local("2026-10-05T10:00:00"))
        val b = item(sessionId = 2L, occurredAt = local("2026-08-01T10:00:00"))
        assertEquals(YearMonth.of(2026, 8), earliestSessionYearMonth(listOf(a, b), zone))
        assertNull(earliestSessionYearMonth(emptyList(), zone))
    }

    @Test
    fun monthTitle_formatsWithLocale() {
        val title = formatCalendarMonthTitle(YearMonth.of(2026, 10), Locale.forLanguageTag("pt-BR"))
        assertTrue(title.contains("2026"))
        assertTrue(title.lowercase(Locale.ROOT).contains("outubro") || title.contains("Outubro"))
    }

    @Test
    fun weekdayLabels_startWithConfiguredFirstDay() {
        val labels = calendarWeekdayLabels(DayOfWeek.SUNDAY, Locale.forLanguageTag("pt-BR"))
        assertEquals(7, labels.size)
        assertTrue(labels.first().lowercase(Locale.ROOT).startsWith("dom"))
    }

    private fun local(dateTime: String): Long =
        LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()

    private fun item(
        sessionId: Long,
        occurredAt: Long? = null,
        startedAt: Long? = null,
        endedAt: Long? = occurredAt,
        volume: Double = 100.0,
    ) = WorkoutHistoryItem(
        sessionId = sessionId,
        workoutName = "Treino",
        startedAtMillis = startedAt ?: occurredAt ?: 0L,
        endedAtMillis = endedAt,
        durationMillis = 1_000L,
        volume = volume,
        exerciseCount = 1,
        completedSetCount = 1,
        plannedSetCount = 1,
    )
}

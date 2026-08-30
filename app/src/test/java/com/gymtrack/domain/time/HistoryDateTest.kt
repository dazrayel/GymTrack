package com.gymtrack.domain.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class HistoryDateTest {

    @Test
    fun formatsUtcDateAsDayMonthYear() {
        val millis = Instant.parse("2026-08-28T15:30:00Z").toEpochMilli()
        assertEquals("28/08/2026", formatHistoryDate(millis))
    }

    @Test
    fun formatsFirstDayOfYear() {
        val millis = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli()
        assertEquals("01/01/2026", formatHistoryDate(millis))
    }

    @Test
    fun doesNotShiftDateAtUtcMidnight() {
        val millis = Instant.parse("2026-08-28T00:00:00Z").toEpochMilli()
        assertEquals("28/08/2026", formatHistoryDate(millis))
    }

    @Test
    fun formatsUtcTimeAsHoursAndMinutes() {
        val millis = Instant.parse("2026-08-28T07:32:00Z").toEpochMilli()
        assertEquals("07:32", formatHistoryTime(millis))
    }

    @Test
    fun formatsUtcTimeAtEndOfHour() {
        val millis = Instant.parse("2026-08-28T08:18:45Z").toEpochMilli()
        assertEquals("08:18", formatHistoryTime(millis))
    }
}

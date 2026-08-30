package com.gymtrack.domain.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class LocalDateTimeFormatTest {

    private val saoPaulo = ZoneId.of("America/Sao_Paulo")

    @Test
    fun formatLocalDate_usesProvidedZone() {
        val utcMidnight = java.time.Instant.parse("2026-08-31T02:00:00Z").toEpochMilli()
        assertEquals("30/08/2026", formatLocalDate(utcMidnight, saoPaulo))
        assertEquals("31/08/2026", formatHistoryDate(utcMidnight))
    }

    @Test
    fun formatLocalTime_usesProvidedZone() {
        val millis = LocalDateTime.parse("2026-08-30T10:32:00").atZone(saoPaulo).toInstant().toEpochMilli()
        assertEquals("10:32", formatLocalTime(millis, saoPaulo))
        assertEquals("13:32", formatLocalTime(millis, ZoneOffset.UTC))
    }

    @Test
    fun formatDashboardDuration_minutesOnly() {
        assertEquals("48min", formatDashboardDuration(48 * 60_000L))
    }

    @Test
    fun formatDashboardDuration_hoursAndMinutes() {
        assertEquals("2h 17min", formatDashboardDuration((2 * 60 + 17) * 60_000L))
    }

    @Test
    fun formatDashboardDuration_wholeHours() {
        assertEquals("2h", formatDashboardDuration(2 * 60 * 60_000L))
    }

    @Test
    fun formatLocalDayOfMonth_usesProvidedZone() {
        val millis = java.time.Instant.parse("2026-08-31T02:00:00Z").toEpochMilli()
        assertEquals("30", formatLocalDayOfMonth(millis, saoPaulo))
    }
}

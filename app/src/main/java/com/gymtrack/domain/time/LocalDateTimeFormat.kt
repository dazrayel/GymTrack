package com.gymtrack.domain.time

import java.time.Instant
import java.time.ZoneId

/**
 * Formats a timestamp as `dd/MM/yyyy` in [zoneId].
 * Independent of [formatHistoryDate], which remains UTC for the History screen.
 */
fun formatLocalDate(epochMillis: Long, zoneId: ZoneId): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
    return "%02d/%02d/%04d".format(date.dayOfMonth, date.monthValue, date.year)
}

/**
 * Formats a timestamp as `HH:mm` in [zoneId].
 */
fun formatLocalTime(epochMillis: Long, zoneId: ZoneId): String {
    val time = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalTime()
    return "%02d:%02d".format(time.hour, time.minute)
}

/**
 * Day-of-month label for dashboard trend bars, in [zoneId].
 */
fun formatLocalDayOfMonth(epochMillis: Long, zoneId: ZoneId): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
    return date.dayOfMonth.toString()
}

/**
 * Compact duration for dashboard cards, e.g. `48min` or `2h 17min`.
 */
fun formatDashboardDuration(elapsedMillis: Long): String {
    val totalMinutes = elapsedMillis.coerceAtLeast(0L) / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0L && minutes > 0L -> "${hours}h ${minutes}min"
        hours > 0L -> "${hours}h"
        else -> "${minutes}min"
    }
}

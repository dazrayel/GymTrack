package com.gymtrack.domain.time

import java.time.Instant
import java.time.ZoneOffset

/**
 * Formats a session timestamp as `dd/MM/yyyy` in UTC.
 * UTC keeps JVM tests independent of the host timezone and locale.
 */
fun formatHistoryDate(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return "%02d/%02d/%04d".format(date.dayOfMonth, date.monthValue, date.year)
}

/**
 * Formats a session timestamp as `HH:mm` in UTC, matching [formatHistoryDate].
 */
fun formatHistoryTime(epochMillis: Long): String {
    val time = Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalTime()
    return "%02d:%02d".format(time.hour, time.minute)
}

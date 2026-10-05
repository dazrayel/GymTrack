package com.gymtrack.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Single calendar cell for a month grid. [date] is null for leading/trailing padding.
 */
data class CalendarDayCell(
    val date: LocalDate?,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val hasWorkouts: Boolean,
    val sessionCount: Int,
)

data class CalendarMonthSummary(
    val sessionCount: Int,
    val trainedDayCount: Int,
)

/**
 * Groups completed history items by local calendar day using the same
 * [occurredAtMillis] + [zoneId] rule as Home / achievements trained-day counts.
 *
 * Sessions for the same day are sorted newest-first by occurred-at.
 */
fun groupSessionsByLocalDay(
    sessions: List<WorkoutHistoryItem>,
    zoneId: ZoneId,
): Map<LocalDate, List<WorkoutHistoryItem>> {
    return sessions
        .groupBy { localDateOfOccurredAt(it, zoneId) }
        .mapValues { (_, items) ->
            items.sortedByDescending { occurredAtMillis(it) }
        }
}

fun localDateOfOccurredAt(item: WorkoutHistoryItem, zoneId: ZoneId): LocalDate {
    return Instant.ofEpochMilli(occurredAtMillis(item))
        .atZone(zoneId)
        .toLocalDate()
}

fun calendarMonthSummary(
    yearMonth: YearMonth,
    sessionsByDay: Map<LocalDate, List<WorkoutHistoryItem>>,
): CalendarMonthSummary {
    val daysInMonth = sessionsByDay.filterKeys { YearMonth.from(it) == yearMonth }
    return CalendarMonthSummary(
        sessionCount = daysInMonth.values.sumOf { it.size },
        trainedDayCount = daysInMonth.size,
    )
}

fun sessionsForLocalDate(
    date: LocalDate?,
    sessionsByDay: Map<LocalDate, List<WorkoutHistoryItem>>,
): List<WorkoutHistoryItem> {
    if (date == null) return emptyList()
    return sessionsByDay[date].orEmpty()
}

/**
 * Builds a compact month grid (weeks of 7 cells) for [yearMonth].
 * Padding cells have [CalendarDayCell.date] = null.
 */
fun buildCalendarMonthCells(
    yearMonth: YearMonth,
    sessionsByDay: Map<LocalDate, List<WorkoutHistoryItem>>,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
): List<CalendarDayCell> {
    val firstOfMonth = yearMonth.atDay(1)
    val weekFields = WeekFields.of(firstDayOfWeek, 1)
    val leadingEmpty = firstOfMonth.get(weekFields.dayOfWeek()) - 1
    val length = yearMonth.lengthOfMonth()
    val totalCells = ((leadingEmpty + length + 6) / 7) * 7

    return List(totalCells) { index ->
        val dayOffset = index - leadingEmpty
        if (dayOffset !in 0 until length) {
            CalendarDayCell(
                date = null,
                isCurrentMonth = false,
                isToday = false,
                hasWorkouts = false,
                sessionCount = 0,
            )
        } else {
            val date = yearMonth.atDay(dayOffset + 1)
            val sessions = sessionsByDay[date].orEmpty()
            CalendarDayCell(
                date = date,
                isCurrentMonth = true,
                isToday = date == today,
                hasWorkouts = sessions.isNotEmpty(),
                sessionCount = sessions.size,
            )
        }
    }
}

fun formatCalendarMonthTitle(yearMonth: YearMonth, locale: Locale): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
    val raw = yearMonth.format(formatter)
    return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

fun calendarWeekdayLabels(firstDayOfWeek: DayOfWeek, locale: Locale): List<String> {
    return (0 until 7).map { offset ->
        val day = firstDayOfWeek.plus(offset.toLong())
        day.getDisplayName(TextStyle.SHORT_STANDALONE, locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            .trimEnd('.')
    }
}

fun canNavigateToNextMonth(visible: YearMonth, current: YearMonth): Boolean =
    visible < current

fun canNavigateToPreviousMonth(
    visible: YearMonth,
    earliest: YearMonth?,
    current: YearMonth,
): Boolean {
    val floor = earliest ?: current.minusYears(2)
    return visible > floor
}

fun earliestSessionYearMonth(
    sessions: List<WorkoutHistoryItem>,
    zoneId: ZoneId,
): YearMonth? {
    if (sessions.isEmpty()) return null
    return sessions
        .map { YearMonth.from(localDateOfOccurredAt(it, zoneId)) }
        .minOrNull()
}

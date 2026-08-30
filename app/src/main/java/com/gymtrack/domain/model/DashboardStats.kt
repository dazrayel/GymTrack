package com.gymtrack.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class PeriodBounds(
    val startMillis: Long,
    val endMillis: Long,
) {
    fun contains(occurredAtMillis: Long): Boolean =
        occurredAtMillis >= startMillis && occurredAtMillis < endMillis
}

enum class DashboardPeriod {
    WEEK,
    MONTH,
    ALL,
}

data class PeriodDashboardStats(
    val sessionCount: Int,
    val volume: Double,
    val durationMillis: Long,
    val distinctExerciseCount: Int,
) {
    val isEmpty: Boolean
        get() = sessionCount == 0 && distinctExerciseCount == 0 && volume == 0.0 && durationMillis == 0L

    companion object {
        val Empty = PeriodDashboardStats(
            sessionCount = 0,
            volume = 0.0,
            durationMillis = 0L,
            distinctExerciseCount = 0,
        )
    }
}

fun occurredAtMillis(startedAtMillis: Long, endedAtMillis: Long?): Long =
    endedAtMillis ?: startedAtMillis

fun occurredAtMillis(item: WorkoutHistoryItem): Long =
    occurredAtMillis(item.startedAtMillis, item.endedAtMillis)

fun isoWeekBounds(nowMillis: Long, zoneId: ZoneId): PeriodBounds {
    val localDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val monday = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val start = monday.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val end = monday.plusWeeks(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    return PeriodBounds(startMillis = start, endMillis = end)
}

fun isoMonthBounds(nowMillis: Long, zoneId: ZoneId): PeriodBounds {
    val firstOfMonth = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate().withDayOfMonth(1)
    val start = firstOfMonth.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val end = firstOfMonth.plusMonths(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    return PeriodBounds(startMillis = start, endMillis = end)
}

fun periodBounds(
    period: DashboardPeriod,
    nowMillis: Long,
    zoneId: ZoneId,
): PeriodBounds? = when (period) {
    DashboardPeriod.WEEK -> isoWeekBounds(nowMillis, zoneId)
    DashboardPeriod.MONTH -> isoMonthBounds(nowMillis, zoneId)
    DashboardPeriod.ALL -> null
}

data class DailyVolume(
    val dayStartMillis: Long,
    val volume: Double,
)

fun localDateStartMillis(epochMillis: Long, zoneId: ZoneId): Long {
    return Instant.ofEpochMilli(epochMillis)
        .atZone(zoneId)
        .toLocalDate()
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()
}

fun trainedDayCount(
    sessions: List<WorkoutHistoryItem>,
    bounds: PeriodBounds?,
    zoneId: ZoneId,
): Int {
    return sessions
        .map { occurredAtMillis(it) }
        .filter { bounds == null || bounds.contains(it) }
        .map { localDateStartMillis(it, zoneId) }
        .toSet()
        .size
}

fun lastSevenLocalDayBounds(nowMillis: Long, zoneId: ZoneId): PeriodBounds {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val start = today.minusDays(6).atStartOfDay(zoneId).toInstant().toEpochMilli()
    val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    return PeriodBounds(startMillis = start, endMillis = end)
}

fun dailyVolumeTrend(
    sessions: List<WorkoutHistoryItem>,
    nowMillis: Long,
    zoneId: ZoneId,
): List<DailyVolume> {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val bounds = lastSevenLocalDayBounds(nowMillis, zoneId)
    val volumeByDayStart = sessions
        .filter { bounds.contains(occurredAtMillis(it)) }
        .groupBy { localDateStartMillis(occurredAtMillis(it), zoneId) }
        .mapValues { (_, items) -> items.sumOf { it.volume } }
    return (0L..6L).map { offset ->
        val day = today.minusDays(6L - offset)
        val dayStart = day.atStartOfDay(zoneId).toInstant().toEpochMilli()
        DailyVolume(
            dayStartMillis = dayStart,
            volume = volumeByDayStart[dayStart] ?: 0.0,
        )
    }
}

fun dashboardPeriodStats(
    sessions: List<WorkoutHistoryItem>,
    sets: List<CompletedSetRecord>,
    bounds: PeriodBounds?,
): PeriodDashboardStats {
    val periodSessions = sessions.filter { bounds == null || bounds.contains(occurredAtMillis(it)) }
    val periodSessionIds = periodSessions.map { it.sessionId }.toSet()
    val distinctNames = sets
        .filter { set ->
            set.sessionId in periodSessionIds &&
                (bounds == null || bounds.contains(set.occurredAtMillis))
        }
        .map { it.exerciseName }
        .toSet()
    return PeriodDashboardStats(
        sessionCount = periodSessions.size,
        volume = periodSessions.sumOf { it.volume },
        durationMillis = periodSessions.sumOf { it.durationMillis },
        distinctExerciseCount = distinctNames.size,
    )
}

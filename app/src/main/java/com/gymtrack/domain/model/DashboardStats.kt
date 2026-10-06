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

/**
 * One day cell for the Home weekly activity strip (Mon→Sun display order).
 * [trained] is true when at least one completed history session falls on that local day.
 */
data class HomeActivityDay(
    val dayStartMillis: Long,
    val dayOfWeek: DayOfWeek,
    val trained: Boolean,
    val isToday: Boolean,
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

/**
 * Builds the last 7 local days (today + 6 previous), keyed for display as Mon→Sun.
 * Uses the same [occurredAtMillis] / local-day rule as [trainedDayCount] / [dailyVolumeTrend].
 * Input [sessions] are completed-history rows only.
 */
fun homeActivityWeek(
    sessions: List<WorkoutHistoryItem>,
    nowMillis: Long,
    zoneId: ZoneId,
): List<HomeActivityDay> {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val bounds = lastSevenLocalDayBounds(nowMillis, zoneId)
    val trainedDayStarts = sessions
        .map { occurredAtMillis(it) }
        .filter { bounds.contains(it) }
        .map { localDateStartMillis(it, zoneId) }
        .toSet()
    val byDayOfWeek = (0L..6L).associate { offset ->
        val day = today.minusDays(offset)
        day.dayOfWeek to day
    }
    return listOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY,
    ).map { dow ->
        val day = requireNotNull(byDayOfWeek[dow])
        val dayStart = day.atStartOfDay(zoneId).toInstant().toEpochMilli()
        HomeActivityDay(
            dayStartMillis = dayStart,
            dayOfWeek = dow,
            trained = dayStart in trainedDayStarts,
            isToday = day == today,
        )
    }
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

/** Inclusive local-day window of [dayCount] days ending today (today included). */
fun rollingDayBounds(nowMillis: Long, zoneId: ZoneId, dayCount: Int): PeriodBounds {
    require(dayCount > 0)
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val start = today.minusDays((dayCount - 1).toLong()).atStartOfDay(zoneId).toInstant().toEpochMilli()
    val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    return PeriodBounds(startMillis = start, endMillis = end)
}

fun previousIsoWeekBounds(nowMillis: Long, zoneId: ZoneId): PeriodBounds {
    val current = isoWeekBounds(nowMillis, zoneId)
    val weekMillis = current.endMillis - current.startMillis
    return PeriodBounds(
        startMillis = current.startMillis - weekMillis,
        endMillis = current.startMillis,
    )
}

fun previousRollingDayBounds(nowMillis: Long, zoneId: ZoneId, dayCount: Int): PeriodBounds {
    val current = rollingDayBounds(nowMillis, zoneId, dayCount)
    val span = current.endMillis - current.startMillis
    return PeriodBounds(
        startMillis = current.startMillis - span,
        endMillis = current.startMillis,
    )
}

data class DashboardPeriodTotals(
    val sessionCount: Int,
    val completedSetCount: Int,
    val volume: Double,
) {
    val isEmpty: Boolean
        get() = sessionCount == 0 && completedSetCount == 0 && volume == 0.0

    companion object {
        val Empty = DashboardPeriodTotals(0, 0, 0.0)
    }
}

fun dashboardPeriodTotals(
    sessions: List<WorkoutHistoryItem>,
    bounds: PeriodBounds?,
): DashboardPeriodTotals {
    val periodSessions = sessions.filter { bounds == null || bounds.contains(occurredAtMillis(it)) }
    return DashboardPeriodTotals(
        sessionCount = periodSessions.size,
        completedSetCount = periodSessions.sumOf { it.completedSetCount },
        volume = periodSessions.sumOf { it.volume },
    )
}

/**
 * How a metric changed versus a prior period.
 * Never represents division by zero as infinity — uses [New] when previous is zero and current > 0.
 */
sealed class MetricChange {
    data class Absolute(val delta: Int) : MetricChange()
    data class Percent(val percent: Int) : MetricChange()
    data object New : MetricChange()
    data object Unchanged : MetricChange()
}

fun absoluteMetricChange(current: Int, previous: Int): MetricChange {
    val delta = current - previous
    return when {
        previous == 0 && current > 0 -> MetricChange.New
        delta == 0 -> MetricChange.Unchanged
        else -> MetricChange.Absolute(delta)
    }
}

fun percentMetricChange(current: Double, previous: Double): MetricChange {
    return when {
        previous == 0.0 && current > 0.0 -> MetricChange.New
        previous == 0.0 && current == 0.0 -> MetricChange.Unchanged
        else -> {
            val percent = kotlin.math.round(((current - previous) / previous) * 100.0).toInt()
            if (percent == 0) MetricChange.Unchanged else MetricChange.Percent(percent)
        }
    }
}

fun percentMetricChange(current: Int, previous: Int): MetricChange =
    percentMetricChange(current.toDouble(), previous.toDouble())

/** Best available lift highlight from existing PR aggregation — not a new PR rule. */
fun bestAvailablePerformance(
    records: List<ExercisePersonalRecords>,
): ExercisePersonalRecords? {
    return records.maxWithOrNull(
        compareBy<ExercisePersonalRecords> { it.bestWeight }
            .thenBy { it.bestWeightReps }
            .thenBy { it.bestVolume },
    )
}

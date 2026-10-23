package com.gymtrack.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Maximum number of **complete calendar days without a completed workout** that may
 * elapse after the last trained day (or between two trained days) before the streak
 * is considered broken.
 *
 * - Last workout today → 0 idle days → streak valid
 * - Last workout yesterday → 1 idle day → valid
 * - Last workout 2 days ago → 2 idle days → valid
 * - Last workout 3+ days ago → streak lost
 *
 * Between two trained days, up to [MAX_IDLE_DAYS_BEFORE_STREAK_BREAK] idle days are
 * allowed (e.g. Mon→Thu has Tue+Wed idle = 2, ChronoUnit gap 3).
 */
const val MAX_IDLE_DAYS_BEFORE_STREAK_BREAK: Int = 2

/**
 * Current workout streak length: number of distinct local calendar days with at least
 * one completed session in the contiguous streak ending at the most recent trained day.
 *
 * Idle gaps of at most [MAX_IDLE_DAYS_BEFORE_STREAK_BREAK] complete days do not break
 * the streak and are not counted in the returned value. Only completed history rows
 * should be passed ([WorkoutHistoryItem] from completed sessions).
 *
 * Uses local calendar dates via [zoneId] (not fixed 24h windows) and [occurredAtMillis]
 * (prefers `endedAt`, falls back to `startedAt`).
 */
fun workoutStreakDays(
    sessions: List<WorkoutHistoryItem>,
    nowMillis: Long,
    zoneId: ZoneId,
    maxIdleDays: Int = MAX_IDLE_DAYS_BEFORE_STREAK_BREAK,
): Int {
    require(maxIdleDays >= 0)
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val trainedDays: List<LocalDate> = sessions
        .map { occurredAtMillis(it) }
        .map { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }
        .toSortedSet()
        .toList()
    if (trainedDays.isEmpty()) return 0

    val lastTrained = trainedDays.last()
    val idleSinceLast = ChronoUnit.DAYS.between(lastTrained, today)
    if (idleSinceLast > maxIdleDays) return 0

    // Max ChronoUnit gap between consecutive trained days = idle days + 1
    // (Mon→Thu = 3 when 2 idle days Tue/Wed).
    val maxGapBetweenTrainedDays = maxIdleDays.toLong() + 1L
    var streak = 1
    for (index in trainedDays.lastIndex downTo 1) {
        val newer = trainedDays[index]
        val older = trainedDays[index - 1]
        val gap = ChronoUnit.DAYS.between(older, newer)
        if (gap > maxGapBetweenTrainedDays) break
        streak++
    }
    return streak
}

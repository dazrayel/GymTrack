package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class DailyVolumeTrendTest {

    private val zone: ZoneId = ZoneId.of("America/Sao_Paulo")

    @Test
    fun alwaysReturnsSevenItems() {
        val now = local("2026-08-26T12:00:00")
        assertEquals(7, dailyVolumeTrend(emptyList(), now, zone).size)
        assertEquals(7, dailyVolumeTrend(listOf(item(1L, now)), now, zone).size)
    }

    @Test
    fun isChronologicalFromTodayMinusSixToToday() {
        val now = local("2026-08-26T12:00:00")
        val trend = dailyVolumeTrend(emptyList(), now, zone)
        val starts = trend.map { it.dayStartMillis }
        assertEquals(starts.sorted(), starts)
        assertEquals(local("2026-08-20T00:00:00"), trend.first().dayStartMillis)
        assertEquals(local("2026-08-26T00:00:00"), trend.last().dayStartMillis)
    }

    @Test
    fun daysWithoutTraining_areZero() {
        val now = local("2026-08-26T12:00:00")
        val session = item(1L, local("2026-08-26T10:00:00"), volume = 80.0)
        val trend = dailyVolumeTrend(listOf(session), now, zone)
        assertEquals(0.0, trend[0].volume, 0.001)
        assertEquals(80.0, trend[6].volume, 0.001)
        assertEquals(6, trend.count { it.volume == 0.0 })
    }

    @Test
    fun twoSessionsOnSameDay_sumVolume() {
        val now = local("2026-08-26T12:00:00")
        val a = item(1L, local("2026-08-24T08:00:00"), volume = 100.0)
        val b = item(2L, local("2026-08-24T18:00:00"), volume = 50.0)
        val trend = dailyVolumeTrend(listOf(a, b), now, zone)
        val day = trend.single { it.dayStartMillis == local("2026-08-24T00:00:00") }
        assertEquals(150.0, day.volume, 0.001)
    }

    @Test
    fun usesWorkoutHistoryItemVolume() {
        val now = local("2026-08-26T12:00:00")
        val session = item(1L, local("2026-08-25T10:00:00"), volume = 432.5)
        val trend = dailyVolumeTrend(listOf(session), now, zone)
        assertEquals(432.5, trend.single { it.volume > 0 }.volume, 0.001)
    }

    @Test
    fun zeroWeightSession_hasZeroVolume() {
        val now = local("2026-08-26T12:00:00")
        val session = item(1L, local("2026-08-25T10:00:00"), volume = 0.0)
        val trend = dailyVolumeTrend(listOf(session), now, zone)
        assertEquals(0.0, trend.single { it.dayStartMillis == local("2026-08-25T00:00:00") }.volume, 0.001)
    }

    @Test
    fun zeroVolumeSession_stillOccupiesTrainedDayInIsoWeek() {
        val now = local("2026-08-26T12:00:00")
        val session = item(1L, local("2026-08-25T10:00:00"), volume = 0.0)
        assertEquals(1, trainedDayCount(listOf(session), isoWeekBounds(now, zone), zone))
        val trend = dailyVolumeTrend(listOf(session), now, zone)
        assertEquals(0.0, trend.single { it.dayStartMillis == local("2026-08-25T00:00:00") }.volume, 0.001)
    }

    @Test
    fun sessionEightDaysAgo_isExcluded() {
        val now = local("2026-08-26T12:00:00")
        val old = item(1L, local("2026-08-18T10:00:00"), volume = 900.0)
        val trend = dailyVolumeTrend(listOf(old), now, zone)
        assertTrue(trend.all { it.volume == 0.0 })
    }

    @Test
    fun sessionToday_isLastItem() {
        val now = local("2026-08-26T12:00:00")
        val today = item(1L, local("2026-08-26T09:00:00"), volume = 40.0)
        val trend = dailyVolumeTrend(listOf(today), now, zone)
        assertEquals(40.0, trend.last().volume, 0.001)
        assertEquals(0.0, trend.dropLast(1).sumOf { it.volume }, 0.001)
    }

    @Test
    fun sundayToMonday_isRollingSevenDaysNotIsoWeek() {
        val monday = local("2026-08-31T12:00:00")
        val sunday = item(1L, local("2026-08-30T18:00:00"), volume = 70.0)
        val trend = dailyVolumeTrend(listOf(sunday), monday, zone)
        assertEquals(7, trend.size)
        assertEquals(local("2026-08-25T00:00:00"), trend.first().dayStartMillis)
        assertEquals(local("2026-08-31T00:00:00"), trend.last().dayStartMillis)
        assertEquals(70.0, trend.single { it.dayStartMillis == local("2026-08-30T00:00:00") }.volume, 0.001)
        assertEquals(0, trainedDayCount(listOf(sunday), isoWeekBounds(monday, zone), zone))
    }

    @Test
    fun usesExplicitTimezone() {
        val now = local("2026-08-26T12:00:00")
        val bounds = lastSevenLocalDayBounds(now, zone)
        assertEquals(local("2026-08-20T00:00:00"), bounds.startMillis)
        assertEquals(local("2026-08-27T00:00:00"), bounds.endMillis)
    }

    @Test
    fun rollingWindow_isNotIsoWeek() {
        val wednesday = local("2026-08-26T12:00:00")
        val iso = isoWeekBounds(wednesday, zone)
        val rolling = lastSevenLocalDayBounds(wednesday, zone)
        assertEquals(local("2026-08-24T00:00:00"), iso.startMillis)
        assertEquals(local("2026-08-20T00:00:00"), rolling.startMillis)
    }

    @Test
    fun windowStartBelongs_windowEndDoesNot() {
        val now = local("2026-08-26T12:00:00")
        val bounds = lastSevenLocalDayBounds(now, zone)
        val atStart = item(1L, occurredAt = bounds.startMillis, volume = 10.0)
        val atEnd = item(2L, occurredAt = bounds.endMillis, volume = 99.0)
        val trend = dailyVolumeTrend(listOf(atStart, atEnd), now, zone)
        assertEquals(10.0, trend.first().volume, 0.001)
        assertTrue(trend.none { it.volume == 99.0 })
    }

    @Test
    fun endedAtTakesPriorityOverStartedAt() {
        val now = local("2026-08-26T12:00:00")
        val session = WorkoutHistoryItem(
            sessionId = 1L,
            workoutName = "Treino",
            startedAtMillis = local("2026-08-24T23:00:00"),
            endedAtMillis = local("2026-08-25T00:30:00"),
            durationMillis = 90 * 60_000L,
            volume = 55.0,
            exerciseCount = 1,
            completedSetCount = 1,
            plannedSetCount = 1,
        )
        val trend = dailyVolumeTrend(listOf(session), now, zone)
        assertEquals(55.0, trend.single { it.dayStartMillis == local("2026-08-25T00:00:00") }.volume, 0.001)
        assertEquals(0.0, trend.single { it.dayStartMillis == local("2026-08-24T00:00:00") }.volume, 0.001)
    }

    @Test
    fun sameInstant_mapsToDifferentLocalDaysInUtcAndSaoPaulo() {
        val instant = Instant.parse("2026-08-26T02:00:00Z").toEpochMilli()
        val utc = ZoneOffset.UTC
        val session = item(1L, occurredAt = instant, volume = 12.0)
        val spTrend = dailyVolumeTrend(listOf(session), instant, zone)
        val utcTrend = dailyVolumeTrend(listOf(session), instant, utc)
        assertEquals(12.0, spTrend.single { it.dayStartMillis == local("2026-08-25T00:00:00") }.volume, 0.001)
        assertEquals(
            12.0,
            utcTrend.single {
                it.dayStartMillis == Instant.parse("2026-08-26T00:00:00Z").toEpochMilli()
            }.volume,
            0.001,
        )
    }

    private fun local(dateTime: String): Long =
        LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()

    private fun item(
        sessionId: Long,
        occurredAt: Long,
        volume: Double = 100.0,
    ) = WorkoutHistoryItem(
        sessionId = sessionId,
        workoutName = "Treino",
        startedAtMillis = occurredAt,
        endedAtMillis = occurredAt,
        durationMillis = 1_000L,
        volume = volume,
        exerciseCount = 1,
        completedSetCount = 1,
        plannedSetCount = 1,
    )
}

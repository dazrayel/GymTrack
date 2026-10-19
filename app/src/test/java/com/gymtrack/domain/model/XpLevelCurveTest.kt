package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XpLevelCurveTest {

    @Test
    fun level1_atZeroXp() {
        val progress = levelProgressFromTotalXp(0)
        assertEquals(1, progress.level)
        assertEquals(0L, progress.xpIntoLevel)
        assertEquals(134L, progress.xpForNextLevel)
        assertEquals(134L, progress.xpRemainingToNextLevel)
        assertFalse(progress.isMaxLevel)
    }

    @Test
    fun level2_exactlyAtThreshold() {
        val progress = levelProgressFromTotalXp(134)
        assertEquals(2, progress.level)
        assertEquals(0L, progress.xpIntoLevel)
        assertEquals(301L, progress.xpForNextLevel)
    }

    @Test
    fun level2_oneXpBeforeThreshold() {
        val progress = levelProgressFromTotalXp(133)
        assertEquals(1, progress.level)
        assertEquals(133L, progress.xpIntoLevel)
        assertEquals(1L, progress.xpRemainingToNextLevel)
    }

    @Test
    fun level2_oneXpAfterThreshold() {
        val progress = levelProgressFromTotalXp(135)
        assertEquals(2, progress.level)
        assertEquals(1L, progress.xpIntoLevel)
    }

    @Test
    fun milestones_matchApprovedTable() {
        assertEquals(10, levelProgressFromTotalXp(5_609).level)
        assertEquals(20, levelProgressFromTotalXp(19_537).level)
        assertEquals(30, levelProgressFromTotalXp(38_594).level)
        assertEquals(40, levelProgressFromTotalXp(61_388).level)
        assertEquals(50, levelProgressFromTotalXp(100_000).level)
    }

    @Test
    fun level50_atCap() {
        val progress = levelProgressFromTotalXp(100_000)
        assertEquals(50, progress.level)
        assertTrue(progress.isMaxLevel)
        assertNull(progress.xpForNextLevel)
        assertNull(progress.xpRemainingToNextLevel)
        assertEquals(1f, progress.progressFraction)
    }

    @Test
    fun level50_preservesXpAboveCap() {
        val progress = levelProgressFromTotalXp(102_500)
        assertEquals(50, progress.level)
        assertEquals(102_500L, progress.totalXp)
        assertTrue(progress.isMaxLevel)
    }

    @Test
    fun optionA_level49To50Uses13445() {
        assertEquals(13_445, XpLevelCurve.xpToNextLevel(49))
        assertEquals(
            100_000,
            XpLevelCurve.cumulativeXpByLevel[49] + XpLevelCurve.xpToNextLevel(49)!!,
        )
    }

    @Test
    fun cumulativeTableIsInternallyConsistent() {
        for (level in 1 until XpLevelCurve.MAX_LEVEL) {
            val expected = XpLevelCurve.cumulativeXpByLevel[level] +
                XpLevelCurve.xpToNextLevel(level)!!
            assertEquals(
                "level $level -> ${level + 1}",
                XpLevelCurve.cumulativeXpByLevel[level + 1],
                expected,
            )
        }
    }

    @Test
    fun xpEventAmounts_matchSpec() {
        assertEquals(100, XpEventType.WORKOUT_COMPLETED.amount)
        assertEquals(25, XpEventType.PLANNED_WORKOUT_COMPLETED.amount)
        assertEquals(25, XpEventType.FIRST_WORKOUT_OF_WEEK.amount)
        assertEquals(30, XpEventType.PROGRESSION_WEIGHT.amount)
        assertEquals(20, XpEventType.PROGRESSION_REPS.amount)
        assertEquals(50, XpEventType.PERSONAL_RECORD.amount)
        assertEquals(0, XpEventType.MISSION_COMPLETED.amount) // amount from MissionDefinition
    }
}

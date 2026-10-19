package com.gymtrack.domain.model

/**
 * Official Phase 5.2 XP curve (Option A approved):
 * level 49 → 50 uses 13_445 XP so cumulative level 50 remains 100_000.
 */
object XpLevelCurve {
    const val MAX_LEVEL = 50
    const val MAX_LEVEL_CUMULATIVE_XP = 100_000

    /**
     * Cumulative XP required to *reach* each level (index = level).
     * Index 0 unused; level 1 starts at 0.
     */
    val cumulativeXpByLevel: IntArray = intArrayOf(
        0, // unused
        0, // 1
        134, // 2
        435, // 3
        867, // 4
        1_413, // 5
        2_065, // 6
        2_815, // 7
        3_659, // 8
        4_591, // 9
        5_609, // 10
        6_706, // 11
        7_877, // 12
        9_119, // 13
        10_428, // 14
        11_801, // 15
        13_235, // 16
        14_728, // 17
        16_278, // 18
        17_882, // 19
        19_537, // 20
        21_243, // 21
        22_997, // 22
        24_798, // 23
        26_644, // 24
        28_534, // 25
        30_466, // 26
        32_439, // 27
        34_452, // 28
        36_504, // 29
        38_594, // 30
        40_721, // 31
        42_884, // 32
        45_082, // 33
        47_314, // 34
        49_580, // 35
        51_879, // 36
        54_210, // 37
        56_572, // 38
        58_965, // 39
        61_388, // 40
        63_840, // 41
        66_321, // 42
        68_831, // 43
        71_369, // 44
        73_935, // 45
        76_522, // 46
        79_816, // 47
        83_161, // 48
        86_555, // 49
        100_000, // 50 (Option A: 86_555 + 13_445)
    )

    /** XP needed to advance from [level] to level+1. Null at max level. */
    fun xpToNextLevel(level: Int): Int? {
        if (level < 1 || level >= MAX_LEVEL) return null
        return cumulativeXpByLevel[level + 1] - cumulativeXpByLevel[level]
    }
}

/**
 * Derived level progress from total XP (source of truth).
 */
data class LevelProgress(
    val totalXp: Long,
    val level: Int,
    val xpIntoLevel: Long,
    val xpForNextLevel: Long?,
    val xpRemainingToNextLevel: Long?,
    val progressFraction: Float,
) {
    val isMaxLevel: Boolean get() = level >= XpLevelCurve.MAX_LEVEL
}

fun levelProgressFromTotalXp(totalXp: Long): LevelProgress {
    val safeXp = totalXp.coerceAtLeast(0L)
    var level = 1
    for (candidate in 2..XpLevelCurve.MAX_LEVEL) {
        if (safeXp >= XpLevelCurve.cumulativeXpByLevel[candidate]) {
            level = candidate
        } else {
            break
        }
    }
    val floor = XpLevelCurve.cumulativeXpByLevel[level].toLong()
    val xpIntoLevel = safeXp - floor
    if (level >= XpLevelCurve.MAX_LEVEL) {
        return LevelProgress(
            totalXp = safeXp,
            level = XpLevelCurve.MAX_LEVEL,
            xpIntoLevel = xpIntoLevel,
            xpForNextLevel = null,
            xpRemainingToNextLevel = null,
            progressFraction = 1f,
        )
    }
    val need = XpLevelCurve.xpToNextLevel(level)!!.toLong()
    val remaining = (need - xpIntoLevel).coerceAtLeast(0L)
    val fraction = if (need <= 0L) {
        1f
    } else {
        (xpIntoLevel.toFloat() / need.toFloat()).coerceIn(0f, 1f)
    }
    return LevelProgress(
        totalXp = safeXp,
        level = level,
        xpIntoLevel = xpIntoLevel,
        xpForNextLevel = need,
        xpRemainingToNextLevel = remaining,
        progressFraction = fraction,
    )
}

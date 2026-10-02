package com.gymtrack.domain.model

import java.time.ZoneId
import kotlin.math.floor
import kotlin.math.min

/**
 * Evaluates MVP achievements from completed-session history already loaded by callers.
 * Does not access persistence; recomputes whenever input lists change (e.g. after session delete).
 */
fun evaluateAchievements(
    sessions: List<WorkoutHistoryItem>,
    completedSets: List<CompletedSetRecord>,
    zoneId: ZoneId,
): List<AchievementStatus> {
    val sessionCount = sessions.size.toLong()
    val totalVolumeKg = floor(sessions.sumOf { it.volume }).toLong()
    val setCount = completedSets.size.toLong()
    val distinctExerciseCount = completedSets.map { it.exerciseName }.toSet().size.toLong()
    val prExerciseCount = historicalPersonalRecords(completedSets).size.toLong()
    val trainedDays = trainedDayCount(sessions, bounds = null, zoneId).toLong()

    return AchievementCatalog.map { definition ->
        val raw = when (definition.metric) {
            AchievementMetric.SESSION_COUNT -> sessionCount
            AchievementMetric.TOTAL_VOLUME_KG -> totalVolumeKg
            AchievementMetric.SET_COUNT -> setCount
            AchievementMetric.DISTINCT_EXERCISE_COUNT -> distinctExerciseCount
            AchievementMetric.PR_EXERCISE_COUNT -> prExerciseCount
            AchievementMetric.TRAINED_DAYS -> trainedDays
        }
        val target = definition.target
        AchievementStatus(
            achievementId = definition.id,
            unlocked = raw >= target,
            current = min(raw, target),
            target = target,
        )
    }
}

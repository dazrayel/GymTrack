package com.gymtrack.domain.model

/**
 * Presentation helpers for workout summary rewards.
 * Consumes existing [XpEvent] / achievement evaluation — no new award rules.
 */

data class SessionXpBreakdown(
    val events: List<XpEvent>,
) {
    val totalAmount: Int get() = events.sumOf { it.amount }
    val isEmpty: Boolean get() = events.isEmpty()
}

data class SessionMissionReward(
    val definitionId: String,
    val periodKey: String,
    val xpAmount: Int,
)

data class SessionProgressionHighlight(
    val exerciseName: String,
    val action: ProgressionAction,
)

data class SessionPrHighlight(
    val exerciseName: String,
    val weight: Double,
    val reps: Int,
)

/**
 * XP events attributed to [sessionId] via [XpSourceKeys] (contain `session:$sessionId`).
 * Does not invent amounts — filters the ledger.
 */
fun xpEventsForSession(events: List<XpEvent>, sessionId: Long): SessionXpBreakdown {
    val needle = "session:$sessionId"
    return SessionXpBreakdown(
        events = events.filter { event ->
            event.type != XpEventType.MISSION_COMPLETED &&
                event.sourceKey.contains(needle)
        },
    )
}

/**
 * Mission XP events likely awarded when finishing [sessionEndedAtMillis]
 * (mission sourceKey has no session id).
 */
fun missionRewardsNearSessionEnd(
    events: List<XpEvent>,
    sessionEndedAtMillis: Long?,
    windowMillis: Long = 120_000L,
): List<SessionMissionReward> {
    if (sessionEndedAtMillis == null) return emptyList()
    return events
        .filter { it.type == XpEventType.MISSION_COMPLETED }
        .filter { kotlin.math.abs(it.createdAtMillis - sessionEndedAtMillis) <= windowMillis }
        .mapNotNull { event ->
            val parts = event.sourceKey.split(':')
            if (parts.size < 3 || parts[0] != "MISSION_COMPLETED") return@mapNotNull null
            SessionMissionReward(
                definitionId = parts[1],
                periodKey = parts.drop(2).joinToString(":"),
                xpAmount = event.amount,
            )
        }
        .distinctBy { it.definitionId to it.periodKey }
}

/**
 * Achievements unlocked only when this session is included in history.
 */
fun newlyUnlockedAchievementIds(
    withSession: List<AchievementStatus>,
    withoutSession: List<AchievementStatus>,
): List<String> {
    val lockedWithout = withoutSession.filter { !it.unlocked }.map { it.achievementId }.toSet()
    return withSession
        .filter { it.unlocked && it.achievementId in lockedWithout }
        .map { it.achievementId }
}

fun progressionHighlightsFromXp(events: List<XpEvent>, sessionId: Long): List<SessionProgressionHighlight> {
    val needle = "session:$sessionId"
    return events.mapNotNull { event ->
        if (!event.sourceKey.contains(needle)) return@mapNotNull null
        val action = when (event.type) {
            XpEventType.PROGRESSION_WEIGHT -> ProgressionAction.INCREASE_WEIGHT
            XpEventType.PROGRESSION_REPS -> ProgressionAction.INCREASE_REPS
            else -> return@mapNotNull null
        }
        val name = exerciseNameFromSourceKey(event.sourceKey) ?: return@mapNotNull null
        SessionProgressionHighlight(exerciseName = name, action = action)
    }.distinctBy { it.exerciseName to it.action }
}

private fun exerciseNameFromSourceKey(sourceKey: String): String? {
    val marker = ":exercise:"
    val index = sourceKey.indexOf(marker)
    if (index < 0) return null
    return sourceKey.substring(index + marker.length).takeIf { it.isNotBlank() }
}

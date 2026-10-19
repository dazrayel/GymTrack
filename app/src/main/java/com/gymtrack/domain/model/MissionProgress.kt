package com.gymtrack.domain.model

import java.time.DayOfWeek
import java.time.ZoneId

/**
 * Snapshot used to evaluate mission eligibility and derived progress.
 */
data class MissionEvaluationContext(
    val nowMillis: Long,
    val zoneId: ZoneId,
    val completedSessions: List<MissionSessionSnapshot>,
    /** Planned weekday slots (ISO DayOfWeek → workoutId). */
    val weeklyPlans: Map<DayOfWeek, Long>,
    /** Existing instances keyed by definitionId → periodKey. */
    val instances: List<MissionInstance>,
    /**
     * Sessions that earned a load progression (INCREASE_WEIGHT or INCREASE_REPS)
     * after their completion, keyed by sessionId (count of progressing exercises).
     */
    val progressionCountsBySessionId: Map<Long, Int>,
)

data class MissionSessionSnapshot(
    val sessionId: Long,
    val workoutId: Long?,
    val occurredAtMillis: Long,
)

fun isMissionEligible(
    definition: MissionDefinition,
    context: MissionEvaluationContext,
): Boolean {
    when (definition.id) {
        MissionIds.START_JOURNEY,
        MissionIds.GROWING_STRENGTH,
        MissionIds.VETERAN,
        -> {
            if (context.instances.any {
                    it.definitionId == definition.id && it.status == MissionStatus.COMPLETED
                }
            ) {
                return false
            }
        }
        MissionIds.TRAINING_DAY -> {
            val today = localDayOfWeek(context.nowMillis, context.zoneId)
            if (context.weeklyPlans[today] == null) return false
        }
        MissionIds.FULL_WEEK -> {
            if (context.weeklyPlans.isEmpty()) return false
        }
    }
    return true
}

/**
 * Resolves target for a definition in context. [FULL_WEEK] uses plan size.
 */
fun resolveMissionTarget(
    definition: MissionDefinition,
    context: MissionEvaluationContext,
): Int {
    if (definition.id == MissionIds.FULL_WEEK) {
        return context.weeklyPlans.size.coerceAtLeast(0)
    }
    return definition.target ?: 0
}

fun computeMissionProgress(
    definition: MissionDefinition,
    instance: MissionInstance,
    context: MissionEvaluationContext,
): Int {
    val start = instance.startedAtMillis
    val zone = context.zoneId
    val sessionsAfterStart = context.completedSessions.filter { it.occurredAtMillis >= start }

    return when (definition.id) {
        MissionIds.FIRST_STEP -> {
            val dayKey = instance.periodKey
            sessionsAfterStart.count { localDateKey(it.occurredAtMillis, zone) == dayKey }
                .coerceAtMost(1)
        }
        MissionIds.TRAINING_DAY -> {
            val dayKey = instance.periodKey
            val dayOfWeek = java.time.LocalDate.parse(dayKey).dayOfWeek
            val plannedId = context.weeklyPlans[dayOfWeek] ?: return 0
            sessionsAfterStart.count { session ->
                localDateKey(session.occurredAtMillis, zone) == dayKey &&
                    session.workoutId != null &&
                    session.workoutId == plannedId
            }.coerceAtMost(1)
        }
        MissionIds.CONSISTENCY -> {
            val weekKey = instance.periodKey
            sessionsAfterStart.count { isoWeekKey(it.occurredAtMillis, zone) == weekKey }
                .coerceAtMost(definition.target ?: 3)
        }
        MissionIds.FULL_WEEK -> {
            val weekKey = instance.periodKey
            val sessionsInWeek = sessionsAfterStart.filter {
                isoWeekKey(it.occurredAtMillis, zone) == weekKey
            }
            var satisfied = 0
            for ((planDay, workoutId) in context.weeklyPlans) {
                val matched = sessionsInWeek.any { session ->
                    localDayOfWeek(session.occurredAtMillis, zone) == planDay &&
                        session.workoutId == workoutId
                }
                if (matched) satisfied++
            }
            satisfied
        }
        MissionIds.EVOLUTION -> {
            val weekKey = instance.periodKey
            val sessionIdsInWeek = sessionsAfterStart
                .filter { isoWeekKey(it.occurredAtMillis, zone) == weekKey }
                .map { it.sessionId }
                .toSet()
            val progressionSessions = sessionIdsInWeek.count { sessionId ->
                (context.progressionCountsBySessionId[sessionId] ?: 0) > 0
            }
            progressionSessions.coerceAtMost(1)
        }
        MissionIds.START_JOURNEY -> {
            sessionsAfterStart.size.coerceAtMost(definition.target ?: 5)
        }
        MissionIds.GROWING_STRENGTH -> {
            val count = sessionsAfterStart.sumOf { session ->
                context.progressionCountsBySessionId[session.sessionId] ?: 0
            }
            count.coerceAtMost(definition.target ?: 5)
        }
        MissionIds.VETERAN -> {
            sessionsAfterStart.size.coerceAtMost(definition.target ?: 25)
        }
        else -> 0
    }
}

/**
 * Selects eligible missions for display (daily ≤2, weekly ≤3, all eligible one-time).
 */
fun selectMissionsForDisplay(
    context: MissionEvaluationContext,
): List<MissionDefinition> {
    val eligible = MissionCatalog.all.filter { isMissionEligible(it, context) }
    val daily = eligible.filter { it.type == MissionType.DAILY }.take(MissionCatalog.MAX_ACTIVE_DAILY)
    val weekly = eligible.filter { it.type == MissionType.WEEKLY }.take(MissionCatalog.MAX_ACTIVE_WEEKLY)
    val oneTime = eligible.filter {
        it.type == MissionType.ONE_TIME || it.type == MissionType.LONG_TERM
    }
    return daily + weekly + oneTime
}

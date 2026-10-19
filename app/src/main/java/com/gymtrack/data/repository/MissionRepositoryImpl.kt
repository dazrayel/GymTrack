package com.gymtrack.data.repository

import androidx.room.withTransaction
import com.gymtrack.data.local.GymTrackDatabase
import com.gymtrack.data.local.dao.MissionInstanceDao
import com.gymtrack.data.local.dao.WeeklyWorkoutPlanDao
import com.gymtrack.data.local.dao.WorkoutSessionDao
import com.gymtrack.data.local.dao.WorkoutSessionExerciseDao
import com.gymtrack.data.local.dao.WorkoutSetDao
import com.gymtrack.data.local.dao.XpEventDao
import com.gymtrack.data.local.entity.MissionInstanceEntity
import com.gymtrack.data.local.entity.XpEventEntity
import com.gymtrack.domain.model.MissionCatalog
import com.gymtrack.domain.model.MissionEvaluationContext
import com.gymtrack.domain.model.MissionInstance
import com.gymtrack.domain.model.MissionProgressView
import com.gymtrack.domain.model.MissionSessionSnapshot
import com.gymtrack.domain.model.MissionStatus
import com.gymtrack.domain.model.MissionType
import com.gymtrack.domain.model.ProgressionAction
import com.gymtrack.domain.model.ProgressionPerformedSet
import com.gymtrack.domain.model.XpEventType
import com.gymtrack.domain.model.XpSourceKeys
import com.gymtrack.domain.model.computeMissionProgress
import com.gymtrack.domain.model.evaluateLoadProgression
import com.gymtrack.domain.model.missionPeriodKey
import com.gymtrack.domain.model.occurredAtMillis
import com.gymtrack.domain.model.resolveMissionTarget
import com.gymtrack.domain.model.selectMissionsForDisplay
import com.gymtrack.domain.repository.MissionRepository
import com.gymtrack.domain.time.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MissionRepositoryImpl @Inject constructor(
    private val database: GymTrackDatabase,
    private val missionInstanceDao: MissionInstanceDao,
    private val xpEventDao: XpEventDao,
    private val sessionDao: WorkoutSessionDao,
    private val sessionExerciseDao: WorkoutSessionExerciseDao,
    private val setDao: WorkoutSetDao,
    private val weeklyWorkoutPlanDao: WeeklyWorkoutPlanDao,
    private val timeProvider: TimeProvider,
) : MissionRepository {

    private val zoneId: ZoneId get() = ZoneId.systemDefault()

    override fun observeMissions(): Flow<List<MissionProgressView>> {
        return combine(
            missionInstanceDao.observeAll(),
            sessionDao.observeCompleted(),
            weeklyWorkoutPlanDao.observeAllWithWorkoutNames(),
        ) { instances, historyRows, planRows ->
            val now = timeProvider.nowMillis()
            val sessions = historyRows.map { row ->
                MissionSessionSnapshot(
                    sessionId = row.sessionId,
                    workoutId = row.workoutId,
                    occurredAtMillis = occurredAtMillis(row.startedAtMillis, row.endedAtMillis),
                )
            }
            val plans = planRows.associate { row ->
                DayOfWeek.of(row.dayOfWeek) to row.workoutId
            }
            val domainInstances = instances.map { it.toDomain() }
            val progressionCounts = buildProgressionCounts(sessions.map { it.sessionId })
            val context = MissionEvaluationContext(
                nowMillis = now,
                zoneId = zoneId,
                completedSessions = sessions,
                weeklyPlans = plans,
                instances = domainInstances,
                progressionCountsBySessionId = progressionCounts,
            )
            buildViews(context)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun ensureCurrentInstances() {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                ensureInstancesInternal(timeProvider.nowMillis())
            }
        }
    }

    override suspend fun evaluateAfterSession(sessionId: Long) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val session = sessionDao.getByIdOnce(sessionId) ?: return@withTransaction
                if (session.status != com.gymtrack.data.local.entity.WorkoutSessionEntity.STATUS_COMPLETED) {
                    return@withTransaction
                }
                val now = timeProvider.nowMillis()
                ensureInstancesInternal(now)
                completeEligibleMissions(now)
            }
        }
    }

    private fun ensureInstancesInternal(nowMillis: Long) {
        val existing = missionInstanceDao.getAllOnce().map { it.toDomain() }
        val plans = weeklyWorkoutPlanDao.getAllOnce().associate {
            DayOfWeek.of(it.dayOfWeek) to it.workoutId
        }
        val context = MissionEvaluationContext(
            nowMillis = nowMillis,
            zoneId = zoneId,
            completedSessions = emptyList(),
            weeklyPlans = plans,
            instances = existing,
            progressionCountsBySessionId = emptyMap(),
        )
        val selected = selectMissionsForDisplay(context)
        for (definition in selected) {
            val periodKey = missionPeriodKey(definition, nowMillis, zoneId)
            if (missionInstanceDao.getByDefinitionAndPeriod(definition.id, periodKey) != null) {
                continue
            }
            // One-time: skip creating if already completed in any period
            if (!definition.repeatable &&
                existing.any {
                    it.definitionId == definition.id && it.status == MissionStatus.COMPLETED
                }
            ) {
                continue
            }
            missionInstanceDao.insertIgnore(
                MissionInstanceEntity(
                    definitionId = definition.id,
                    periodKey = periodKey,
                    status = MissionInstanceEntity.STATUS_ACTIVE,
                    startedAtMillis = nowMillis,
                    completedAtMillis = null,
                ),
            )
        }
    }

    private fun completeEligibleMissions(nowMillis: Long) {
        val historyRows = sessionDao.getCompletedOnce()
        val sessions = historyRows.map { row ->
            MissionSessionSnapshot(
                sessionId = row.sessionId,
                workoutId = row.workoutId,
                occurredAtMillis = occurredAtMillis(row.startedAtMillis, row.endedAtMillis),
            )
        }
        val plans = weeklyWorkoutPlanDao.getAllOnce().associate {
            DayOfWeek.of(it.dayOfWeek) to it.workoutId
        }
        val instances = missionInstanceDao.getAllOnce().map { it.toDomain() }
        val progressionCounts = buildProgressionCounts(sessions.map { it.sessionId })
        val context = MissionEvaluationContext(
            nowMillis = nowMillis,
            zoneId = zoneId,
            completedSessions = sessions,
            weeklyPlans = plans,
            instances = instances,
            progressionCountsBySessionId = progressionCounts,
        )
        for (instance in instances) {
            if (instance.status == MissionStatus.COMPLETED) continue
            val definition = MissionCatalog.byId(instance.definitionId) ?: continue
            val target = resolveMissionTarget(definition, context)
            if (target <= 0) continue
            val progress = computeMissionProgress(definition, instance, context)
            if (progress < target) continue
            val updated = missionInstanceDao.markCompletedIfActive(
                definitionId = instance.definitionId,
                periodKey = instance.periodKey,
                status = MissionInstanceEntity.STATUS_COMPLETED,
                completedAtMillis = nowMillis,
            )
            if (updated == 0) continue
            xpEventDao.insertIgnore(
                XpEventEntity(
                    type = XpEventType.MISSION_COMPLETED.name,
                    amount = definition.xpReward,
                    sourceKey = XpSourceKeys.missionCompleted(
                        instance.definitionId,
                        instance.periodKey,
                    ),
                    createdAtMillis = nowMillis,
                ),
            )
        }
    }

    private fun buildViews(context: MissionEvaluationContext): List<MissionProgressView> {
        val selectedIds = selectMissionsForDisplay(context).map { it.id }.toSet()
        val periodKeysNeeded = MissionCatalog.all.associate { definition ->
            definition.id to missionPeriodKey(definition, context.nowMillis, context.zoneId)
        }
        return context.instances
            .mapNotNull { instance ->
                val definition = MissionCatalog.byId(instance.definitionId) ?: return@mapNotNull null
                val currentPeriod = periodKeysNeeded[definition.id]
                val isCurrentPeriod = instance.periodKey == currentPeriod
                val show = when {
                    instance.status == MissionStatus.COMPLETED && isCurrentPeriod -> true
                    instance.status == MissionStatus.COMPLETED &&
                        (definition.type == MissionType.ONE_TIME || definition.type == MissionType.LONG_TERM) -> true
                    instance.status == MissionStatus.ACTIVE &&
                        isCurrentPeriod &&
                        selectedIds.contains(definition.id) -> true
                    else -> false
                }
                if (!show) return@mapNotNull null
                val target = resolveMissionTarget(definition, context).coerceAtLeast(1)
                val progress = computeMissionProgress(definition, instance, context)
                MissionProgressView(
                    definition = definition,
                    instance = instance,
                    progress = progress.coerceAtMost(target),
                    target = target,
                )
            }
            .sortedWith(
                compareBy<MissionProgressView> {
                    when (it.definition.type) {
                        MissionType.DAILY -> 0
                        MissionType.WEEKLY -> 1
                        MissionType.LONG_TERM, MissionType.ONE_TIME -> 2
                    }
                }.thenBy { it.definition.id },
            )
    }

    private fun buildProgressionCounts(sessionIds: List<Long>): Map<Long, Int> {
        if (sessionIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<Long, Int>()
        for (sessionId in sessionIds.distinct()) {
            val exercises = sessionExerciseDao.getBySessionIdOnce(sessionId)
            val sets = setDao.getBySessionIdOnce(sessionId)
            val byExercise = sets.groupBy { it.sessionExerciseId }
            var count = 0
            for (exercise in exercises) {
                val performed = byExercise[exercise.id]
                    .orEmpty()
                    .sortedBy { it.setIndex }
                    .map {
                        ProgressionPerformedSet(
                            setIndex = it.setIndex,
                            reps = it.reps,
                            weight = it.weight,
                        )
                    }
                val action = evaluateLoadProgression(
                    plannedWeight = exercise.plannedWeight,
                    minRepetitions = exercise.minRepetitions,
                    maxRepetitions = exercise.maxRepetitions,
                    plannedSets = exercise.plannedSets,
                    lastSessionSets = performed,
                ).action
                if (action == ProgressionAction.INCREASE_WEIGHT ||
                    action == ProgressionAction.INCREASE_REPS
                ) {
                    count++
                }
            }
            if (count > 0) {
                result[sessionId] = count
            }
        }
        return result
    }
}

private fun MissionInstanceEntity.toDomain(): MissionInstance =
    MissionInstance(
        id = id,
        definitionId = definitionId,
        periodKey = periodKey,
        status = when (status) {
            MissionInstanceEntity.STATUS_COMPLETED -> MissionStatus.COMPLETED
            else -> MissionStatus.ACTIVE
        },
        startedAtMillis = startedAtMillis,
        completedAtMillis = completedAtMillis,
    )

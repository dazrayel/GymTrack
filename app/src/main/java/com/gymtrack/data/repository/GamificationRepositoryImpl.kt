package com.gymtrack.data.repository

import androidx.room.withTransaction
import com.gymtrack.data.local.GymTrackDatabase
import com.gymtrack.data.local.dao.WeeklyWorkoutPlanDao
import com.gymtrack.data.local.dao.WorkoutSessionDao
import com.gymtrack.data.local.dao.WorkoutSessionExerciseDao
import com.gymtrack.data.local.dao.WorkoutSetDao
import com.gymtrack.data.local.dao.XpEventDao
import com.gymtrack.data.local.entity.XpEventEntity
import com.gymtrack.domain.model.CompletedSetRecord
import com.gymtrack.domain.model.LevelProgress
import com.gymtrack.domain.model.ProgressionPerformedSet
import com.gymtrack.domain.model.SessionXpAwardInput
import com.gymtrack.domain.model.SessionXpExerciseInput
import com.gymtrack.domain.model.XpEvent
import com.gymtrack.domain.model.XpEventType
import com.gymtrack.domain.model.buildSessionXpEvents
import com.gymtrack.domain.model.isoWeekBounds
import com.gymtrack.domain.model.levelProgressFromTotalXp
import com.gymtrack.domain.model.localDayOfWeek
import com.gymtrack.domain.model.occurredAtMillis
import com.gymtrack.domain.repository.GamificationRepository
import com.gymtrack.domain.time.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GamificationRepositoryImpl @Inject constructor(
    private val database: GymTrackDatabase,
    private val xpEventDao: XpEventDao,
    private val sessionDao: WorkoutSessionDao,
    private val sessionExerciseDao: WorkoutSessionExerciseDao,
    private val setDao: WorkoutSetDao,
    private val weeklyWorkoutPlanDao: WeeklyWorkoutPlanDao,
    private val timeProvider: TimeProvider,
) : GamificationRepository {

    private val zoneId: ZoneId get() = ZoneId.systemDefault()

    override fun observeTotalXp(): Flow<Long> = xpEventDao.observeTotalXp()

    override fun observeLevelProgress(): Flow<LevelProgress> =
        xpEventDao.observeTotalXp().map { levelProgressFromTotalXp(it) }

    override fun observeXpEvents(): Flow<List<XpEvent>> =
        xpEventDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun awardXpForCompletedSession(sessionId: Long) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val session = sessionDao.getByIdOnce(sessionId) ?: return@withTransaction
                if (session.status != com.gymtrack.data.local.entity.WorkoutSessionEntity.STATUS_COMPLETED) {
                    return@withTransaction
                }
                val occurredAt = occurredAtMillis(session.startedAtMillis, session.endedAtMillis)
                val weekBounds = isoWeekBounds(occurredAt, zoneId)
                val otherInWeek = sessionDao.getCompletedOnce()
                    .filter { row ->
                        row.sessionId != sessionId &&
                            weekBounds.contains(
                                occurredAtMillis(row.startedAtMillis, row.endedAtMillis),
                            )
                    }
                    .map { it.sessionId }

                val day = localDayOfWeek(occurredAt, zoneId)
                val planned = weeklyWorkoutPlanDao.getByDay(day.value)

                val exercises = sessionExerciseDao.getBySessionId(sessionId).first()
                val sets = setDao.getBySessionId(sessionId).first()
                val setsByExerciseId = sets.groupBy { it.sessionExerciseId }

                val exerciseInputs = exercises.map { exercise ->
                    val performed = setsByExerciseId[exercise.id]
                        .orEmpty()
                        .sortedBy { it.setIndex }
                        .map { set ->
                            ProgressionPerformedSet(
                                setIndex = set.setIndex,
                                reps = set.reps,
                                weight = set.weight,
                            )
                        }
                    SessionXpExerciseInput(
                        exerciseName = exercise.exerciseName,
                        plannedWeight = exercise.plannedWeight,
                        minRepetitions = exercise.minRepetitions,
                        maxRepetitions = exercise.maxRepetitions,
                        plannedSets = exercise.plannedSets,
                        performedSets = performed,
                    )
                }

                val historyRows = setDao.observeCompletedSetHistory().first()
                val history = historyRows.map { row ->
                    CompletedSetRecord(
                        sessionId = row.sessionId,
                        occurredAtMillis = row.occurredAtMillis,
                        exerciseName = row.exerciseName,
                        reps = row.reps,
                        weight = row.weight,
                    )
                }

                val events = buildSessionXpEvents(
                    SessionXpAwardInput(
                        sessionId = sessionId,
                        workoutId = session.workoutId,
                        occurredAtMillis = occurredAt,
                        exercises = exerciseInputs,
                        completedSetHistory = history,
                        otherCompletedSessionsInIsoWeek = otherInWeek,
                        plannedWorkoutIdForDay = planned?.workoutId,
                        zoneId = zoneId,
                        createdAtMillis = timeProvider.nowMillis(),
                    ),
                )

                if (events.isNotEmpty()) {
                    xpEventDao.insertIgnoreAll(events.map { it.toEntity() })
                }
            }
        }
    }
}

private fun XpEventEntity.toDomain(): XpEvent =
    XpEvent(
        id = id,
        type = XpEventType.valueOf(type),
        amount = amount,
        sourceKey = sourceKey,
        createdAtMillis = createdAtMillis,
    )

private fun XpEvent.toEntity(): XpEventEntity =
    XpEventEntity(
        id = id,
        type = type.name,
        amount = amount,
        sourceKey = sourceKey,
        createdAtMillis = createdAtMillis,
    )

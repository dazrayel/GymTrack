package com.gymtrack.data.repository

import androidx.room.withTransaction
import com.gymtrack.data.local.GymTrackDatabase
import com.gymtrack.data.local.dao.WorkoutSessionDao
import com.gymtrack.data.local.dao.WorkoutSessionExerciseDao
import com.gymtrack.data.local.dao.WorkoutSetDao
import com.gymtrack.data.local.entity.WorkoutHistoryRow
import com.gymtrack.data.local.entity.WorkoutSessionEntity
import com.gymtrack.data.local.entity.WorkoutSessionExerciseEntity
import com.gymtrack.data.local.entity.WorkoutSetEntity
import com.gymtrack.domain.model.CompletedSetRecord
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSessionStatus
import com.gymtrack.domain.model.WorkoutSet
import com.gymtrack.domain.model.elapsedMillis
import com.gymtrack.domain.repository.ExerciseRepository
import com.gymtrack.domain.repository.WorkoutRepository
import com.gymtrack.domain.repository.WorkoutSessionRepository
import com.gymtrack.domain.time.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutSessionRepositoryImpl @Inject constructor(
    private val database: GymTrackDatabase,
    private val sessionDao: WorkoutSessionDao,
    private val sessionExerciseDao: WorkoutSessionExerciseDao,
    private val setDao: WorkoutSetDao,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val timeProvider: TimeProvider,
) : WorkoutSessionRepository {

    override suspend fun startSession(workoutId: Long): Long = withContext(Dispatchers.IO) {
        val existing = sessionDao.getInProgressOnce()
        if (existing != null) return@withContext existing.id

        val workout = workoutRepository.getById(workoutId).first()
            ?: throw IllegalArgumentException("Workout not found: $workoutId")
        val template = workoutRepository.getExercises(workoutId).first()
        if (template.isEmpty()) {
            throw IllegalStateException("Workout has no exercises: $workoutId")
        }
        val catalogue = exerciseRepository.getAll().first().associateBy { it.id }

        database.withTransaction {
            val sessionId = sessionDao.insert(
                WorkoutSessionEntity(
                    workoutId = workout.id,
                    workoutName = workout.name,
                    workoutDescription = workout.description,
                    startedAtMillis = timeProvider.nowMillis(),
                    endedAtMillis = null,
                    status = WorkoutSessionEntity.STATUS_IN_PROGRESS,
                    inProgressLock = 1,
                ),
            )
            val snapshots = template.map { we ->
                val exercise = catalogue[we.exerciseId]
                    ?: throw IllegalStateException("Exercise not found: ${we.exerciseId}")
                WorkoutSessionExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = we.exerciseId,
                    position = we.position,
                    exerciseName = exercise.name,
                    muscleGroup = exercise.muscleGroup,
                    equipmentType = exercise.equipmentType,
                    plannedSets = we.sets,
                    minRepetitions = we.minRepetitions,
                    maxRepetitions = we.maxRepetitions,
                    plannedWeight = we.weight,
                    restSeconds = we.restSeconds,
                    notes = we.notes,
                )
            }
            sessionExerciseDao.insertAll(snapshots)
            sessionId
        }
    }

    override suspend fun getSession(id: Long): WorkoutSession? = withContext(Dispatchers.IO) {
        sessionDao.getByIdOnce(id)?.toDomain()
    }

    override fun observeSession(id: Long): Flow<WorkoutSession?> =
        sessionDao.getById(id).map { it?.toDomain() }

    override fun observeInProgress(): Flow<WorkoutSession?> =
        sessionDao.getInProgress().map { it?.toDomain() }

    override fun observeCompletedSessions(): Flow<List<WorkoutHistoryItem>> =
        sessionDao.observeCompleted().map { rows -> rows.map { it.toHistoryItem() } }

    override fun observeCompletedSetHistory(): Flow<List<CompletedSetRecord>> =
        setDao.observeCompletedSetHistory().map { rows ->
            rows.map { row ->
                CompletedSetRecord(
                    sessionId = row.sessionId,
                    occurredAtMillis = row.occurredAtMillis,
                    exerciseName = row.exerciseName,
                    reps = row.reps,
                    weight = row.weight,
                )
            }
        }

    override fun observeSessionExercises(sessionId: Long): Flow<List<WorkoutSessionExercise>> =
        sessionExerciseDao.getBySessionId(sessionId).map { list -> list.map { it.toDomain() } }

    override fun observeSets(sessionExerciseId: Long): Flow<List<WorkoutSet>> =
        setDao.getBySessionExerciseId(sessionExerciseId).map { list -> list.map { it.toDomain() } }

    override suspend fun completeSet(
        sessionExerciseId: Long,
        setIndex: Int,
        reps: Int,
        weight: Double,
    ): Long = withContext(Dispatchers.IO) {
        setDao.insert(
            WorkoutSetEntity(
                sessionExerciseId = sessionExerciseId,
                setIndex = setIndex,
                reps = reps,
                weight = weight,
                completedAtMillis = timeProvider.nowMillis(),
            ),
        )
    }

    override suspend fun startRest(
        sessionId: Long,
        sessionExerciseId: Long,
        afterSetIndex: Int,
        restSeconds: Int,
    ) {
        if (restSeconds <= 0) return
        withContext(Dispatchers.IO) {
            val endsAt = timeProvider.nowMillis() + restSeconds * 1_000L
            val affected = sessionDao.updateRestStarted(
                id = sessionId,
                restEndsAtMillis = endsAt,
                restSessionExerciseId = sessionExerciseId,
                restAfterSetIndex = afterSetIndex,
            )
            if (affected == 0) {
                throw IllegalArgumentException("Session not found: $sessionId")
            }
        }
    }

    override suspend fun pauseRest(sessionId: Long) {
        withContext(Dispatchers.IO) {
            val session = sessionDao.getByIdOnce(sessionId)
                ?: throw IllegalArgumentException("Session not found: $sessionId")
            val endsAt = session.restEndsAtMillis ?: return@withContext
            val remaining = maxOf(0L, endsAt - timeProvider.nowMillis())
            sessionDao.updateRestPaused(sessionId, remaining)
        }
    }

    override suspend fun resumeRest(sessionId: Long) {
        withContext(Dispatchers.IO) {
            val session = sessionDao.getByIdOnce(sessionId)
                ?: throw IllegalArgumentException("Session not found: $sessionId")
            val remaining = session.restPausedRemainingMillis ?: return@withContext
            val endsAt = timeProvider.nowMillis() + remaining
            sessionDao.updateRestResumed(sessionId, endsAt)
        }
    }

    override suspend fun skipRest(sessionId: Long) {
        withContext(Dispatchers.IO) {
            val affected = sessionDao.clearRest(sessionId)
            if (affected == 0) {
                throw IllegalArgumentException("Session not found: $sessionId")
            }
        }
    }

    override suspend fun finishSession(sessionId: Long) {
        withContext(Dispatchers.IO) {
            val session = sessionDao.getByIdOnce(sessionId)
                ?: throw IllegalArgumentException("Session not found: $sessionId")
            if (session.status == WorkoutSessionEntity.STATUS_COMPLETED) {
                sessionDao.clearRest(sessionId)
                return@withContext
            }
            val affected = sessionDao.updateCompletion(
                id = sessionId,
                endedAtMillis = timeProvider.nowMillis(),
                status = WorkoutSessionEntity.STATUS_COMPLETED,
            )
            if (affected == 0) {
                throw IllegalArgumentException("Session not found: $sessionId")
            }
        }
    }
}

private fun WorkoutSessionEntity.toDomain() = WorkoutSession(
    id = id,
    workoutId = workoutId,
    workoutName = workoutName,
    workoutDescription = workoutDescription,
    startedAtMillis = startedAtMillis,
    endedAtMillis = endedAtMillis,
    status = WorkoutSessionStatus.valueOf(status),
    restEndsAtMillis = restEndsAtMillis,
    restPausedRemainingMillis = restPausedRemainingMillis,
    restSessionExerciseId = restSessionExerciseId,
    restAfterSetIndex = restAfterSetIndex,
)

private fun WorkoutSessionExerciseEntity.toDomain() = WorkoutSessionExercise(
    id = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    position = position,
    exerciseName = exerciseName,
    muscleGroup = muscleGroup,
    equipmentType = equipmentType,
    plannedSets = plannedSets,
    minRepetitions = minRepetitions,
    maxRepetitions = maxRepetitions,
    plannedWeight = plannedWeight,
    restSeconds = restSeconds,
    notes = notes,
)

private fun WorkoutSetEntity.toDomain() = WorkoutSet(
    id = id,
    sessionExerciseId = sessionExerciseId,
    setIndex = setIndex,
    reps = reps,
    weight = weight,
    completedAtMillis = completedAtMillis,
)

private fun WorkoutHistoryRow.toHistoryItem(): WorkoutHistoryItem {
    val duration = elapsedMillis(
        session = WorkoutSession(
            id = sessionId,
            workoutName = workoutName,
            startedAtMillis = startedAtMillis,
            endedAtMillis = endedAtMillis,
            status = WorkoutSessionStatus.COMPLETED,
        ),
        nowMillis = endedAtMillis ?: startedAtMillis,
    )
    return WorkoutHistoryItem(
        sessionId = sessionId,
        workoutName = workoutName,
        startedAtMillis = startedAtMillis,
        endedAtMillis = endedAtMillis,
        durationMillis = duration,
        volume = volume,
        exerciseCount = exerciseCount,
        completedSetCount = completedSetCount,
        plannedSetCount = plannedSetCount,
    )
}

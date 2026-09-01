package com.gymtrack.data.repository

import com.gymtrack.data.local.dao.WorkoutDao
import com.gymtrack.data.local.dao.WorkoutExerciseDao
import com.gymtrack.data.local.entity.WorkoutEntity
import com.gymtrack.data.local.entity.WorkoutExerciseEntity
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.repository.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val workoutExerciseDao: WorkoutExerciseDao,
) : WorkoutRepository {

    override fun getAll(): Flow<List<Workout>> =
        workoutDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getById(id: Long): Flow<Workout?> =
        workoutDao.getById(id).map { it?.toDomain() }

    override suspend fun save(workout: Workout): Long = withContext(Dispatchers.IO) {
        workoutDao.insert(workout.toEntity())
    }

    override suspend fun update(workout: Workout) {
        withContext(Dispatchers.IO) { workoutDao.update(workout.toEntity()) }
    }

    override suspend fun delete(workout: Workout) {
        withContext(Dispatchers.IO) { workoutDao.deleteById(workout.id) }
    }

    override fun getExercises(workoutId: Long): Flow<List<WorkoutExercise>> =
        workoutExerciseDao.getByWorkoutId(workoutId).map { list -> list.map { it.toDomain() } }

    override suspend fun addExercise(workoutExercise: WorkoutExercise): Long =
        withContext(Dispatchers.IO) {
            workoutExerciseDao.insert(workoutExercise.toEntity())
        }

    override suspend fun updateExercise(workoutExercise: WorkoutExercise) {
        withContext(Dispatchers.IO) { workoutExerciseDao.insert(workoutExercise.toEntity()) }
    }

    override suspend fun removeExercise(workoutExercise: WorkoutExercise) {
        withContext(Dispatchers.IO) { workoutExerciseDao.deleteById(workoutExercise.id) }
    }

    override suspend fun removeExerciseById(id: Long) {
        withContext(Dispatchers.IO) { workoutExerciseDao.deleteById(id) }
    }

    override suspend fun updateExercisePositions(positions: Map<Long, Int>) {
        withContext(Dispatchers.IO) { workoutExerciseDao.updatePositions(positions) }
    }
}

private fun WorkoutEntity.toDomain() = Workout(
    id = id,
    name = name,
    description = description,
)

private fun Workout.toEntity() = WorkoutEntity(
    id = id,
    name = name,
    description = description,
)

private fun WorkoutExerciseEntity.toDomain() = WorkoutExercise(
    id = id,
    workoutId = workoutId,
    exerciseId = exerciseId,
    position = position,
    sets = sets,
    minRepetitions = minRepetitions,
    maxRepetitions = maxRepetitions,
    weight = weight,
    restSeconds = restSeconds,
    notes = notes,
)

private fun WorkoutExercise.toEntity() = WorkoutExerciseEntity(
    id = id,
    workoutId = workoutId,
    exerciseId = exerciseId,
    position = position,
    sets = sets,
    minRepetitions = minRepetitions,
    maxRepetitions = maxRepetitions,
    weight = weight,
    restSeconds = restSeconds,
    notes = notes,
)

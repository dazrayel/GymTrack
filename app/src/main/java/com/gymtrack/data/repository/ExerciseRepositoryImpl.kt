package com.gymtrack.data.repository

import androidx.room.withTransaction
import com.gymtrack.data.local.GymTrackDatabase
import com.gymtrack.data.local.dao.ExerciseDao
import com.gymtrack.data.local.dao.ExerciseSecondaryMuscleDao
import com.gymtrack.data.local.entity.ExerciseEntity
import com.gymtrack.data.local.entity.ExerciseSecondaryMuscleEntity
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.sanitizedSecondaryMuscles
import com.gymtrack.domain.repository.ExerciseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepositoryImpl @Inject constructor(
    private val database: GymTrackDatabase,
    private val dao: ExerciseDao,
    private val secondaryMuscleDao: ExerciseSecondaryMuscleDao,
) : ExerciseRepository {

    override fun getAll(): Flow<List<Exercise>> =
        combine(dao.getAll(), secondaryMuscleDao.getAll()) { exercises, secondaries ->
            val byExercise = secondaries.groupBy { it.exerciseId }
            exercises.map { entity ->
                entity.toDomain(byExercise[entity.id].orEmpty().map { it.muscle })
            }
        }

    override fun getById(id: Long): Flow<Exercise?> =
        combine(dao.getById(id), secondaryMuscleDao.getMuscles(id)) { entity, muscles ->
            entity?.toDomain(muscles)
        }

    override fun search(query: String): Flow<List<Exercise>> =
        combine(dao.search(query), secondaryMuscleDao.getAll()) { exercises, secondaries ->
            val byExercise = secondaries.groupBy { it.exerciseId }
            exercises.map { entity ->
                entity.toDomain(byExercise[entity.id].orEmpty().map { it.muscle })
            }
        }

    override suspend fun save(exercise: Exercise): Long = withContext(Dispatchers.IO) {
        val secondaries = sanitizedSecondaryMuscles(exercise.muscleGroup, exercise.secondaryMuscles)
        database.withTransaction {
            val id = if (exercise.id == 0L) {
                dao.insert(exercise.copy(secondaryMuscles = secondaries).toEntity())
            } else {
                dao.update(exercise.copy(secondaryMuscles = secondaries).toEntity())
                exercise.id
            }
            secondaryMuscleDao.deleteByExerciseId(id)
            if (secondaries.isNotEmpty()) {
                secondaryMuscleDao.insertAll(
                    secondaries.map { muscle ->
                        ExerciseSecondaryMuscleEntity(exerciseId = id, muscle = muscle)
                    },
                )
            }
            id
        }
    }

    override suspend fun delete(exercise: Exercise) {
        withContext(Dispatchers.IO) { dao.deleteById(exercise.id) }
    }
}

private fun ExerciseEntity.toDomain(secondaryMuscles: List<String>) = Exercise(
    id = id,
    name = name,
    muscleGroup = muscleGroup,
    equipmentType = equipmentType,
    secondaryMuscles = sanitizedSecondaryMuscles(muscleGroup, secondaryMuscles),
    externalSource = externalSource,
    externalId = externalId,
    mediaExternalSource = mediaExternalSource,
    mediaExternalId = mediaExternalId,
)

private fun Exercise.toEntity() = ExerciseEntity(
    id = id,
    name = name,
    muscleGroup = muscleGroup,
    equipmentType = equipmentType,
    externalSource = externalSource,
    externalId = externalId,
    mediaExternalSource = mediaExternalSource,
    mediaExternalId = mediaExternalId,
)

package com.gymtrack.data.repository

import com.gymtrack.data.local.dao.ExerciseDao
import com.gymtrack.data.local.entity.ExerciseEntity
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.repository.ExerciseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepositoryImpl @Inject constructor(
    private val dao: ExerciseDao,
) : ExerciseRepository {

    override fun getAll(): Flow<List<Exercise>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getById(id: Long): Flow<Exercise?> =
        dao.getById(id).map { it?.toDomain() }

    override fun search(query: String): Flow<List<Exercise>> =
        dao.search(query).map { list -> list.map { it.toDomain() } }

    override suspend fun save(exercise: Exercise): Long = withContext(Dispatchers.IO) {
        dao.insert(exercise.toEntity())
    }

    override suspend fun delete(exercise: Exercise) {
        withContext(Dispatchers.IO) { dao.deleteById(exercise.id) }
    }
}

private fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    name = name,
    muscleGroup = muscleGroup,
    equipmentType = equipmentType,
)

private fun Exercise.toEntity() = ExerciseEntity(
    id = id,
    name = name,
    muscleGroup = muscleGroup,
    equipmentType = equipmentType,
)

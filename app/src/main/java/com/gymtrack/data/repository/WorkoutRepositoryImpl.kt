package com.gymtrack.data.repository

import androidx.room.withTransaction
import com.gymtrack.data.local.GymTrackDatabase
import com.gymtrack.data.local.dao.WorkoutBlockDao
import com.gymtrack.data.local.dao.WorkoutDao
import com.gymtrack.data.local.dao.WorkoutExerciseDao
import com.gymtrack.data.local.entity.WorkoutBlockEntity
import com.gymtrack.data.local.entity.WorkoutEntity
import com.gymtrack.data.local.entity.WorkoutExerciseEntity
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutBlock
import com.gymtrack.domain.model.WorkoutBlockType
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
    private val database: GymTrackDatabase,
    private val workoutDao: WorkoutDao,
    private val workoutBlockDao: WorkoutBlockDao,
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

    override fun getBlocks(workoutId: Long): Flow<List<WorkoutBlock>> =
        workoutBlockDao.getByWorkoutId(workoutId).map { list -> list.map { it.toDomain() } }

    override fun getExercisesForWorkout(workoutId: Long): Flow<List<WorkoutExercise>> =
        workoutExerciseDao.getByWorkoutId(workoutId).map { list -> list.map { it.toDomain() } }

    override suspend fun addBlock(block: WorkoutBlock, exercises: List<WorkoutExercise>): Long {
        block.type.validateExerciseCount(exercises.size)
        return withContext(Dispatchers.IO) {
            database.withTransaction {
                // Always append as the next consecutive position so duplicates never occur
                // even if the caller passes a stale position after deletions.
                val nextPosition = workoutBlockDao.getByWorkoutIdOnce(block.workoutId).size
                val blockId = workoutBlockDao.insert(
                    block.copy(id = 0, position = nextPosition).toEntity(),
                )
                exercises.forEachIndexed { index, exercise ->
                    workoutExerciseDao.insert(
                        exercise.copy(id = 0, blockId = blockId, positionInBlock = index).toEntity(),
                    )
                }
                blockId
            }
        }
    }

    override suspend fun duplicateBlock(blockId: Long): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            val original = workoutBlockDao.getByIdOnce(blockId)
                ?: throw IllegalArgumentException("Block not found: $blockId")
            val exercises = workoutExerciseDao.getByBlockIdOnce(blockId)
            WorkoutBlockType.parse(original.type).validateExerciseCount(exercises.size)

            // Shift later blocks upward first (highest position first) so positions stay unique.
            workoutBlockDao.getByWorkoutIdOnce(original.workoutId)
                .filter { it.position > original.position }
                .sortedByDescending { it.position }
                .forEach { block ->
                    workoutBlockDao.updatePosition(block.id, block.position + 1)
                }

            val newBlockId = workoutBlockDao.insert(
                original.copy(
                    id = 0,
                    position = original.position + 1,
                ),
            )
            exercises.forEach { exercise ->
                workoutExerciseDao.insert(
                    exercise.copy(
                        id = 0,
                        blockId = newBlockId,
                    ),
                )
            }
            newBlockId
        }
    }

    override suspend fun updateBlock(block: WorkoutBlock) {
        withContext(Dispatchers.IO) {
            // Use UPDATE instead of INSERT REPLACE: REPLACE deletes the row first and
            // CASCADE would wipe every WorkoutExercise linked to the block.
            workoutBlockDao.update(
                id = block.id,
                workoutId = block.workoutId,
                position = block.position,
                type = block.type.name,
                rounds = block.rounds,
                restSeconds = block.restSeconds,
            )
        }
    }

    override suspend fun updateBlockExercise(exercise: WorkoutExercise) {
        withContext(Dispatchers.IO) { workoutExerciseDao.insert(exercise.toEntity()) }
    }

    override suspend fun replaceBlockExercise(exerciseRowId: Long, newCatalogueExerciseId: Long) {
        withContext(Dispatchers.IO) {
            val existing = workoutExerciseDao.getByIdOnce(exerciseRowId) ?: return@withContext
            workoutExerciseDao.insert(existing.copy(exerciseId = newCatalogueExerciseId))
        }
    }

    override suspend fun removeBlock(blockId: Long) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                val existing = workoutBlockDao.getByIdOnce(blockId) ?: return@withTransaction
                val workoutId = existing.workoutId
                workoutBlockDao.deleteById(blockId)
                // Compact remaining positions to 0..n-1 so the next addBlock appends safely.
                workoutBlockDao.getByWorkoutIdOnce(workoutId).forEachIndexed { index, block ->
                    if (block.position != index) {
                        workoutBlockDao.updatePosition(block.id, index)
                    }
                }
            }
        }
    }

    override suspend fun updateBlockPositions(positions: Map<Long, Int>) {
        withContext(Dispatchers.IO) { workoutBlockDao.updatePositions(positions) }
    }

    override suspend fun addExercise(workoutExercise: WorkoutExercise): Long {
        error("Use addBlock() to create workout exercises")
    }

    override suspend fun updateExercise(workoutExercise: WorkoutExercise) {
        updateBlockExercise(workoutExercise)
    }

    override suspend fun removeExercise(workoutExercise: WorkoutExercise) {
        removeBlock(workoutExercise.blockId)
    }

    override suspend fun removeExerciseById(id: Long) {
        val blockId = withContext(Dispatchers.IO) {
            workoutExerciseDao.getByIdOnce(id)?.blockId
        } ?: return
        removeBlock(blockId)
    }

    override suspend fun updateExercisePositions(positions: Map<Long, Int>) {
        withContext(Dispatchers.IO) {
            val blockPositions = mutableMapOf<Long, Int>()
            positions.forEach { (exerciseId, position) ->
                val entity = workoutExerciseDao.getByIdOnce(exerciseId) ?: return@forEach
                blockPositions[entity.blockId] = position
            }
            workoutBlockDao.updatePositions(blockPositions)
        }
    }
}

private fun WorkoutEntity.toDomain() = Workout(id = id, name = name, description = description)

private fun Workout.toEntity() = WorkoutEntity(id = id, name = name, description = description)

private fun WorkoutBlockEntity.toDomain() = WorkoutBlock(
    id = id,
    workoutId = workoutId,
    position = position,
    type = WorkoutBlockType.parse(type),
    rounds = rounds,
    restSeconds = restSeconds,
)

private fun WorkoutBlock.toEntity() = WorkoutBlockEntity(
    id = id,
    workoutId = workoutId,
    position = position,
    type = type.name,
    rounds = rounds,
    restSeconds = restSeconds,
)

private fun WorkoutExerciseEntity.toDomain() = WorkoutExercise(
    id = id,
    blockId = blockId,
    exerciseId = exerciseId,
    positionInBlock = positionInBlock,
    minRepetitions = minRepetitions,
    maxRepetitions = maxRepetitions,
    weight = weight,
    notes = notes,
)

private fun WorkoutExercise.toEntity() = WorkoutExerciseEntity(
    id = id,
    blockId = blockId,
    exerciseId = exerciseId,
    positionInBlock = positionInBlock,
    minRepetitions = minRepetitions,
    maxRepetitions = maxRepetitions,
    weight = weight,
    notes = notes,
)

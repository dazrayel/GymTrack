package com.gymtrack.domain.repository

import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutBlock
import com.gymtrack.domain.model.WorkoutExercise
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {

    // ─── Workout operations ───────────────────────────────────────────────────

    fun getAll(): Flow<List<Workout>>

    fun getById(id: Long): Flow<Workout?>

    /**
     * Ids of workout templates that currently have at least one exercise
     * (via workout_blocks). Used for next-workout recommendation candidates.
     */
    fun observeWorkoutIdsWithExercises(): Flow<Set<Long>>

    suspend fun save(workout: Workout): Long

    suspend fun update(workout: Workout)

    suspend fun delete(workout: Workout)

    // ─── WorkoutBlock operations ──────────────────────────────────────────────

    fun getBlocks(workoutId: Long): Flow<List<WorkoutBlock>>

    /**
     * Returns all exercises for a workout ordered by block position then
     * positionInBlock, via an inner JOIN through workout_blocks.
     */
    fun getExercisesForWorkout(workoutId: Long): Flow<List<WorkoutExercise>>

    /**
     * Validates the block's type against the exercise count, then inserts the block
     * and all exercises in a single transaction.
     *
     * @return the new block id
     */
    suspend fun addBlock(block: WorkoutBlock, exercises: List<WorkoutExercise>): Long

    /**
     * Creates an independent copy of [blockId] inserted immediately after the original
     * (`position = original.position + 1`), shifting later blocks by +1.
     * New block and exercise row IDs are generated; catalogue [WorkoutExercise.exerciseId]
     * references are preserved.
     *
     * @return the new block id
     */
    suspend fun duplicateBlock(blockId: Long): Long

    suspend fun updateBlock(block: WorkoutBlock)

    suspend fun updateBlockExercise(exercise: WorkoutExercise)

    /**
     * Swaps the catalogue exercise referenced by [exerciseRowId] to [newCatalogueExerciseId].
     */
    suspend fun replaceBlockExercise(exerciseRowId: Long, newCatalogueExerciseId: Long)

    /**
     * Deletes the block and renumbers remaining blocks in the same workout to consecutive
     * positions `0..n-1`. Does not recreate exercises; CASCADE removes child rows.
     */
    suspend fun removeBlock(blockId: Long)

    /**
     * Updates only the [WorkoutBlock.position] of each entry.
     * Keys are WorkoutBlock IDs; values are the new positions.
     * Applied atomically.
     */
    suspend fun updateBlockPositions(positions: Map<Long, Int>)

    // ─── Thin wrappers kept for ViewModel compatibility ───────────────────────

    /** Alias for [getExercisesForWorkout]. */
    fun getExercises(workoutId: Long): Flow<List<WorkoutExercise>> = getExercisesForWorkout(workoutId)

    /** Creates a SINGLE block containing this exercise; returns the block id. */
    suspend fun addExercise(workoutExercise: WorkoutExercise): Long

    /** Updates the exercise parameters. */
    suspend fun updateExercise(workoutExercise: WorkoutExercise)

    suspend fun removeExercise(workoutExercise: WorkoutExercise)

    suspend fun removeExerciseById(id: Long)

    /** Reorders blocks by exercise id (1:1 SINGLE-block assumption). */
    suspend fun updateExercisePositions(positions: Map<Long, Int>)
}

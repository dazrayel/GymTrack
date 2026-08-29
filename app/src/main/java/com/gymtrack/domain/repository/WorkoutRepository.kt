package com.gymtrack.domain.repository

import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutExercise
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {

    // ─── Workout operations ───────────────────────────────────────────────────

    fun getAll(): Flow<List<Workout>>

    fun getById(id: Long): Flow<Workout?>

    suspend fun save(workout: Workout): Long

    suspend fun update(workout: Workout)

    suspend fun delete(workout: Workout)

    // ─── WorkoutExercise operations ───────────────────────────────────────────

    fun getExercises(workoutId: Long): Flow<List<WorkoutExercise>>

    suspend fun addExercise(workoutExercise: WorkoutExercise): Long

    suspend fun updateExercise(workoutExercise: WorkoutExercise)

    suspend fun removeExercise(workoutExercise: WorkoutExercise)

    suspend fun removeExerciseById(id: Long)
}

package com.gymtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymtrack.data.local.entity.WorkoutExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutExerciseDao {

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY position ASC")
    fun getByWorkoutId(workoutId: Long): Flow<List<WorkoutExerciseEntity>>

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    fun getById(id: Long): Flow<WorkoutExerciseEntity?>

    /**
     * Blocking insert/replace — Room 2.6.1 + KSP 2.3.x incompatibility prevents suspend DAO
     * methods with non-Unit return types. Caller must dispatch on Dispatchers.IO.
     * Used for both insert (id=0) and update (id>0) via REPLACE strategy.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(workoutExercise: WorkoutExerciseEntity): Long

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    fun deleteById(id: Long): Int

    @Query("DELETE FROM workout_exercises WHERE workoutId = :workoutId")
    fun deleteByWorkoutId(workoutId: Long): Int
}

package com.gymtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.gymtrack.data.local.entity.WorkoutBlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutBlockDao {

    @Query("SELECT * FROM workout_blocks WHERE workoutId = :workoutId ORDER BY position ASC")
    fun getByWorkoutId(workoutId: Long): Flow<List<WorkoutBlockEntity>>

    @Query("SELECT * FROM workout_blocks WHERE workoutId = :workoutId ORDER BY position ASC")
    fun getByWorkoutIdOnce(workoutId: Long): List<WorkoutBlockEntity>

    @Query("SELECT * FROM workout_blocks WHERE id = :id")
    fun getByIdOnce(id: Long): WorkoutBlockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(block: WorkoutBlockEntity): Long

    @Query("UPDATE workout_blocks SET position = :position WHERE id = :id")
    fun updatePosition(id: Long, position: Int): Int

    @Transaction
    fun updatePositions(positions: Map<Long, Int>) {
        positions.forEach { (id, position) -> updatePosition(id, position) }
    }

    @Query("DELETE FROM workout_blocks WHERE id = :id")
    fun deleteById(id: Long): Int

    @Query("DELETE FROM workout_blocks WHERE workoutId = :workoutId")
    fun deleteByWorkoutId(workoutId: Long): Int
}

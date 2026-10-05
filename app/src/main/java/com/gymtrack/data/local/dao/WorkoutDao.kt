package com.gymtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gymtrack.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Query("SELECT * FROM workouts ORDER BY position ASC")
    fun getAll(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts ORDER BY position ASC")
    fun getAllOnce(): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE id = :id")
    fun getById(id: Long): Flow<WorkoutEntity?>

    /** Next append position: `max(position) + 1`, or `0` when the table is empty. */
    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM workouts")
    fun nextPosition(): Int

    /**
     * Blocking insert — Room 2.6.1 + KSP 2.3.x incompatibility prevents suspend DAO
     * methods with non-Unit return types. Caller must dispatch on Dispatchers.IO.
     *
     * New workouts only (`id = 0`). Do **not** use this to persist edits: SQLite
     * `INSERT OR REPLACE` deletes the previous row first, which cascades to
     * `workout_exercises`.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(workout: WorkoutEntity): Long

    /**
     * Blocking update of name/description/position without replacing the primary key row.
     */
    @Update
    fun update(workout: WorkoutEntity): Int

    @Query("UPDATE workouts SET position = :position WHERE id = :id")
    fun updatePosition(id: Long, position: Int): Int

    @Transaction
    fun updatePositions(positions: Map<Long, Int>) {
        positions.forEach { (id, position) -> updatePosition(id, position) }
    }

    @Query("DELETE FROM workouts WHERE id = :id")
    fun deleteById(id: Long): Int
}

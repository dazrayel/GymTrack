package com.gymtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymtrack.data.local.entity.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Query("SELECT * FROM workouts ORDER BY name ASC")
    fun getAll(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    fun getById(id: Long): Flow<WorkoutEntity?>

    /**
     * Blocking insert/replace — Room 2.6.1 + KSP 2.3.x incompatibility prevents suspend DAO
     * methods with non-Unit return types. Caller must dispatch on Dispatchers.IO.
     * Used for both insert (id=0) and update (id>0) via REPLACE strategy.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(workout: WorkoutEntity): Long

    @Query("DELETE FROM workouts WHERE id = :id")
    fun deleteById(id: Long): Int
}

package com.gymtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gymtrack.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun getById(id: Long): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<ExerciseEntity>>

    /**
     * Blocking insert — Room 2.6.1 + KSP 2.3.x incompatibility prevents suspend DAO methods
     * with non-Unit return types. Caller must dispatch on Dispatchers.IO.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(exercise: ExerciseEntity): Long

    @Update
    fun update(exercise: ExerciseEntity): Int

    @Query("DELETE FROM exercises WHERE id = :id")
    fun deleteById(id: Long): Int
}

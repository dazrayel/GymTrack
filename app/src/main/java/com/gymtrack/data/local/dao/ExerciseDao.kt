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

    @Query(
        "SELECT * FROM exercises WHERE externalSource = :externalSource AND externalId = :externalId LIMIT 1",
    )
    fun getByExternalIdentity(externalSource: String, externalId: String): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM exercises WHERE externalSource = :externalSource")
    fun countByExternalSource(externalSource: String): Int

    /**
     * Blocking insert — Room 2.6.1 + KSP 2.3.x incompatibility prevents suspend DAO methods
     * with non-Unit return types. Caller must dispatch on Dispatchers.IO.
     *
     * REPLACE on PK (or unique-index) conflict deletes the old row then inserts a new one.
     * That can CASCADE `workout_exercises`. Catalog import must use [insertImported] + [updateImported].
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertImported(exercise: ExerciseEntity): Long

    @Update
    fun update(exercise: ExerciseEntity): Int

    @Query(
        "UPDATE exercises SET name = :name, muscleGroup = :muscleGroup, equipmentType = :equipmentType " +
            "WHERE id = :id AND externalSource = :externalSource AND externalId = :externalId",
    )
    fun updateImported(
        id: Long,
        externalSource: String,
        externalId: String,
        name: String,
        muscleGroup: String,
        equipmentType: String,
    ): Int

    @Query("DELETE FROM exercises WHERE id = :id")
    fun deleteById(id: Long): Int
}

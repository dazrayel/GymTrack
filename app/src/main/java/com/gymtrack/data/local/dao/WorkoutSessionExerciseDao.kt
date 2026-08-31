package com.gymtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gymtrack.data.local.entity.WorkoutSessionExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionExerciseDao {

    @Query(
        "SELECT * FROM workout_session_exercises WHERE sessionId = :sessionId ORDER BY position ASC",
    )
    fun getBySessionId(sessionId: Long): Flow<List<WorkoutSessionExerciseEntity>>

    /**
     * Blocking insert — Room 2.6.1 + KSP 2.3.x incompatibility prevents suspend DAO
     * methods with non-Unit return types. Caller must dispatch on Dispatchers.IO.
     */
    @Insert
    fun insert(sessionExercise: WorkoutSessionExerciseEntity): Long

    @Insert
    fun insertAll(sessionExercises: List<WorkoutSessionExerciseEntity>): List<Long>

    @Query("SELECT * FROM workout_session_exercises WHERE id = :id")
    fun getByIdOnce(id: Long): WorkoutSessionExerciseEntity?

    @Query("UPDATE workout_session_exercises SET status = :status WHERE id = :id")
    fun updateStatus(id: Long, status: String): Int

    @Query(
        "UPDATE workout_session_exercises SET status = :pending " +
            "WHERE sessionId = :sessionId AND status = :inProgress",
    )
    fun clearInProgress(sessionId: Long, pending: String, inProgress: String): Int

    @Query("DELETE FROM workout_session_exercises WHERE id = :id")
    fun deleteById(id: Long): Int
}

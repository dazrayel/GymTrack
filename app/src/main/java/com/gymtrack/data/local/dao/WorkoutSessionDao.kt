package com.gymtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gymtrack.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun getById(id: Long): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    fun getByIdOnce(id: Long): WorkoutSessionEntity?

    @Query(
        "SELECT * FROM workout_sessions WHERE status = :status LIMIT 1",
    )
    fun getInProgress(
        status: String = WorkoutSessionEntity.STATUS_IN_PROGRESS,
    ): Flow<WorkoutSessionEntity?>

    @Query(
        "SELECT * FROM workout_sessions WHERE status = :status LIMIT 1",
    )
    fun getInProgressOnce(
        status: String = WorkoutSessionEntity.STATUS_IN_PROGRESS,
    ): WorkoutSessionEntity?

    /**
     * Blocking insert — Room 2.6.1 + KSP 2.3.x incompatibility prevents suspend DAO
     * methods with non-Unit return types. Caller must dispatch on Dispatchers.IO.
     */
    @Insert
    fun insert(session: WorkoutSessionEntity): Long

    /**
     * Blocking update of completion fields only — same Room/KSP constraint as [insert].
     */
    @Query(
        "UPDATE workout_sessions SET endedAtMillis = :endedAtMillis, status = :status, " +
            "restEndsAtMillis = NULL, restPausedRemainingMillis = NULL, " +
            "restSessionExerciseId = NULL, restAfterSetIndex = NULL WHERE id = :id",
    )
    fun updateCompletion(id: Long, endedAtMillis: Long?, status: String): Int

    @Query(
        "UPDATE workout_sessions SET " +
            "restEndsAtMillis = :restEndsAtMillis, " +
            "restPausedRemainingMillis = NULL, " +
            "restSessionExerciseId = :restSessionExerciseId, " +
            "restAfterSetIndex = :restAfterSetIndex " +
            "WHERE id = :id",
    )
    fun updateRestStarted(
        id: Long,
        restEndsAtMillis: Long,
        restSessionExerciseId: Long,
        restAfterSetIndex: Int,
    ): Int

    @Query(
        "UPDATE workout_sessions SET restEndsAtMillis = NULL, restPausedRemainingMillis = :remainingMillis " +
            "WHERE id = :id",
    )
    fun updateRestPaused(id: Long, remainingMillis: Long): Int

    @Query(
        "UPDATE workout_sessions SET restEndsAtMillis = :restEndsAtMillis, restPausedRemainingMillis = NULL " +
            "WHERE id = :id",
    )
    fun updateRestResumed(id: Long, restEndsAtMillis: Long): Int

    @Query(
        "UPDATE workout_sessions SET restEndsAtMillis = NULL, restPausedRemainingMillis = NULL, " +
            "restSessionExerciseId = NULL, restAfterSetIndex = NULL WHERE id = :id",
    )
    fun clearRest(id: Long): Int

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    fun deleteById(id: Long): Int
}

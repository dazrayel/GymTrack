package com.gymtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gymtrack.data.local.entity.CompletedSetHistoryRow
import com.gymtrack.data.local.entity.WorkoutSessionEntity
import com.gymtrack.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSetDao {

    @Query(
        "SELECT * FROM workout_sets WHERE sessionExerciseId = :sessionExerciseId ORDER BY setIndex ASC",
    )
    fun getBySessionExerciseId(sessionExerciseId: Long): Flow<List<WorkoutSetEntity>>

    @Query(
        """
        SELECT workout_sets.* FROM workout_sets
        INNER JOIN workout_session_exercises
            ON workout_sets.sessionExerciseId = workout_session_exercises.id
        WHERE workout_session_exercises.sessionId = :sessionId
        ORDER BY workout_session_exercises.position ASC, workout_sets.setIndex ASC
        """,
    )
    fun getBySessionId(sessionId: Long): Flow<List<WorkoutSetEntity>>

    @Query(
        """
        SELECT
            s.id AS sessionId,
            COALESCE(s.endedAtMillis, s.startedAtMillis) AS occurredAtMillis,
            se.exerciseName AS exerciseName,
            ws.reps AS reps,
            ws.weight AS weight
        FROM workout_sets AS ws
        INNER JOIN workout_session_exercises AS se
            ON ws.sessionExerciseId = se.id
        INNER JOIN workout_sessions AS s
            ON se.sessionId = s.id
        WHERE s.status = :status
        """,
    )
    fun observeCompletedSetHistory(
        status: String = WorkoutSessionEntity.STATUS_COMPLETED,
    ): Flow<List<CompletedSetHistoryRow>>

    /**
     * Blocking insert — Room 2.6.1 + KSP 2.3.x incompatibility prevents suspend DAO
     * methods with non-Unit return types. Caller must dispatch on Dispatchers.IO.
     * A row represents a completed set; uniqueness of (sessionExerciseId, setIndex) is enforced by Room.
     */
    @Insert
    fun insert(set: WorkoutSetEntity): Long

    @Query("SELECT COUNT(*) FROM workout_sets WHERE sessionExerciseId = :sessionExerciseId")
    fun countBySessionExerciseId(sessionExerciseId: Long): Int
}

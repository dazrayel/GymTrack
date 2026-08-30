package com.gymtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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

    /**
     * Blocking insert — Room 2.6.1 + KSP 2.3.x incompatibility prevents suspend DAO
     * methods with non-Unit return types. Caller must dispatch on Dispatchers.IO.
     * A row represents a completed set; uniqueness of (sessionExerciseId, setIndex) is enforced by Room.
     */
    @Insert
    fun insert(set: WorkoutSetEntity): Long
}

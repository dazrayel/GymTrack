package com.gymtrack.domain.repository

import com.gymtrack.domain.model.CompletedSetRecord
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow

interface WorkoutSessionRepository {

    /**
     * Creates a session snapshot from the current workout template, or returns the
     * existing [IN_PROGRESS][com.gymtrack.domain.model.WorkoutSessionStatus.IN_PROGRESS]
     * session id if one is already active.
     */
    suspend fun startSession(workoutId: Long): Long

    suspend fun getSession(id: Long): WorkoutSession?

    fun observeSession(id: Long): Flow<WorkoutSession?>

    fun observeInProgress(): Flow<WorkoutSession?>

    fun observeCompletedSessions(): Flow<List<WorkoutHistoryItem>>

    fun observeCompletedSetHistory(): Flow<List<CompletedSetRecord>>

    fun observeSessionExercises(sessionId: Long): Flow<List<WorkoutSessionExercise>>

    fun observeSets(sessionExerciseId: Long): Flow<List<WorkoutSet>>

    suspend fun completeSet(
        sessionExerciseId: Long,
        setIndex: Int,
        reps: Int,
        weight: Double,
    ): Long

    suspend fun startRest(
        sessionId: Long,
        sessionExerciseId: Long,
        afterSetIndex: Int,
        restSeconds: Int,
    )

    suspend fun pauseRest(sessionId: Long)

    suspend fun resumeRest(sessionId: Long)

    suspend fun skipRest(sessionId: Long)

    suspend fun finishSession(sessionId: Long)
}

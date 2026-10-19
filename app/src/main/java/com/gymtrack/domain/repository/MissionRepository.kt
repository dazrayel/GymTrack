package com.gymtrack.domain.repository

import com.gymtrack.domain.model.MissionProgressView
import kotlinx.coroutines.flow.Flow

interface MissionRepository {

    fun observeMissions(): Flow<List<MissionProgressView>>

    /**
     * Ensures current-period instances exist and completes any missions that meet their target.
     * Idempotent. Safe after repeated [finishSession] calls.
     */
    suspend fun evaluateAfterSession(sessionId: Long)

    /** Ensures instances for the current day/week/one-time without requiring a session. */
    suspend fun ensureCurrentInstances()
}

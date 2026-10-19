package com.gymtrack.domain.repository

import com.gymtrack.domain.model.LevelProgress
import com.gymtrack.domain.model.XpEvent
import kotlinx.coroutines.flow.Flow

interface GamificationRepository {

    fun observeTotalXp(): Flow<Long>

    fun observeLevelProgress(): Flow<LevelProgress>

    fun observeXpEvents(): Flow<List<XpEvent>>

    /**
     * Awards Phase 5.2 XP for a session that just transitioned to COMPLETED.
     * Idempotent via unique [XpEvent.sourceKey]. Safe to call more than once.
     */
    suspend fun awardXpForCompletedSession(sessionId: Long)
}

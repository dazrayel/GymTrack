package com.gymtrack.domain.repository

import com.gymtrack.domain.model.LevelProgress
import com.gymtrack.domain.model.XpEvent
import com.gymtrack.domain.model.levelProgressFromTotalXp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/** In-memory gamification stub for unit tests. */
class FakeGamificationRepository(
    initialTotalXp: Long = 0L,
) : GamificationRepository {

    private val totalXp = MutableStateFlow(initialTotalXp)
    private val events = MutableStateFlow<List<XpEvent>>(emptyList())
    var awardCalls: Int = 0
        private set

    override fun observeTotalXp(): Flow<Long> = totalXp.asStateFlow()

    override fun observeLevelProgress(): Flow<LevelProgress> =
        totalXp.map { levelProgressFromTotalXp(it) }

    override fun observeXpEvents(): Flow<List<XpEvent>> = events.asStateFlow()

    override suspend fun awardXpForCompletedSession(sessionId: Long) {
        awardCalls++
    }

    fun setTotalXp(value: Long) {
        totalXp.value = value
    }

    fun setXpEvents(value: List<XpEvent>) {
        events.value = value
    }
}

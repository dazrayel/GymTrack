package com.gymtrack.domain.repository

import com.gymtrack.domain.model.MissionProgressView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeMissionRepository : MissionRepository {
    private val missions = MutableStateFlow<List<MissionProgressView>>(emptyList())
    var evaluateCalls: Int = 0
        private set

    override fun observeMissions(): Flow<List<MissionProgressView>> = missions.asStateFlow()

    override suspend fun evaluateAfterSession(sessionId: Long) {
        evaluateCalls++
    }

    override suspend fun ensureCurrentInstances() = Unit

    fun setMissions(value: List<MissionProgressView>) {
        missions.value = value
    }
}

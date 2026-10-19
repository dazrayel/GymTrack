package com.gymtrack.presentation.rpg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.domain.model.AchievementCatalog
import com.gymtrack.domain.model.MissionStatus
import com.gymtrack.domain.model.evaluateAchievements
import com.gymtrack.domain.model.evaluateTitles
import com.gymtrack.domain.model.historicalPersonalRecords
import com.gymtrack.domain.model.resolveActiveTitleId
import com.gymtrack.domain.repository.ActiveTitleRepository
import com.gymtrack.domain.repository.GamificationRepository
import com.gymtrack.domain.repository.MissionRepository
import com.gymtrack.domain.repository.WorkoutSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class RpgProfileViewModel @Inject constructor(
    private val gamificationRepository: GamificationRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val missionRepository: MissionRepository,
    private val activeTitleRepository: ActiveTitleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RpgProfileUiState())
    val uiState: StateFlow<RpgProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            missionRepository.ensureCurrentInstances()
        }
        observeProfile()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            combine(
                gamificationRepository.observeLevelProgress(),
                sessionRepository.observeCompletedSessions(),
                sessionRepository.observeCompletedSetHistory(),
                missionRepository.observeMissions(),
                activeTitleRepository.activeTitleId,
            ) { levelProgress, sessions, sets, missions, preferredTitleId ->
                ProfileEmission(
                    levelProgress = levelProgress,
                    sessions = sessions,
                    sets = sets,
                    missions = missions,
                    preferredTitleId = preferredTitleId,
                )
            }
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message)
                    }
                }
                .collect { emission ->
                    val achievements = evaluateAchievements(
                        sessions = emission.sessions,
                        completedSets = emission.sets,
                        zoneId = ZoneId.systemDefault(),
                    )
                    val titles = evaluateTitles(achievements, emission.levelProgress)
                    val resolvedActiveId = resolveActiveTitleId(
                        preferredId = emission.preferredTitleId,
                        titles = titles,
                    )
                    if (emission.preferredTitleId != null && resolvedActiveId == null) {
                        // Preference is stale after revocation — clear without picking another.
                        activeTitleRepository.setActiveTitleId(null)
                    }
                    val completedMissions = emission.missions.count {
                        it.instance.status == MissionStatus.COMPLETED
                    }
                    val activeMissions = emission.missions.count {
                        it.instance.status == MissionStatus.ACTIVE
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            levelProgress = emission.levelProgress,
                            titles = titles,
                            activeTitleId = resolvedActiveId,
                            unlockedAchievementCount = achievements.count { status -> status.unlocked },
                            totalAchievementCount = AchievementCatalog.size,
                            activeMissionCount = activeMissions,
                            completedMissionCount = completedMissions,
                            personalRecordCount = historicalPersonalRecords(emission.sets).size,
                            completedWorkoutCount = emission.sessions.size,
                        )
                    }
                }
        }
    }

    fun selectTitle(titleId: String) {
        viewModelScope.launch {
            val current = _uiState.value
            val unlocked = current.titles.any {
                it.definition.id == titleId && it.unlocked
            }
            if (!unlocked) return@launch
            activeTitleRepository.setActiveTitleId(titleId)
        }
    }

    fun clearActiveTitle() {
        viewModelScope.launch {
            activeTitleRepository.setActiveTitleId(null)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private data class ProfileEmission(
        val levelProgress: com.gymtrack.domain.model.LevelProgress,
        val sessions: List<com.gymtrack.domain.model.WorkoutHistoryItem>,
        val sets: List<com.gymtrack.domain.model.CompletedSetRecord>,
        val missions: List<com.gymtrack.domain.model.MissionProgressView>,
        val preferredTitleId: String?,
    )
}

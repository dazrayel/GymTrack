package com.gymtrack.presentation.rpg

import com.gymtrack.domain.model.LevelProgress
import com.gymtrack.domain.model.TitleStatus

data class RpgProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val levelProgress: LevelProgress? = null,
    val titles: List<TitleStatus> = emptyList(),
    /** Validated active title id; null means "Sem título". */
    val activeTitleId: String? = null,
    val unlockedAchievementCount: Int = 0,
    val totalAchievementCount: Int = 0,
    val activeMissionCount: Int = 0,
    val completedMissionCount: Int = 0,
    val personalRecordCount: Int = 0,
    val completedWorkoutCount: Int = 0,
)

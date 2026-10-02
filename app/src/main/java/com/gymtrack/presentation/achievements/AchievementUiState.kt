package com.gymtrack.presentation.achievements

import com.gymtrack.domain.model.AchievementStatus

data class AchievementUiState(
    val achievements: List<AchievementStatus> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

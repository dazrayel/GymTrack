package com.gymtrack.presentation.missions

import com.gymtrack.domain.model.MissionProgressView

data class MissionUiState(
    val missions: List<MissionProgressView> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

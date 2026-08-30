package com.gymtrack.presentation.stats

import com.gymtrack.domain.model.ExercisePerformancePoint
import com.gymtrack.domain.model.ExercisePersonalRecords

data class ExerciseStatsUiState(
    val exerciseName: String,
    val performanceHistory: List<ExercisePerformancePoint> = emptyList(),
    val personalRecord: ExercisePersonalRecords? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val showEmpty: Boolean
        get() = !isLoading && performanceHistory.isEmpty() && error == null
}

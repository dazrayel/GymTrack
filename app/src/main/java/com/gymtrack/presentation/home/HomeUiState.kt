package com.gymtrack.presentation.home

import com.gymtrack.domain.model.AchievementCatalog
import com.gymtrack.domain.model.DailyVolume
import com.gymtrack.domain.model.DashboardPeriod
import com.gymtrack.domain.model.ExercisePersonalRecords
import com.gymtrack.domain.model.PeriodDashboardStats
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.model.WorkoutSession

data class HomeUiState(
    val recentWorkout: WorkoutHistoryItem? = null,
    val nextWorkout: Workout? = null,
    val inProgressSession: WorkoutSession? = null,
    val selectedPeriod: DashboardPeriod = DashboardPeriod.WEEK,
    val periodStats: PeriodDashboardStats = PeriodDashboardStats.Empty,
    val trainedDayCount: Int = 0,
    val dailyVolumeTrend: List<DailyVolume> = emptyList(),
    val records: List<ExercisePersonalRecords> = emptyList(),
    val unlockedAchievementCount: Int = 0,
    val totalAchievementCount: Int = AchievementCatalog.size,
    val isLoading: Boolean = true,
    val error: String? = null,
    val userDisplayName: String? = null,
) {
    val showEmpty: Boolean
        get() = !isLoading && recentWorkout == null
}

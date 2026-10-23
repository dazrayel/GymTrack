package com.gymtrack.presentation.home

import com.gymtrack.domain.model.AchievementCatalog
import com.gymtrack.domain.model.DashboardPeriod
import com.gymtrack.domain.model.ExercisePersonalRecords
import com.gymtrack.domain.model.HomeActivityDay
import com.gymtrack.domain.model.LevelProgress
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
    /** Distinct trained days in the selected dashboard period (frequency card). */
    val trainedDayCount: Int = 0,
    /**
     * Current workout streak for the Home fire chip: distinct trained days in the
     * contiguous streak, allowing up to two complete idle days without breaking.
     */
    val streakDays: Int = 0,
    val activityWeekDays: List<HomeActivityDay> = emptyList(),
    val records: List<ExercisePersonalRecords> = emptyList(),
    val levelProgress: LevelProgress? = null,
    val activeMissionCount: Int = 0,
    val missionHighlightTitleRes: Int? = null,
    val missionHighlightProgress: Int = 0,
    val missionHighlightTarget: Int = 0,
    val unlockedAchievementCount: Int = 0,
    val totalAchievementCount: Int = AchievementCatalog.size,
    val isLoading: Boolean = true,
    val error: String? = null,
    val userDisplayName: String? = null,
) {
    val showEmpty: Boolean
        get() = !isLoading && recentWorkout == null
}

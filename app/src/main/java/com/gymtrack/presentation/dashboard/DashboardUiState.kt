package com.gymtrack.presentation.dashboard

import com.gymtrack.domain.model.AchievementCatalog
import com.gymtrack.domain.model.ExercisePersonalRecords
import com.gymtrack.domain.model.MetricChange
import com.gymtrack.domain.model.Workout

data class DashboardUiState(
    val displayName: String? = null,
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val error: String? = null,
    val weeklySessionCount: Int = 0,
    val weeklySetCount: Int = 0,
    val weeklyVolume: Double = 0.0,
    val hasPreviousWeekData: Boolean = false,
    val sessionChange: MetricChange? = null,
    val setChange: MetricChange? = null,
    val volumeChange: MetricChange? = null,
    val trainedDaysLast30: Int = 0,
    val volumeLast30: Double = 0.0,
    val hasPreviousVolumePeriod: Boolean = false,
    val volumePeriodChange: MetricChange? = null,
    val highlightRecord: ExercisePersonalRecords? = null,
    val unlockedAchievementCount: Int = 0,
    val totalAchievementCount: Int = AchievementCatalog.size,
    val nextWorkout: Workout? = null,
    val hasWeeklyPlan: Boolean = false,
    val todayPlanWorkoutName: String? = null,
    val tomorrowPlanWorkoutName: String? = null,
)

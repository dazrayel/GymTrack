package com.gymtrack.presentation.planning

import com.gymtrack.domain.model.WeeklyPlanDaySlot
import com.gymtrack.domain.model.Workout
import java.time.DayOfWeek

data class WeeklyPlanningUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val days: List<WeeklyPlanDaySlot> = emptyList(),
    val availableWorkouts: List<Workout> = emptyList(),
    val plannedDayCount: Int = 0,
    val pickerDay: DayOfWeek? = null,
    val dayActionsDay: DayOfWeek? = null,
) {
    val hasWorkouts: Boolean get() = availableWorkouts.isNotEmpty()
    val isPickerOpen: Boolean get() = pickerDay != null
    val isDayActionsOpen: Boolean get() = dayActionsDay != null
}

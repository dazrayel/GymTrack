package com.gymtrack.domain.model

import java.time.DayOfWeek

/**
 * A planned workout template assigned to a weekday in the recurring weekly routine.
 * Independent of [WorkoutSession] history.
 */
data class WeeklyWorkoutPlan(
    val dayOfWeek: DayOfWeek,
    val workoutId: Long,
    val workoutName: String,
)

/**
 * UI-facing slot for one weekday in the standard weekly routine.
 */
data class WeeklyPlanDaySlot(
    val dayOfWeek: DayOfWeek,
    val plannedWorkoutId: Long? = null,
    val plannedWorkoutName: String? = null,
) {
    val hasPlan: Boolean get() = plannedWorkoutId != null
}

/**
 * Builds the seven weekday slots ordered from [firstDayOfWeek], merging assigned plans.
 */
fun buildWeeklyPlanSlots(
    plans: List<WeeklyWorkoutPlan>,
    firstDayOfWeek: DayOfWeek,
): List<WeeklyPlanDaySlot> {
    val byDay = plans.associateBy { it.dayOfWeek }
    return (0 until 7).map { offset ->
        val day = firstDayOfWeek.plus(offset.toLong())
        val plan = byDay[day]
        WeeklyPlanDaySlot(
            dayOfWeek = day,
            plannedWorkoutId = plan?.workoutId,
            plannedWorkoutName = plan?.workoutName,
        )
    }
}

package com.gymtrack.data.local.entity

/**
 * Joined row for observing weekly plans with workout names.
 */
data class WeeklyWorkoutPlanRow(
    val dayOfWeek: Int,
    val workoutId: Long,
    val workoutName: String,
)

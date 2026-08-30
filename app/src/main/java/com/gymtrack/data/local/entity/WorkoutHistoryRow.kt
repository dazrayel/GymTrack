package com.gymtrack.data.local.entity

/**
 * Aggregated row for the completed-session history list.
 * Not a Room entity — mapped from a single SQL query to avoid N+1 loads.
 */
data class WorkoutHistoryRow(
    val sessionId: Long,
    val workoutName: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val volume: Double,
    val exerciseCount: Int,
    val completedSetCount: Int,
    val plannedSetCount: Int,
)

package com.gymtrack.domain.model

data class WorkoutHistoryItem(
    val sessionId: Long,
    val workoutName: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val durationMillis: Long,
    val volume: Double,
    val exerciseCount: Int,
    val completedSetCount: Int,
    val plannedSetCount: Int,
)

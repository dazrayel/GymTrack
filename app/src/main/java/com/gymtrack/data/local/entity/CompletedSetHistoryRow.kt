package com.gymtrack.data.local.entity

data class CompletedSetHistoryRow(
    val sessionId: Long,
    val occurredAtMillis: Long,
    val exerciseName: String,
    val reps: Int,
    val weight: Double,
)

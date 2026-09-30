package com.gymtrack.domain.model

data class WorkoutBlock(
    val id: Long = 0,
    val workoutId: Long,
    val position: Int,
    val type: WorkoutBlockType,
    val rounds: Int,
    val restSeconds: Int,
)

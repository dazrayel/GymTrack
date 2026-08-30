package com.gymtrack.domain.model

data class WorkoutSet(
    val id: Long = 0,
    val sessionExerciseId: Long,
    val setIndex: Int,
    val reps: Int,
    val weight: Double,
    val completedAtMillis: Long,
)

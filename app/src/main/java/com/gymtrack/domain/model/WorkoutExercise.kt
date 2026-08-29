package com.gymtrack.domain.model

data class WorkoutExercise(
    val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val position: Int,
    val sets: Int,
    val minRepetitions: Int,
    val maxRepetitions: Int,
    val weight: Double,
    val restSeconds: Int,
    val notes: String = "",
)

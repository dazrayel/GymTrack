package com.gymtrack.domain.model

data class WorkoutSession(
    val id: Long = 0,
    val workoutId: Long? = null,
    val workoutName: String,
    val workoutDescription: String = "",
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val status: WorkoutSessionStatus,
    val restEndsAtMillis: Long? = null,
    val restPausedRemainingMillis: Long? = null,
    val restSessionExerciseId: Long? = null,
    val restAfterSetIndex: Int? = null,
)

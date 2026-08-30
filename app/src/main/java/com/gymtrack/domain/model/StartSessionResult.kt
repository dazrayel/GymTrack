package com.gymtrack.domain.model

sealed interface StartSessionResult {

    data class Created(val sessionId: Long) : StartSessionResult

    data class Resumed(val sessionId: Long) : StartSessionResult

    data class BlockedOtherWorkout(
        val sessionId: Long,
        val workoutName: String,
    ) : StartSessionResult
}

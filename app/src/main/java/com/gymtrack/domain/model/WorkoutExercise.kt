package com.gymtrack.domain.model

/**
 * An exercise slot inside a [WorkoutBlock].
 * Round/set count and rest live on the parent block so bi/tri-sets share them.
 */
data class WorkoutExercise(
    val id: Long = 0,
    val blockId: Long,
    val exerciseId: Long,
    val positionInBlock: Int,
    val minRepetitions: Int,
    val maxRepetitions: Int,
    val weight: Double,
    val notes: String = "",
)

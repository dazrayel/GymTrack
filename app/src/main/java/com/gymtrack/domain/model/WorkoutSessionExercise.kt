package com.gymtrack.domain.model

data class WorkoutSessionExercise(
    val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long? = null,
    val position: Int,
    val exerciseName: String,
    val muscleGroup: String,
    val equipmentType: String,
    val plannedSets: Int,
    val minRepetitions: Int,
    val maxRepetitions: Int,
    val plannedWeight: Double,
    val restSeconds: Int,
    val notes: String = "",
    val secondaryMuscles: List<String> = emptyList(),
    val status: WorkoutSessionExerciseStatus = WorkoutSessionExerciseStatus.PENDING,
    val blockType: WorkoutBlockType = WorkoutBlockType.SINGLE,
    val blockPosition: Int = 0,
    val positionInBlock: Int = 0,
)

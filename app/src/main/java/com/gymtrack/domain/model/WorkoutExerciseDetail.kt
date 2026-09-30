package com.gymtrack.domain.model

data class WorkoutExerciseDetail(
    val workoutExercise: WorkoutExercise,
    val exercise: Exercise,
) {
    val id: Long get() = workoutExercise.id
    val blockId: Long get() = workoutExercise.blockId
    val positionInBlock: Int get() = workoutExercise.positionInBlock
    val minRepetitions: Int get() = workoutExercise.minRepetitions
    val maxRepetitions: Int get() = workoutExercise.maxRepetitions
    val weight: Double get() = workoutExercise.weight
    val notes: String get() = workoutExercise.notes
}

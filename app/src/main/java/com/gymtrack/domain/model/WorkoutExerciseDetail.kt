package com.gymtrack.domain.model

/**
 * Projection that combines a [WorkoutExercise] (join record) with the full
 * [Exercise] it references. Built in the ViewModel by joining two Flows in
 * memory — no new Room query required.
 */
data class WorkoutExerciseDetail(
    val workoutExercise: WorkoutExercise,
    val exercise: Exercise,
) {
    val id: Long get() = workoutExercise.id
    val position: Int get() = workoutExercise.position
    val sets: Int get() = workoutExercise.sets
    val minRepetitions: Int get() = workoutExercise.minRepetitions
    val maxRepetitions: Int get() = workoutExercise.maxRepetitions
    val weight: Double get() = workoutExercise.weight
    val restSeconds: Int get() = workoutExercise.restSeconds
    val notes: String get() = workoutExercise.notes
}

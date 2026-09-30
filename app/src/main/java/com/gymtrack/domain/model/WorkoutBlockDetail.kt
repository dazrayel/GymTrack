package com.gymtrack.domain.model

data class WorkoutBlockDetail(
    val block: WorkoutBlock,
    val items: List<WorkoutExerciseDetail>,
) {
    val id: Long get() = block.id
    val position: Int get() = block.position
    val type: WorkoutBlockType get() = block.type
    val rounds: Int get() = block.rounds
    val restSeconds: Int get() = block.restSeconds
}

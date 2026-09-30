package com.gymtrack.domain.model

data class Exercise(
    val id: Long = 0,
    val name: String,
    val muscleGroup: String,
    val equipmentType: String,
    val secondaryMuscles: List<String> = emptyList(),
    val externalSource: String? = null,
    val externalId: String? = null,
)

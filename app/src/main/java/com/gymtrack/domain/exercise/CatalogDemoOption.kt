package com.gymtrack.domain.exercise

/**
 * A catalog entry that can be used as demonstration media for a manual exercise.
 * Identity of the user's exercise is never changed when selecting this option.
 */
data class CatalogDemoOption(
    val mediaExternalSource: String,
    val mediaExternalId: String,
    val displayName: String,
    val muscleGroup: String,
)

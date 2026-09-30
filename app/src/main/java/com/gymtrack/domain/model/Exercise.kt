package com.gymtrack.domain.model

data class Exercise(
    val id: Long = 0,
    val name: String,
    val muscleGroup: String,
    val equipmentType: String,
    val secondaryMuscles: List<String> = emptyList(),
    val externalSource: String? = null,
    val externalId: String? = null,
    /** Optional catalog media link; independent of [externalSource]/[externalId] identity. */
    val mediaExternalSource: String? = null,
    val mediaExternalId: String? = null,
) {
    val hasCatalogIdentity: Boolean
        get() = !externalSource.isNullOrBlank() && !externalId.isNullOrBlank()

    val hasSelectedMedia: Boolean
        get() = !mediaExternalSource.isNullOrBlank() && !mediaExternalId.isNullOrBlank()
}

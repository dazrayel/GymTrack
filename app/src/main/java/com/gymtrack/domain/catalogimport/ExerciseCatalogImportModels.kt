package com.gymtrack.domain.catalogimport

class ExerciseCatalogPackException(message: String) : IllegalArgumentException(message)

data class NormalizedCatalogRecord(
    val source: String?,
    val externalId: String?,
    val name: String?,
    val muscleGroup: String?,
    val equipmentType: String?,
    val secondaryMuscles: List<String>,
    val sourceEquipment: String?,
    val sourcePrimaryMuscles: List<String>,
)

data class ImportableCatalogExercise(
    val externalSource: String,
    val externalId: String,
    val name: String,
    val muscleGroup: String,
    val equipmentType: String,
    val secondaryMuscles: List<String>,
)

data class RejectedCatalogRecord(
    val externalSource: String?,
    val externalId: String?,
    val reason: String,
)

data class ExerciseCatalogImportResult(
    val source: String,
    val inputCount: Int,
    val importableCount: Int,
    val inserted: Int,
    val updated: Int,
    val unchanged: Int,
    val rejected: Int,
    val errors: Int,
    val rejections: List<RejectedCatalogRecord>,
) {
    val importedCount: Int get() = inserted + updated + unchanged

    fun summary(): String = buildString {
        appendLine("Source: $source")
        appendLine("Input: $inputCount")
        appendLine("Importable: $importableCount")
        appendLine("Rejected: $rejected")
        appendLine("Inserted: $inserted")
        appendLine("Updated: $updated")
        appendLine("Unchanged: $unchanged")
        appendLine("Errors: $errors")
    }
}

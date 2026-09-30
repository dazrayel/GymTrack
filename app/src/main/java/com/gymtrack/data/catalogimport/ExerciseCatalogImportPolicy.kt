package com.gymtrack.data.catalogimport

import com.gymtrack.domain.catalogimport.ExerciseCatalogPackException
import com.gymtrack.domain.catalogimport.ImportableCatalogExercise
import com.gymtrack.domain.catalogimport.NormalizedCatalogRecord
import com.gymtrack.domain.catalogimport.RejectedCatalogRecord
import com.gymtrack.domain.model.EQUIPMENT_TYPES
import com.gymtrack.domain.model.MUSCLE_GROUPS
import com.gymtrack.domain.model.sanitizedSecondaryMuscles

object ExerciseCatalogImportPolicy {

    const val SOURCE_FREE_EXERCISE_DB = "free-exercise-db"
    const val SOURCE_WGER = "wger"

    val SUPPORTED_SOURCES: Set<String> = setOf(SOURCE_FREE_EXERCISE_DB, SOURCE_WGER)

    private val UNMAPPED_PRIMARY = setOf("neck", "abductors", "adductors")
    private val EQUIPMENT_FALLBACK_OUTRO = setOf("medicine ball", "exercise ball", "foam roll")
    private val EQUIPMENT_FALLBACK_BARRA = setOf("e-z curl bar")

    data class Classification(
        val importable: List<ImportableCatalogExercise>,
        val rejected: List<RejectedCatalogRecord>,
    )

    fun validatePack(records: List<NormalizedCatalogRecord>) {
        val seen = HashSet<Pair<String, String>>()
        records.forEachIndexed { index, record ->
            val source = record.source?.trim().orEmpty()
            val externalId = record.externalId?.trim().orEmpty()
            if (source.isNotEmpty() && source !in SUPPORTED_SOURCES) {
                throw ExerciseCatalogPackException(
                    "Unsupported catalog source '$source' at index $index",
                )
            }
            if (source.isNotEmpty() && externalId.isNotEmpty()) {
                val key = source to externalId
                if (!seen.add(key)) {
                    throw ExerciseCatalogPackException(
                        "Duplicate external identity ($source, $externalId) in pack",
                    )
                }
            }
        }
    }

    fun classify(records: List<NormalizedCatalogRecord>): Classification {
        val importable = ArrayList<ImportableCatalogExercise>()
        val rejected = ArrayList<RejectedCatalogRecord>()
        for (record in records) {
            when (val outcome = classifyOne(record)) {
                is RowOutcome.Import -> importable += outcome.exercise
                is RowOutcome.Reject -> rejected += outcome.rejection
            }
        }
        return Classification(importable, rejected)
    }

    private sealed class RowOutcome {
        data class Import(val exercise: ImportableCatalogExercise) : RowOutcome()
        data class Reject(val rejection: RejectedCatalogRecord) : RowOutcome()
    }

    private fun classifyOne(record: NormalizedCatalogRecord): RowOutcome {
        val source = record.source?.trim().orEmpty()
        val externalId = record.externalId?.trim().orEmpty()
        fun reject(reason: String) = RowOutcome.Reject(
            RejectedCatalogRecord(
                externalSource = source.ifBlank { record.source },
                externalId = externalId.ifBlank { record.externalId },
                reason = reason,
            ),
        )

        if (source.isEmpty()) return reject("missing_source")
        if (externalId.isEmpty()) return reject("missing_external_id")
        if (source !in SUPPORTED_SOURCES) return reject("unsupported_source")

        val name = record.name?.trim().orEmpty()
        if (name.isEmpty()) return reject("missing_name")

        if (record.sourcePrimaryMuscles.any { it.trim().lowercase() in UNMAPPED_PRIMARY }) {
            return reject("unmapped_primary")
        }

        val muscleGroup = record.muscleGroup?.trim().orEmpty()
        if (muscleGroup.isEmpty() || muscleGroup !in MUSCLE_GROUPS) {
            return reject("invalid_muscle_group")
        }

        val equipmentType = resolveEquipment(record) ?: return reject("invalid_equipment")

        val secondaries = sanitizedSecondaryMuscles(
            muscleGroup,
            record.secondaryMuscles.filter { it.trim() in MUSCLE_GROUPS },
        )

        return RowOutcome.Import(
            ImportableCatalogExercise(
                externalSource = source,
                externalId = externalId,
                name = name,
                muscleGroup = muscleGroup,
                equipmentType = equipmentType,
                secondaryMuscles = secondaries,
            ),
        )
    }

    /**
     * Uses canonical [NormalizedCatalogRecord.equipmentType] when already in the GymTrack catalog.
     * Applies Etapa 2.5 fallbacks from `sourceData.equipment` for ball/foam/EZ-bar.
     * Source equipment JSON null is not mapped to Outro.
     */
    fun resolveEquipment(record: NormalizedCatalogRecord): String? {
        val mapped = record.equipmentType?.trim().orEmpty()
        if (mapped in EQUIPMENT_TYPES) return mapped
        val sourceEquipment = record.sourceEquipment?.trim()?.lowercase().orEmpty()
        return when {
            sourceEquipment in EQUIPMENT_FALLBACK_OUTRO -> "Outro"
            sourceEquipment in EQUIPMENT_FALLBACK_BARRA -> "Barra"
            else -> null
        }
    }
}

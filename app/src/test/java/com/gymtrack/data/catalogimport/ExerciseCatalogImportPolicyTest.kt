package com.gymtrack.data.catalogimport

import com.gymtrack.domain.catalogimport.NormalizedCatalogRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseCatalogImportPolicyTest {

    @Test
    fun classify_mapsCanonicalRowToImportable() {
        val classified = ExerciseCatalogImportPolicy.classify(
            listOf(
                record(
                    name = "Bench Press",
                    muscleGroup = "Peitoral",
                    equipmentType = "Barra",
                    sourceEquipment = "barbell",
                    sourcePrimaryMuscles = listOf("chest"),
                ),
            ),
        )
        assertEquals(1, classified.importable.size)
        assertEquals(0, classified.rejected.size)
        assertEquals("Barra", classified.importable.single().equipmentType)
    }

    @Test
    fun classify_rejectsUnmappedPrimary() {
        val classified = ExerciseCatalogImportPolicy.classify(
            listOf(
                record(
                    externalId = "Neck_Bridge",
                    muscleGroup = null,
                    equipmentType = "Peso corporal",
                    sourceEquipment = "body only",
                    sourcePrimaryMuscles = listOf("neck"),
                ),
            ),
        )
        assertEquals("unmapped_primary", classified.rejected.single().reason)
    }

    @Test
    fun classify_rejectsNullSourceEquipmentWithoutCanonicalType() {
        val classified = ExerciseCatalogImportPolicy.classify(
            listOf(
                record(
                    equipmentType = null,
                    sourceEquipment = null,
                    sourcePrimaryMuscles = listOf("chest"),
                ),
            ),
        )
        assertEquals("invalid_equipment", classified.rejected.single().reason)
    }

    @Test
    fun classify_doesNotMapNullEquipmentToOutro() {
        assertNull(
            ExerciseCatalogImportPolicy.resolveEquipment(
                record(equipmentType = null, sourceEquipment = null),
            ),
        )
    }

    @Test
    fun classify_appliesOutroFallbackForMedicineBall() {
        val classified = ExerciseCatalogImportPolicy.classify(
            listOf(
                record(
                    equipmentType = null,
                    sourceEquipment = "medicine ball",
                    sourcePrimaryMuscles = listOf("shoulders"),
                    muscleGroup = "Ombros",
                ),
            ),
        )
        assertEquals("Outro", classified.importable.single().equipmentType)
    }

    @Test
    fun classify_appliesBarraFallbackForEzCurlBar() {
        val classified = ExerciseCatalogImportPolicy.classify(
            listOf(
                record(
                    equipmentType = null,
                    sourceEquipment = "e-z curl bar",
                    sourcePrimaryMuscles = listOf("biceps"),
                    muscleGroup = "Bíceps",
                ),
            ),
        )
        assertEquals("Barra", classified.importable.single().equipmentType)
    }

    @Test
    fun classify_rejectsMissingIdentity() {
        val missingSource = ExerciseCatalogImportPolicy.classify(
            listOf(record(source = null)),
        )
        val missingId = ExerciseCatalogImportPolicy.classify(
            listOf(record(externalId = null)),
        )
        assertEquals("missing_source", missingSource.rejected.single().reason)
        assertEquals("missing_external_id", missingId.rejected.single().reason)
    }

    @Test
    fun classify_full876Archive_matchesStage25Counts() {
        // Stage 2.5 baseline lives on the archived full pack, not the curated asset.
        val json = javaClass.getResourceAsStream("/exercises/gymtrack-exercises-full-876.json")!!
            .bufferedReader()
            .use { it.readText() }
        val records = NormalizedExerciseCatalogParser.parse(json)
        ExerciseCatalogImportPolicy.validatePack(records)
        val classified = ExerciseCatalogImportPolicy.classify(records)
        assertEquals(876, records.size)
        assertEquals(777, classified.importable.size)
        assertEquals(99, classified.rejected.size)
        assertEquals(876, classified.importable.size + classified.rejected.size)
        assertTrue(classified.importable.all { it.externalSource == "free-exercise-db" })
        assertTrue(classified.importable.all { it.externalId.isNotBlank() })
        assertEquals(777, classified.importable.map { it.externalId }.distinct().size)
    }

    @Test
    fun classify_publishedV21Catalog_isExactly136Importable() {
        val json = javaClass.getResourceAsStream("/exercises/gymtrack-exercises.json")!!
            .bufferedReader()
            .use { it.readText() }
        val records = NormalizedExerciseCatalogParser.parse(json)
        ExerciseCatalogImportPolicy.validatePack(records)
        val classified = ExerciseCatalogImportPolicy.classify(records)
        assertEquals(136, records.size)
        assertEquals(136, classified.importable.size)
        assertEquals(0, classified.rejected.size)
        assertTrue(classified.importable.all { it.externalSource == "free-exercise-db" })
        assertTrue(classified.importable.all { it.externalId.isNotBlank() })
        assertEquals(136, classified.importable.map { it.externalId }.distinct().size)
    }

    private fun assertTrue(value: Boolean) {
        org.junit.Assert.assertTrue(value)
    }

    private fun record(
        source: String? = "free-exercise-db",
        externalId: String? = "abc",
        name: String? = "Name",
        muscleGroup: String? = "Peitoral",
        equipmentType: String? = "Barra",
        secondaryMuscles: List<String> = emptyList(),
        sourceEquipment: String? = "barbell",
        sourcePrimaryMuscles: List<String> = listOf("chest"),
    ) = NormalizedCatalogRecord(
        source = source,
        externalId = externalId,
        name = name,
        muscleGroup = muscleGroup,
        equipmentType = equipmentType,
        secondaryMuscles = secondaryMuscles,
        sourceEquipment = sourceEquipment,
        sourcePrimaryMuscles = sourcePrimaryMuscles,
    )
}

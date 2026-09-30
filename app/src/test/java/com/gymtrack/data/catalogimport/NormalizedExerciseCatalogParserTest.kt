package com.gymtrack.data.catalogimport

import com.gymtrack.domain.catalogimport.ExerciseCatalogPackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class NormalizedExerciseCatalogParserTest {

    @Test
    fun parse_validArray_readsCanonicalFields() {
        val records = NormalizedExerciseCatalogParser.parse(
            """
            [
              {
                "source": "free-exercise-db",
                "externalId": "Bench_Press",
                "name": "Bench Press",
                "muscleGroup": "Peitoral",
                "secondaryMuscles": ["Ombros", "Tríceps"],
                "equipmentType": "Barra",
                "sourceData": {
                  "equipment": "barbell",
                  "primaryMuscles": ["chest"]
                }
              }
            ]
            """.trimIndent(),
        )
        assertEquals(1, records.size)
        val record = records.single()
        assertEquals("free-exercise-db", record.source)
        assertEquals("Bench_Press", record.externalId)
        assertEquals("Bench Press", record.name)
        assertEquals("Peitoral", record.muscleGroup)
        assertEquals(listOf("Ombros", "Tríceps"), record.secondaryMuscles)
        assertEquals("Barra", record.equipmentType)
        assertEquals("barbell", record.sourceEquipment)
        assertEquals(listOf("chest"), record.sourcePrimaryMuscles)
    }

    @Test
    fun parse_emptyArray_returnsEmptyList() {
        assertTrue(NormalizedExerciseCatalogParser.parse("[]").isEmpty())
    }

    @Test
    fun parse_invalidJson_throwsPackException() {
        try {
            NormalizedExerciseCatalogParser.parse("{")
            fail("Expected pack exception")
        } catch (_: ExerciseCatalogPackException) {
            // expected
        }
    }

    @Test
    fun parse_nonArrayRoot_throwsPackException() {
        try {
            NormalizedExerciseCatalogParser.parse("""{"source":"free-exercise-db"}""")
            fail("Expected pack exception")
        } catch (_: ExerciseCatalogPackException) {
            // expected
        }
    }

    @Test
    fun parse_missingRequiredStringFields_keepsNulls() {
        val records = NormalizedExerciseCatalogParser.parse(
            """[{"name":"Only name"}]""",
        )
        val record = records.single()
        assertEquals(null, record.source)
        assertEquals(null, record.externalId)
        assertEquals("Only name", record.name)
        assertEquals(null, record.muscleGroup)
        assertEquals(null, record.equipmentType)
    }

    @Test
    fun parse_nullExternalIdentityFields_areNull() {
        val records = NormalizedExerciseCatalogParser.parse(
            """
            [{
              "source": null,
              "externalId": null,
              "name": "X",
              "muscleGroup": "Peitoral",
              "equipmentType": "Barra",
              "secondaryMuscles": []
            }]
            """.trimIndent(),
        )
        assertEquals(null, records.single().source)
        assertEquals(null, records.single().externalId)
    }

    @Test
    fun parse_duplicateExternalId_isDetectedByPolicy() {
        val records = NormalizedExerciseCatalogParser.parse(
            """
            [
              {
                "source": "free-exercise-db",
                "externalId": "abc",
                "name": "A",
                "muscleGroup": "Peitoral",
                "equipmentType": "Barra",
                "secondaryMuscles": []
              },
              {
                "source": "free-exercise-db",
                "externalId": "abc",
                "name": "B",
                "muscleGroup": "Peitoral",
                "equipmentType": "Barra",
                "secondaryMuscles": []
              }
            ]
            """.trimIndent(),
        )
        try {
            ExerciseCatalogImportPolicy.validatePack(records)
            fail("Expected duplicate identity to fail the pack")
        } catch (e: ExerciseCatalogPackException) {
            assertTrue(e.message!!.contains("Duplicate external identity"))
        }
    }

    @Test
    fun parse_unsupportedSource_isDetectedByPolicy() {
        val records = NormalizedExerciseCatalogParser.parse(
            """
            [{
              "source": "unknown-catalog",
              "externalId": "abc",
              "name": "A",
              "muscleGroup": "Peitoral",
              "equipmentType": "Barra",
              "secondaryMuscles": []
            }]
            """.trimIndent(),
        )
        try {
            ExerciseCatalogImportPolicy.validatePack(records)
            fail("Expected unsupported source to fail the pack")
        } catch (e: ExerciseCatalogPackException) {
            assertTrue(e.message!!.contains("Unsupported catalog source"))
        }
    }

    @Test
    fun parse_nonStringName_throwsPackException() {
        try {
            NormalizedExerciseCatalogParser.parse(
                """[{"source":"free-exercise-db","externalId":"a","name":1}]""",
            )
            fail("Expected pack exception")
        } catch (_: ExerciseCatalogPackException) {
            // expected
        }
    }
}

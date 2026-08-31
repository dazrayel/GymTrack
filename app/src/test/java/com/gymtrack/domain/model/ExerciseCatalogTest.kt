package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseCatalogTest {

    @Test
    fun muscleGroups_matchClosedCatalog() {
        assertEquals(
            listOf(
                "Peitoral",
                "Costas",
                "Ombros",
                "Bíceps",
                "Tríceps",
                "Antebraço",
                "Quadríceps",
                "Posteriores",
                "Glúteos",
                "Panturrilhas",
                "Lombar",
                "Trapézio",
                "Abdômen",
            ),
            MUSCLE_GROUPS,
        )
    }

    @Test
    fun equipmentTypes_includeExplicitOther() {
        assertEquals("Outro", EQUIPMENT_TYPES.last())
        assertEquals(
            listOf(
                "Barra",
                "Halteres",
                "Máquina",
                "Smith",
                "Cabos",
                "Peso corporal",
                "Kettlebell",
                "Elástico",
                "Outro",
            ),
            EQUIPMENT_TYPES,
        )
    }

    @Test
    fun normalizeMuscleGroup_mapsKnownSynonymsOnly() {
        assertEquals("Peitoral", normalizeMuscleGroup("Peito"))
        assertEquals("Peitoral", normalizeMuscleGroup("Chest"))
        assertEquals("Costas", normalizeMuscleGroup("Back"))
        assertEquals("Legs", normalizeMuscleGroup("Legs"))
        assertEquals("Pernas", normalizeMuscleGroup("Pernas"))
        assertEquals("Custom", normalizeMuscleGroup("Custom"))
    }

    @Test
    fun normalizeEquipmentType_mapsKnownSynonymsOnly() {
        assertEquals("Barra", normalizeEquipmentType("barbell"))
        assertEquals("Halteres", normalizeEquipmentType("Dumbbell"))
        assertEquals("xyz", normalizeEquipmentType("xyz"))
    }

    @Test
    fun sanitizedSecondaryMuscles_dropsPrimaryDuplicatesAndEmpties() {
        assertEquals(
            listOf("Costas", "Ombros"),
            sanitizedSecondaryMuscles("Peitoral", listOf("Ombros", "Peitoral", "Ombros", " Costas ", "")),
        )
    }

    @Test
    fun serializeSecondaryMuscles_isDeterministic() {
        assertEquals("Ombros,Tríceps", serializeSecondaryMuscles(listOf("Tríceps", "Ombros", "Ombros")))
        assertEquals(listOf("Ombros", "Tríceps"), deserializeSecondaryMuscles("Tríceps, Ombros,Ombros"))
        assertEquals(emptyList<String>(), deserializeSecondaryMuscles(""))
    }
}

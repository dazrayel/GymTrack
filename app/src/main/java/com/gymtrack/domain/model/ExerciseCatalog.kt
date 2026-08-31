package com.gymtrack.domain.model

val MUSCLE_GROUPS: List<String> = listOf(
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
)

val EQUIPMENT_TYPES: List<String> = listOf(
    "Barra",
    "Halteres",
    "Máquina",
    "Smith",
    "Cabos",
    "Peso corporal",
    "Kettlebell",
    "Elástico",
    "Outro",
)

private val MUSCLE_SYNONYMS: Map<String, String> = mapOf(
    "peito" to "Peitoral",
    "chest" to "Peitoral",
    "peitoral" to "Peitoral",
    "back" to "Costas",
    "costas" to "Costas",
    "shoulders" to "Ombros",
    "ombros" to "Ombros",
    "biceps" to "Bíceps",
    "bíceps" to "Bíceps",
    "triceps" to "Tríceps",
    "tríceps" to "Tríceps",
    "forearm" to "Antebraço",
    "forearms" to "Antebraço",
    "antebraço" to "Antebraço",
    "antebraços" to "Antebraço",
    "quadriceps" to "Quadríceps",
    "quads" to "Quadríceps",
    "quadríceps" to "Quadríceps",
    "hamstrings" to "Posteriores",
    "posteriores" to "Posteriores",
    "posteriores de coxa" to "Posteriores",
    "glutes" to "Glúteos",
    "glúteos" to "Glúteos",
    "calves" to "Panturrilhas",
    "panturrilhas" to "Panturrilhas",
    "lower back" to "Lombar",
    "lombar" to "Lombar",
    "traps" to "Trapézio",
    "trapézio" to "Trapézio",
    "abs" to "Abdômen",
    "abdomen" to "Abdômen",
    "abdômen" to "Abdômen",
)

private val EQUIPMENT_SYNONYMS: Map<String, String> = mapOf(
    "barra" to "Barra",
    "barbell" to "Barra",
    "halteres" to "Halteres",
    "dumbbell" to "Halteres",
    "dumbbells" to "Halteres",
    "máquina" to "Máquina",
    "maquina" to "Máquina",
    "machine" to "Máquina",
    "smith" to "Smith",
    "smith machine" to "Smith",
    "cabo" to "Cabos",
    "cabos" to "Cabos",
    "cable" to "Cabos",
    "cables" to "Cabos",
    "peso corporal" to "Peso corporal",
    "bodyweight" to "Peso corporal",
    "kettlebell" to "Kettlebell",
    "elástico" to "Elástico",
    "elastico" to "Elástico",
    "band" to "Elástico",
    "bands" to "Elástico",
    "outro" to "Outro",
    "other" to "Outro",
)

fun normalizeMuscleGroup(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return trimmed
    return MUSCLE_SYNONYMS[trimmed.lowercase()] ?: trimmed
}

fun normalizeEquipmentType(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return trimmed
    return EQUIPMENT_SYNONYMS[trimmed.lowercase()] ?: trimmed
}

fun sanitizedSecondaryMuscles(primary: String, secondaries: List<String>): List<String> {
    val primaryTrimmed = primary.trim()
    return secondaries
        .map { it.trim() }
        .filter { it.isNotEmpty() && it != primaryTrimmed }
        .distinct()
        .sorted()
}

fun serializeSecondaryMuscles(muscles: List<String>): String =
    muscles.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted().joinToString(",")

fun deserializeSecondaryMuscles(raw: String): List<String> =
    raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()

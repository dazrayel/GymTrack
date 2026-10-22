package com.gymtrack.presentation.workouts.execution

import com.gymtrack.domain.model.WEIGHT_PROGRESSION_STEP_KG
import kotlin.math.roundToLong

/** Default weight step for execution steppers (kg). */
const val WEIGHT_ADJUST_STEP_KG = WEIGHT_PROGRESSION_STEP_KG

/** Minimum allowed weight while adjusting (matches complete-set validation). */
const val MIN_WEIGHT_KG = 0.0

/** Minimum allowed reps while adjusting (matches complete-set validation). */
const val MIN_REPS = 1

/**
 * Adjusts a weight text field by [deltaKg] without floating-point drift.
 * Invalid/empty input is treated as 0 before applying the step.
 */
fun adjustWeightInput(raw: String, deltaKg: Double): String {
    val currentTenths = parseWeightToTenths(raw) ?: 0L
    val deltaTenths = (deltaKg * 10.0).roundToLong()
    val nextTenths = (currentTenths + deltaTenths).coerceAtLeast((MIN_WEIGHT_KG * 10.0).roundToLong())
    return formatWeightFromTenths(nextTenths)
}

/**
 * Adjusts a reps text field by [delta]. Invalid/empty input is treated as 0 before clamping.
 */
fun adjustRepsInput(raw: String, delta: Int): String {
    val current = raw.trim().toIntOrNull() ?: 0
    val next = (current + delta).coerceAtLeast(MIN_REPS)
    return next.toString()
}

private fun parseWeightToTenths(raw: String): Long? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val parsed = trimmed.toDoubleOrNull() ?: return null
    return (parsed * 10.0).roundToLong()
}

private fun formatWeightFromTenths(tenths: Long): String {
    if (tenths % 10L == 0L) return (tenths / 10L).toString()
    val whole = tenths / 10L
    val fraction = kotlin.math.abs(tenths % 10L)
    return "$whole.$fraction"
}

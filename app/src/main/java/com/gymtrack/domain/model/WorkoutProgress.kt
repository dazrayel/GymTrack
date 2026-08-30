package com.gymtrack.domain.model

data class WorkoutProgress(
    val completedSets: Int,
    val plannedSets: Int,
    val completedExercises: Int,
    val totalExercises: Int,
    val progressPercent: Int,
)

fun workoutProgress(
    exercises: List<WorkoutSessionExercise>,
    setsByExerciseId: Map<Long, List<WorkoutSet>>,
): WorkoutProgress {
    val plannedSets = exercises.sumOf { it.plannedSets }
    val completedSets = exercises.sumOf { setsByExerciseId[it.id]?.size ?: 0 }
    val completedExercises = exercises.count { exercise ->
        (setsByExerciseId[exercise.id]?.size ?: 0) >= exercise.plannedSets
    }
    val progressPercent = if (plannedSets <= 0) {
        0
    } else {
        ((completedSets * 100L) / plannedSets).toInt().coerceIn(0, 100)
    }
    return WorkoutProgress(
        completedSets = completedSets,
        plannedSets = plannedSets,
        completedExercises = completedExercises,
        totalExercises = exercises.size,
        progressPercent = progressPercent,
    )
}

fun workoutVolume(setsByExerciseId: Map<Long, List<WorkoutSet>>): Double =
    setsByExerciseId.values.asSequence().flatten().sumOf { set -> set.reps * set.weight }

fun exerciseVolume(sets: List<WorkoutSet>): Double =
    sets.sumOf { set -> set.reps * set.weight }

fun formatVolumeKg(volume: Double): String {
    val roundedTenths = kotlin.math.round(volume * 10.0) / 10.0
    val isWhole = kotlin.math.abs(roundedTenths - roundedTenths.toLong()) < 0.0001
    val number = if (isWhole) {
        formatGroupedInt(roundedTenths.toLong())
    } else {
        val whole = kotlin.math.abs(roundedTenths).toLong()
        val tenth = ((kotlin.math.abs(roundedTenths) * 10.0).toLong()) % 10L
        val sign = if (roundedTenths < 0) "-" else ""
        "$sign${formatGroupedInt(whole)},$tenth"
    }
    return "$number kg"
}

private fun formatGroupedInt(value: Long): String {
    val negative = value < 0
    val digits = kotlin.math.abs(value).toString()
    val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
    return if (negative) "-$grouped" else grouped
}

fun elapsedMillis(session: WorkoutSession?, nowMillis: Long): Long {
    val startedAt = session?.startedAtMillis ?: return 0L
    val endAt = if (session.status == WorkoutSessionStatus.COMPLETED) {
        session.endedAtMillis ?: nowMillis
    } else {
        nowMillis
    }
    return maxOf(0L, endAt - startedAt)
}

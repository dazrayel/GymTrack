package com.gymtrack.domain.model

data class CompletedSetRecord(
    val sessionId: Long,
    val occurredAtMillis: Long,
    val exerciseName: String,
    val reps: Int,
    val weight: Double,
)

data class SessionExerciseStats(
    val sessionId: Long,
    val occurredAtMillis: Long,
    val exerciseName: String,
    val bestWeight: Double,
    val repsAtBestWeight: Int,
    val bestReps: Int,
    val weightAtBestReps: Double,
    val volume: Double,
)

data class ExercisePersonalRecords(
    val exerciseName: String,
    val bestWeight: Double,
    val bestWeightReps: Int,
    val bestWeightSessionId: Long,
    val bestReps: Int,
    val bestRepsWeight: Double,
    val bestRepsSessionId: Long,
    val bestVolume: Double,
    val bestVolumeSessionId: Long,
)

data class ExerciseProgress(
    val exerciseName: String,
    val sessionBestWeight: Double?,
    val sessionBestReps: Int?,
    val sessionVolume: Double?,
    val historicalBest: ExercisePersonalRecords?,
    val previousBest: ExercisePersonalRecords?,
    val previousSession: SessionExerciseStats?,
    val weightDelta: Double?,
    val repsDelta: Int?,
    val volumeDelta: Double?,
)

fun sessionExerciseStats(records: List<CompletedSetRecord>): List<SessionExerciseStats> {
    return records
        .groupBy { it.sessionId to it.exerciseName }
        .map { (_, sets) ->
            val bestWeightSet = sets.maxWith(
                compareBy<CompletedSetRecord> { it.weight }.thenBy { it.reps },
            )
            val bestRepsSet = sets.maxWith(
                compareBy<CompletedSetRecord> { it.reps }.thenBy { it.weight },
            )
            val first = sets.first()
            SessionExerciseStats(
                sessionId = first.sessionId,
                occurredAtMillis = first.occurredAtMillis,
                exerciseName = first.exerciseName,
                bestWeight = bestWeightSet.weight,
                repsAtBestWeight = bestWeightSet.reps,
                bestReps = bestRepsSet.reps,
                weightAtBestReps = bestRepsSet.weight,
                volume = sets.sumOf { it.reps * it.weight },
            )
        }
}

fun personalRecords(stats: List<SessionExerciseStats>): ExercisePersonalRecords? {
    if (stats.isEmpty()) return null
    val byWeight = stats.maxWith(
        compareBy<SessionExerciseStats> { it.bestWeight }
            .thenBy { it.repsAtBestWeight }
            .thenBy { it.occurredAtMillis }
            .thenBy { it.sessionId },
    )
    val byReps = stats.maxWith(
        compareBy<SessionExerciseStats> { it.bestReps }
            .thenBy { it.weightAtBestReps }
            .thenBy { it.occurredAtMillis }
            .thenBy { it.sessionId },
    )
    val byVolume = stats.maxWith(
        compareBy<SessionExerciseStats> { it.volume }
            .thenBy { it.occurredAtMillis }
            .thenBy { it.sessionId },
    )
    return ExercisePersonalRecords(
        exerciseName = stats.first().exerciseName,
        bestWeight = byWeight.bestWeight,
        bestWeightReps = byWeight.repsAtBestWeight,
        bestWeightSessionId = byWeight.sessionId,
        bestReps = byReps.bestReps,
        bestRepsWeight = byReps.weightAtBestReps,
        bestRepsSessionId = byReps.sessionId,
        bestVolume = byVolume.volume,
        bestVolumeSessionId = byVolume.sessionId,
    )
}

fun historicalPersonalRecords(
    records: List<CompletedSetRecord>,
): List<ExercisePersonalRecords> {
    return sessionExerciseStats(records)
        .groupBy { it.exerciseName }
        .mapNotNull { (_, stats) -> personalRecords(stats) }
        .sortedBy { it.exerciseName }
}

fun isSessionBefore(
    candidate: SessionExerciseStats,
    currentSessionId: Long,
    currentOccurredAtMillis: Long,
): Boolean {
    return when {
        candidate.occurredAtMillis < currentOccurredAtMillis -> true
        candidate.occurredAtMillis > currentOccurredAtMillis -> false
        else -> candidate.sessionId < currentSessionId
    }
}

fun exerciseProgress(
    exerciseName: String,
    currentSessionId: Long,
    currentOccurredAtMillis: Long,
    history: List<CompletedSetRecord>,
): ExerciseProgress {
    val stats = sessionExerciseStats(history.filter { it.exerciseName == exerciseName })
    val current = stats.find { it.sessionId == currentSessionId }
    val previous = stats.filter {
        isSessionBefore(it, currentSessionId, currentOccurredAtMillis)
    }
    val previousSession = previous.maxWithOrNull(
        compareBy<SessionExerciseStats> { it.occurredAtMillis }.thenBy { it.sessionId },
    )
    val previousPr = personalRecords(previous)
    val historicalPr = personalRecords(stats)
    return ExerciseProgress(
        exerciseName = exerciseName,
        sessionBestWeight = current?.bestWeight,
        sessionBestReps = current?.bestReps,
        sessionVolume = current?.volume,
        historicalBest = historicalPr,
        previousBest = previousPr,
        previousSession = previousSession,
        weightDelta = if (current != null && previousPr != null) {
            current.bestWeight - previousPr.bestWeight
        } else {
            null
        },
        repsDelta = if (current != null && previousPr != null) {
            current.bestReps - previousPr.bestReps
        } else {
            null
        },
        volumeDelta = if (current != null && previousPr != null) {
            current.volume - previousPr.bestVolume
        } else {
            null
        },
    )
}

fun formatSignedKg(delta: Double): String {
    val formatted = formatVolumeKg(kotlin.math.abs(delta))
    return when {
        delta > 0.0 -> "+$formatted"
        delta < 0.0 -> "-$formatted"
        else -> formatted
    }
}

fun formatSignedInt(delta: Int): String =
    if (delta > 0) "+$delta" else "$delta"

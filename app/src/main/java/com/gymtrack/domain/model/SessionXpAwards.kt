package com.gymtrack.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/**
 * Inputs for building idempotent XP events after a session becomes COMPLETED.
 * Pure domain — no Room / UI.
 */
data class SessionXpAwardInput(
    val sessionId: Long,
    val workoutId: Long?,
    val occurredAtMillis: Long,
    val exercises: List<SessionXpExerciseInput>,
    val completedSetHistory: List<CompletedSetRecord>,
    val otherCompletedSessionsInIsoWeek: List<Long>,
    val plannedWorkoutIdForDay: Long?,
    val zoneId: ZoneId,
    val createdAtMillis: Long,
)

data class SessionXpExerciseInput(
    val exerciseName: String,
    val plannedWeight: Double,
    val minRepetitions: Int,
    val maxRepetitions: Int,
    val plannedSets: Int,
    val performedSets: List<ProgressionPerformedSet>,
)

/**
 * Builds XP events for a newly completed session. Callers persist with IGNORE on [XpEvent.sourceKey].
 */
fun buildSessionXpEvents(input: SessionXpAwardInput): List<XpEvent> {
    val events = mutableListOf<XpEvent>()

    events += XpEvent(
        type = XpEventType.WORKOUT_COMPLETED,
        amount = XpEventType.WORKOUT_COMPLETED.amount,
        sourceKey = XpSourceKeys.workoutCompleted(input.sessionId),
        createdAtMillis = input.createdAtMillis,
    )

    val plannedId = input.plannedWorkoutIdForDay
    if (plannedId != null && input.workoutId != null && plannedId == input.workoutId) {
        events += XpEvent(
            type = XpEventType.PLANNED_WORKOUT_COMPLETED,
            amount = XpEventType.PLANNED_WORKOUT_COMPLETED.amount,
            sourceKey = XpSourceKeys.plannedWorkoutCompleted(input.sessionId),
            createdAtMillis = input.createdAtMillis,
        )
    }

    if (input.otherCompletedSessionsInIsoWeek.isEmpty()) {
        val weekKey = isoWeekKey(input.occurredAtMillis, input.zoneId)
        events += XpEvent(
            type = XpEventType.FIRST_WORKOUT_OF_WEEK,
            amount = XpEventType.FIRST_WORKOUT_OF_WEEK.amount,
            sourceKey = XpSourceKeys.firstWorkoutOfWeek(weekKey),
            createdAtMillis = input.createdAtMillis,
        )
    }

    for (exercise in input.exercises) {
        val suggestion = evaluateLoadProgression(
            plannedWeight = exercise.plannedWeight,
            minRepetitions = exercise.minRepetitions,
            maxRepetitions = exercise.maxRepetitions,
            plannedSets = exercise.plannedSets,
            lastSessionSets = exercise.performedSets,
        )
        when (suggestion.action) {
            ProgressionAction.INCREASE_WEIGHT -> {
                events += XpEvent(
                    type = XpEventType.PROGRESSION_WEIGHT,
                    amount = XpEventType.PROGRESSION_WEIGHT.amount,
                    sourceKey = XpSourceKeys.progressionWeight(input.sessionId, exercise.exerciseName),
                    createdAtMillis = input.createdAtMillis,
                )
            }
            ProgressionAction.INCREASE_REPS -> {
                events += XpEvent(
                    type = XpEventType.PROGRESSION_REPS,
                    amount = XpEventType.PROGRESSION_REPS.amount,
                    sourceKey = XpSourceKeys.progressionReps(input.sessionId, exercise.exerciseName),
                    createdAtMillis = input.createdAtMillis,
                )
            }
            ProgressionAction.MAINTAIN, ProgressionAction.NO_HISTORY -> Unit
        }

        val progress = exerciseProgress(
            exerciseName = exercise.exerciseName,
            currentSessionId = input.sessionId,
            currentOccurredAtMillis = input.occurredAtMillis,
            history = input.completedSetHistory,
        )
        if (isNewPersonalRecord(progress)) {
            events += XpEvent(
                type = XpEventType.PERSONAL_RECORD,
                amount = XpEventType.PERSONAL_RECORD.amount,
                sourceKey = XpSourceKeys.personalRecord(input.sessionId, exercise.exerciseName),
                createdAtMillis = input.createdAtMillis,
            )
        }
    }

    return events
}

/**
 * Matches existing summary semantics: improvement over previous best, or first PR for the exercise.
 */
fun isNewPersonalRecord(progress: ExerciseProgress): Boolean {
    if (progress.sessionBestWeight == null && progress.sessionBestReps == null) return false
    val previous = progress.previousBest
    if (previous == null) return true
    val weightUp = (progress.weightDelta ?: 0.0) > 0.0
    val repsUp = (progress.repsDelta ?: 0) > 0
    val volumeUp = (progress.volumeDelta ?: 0.0) > 0.0
    return weightUp || repsUp || volumeUp
}

fun localDayOfWeek(epochMillis: Long, zoneId: ZoneId): DayOfWeek =
    Instant.ofEpochMilli(epochMillis).atZone(zoneId).dayOfWeek

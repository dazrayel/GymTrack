package com.gymtrack.presentation.workouts.summary

import com.gymtrack.domain.model.CompletedSetRecord
import com.gymtrack.domain.model.ExerciseProgress
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSet
import com.gymtrack.domain.model.elapsedMillis
import com.gymtrack.domain.model.exerciseProgress
import com.gymtrack.domain.model.exerciseVolume
import com.gymtrack.domain.model.workoutProgress
import com.gymtrack.domain.model.workoutVolume
import com.gymtrack.domain.time.formatHistoryDate
import com.gymtrack.domain.time.formatHistoryTime

data class SessionExerciseSummary(
    val exerciseName: String,
    val muscleGroup: String,
    val completedSets: Int,
    val plannedSets: Int,
    val sets: List<WorkoutSet>,
    val volume: Double,
    val progress: ExerciseProgress,
)

data class WorkoutSessionSummaryUiState(
    val sessionId: Long,
    val session: WorkoutSession? = null,
    val exercises: List<WorkoutSessionExercise> = emptyList(),
    val setsByExerciseId: Map<Long, List<WorkoutSet>> = emptyMap(),
    val completedSetHistory: List<CompletedSetRecord> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val sessionNotFound: Boolean = false,
) {
    val durationMillis: Long
        get() = elapsedMillis(session, nowMillis = session?.endedAtMillis ?: 0L)

    val startedDate: String
        get() = session?.let { formatHistoryDate(it.startedAtMillis) }.orEmpty()

    val startedTime: String
        get() = session?.let { formatHistoryTime(it.startedAtMillis) }.orEmpty()

    val endedTime: String
        get() = session?.endedAtMillis?.let { formatHistoryTime(it) }.orEmpty()

    val progress
        get() = workoutProgress(exercises, setsByExerciseId)

    val volume: Double
        get() = workoutVolume(setsByExerciseId)

    val canNavigateToWorkout: Boolean
        get() = session?.workoutId != null

    val exerciseSummaries: List<SessionExerciseSummary>
        get() {
            val sid = session?.id ?: sessionId
            val occurredAt = session?.endedAtMillis ?: session?.startedAtMillis ?: 0L
            val currentRecords = exercises.flatMap { exercise ->
                setsByExerciseId[exercise.id].orEmpty().map { set ->
                    CompletedSetRecord(
                        sessionId = sid,
                        occurredAtMillis = occurredAt,
                        exerciseName = exercise.exerciseName,
                        reps = set.reps,
                        weight = set.weight,
                    )
                }
            }
            val history = completedSetHistory.filter { it.sessionId != sid } + currentRecords
            return exercises.map { exercise ->
                val sets = setsByExerciseId[exercise.id].orEmpty().sortedBy { it.setIndex }
                SessionExerciseSummary(
                    exerciseName = exercise.exerciseName,
                    muscleGroup = exercise.muscleGroup,
                    completedSets = sets.size,
                    plannedSets = exercise.plannedSets,
                    sets = sets,
                    volume = exerciseVolume(sets),
                    progress = exerciseProgress(
                        exerciseName = exercise.exerciseName,
                        currentSessionId = sid,
                        currentOccurredAtMillis = occurredAt,
                        history = history,
                    ),
                )
            }
        }
}

package com.gymtrack.presentation.workouts.summary

import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSet
import com.gymtrack.domain.model.elapsedMillis
import com.gymtrack.domain.model.exerciseVolume
import com.gymtrack.domain.model.workoutProgress
import com.gymtrack.domain.model.workoutVolume

data class SessionExerciseSummary(
    val exerciseName: String,
    val muscleGroup: String,
    val completedSets: Int,
    val plannedSets: Int,
    val sets: List<WorkoutSet>,
    val volume: Double,
)

data class WorkoutSessionSummaryUiState(
    val sessionId: Long,
    val session: WorkoutSession? = null,
    val exercises: List<WorkoutSessionExercise> = emptyList(),
    val setsByExerciseId: Map<Long, List<WorkoutSet>> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val durationMillis: Long
        get() = elapsedMillis(session, nowMillis = session?.endedAtMillis ?: 0L)

    val progress
        get() = workoutProgress(exercises, setsByExerciseId)

    val volume: Double
        get() = workoutVolume(setsByExerciseId)

    val canNavigateToWorkout: Boolean
        get() = session?.workoutId != null

    val exerciseSummaries: List<SessionExerciseSummary>
        get() = exercises.map { exercise ->
            val sets = setsByExerciseId[exercise.id].orEmpty()
            SessionExerciseSummary(
                exerciseName = exercise.exerciseName,
                muscleGroup = exercise.muscleGroup,
                completedSets = sets.size,
                plannedSets = exercise.plannedSets,
                sets = sets,
                volume = exerciseVolume(sets),
            )
        }
}

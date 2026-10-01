package com.gymtrack.domain.model

/**
 * Picks the next executable workout template using creation order (`id ASC`)
 * and completed-session history.
 *
 * Only workouts whose ids appear in [workoutIdsWithExercises] are candidates
 * (templates without exercises are excluded). [IN_PROGRESS][WorkoutSessionStatus.IN_PROGRESS]
 * sessions are ignored. Completed sessions with a null or unknown [WorkoutSession.workoutId]
 * are ignored.
 *
 * Preference: first never-completed candidate by `id ASC`; otherwise the circular
 * successor of the most recent completed session that still maps to a candidate.
 */
fun recommendNextWorkout(
    workouts: List<Workout>,
    sessions: List<WorkoutSession>,
    workoutIdsWithExercises: Set<Long>,
): Workout? {
    val candidates = workouts
        .asSequence()
        .filter { it.id in workoutIdsWithExercises }
        .sortedBy { it.id }
        .toList()
    if (candidates.isEmpty()) return null

    val candidateIds = candidates.mapTo(mutableSetOf()) { it.id }
    val completedForCandidates = sessions.asSequence()
        .filter { it.status == WorkoutSessionStatus.COMPLETED }
        .mapNotNull { session ->
            val workoutId = session.workoutId ?: return@mapNotNull null
            if (workoutId !in candidateIds) return@mapNotNull null
            session
        }
        .toList()

    val completedWorkoutIds = completedForCandidates.mapNotNullTo(mutableSetOf()) { it.workoutId }
    val neverCompleted = candidates.filter { it.id !in completedWorkoutIds }
    if (neverCompleted.isNotEmpty()) {
        return neverCompleted.first()
    }

    val lastCompleted = completedForCandidates.maxWithOrNull(
        compareBy<WorkoutSession> { it.endedAtMillis ?: it.startedAtMillis }
            .thenBy { it.id },
    ) ?: return candidates.first()

    val lastWorkoutId = lastCompleted.workoutId ?: return candidates.first()
    val index = candidates.indexOfFirst { it.id == lastWorkoutId }
    if (index < 0) return candidates.first()
    return candidates[(index + 1) % candidates.size]
}

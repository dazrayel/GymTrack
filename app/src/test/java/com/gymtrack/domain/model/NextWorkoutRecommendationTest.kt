package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextWorkoutRecommendationTest {

    private val workoutA = Workout(id = 1, name = "A")
    private val workoutB = Workout(id = 2, name = "B")
    private val workoutC = Workout(id = 3, name = "C")
    private val allWorkouts = listOf(workoutA, workoutB, workoutC)
    private val allExecutable = setOf(1L, 2L, 3L)

    @Test
    fun emptyCandidates_returnsNull() {
        assertNull(
            recommendNextWorkout(
                workouts = emptyList(),
                sessions = emptyList(),
                workoutIdsWithExercises = emptySet(),
            ),
        )
        assertNull(
            recommendNextWorkout(
                workouts = allWorkouts,
                sessions = emptyList(),
                workoutIdsWithExercises = emptySet(),
            ),
        )
    }

    @Test
    fun singleWorkout_withoutHistory_returnsThatWorkout() {
        val result = recommendNextWorkout(
            workouts = listOf(workoutA),
            sessions = emptyList(),
            workoutIdsWithExercises = setOf(1L),
        )
        assertEquals(workoutA, result)
    }

    @Test
    fun multipleWorkouts_withoutHistory_returnsFirstByIdAsc() {
        val unordered = listOf(workoutC, workoutA, workoutB)
        val result = recommendNextWorkout(
            workouts = unordered,
            sessions = emptyList(),
            workoutIdsWithExercises = allExecutable,
        )
        assertEquals(workoutA, result)
    }

    @Test
    fun firstCompleted_returnsSecond() {
        val result = recommendNextWorkout(
            workouts = allWorkouts,
            sessions = listOf(completed(sessionId = 10, workoutId = 1, endedAt = 100)),
            workoutIdsWithExercises = allExecutable,
        )
        assertEquals(workoutB, result)
    }

    @Test
    fun middleCompleted_returnsNext() {
        val result = recommendNextWorkout(
            workouts = allWorkouts,
            sessions = listOf(
                completed(sessionId = 10, workoutId = 1, endedAt = 100),
                completed(sessionId = 11, workoutId = 2, endedAt = 200),
            ),
            workoutIdsWithExercises = allExecutable,
        )
        assertEquals(workoutC, result)
    }

    @Test
    fun lastCompleted_rotatesToFirst() {
        val result = recommendNextWorkout(
            workouts = allWorkouts,
            sessions = listOf(
                completed(sessionId = 10, workoutId = 1, endedAt = 100),
                completed(sessionId = 11, workoutId = 2, endedAt = 200),
                completed(sessionId = 12, workoutId = 3, endedAt = 300),
            ),
            workoutIdsWithExercises = allExecutable,
        )
        assertEquals(workoutA, result)
    }

    @Test
    fun neverCompletedExists_returnsFirstNeverCompleted() {
        val result = recommendNextWorkout(
            workouts = allWorkouts,
            sessions = listOf(
                completed(sessionId = 10, workoutId = 1, endedAt = 100),
                completed(sessionId = 11, workoutId = 2, endedAt = 200),
            ),
            workoutIdsWithExercises = allExecutable,
        )
        assertEquals(workoutC, result)
    }

    @Test
    fun neverCompletedInMiddle_respectsIdAsc() {
        val result = recommendNextWorkout(
            workouts = allWorkouts,
            sessions = listOf(
                completed(sessionId = 10, workoutId = 1, endedAt = 100),
                completed(sessionId = 11, workoutId = 3, endedAt = 300),
            ),
            workoutIdsWithExercises = allExecutable,
        )
        assertEquals(workoutB, result)
    }

    @Test
    fun inProgressSession_doesNotInfluence() {
        val result = recommendNextWorkout(
            workouts = allWorkouts,
            sessions = listOf(
                inProgress(sessionId = 99, workoutId = 1, startedAt = 999),
            ),
            workoutIdsWithExercises = allExecutable,
        )
        assertEquals(workoutA, result)
    }

    @Test
    fun nullWorkoutId_doesNotInfluence() {
        val result = recommendNextWorkout(
            workouts = allWorkouts,
            sessions = listOf(
                completed(sessionId = 10, workoutId = null, endedAt = 500),
                completed(sessionId = 11, workoutId = 1, endedAt = 100),
            ),
            workoutIdsWithExercises = allExecutable,
        )
        // Only A is a valid completed candidate → never-done B is first by id ASC.
        assertEquals(workoutB, result)
    }

    @Test
    fun sessionForMissingWorkout_doesNotInfluence() {
        val result = recommendNextWorkout(
            workouts = allWorkouts,
            sessions = listOf(
                completed(sessionId = 10, workoutId = 99, endedAt = 500),
                completed(sessionId = 11, workoutId = 1, endedAt = 100),
            ),
            workoutIdsWithExercises = allExecutable,
        )
        assertEquals(workoutB, result)
    }

    @Test
    fun multipleSessionsSameWorkout_usesMostRecent() {
        val result = recommendNextWorkout(
            workouts = allWorkouts,
            sessions = listOf(
                completed(sessionId = 10, workoutId = 1, endedAt = 100),
                completed(sessionId = 11, workoutId = 2, endedAt = 200),
                completed(sessionId = 12, workoutId = 3, endedAt = 300),
                completed(sessionId = 13, workoutId = 1, endedAt = 400),
            ),
            workoutIdsWithExercises = allExecutable,
        )
        // All completed; most recent is A @ 400 → next is B.
        assertEquals(workoutB, result)
    }

    @Test
    fun historyOnlyForMissingWorkouts_returnsFirstCandidate() {
        val result = recommendNextWorkout(
            workouts = allWorkouts,
            sessions = listOf(
                completed(sessionId = 10, workoutId = 50, endedAt = 100),
                completed(sessionId = 11, workoutId = null, endedAt = 200),
            ),
            workoutIdsWithExercises = allExecutable,
        )
        assertEquals(workoutA, result)
    }

    @Test
    fun allCandidatesCompleted_appliesCircularRotation() {
        val result = recommendNextWorkout(
            workouts = allWorkouts,
            sessions = listOf(
                completed(sessionId = 10, workoutId = 1, endedAt = 100),
                completed(sessionId = 11, workoutId = 2, endedAt = 200),
                completed(sessionId = 12, workoutId = 3, endedAt = 250),
                completed(sessionId = 13, workoutId = 2, endedAt = 400),
            ),
            workoutIdsWithExercises = allExecutable,
        )
        // Most recent completed among candidates is B → next is C.
        assertEquals(workoutC, result)
    }

    @Test
    fun workoutsWithoutExercises_areExcludedFromCandidates() {
        val emptyTemplate = Workout(id = 0, name = "Empty")
        val result = recommendNextWorkout(
            workouts = listOf(emptyTemplate, workoutA, workoutB),
            sessions = emptyList(),
            workoutIdsWithExercises = setOf(1L, 2L),
        )
        assertEquals(workoutA, result)
    }

    @Test
    fun tieOnEndedAt_usesHigherSessionIdAsMoreRecent() {
        val result = recommendNextWorkout(
            workouts = allWorkouts,
            sessions = listOf(
                completed(sessionId = 10, workoutId = 1, endedAt = 100),
                completed(sessionId = 11, workoutId = 2, endedAt = 100),
                completed(sessionId = 12, workoutId = 3, endedAt = 50),
            ),
            workoutIdsWithExercises = allExecutable,
        )
        // Same endedAt: session 11 (B) wins over 10 (A) → next is C.
        assertEquals(workoutC, result)
    }

    private fun completed(
        sessionId: Long,
        workoutId: Long?,
        endedAt: Long,
    ): WorkoutSession = WorkoutSession(
        id = sessionId,
        workoutId = workoutId,
        workoutName = "Session $sessionId",
        startedAtMillis = endedAt - 1,
        endedAtMillis = endedAt,
        status = WorkoutSessionStatus.COMPLETED,
    )

    private fun inProgress(
        sessionId: Long,
        workoutId: Long?,
        startedAt: Long,
    ): WorkoutSession = WorkoutSession(
        id = sessionId,
        workoutId = workoutId,
        workoutName = "In progress $sessionId",
        startedAtMillis = startedAt,
        endedAtMillis = null,
        status = WorkoutSessionStatus.IN_PROGRESS,
    )
}

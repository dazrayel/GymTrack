package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSessionExerciseStatusTest {

    private val supino = sessionExercise(id = 1, position = 0, plannedSets = 3)
    private val crucifixo = sessionExercise(id = 2, position = 1, plannedSets = 3)
    private val triceps = sessionExercise(id = 3, position = 2, plannedSets = 3)

    @Test
    fun current_isFirstIncompleteWhenNoneSkipped() {
        val index = resolveCurrentExerciseIndex(
            listOf(supino, crucifixo),
            mapOf(1L to emptyList(), 2L to emptyList()),
        )
        assertEquals(0, index)
    }

    @Test
    fun current_skipsSkippedExercise() {
        val index = resolveCurrentExerciseIndex(
            listOf(supino.copy(status = WorkoutSessionExerciseStatus.SKIPPED), crucifixo),
            mapOf(1L to emptyList(), 2L to emptyList()),
        )
        assertEquals(1, index)
    }

    @Test
    fun current_prefersInProgressEvenIfEarlierIsIncomplete() {
        val index = resolveCurrentExerciseIndex(
            listOf(
                supino.copy(status = WorkoutSessionExerciseStatus.SKIPPED),
                crucifixo,
                triceps.copy(status = WorkoutSessionExerciseStatus.IN_PROGRESS),
            ),
            mapOf(1L to emptyList(), 2L to emptyList(), 3L to listOf(set(3, 0))),
        )
        assertEquals(2, index)
    }

    @Test
    fun current_ignoresCompletedAndSkipped_selectsNextPending() {
        val desenvolvimento = sessionExercise(id = 4, position = 3, plannedSets = 3)
        val index = resolveCurrentExerciseIndex(
            listOf(
                supino.copy(status = WorkoutSessionExerciseStatus.COMPLETED),
                crucifixo.copy(status = WorkoutSessionExerciseStatus.SKIPPED),
                triceps,
                desenvolvimento.copy(status = WorkoutSessionExerciseStatus.COMPLETED),
            ),
            mapOf(
                1L to listOf(set(1, 0), set(1, 1), set(1, 2)),
                2L to emptyList(),
                3L to emptyList(),
                4L to listOf(set(4, 0), set(4, 1), set(4, 2)),
            ),
        )
        assertEquals(2, index)
    }

    @Test
    fun current_isNoneWhenOnlyCompletedAndSkippedRemain() {
        val desenvolvimento = sessionExercise(id = 4, position = 3, plannedSets = 3)
        val exercises = listOf(
            supino.copy(status = WorkoutSessionExerciseStatus.COMPLETED),
            crucifixo.copy(status = WorkoutSessionExerciseStatus.SKIPPED),
            triceps.copy(status = WorkoutSessionExerciseStatus.SKIPPED),
            desenvolvimento.copy(status = WorkoutSessionExerciseStatus.COMPLETED),
        )
        val sets = mapOf(
            1L to listOf(set(1, 0), set(1, 1), set(1, 2)),
            2L to emptyList(),
            3L to emptyList(),
            4L to listOf(set(4, 0), set(4, 1), set(4, 2)),
        )
        assertEquals(-1, resolveCurrentExerciseIndex(exercises, sets))
        assertFalse(areAllSessionExercisesComplete(exercises, sets))
    }

    @Test
    fun current_isNoneWhenOnlySkippedRemain() {
        val index = resolveCurrentExerciseIndex(
            listOf(supino.copy(status = WorkoutSessionExerciseStatus.SKIPPED)),
            mapOf(1L to emptyList()),
        )
        assertEquals(-1, index)
        assertFalse(
            areAllSessionExercisesComplete(
                listOf(supino.copy(status = WorkoutSessionExerciseStatus.SKIPPED)),
                mapOf(1L to emptyList()),
            ),
        )
    }

    @Test
    fun skippedIsNotCompleteWithoutAllSets() {
        assertFalse(isSessionExerciseComplete(supino.copy(status = WorkoutSessionExerciseStatus.SKIPPED), 0))
        assertFalse(isSessionExerciseComplete(supino.copy(status = WorkoutSessionExerciseStatus.SKIPPED), 2))
        assertTrue(isSessionExerciseComplete(supino.copy(status = WorkoutSessionExerciseStatus.SKIPPED), 3))
    }

    @Test
    fun unknownStatus_defaultsToPending() {
        assertEquals(WorkoutSessionExerciseStatus.PENDING, parseWorkoutSessionExerciseStatus("NOPE"))
    }

    private fun sessionExercise(
        id: Long,
        position: Int,
        plannedSets: Int,
    ) = WorkoutSessionExercise(
        id = id,
        sessionId = 1L,
        position = position,
        exerciseName = "E$id",
        muscleGroup = "Peito",
        equipmentType = "Barra",
        plannedSets = plannedSets,
        minRepetitions = 8,
        maxRepetitions = 12,
        plannedWeight = 40.0,
        restSeconds = 0,
    )

    private fun set(sessionExerciseId: Long, setIndex: Int) = WorkoutSet(
        id = sessionExerciseId * 10 + setIndex,
        sessionExerciseId = sessionExerciseId,
        setIndex = setIndex,
        reps = 8,
        weight = 40.0,
        completedAtMillis = 1L,
    )
}

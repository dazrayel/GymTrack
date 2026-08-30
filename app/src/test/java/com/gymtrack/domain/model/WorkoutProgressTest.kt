package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutProgressTest {

    private val exerciseA = WorkoutSessionExercise(
        id = 10L,
        sessionId = 1L,
        exerciseId = 1L,
        position = 0,
        exerciseName = "Supino",
        muscleGroup = "Peito",
        equipmentType = "Barra",
        plannedSets = 3,
        minRepetitions = 8,
        maxRepetitions = 12,
        plannedWeight = 60.0,
        restSeconds = 0,
    )

    private val exerciseB = WorkoutSessionExercise(
        id = 20L,
        sessionId = 1L,
        exerciseId = 2L,
        position = 1,
        exerciseName = "Crucifixo",
        muscleGroup = "Peito",
        equipmentType = "Halteres",
        plannedSets = 2,
        minRepetitions = 10,
        maxRepetitions = 12,
        plannedWeight = 14.0,
        restSeconds = 0,
    )

    @Test
    fun zeroCompletedOfPlannedSets_isZeroPercent() {
        val progress = workoutProgress(
            exercises = listOf(exerciseA, exerciseB),
            setsByExerciseId = emptyMap(),
        )
        assertEquals(0, progress.completedSets)
        assertEquals(5, progress.plannedSets)
        assertEquals(0, progress.progressPercent)
    }

    @Test
    fun partialProgress_roundsDownToIntegerPercent() {
        val progress = workoutProgress(
            exercises = listOf(exerciseA, exerciseB),
            setsByExerciseId = mapOf(
                exerciseA.id to listOf(set(exerciseA.id, 0), set(exerciseA.id, 1)),
            ),
        )
        assertEquals(2, progress.completedSets)
        assertEquals(5, progress.plannedSets)
        assertEquals(40, progress.progressPercent)
    }

    @Test
    fun allSetsCompleted_isOneHundredPercent() {
        val progress = workoutProgress(
            exercises = listOf(exerciseA, exerciseB),
            setsByExerciseId = mapOf(
                exerciseA.id to listOf(set(exerciseA.id, 0), set(exerciseA.id, 1), set(exerciseA.id, 2)),
                exerciseB.id to listOf(set(exerciseB.id, 0), set(exerciseB.id, 1)),
            ),
        )
        assertEquals(5, progress.completedSets)
        assertEquals(5, progress.plannedSets)
        assertEquals(100, progress.progressPercent)
        assertEquals(2, progress.completedExercises)
        assertEquals(2, progress.totalExercises)
    }

    @Test
    fun sessionWithoutExercises_isZeroPercent() {
        val progress = workoutProgress(exercises = emptyList(), setsByExerciseId = emptyMap())
        assertEquals(0, progress.completedSets)
        assertEquals(0, progress.plannedSets)
        assertEquals(0, progress.completedExercises)
        assertEquals(0, progress.totalExercises)
        assertEquals(0, progress.progressPercent)
    }

    @Test
    fun completedExercises_countOnlyFullyFinishedExercises() {
        val progress = workoutProgress(
            exercises = listOf(exerciseA, exerciseB),
            setsByExerciseId = mapOf(
                exerciseA.id to listOf(set(exerciseA.id, 0), set(exerciseA.id, 1), set(exerciseA.id, 2)),
                exerciseB.id to listOf(set(exerciseB.id, 0)),
            ),
        )
        assertEquals(1, progress.completedExercises)
        assertEquals(2, progress.totalExercises)
        assertEquals(4, progress.completedSets)
    }

    @Test
    fun completedSets_countAllPersistedSets() {
        val progress = workoutProgress(
            exercises = listOf(exerciseA, exerciseB),
            setsByExerciseId = mapOf(
                exerciseA.id to listOf(set(exerciseA.id, 0)),
                exerciseB.id to listOf(set(exerciseB.id, 0)),
            ),
        )
        assertEquals(2, progress.completedSets)
        assertEquals(5, progress.plannedSets)
        assertEquals(0, progress.completedExercises)
    }

    @Test
    fun elapsed_usesNowMinusStartedAtWhileInProgress() {
        val session = WorkoutSession(
            workoutName = "Push",
            startedAtMillis = 1_000L,
            status = WorkoutSessionStatus.IN_PROGRESS,
        )
        assertEquals(60_000L, elapsedMillis(session, nowMillis = 61_000L))
    }

    @Test
    fun elapsed_usesEndedAtWhenCompleted() {
        val session = WorkoutSession(
            workoutName = "Push",
            startedAtMillis = 1_000L,
            endedAtMillis = 4_000L,
            status = WorkoutSessionStatus.COMPLETED,
        )
        assertEquals(3_000L, elapsedMillis(session, nowMillis = 99_000L))
    }

    @Test
    fun volume_sumsOnlyPersistedSets() {
        val volume = workoutVolume(
            mapOf(
                exerciseA.id to listOf(set(exerciseA.id, 0, reps = 8, weight = 60.0)),
                exerciseB.id to listOf(set(exerciseB.id, 0, reps = 10, weight = 14.0)),
            ),
        )
        assertEquals(8 * 60.0 + 10 * 14.0, volume, 0.001)
    }

    @Test
    fun volume_ignoresExercisesWithoutSets() {
        val volume = workoutVolume(
            mapOf(exerciseA.id to listOf(set(exerciseA.id, 0, reps = 5, weight = 50.0))),
        )
        assertEquals(250.0, volume, 0.001)
        assertEquals(0.0, exerciseVolume(emptyList()), 0.001)
    }

    @Test
    fun formatVolumeKg_usesThousandsSeparator() {
        assertEquals("2.450 kg", formatVolumeKg(2_450.0))
        assertEquals("480 kg", formatVolumeKg(480.0))
    }

    private fun set(
        sessionExerciseId: Long,
        setIndex: Int,
        reps: Int = 8,
        weight: Double = 60.0,
    ) = WorkoutSet(
        id = sessionExerciseId * 10 + setIndex,
        sessionExerciseId = sessionExerciseId,
        setIndex = setIndex,
        reps = reps,
        weight = weight,
        completedAtMillis = 1L,
    )
}

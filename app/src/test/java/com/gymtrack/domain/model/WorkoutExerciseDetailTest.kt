package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutExerciseDetailTest {

    private val exercise = Exercise(
        id = 10L,
        name = "Supino reto",
        muscleGroup = "Peitoral",
        equipmentType = "Barra",
    )

    private val workoutExercise = WorkoutExercise(
        id = 1L,
        workoutId = 100L,
        exerciseId = 10L,
        position = 0,
        sets = 4,
        minRepetitions = 8,
        maxRepetitions = 12,
        weight = 60.0,
        restSeconds = 90,
        notes = "Controle na descida",
    )

    @Test
    fun detail_delegatesFieldsToWorkoutExercise() {
        val detail = WorkoutExerciseDetail(workoutExercise, exercise)

        assertEquals(1L, detail.id)
        assertEquals(0, detail.position)
        assertEquals(4, detail.sets)
        assertEquals(8, detail.minRepetitions)
        assertEquals(12, detail.maxRepetitions)
        assertEquals(60.0, detail.weight, 0.001)
        assertEquals(90, detail.restSeconds)
        assertEquals("Controle na descida", detail.notes)
    }

    @Test
    fun detail_exposesExercise() {
        val detail = WorkoutExerciseDetail(workoutExercise, exercise)

        assertEquals("Supino reto", detail.exercise.name)
        assertEquals("Peitoral", detail.exercise.muscleGroup)
        assertEquals("Barra", detail.exercise.equipmentType)
    }

    @Test
    fun detail_equality_basedOnBothParts() {
        val a = WorkoutExerciseDetail(workoutExercise, exercise)
        val b = WorkoutExerciseDetail(workoutExercise, exercise)
        assertEquals(a, b)
    }

    @Test
    fun detail_copy_updatesWorkoutExercise() {
        val detail = WorkoutExerciseDetail(workoutExercise, exercise)
        val updated = detail.copy(workoutExercise = workoutExercise.copy(sets = 5))

        assertEquals(5, updated.sets)
        assertEquals(4, detail.sets)
    }
}

package com.gymtrack.presentation.workouts.detail

import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.model.WorkoutExerciseDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutDetailUiStateTest {

    private val workout = Workout(id = 1L, name = "Treino A", description = "Peito e tríceps")

    private val exercise = Exercise(id = 10L, name = "Supino", muscleGroup = "Peitoral", equipmentType = "Barra")

    private val workoutExercise = WorkoutExercise(
        id = 1L, workoutId = 1L, exerciseId = 10L, position = 0,
        sets = 4, minRepetitions = 8, maxRepetitions = 12, weight = 60.0, restSeconds = 90,
    )

    private val detail = WorkoutExerciseDetail(workoutExercise, exercise)

    @Test
    fun defaultState_isLoading_andEmpty() {
        val state = WorkoutDetailUiState()

        assertTrue(state.isLoading)
        assertNull(state.workout)
        assertTrue(state.exercises.isEmpty())
        assertNull(state.error)
        assertNull(state.sessionStartedEvent)
    }

    @Test
    fun defaultState_allDialogs_areDismissed() {
        val state = WorkoutDetailUiState()

        assertFalse(state.showEditWorkoutDialog)
        assertFalse(state.showExercisePicker)
        assertNull(state.exerciseToConfigure)
        assertFalse(state.showDeleteConfirmation)
        assertNull(state.exerciseToDelete)
    }

    @Test
    fun copy_workout_updatesOnlyWorkout() {
        val base = WorkoutDetailUiState()
        val updated = base.copy(workout = workout, isLoading = false)

        assertEquals(workout, updated.workout)
        assertFalse(updated.isLoading)
        assertTrue(updated.exercises.isEmpty())
    }

    @Test
    fun copy_exercises_updatesOnlyExercises() {
        val base = WorkoutDetailUiState(workout = workout, isLoading = false)
        val updated = base.copy(exercises = listOf(detail))

        assertEquals(1, updated.exercises.size)
        assertEquals(detail, updated.exercises[0])
        assertEquals(workout, updated.workout)
    }

    @Test
    fun copy_error_canBeSetAndCleared() {
        val withError = WorkoutDetailUiState().copy(error = "Falha ao carregar")
        assertEquals("Falha ao carregar", withError.error)

        val cleared = withError.copy(error = null)
        assertNull(cleared.error)
    }

    @Test
    fun copy_showEditWorkoutDialog_toggled() {
        val state = WorkoutDetailUiState().copy(showEditWorkoutDialog = true)
        assertTrue(state.showEditWorkoutDialog)

        val dismissed = state.copy(showEditWorkoutDialog = false)
        assertFalse(dismissed.showEditWorkoutDialog)
    }

    @Test
    fun copy_showExercisePicker_toggled() {
        val state = WorkoutDetailUiState().copy(showExercisePicker = true)
        assertTrue(state.showExercisePicker)

        val dismissed = state.copy(showExercisePicker = false)
        assertFalse(dismissed.showExercisePicker)
    }

    @Test
    fun copy_exerciseToConfigure_setAndCleared() {
        val state = WorkoutDetailUiState().copy(exerciseToConfigure = detail)
        assertEquals(detail, state.exerciseToConfigure)

        val cleared = state.copy(exerciseToConfigure = null)
        assertNull(cleared.exerciseToConfigure)
    }

    @Test
    fun copy_deleteConfirmation_setAndCleared() {
        val state = WorkoutDetailUiState().copy(
            showDeleteConfirmation = true,
            exerciseToDelete = detail,
        )
        assertTrue(state.showDeleteConfirmation)
        assertEquals(detail, state.exerciseToDelete)

        val cleared = state.copy(showDeleteConfirmation = false, exerciseToDelete = null)
        assertFalse(cleared.showDeleteConfirmation)
        assertNull(cleared.exerciseToDelete)
    }

    @Test
    fun immutability_originalNotModifiedByDerivedCopy() {
        val original = WorkoutDetailUiState()
        original.copy(isLoading = false, workout = workout)

        assertTrue(original.isLoading)
        assertNull(original.workout)
    }
}

package com.gymtrack.presentation.workouts.detail

import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutBlockType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutDetailUiStateTest {

    @Test
    fun defaultState_isLoading_andEmpty() {
        val state = WorkoutDetailUiState()
        assertTrue(state.isLoading)
        assertNull(state.workout)
        assertTrue(state.blocks.isEmpty())
        assertNull(state.error)
        assertNull(state.sessionStartedEvent)
        assertNull(state.inProgressConflict)
    }

    @Test
    fun defaultState_dialogsDismissed() {
        val state = WorkoutDetailUiState()
        assertFalse(state.showEditWorkoutDialog)
        assertFalse(state.showAddTypeDialog)
        assertFalse(state.showBlockBuilder)
        assertFalse(state.showExercisePicker)
        assertNull(state.exerciseToConfigure)
        assertFalse(state.showDeleteConfirmation)
        assertNull(state.blockToDelete)
    }

    @Test
    fun copy_workout_updatesOnlyWorkout() {
        val workout = Workout(id = 1L, name = "Treino A")
        val updated = WorkoutDetailUiState().copy(workout = workout, isLoading = false)
        assertEquals(workout, updated.workout)
        assertFalse(updated.isLoading)
        assertTrue(updated.blocks.isEmpty())
    }

    @Test
    fun copy_pendingBlockType() {
        val state = WorkoutDetailUiState().copy(pendingBlockType = WorkoutBlockType.TRI_SET)
        assertEquals(WorkoutBlockType.TRI_SET, state.pendingBlockType)
    }

    @Test
    fun copy_inProgressConflict_setAndCleared() {
        val conflict = InProgressConflictUiState(sessionId = 9L, workoutName = "Push Day")
        val state = WorkoutDetailUiState().copy(inProgressConflict = conflict)
        assertEquals(9L, state.inProgressConflict?.sessionId)
        val cleared = state.copy(inProgressConflict = null)
        assertNull(cleared.inProgressConflict)
    }
}

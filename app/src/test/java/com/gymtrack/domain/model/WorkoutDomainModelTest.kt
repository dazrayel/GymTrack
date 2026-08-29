package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WorkoutDomainModelTest {

    // ─── Workout ─────────────────────────────────────────────────────────────

    @Test
    fun workout_defaultId_isZero() {
        val workout = Workout(name = "Push Day")
        assertEquals(0L, workout.id)
    }

    @Test
    fun workout_defaultDescription_isEmpty() {
        val workout = Workout(name = "Push Day")
        assertEquals("", workout.description)
    }

    @Test
    fun workout_fieldsAreStoredCorrectly() {
        val workout = Workout(id = 7, name = "Pull Day", description = "Back and biceps")
        assertEquals(7L, workout.id)
        assertEquals("Pull Day", workout.name)
        assertEquals("Back and biceps", workout.description)
    }

    @Test
    fun workout_emptyDescription_isPreserved() {
        val workout = Workout(id = 1, name = "Leg Day", description = "")
        assertEquals("", workout.description)
    }

    @Test
    fun workout_copy_changesOnlySpecifiedField() {
        val original = Workout(id = 1, name = "Push Day", description = "Chest focus")
        val copy = original.copy(name = "Push Day Advanced")
        assertEquals(1L, copy.id)
        assertEquals("Push Day Advanced", copy.name)
        assertEquals("Chest focus", copy.description)
    }

    @Test
    fun workouts_withSameData_areEqual() {
        val a = Workout(id = 1, name = "Push Day", description = "Chest")
        val b = Workout(id = 1, name = "Push Day", description = "Chest")
        assertEquals(a, b)
    }

    @Test
    fun workouts_withDifferentIds_areNotEqual() {
        val a = Workout(id = 1, name = "Push Day", description = "")
        val b = Workout(id = 2, name = "Push Day", description = "")
        assertNotEquals(a, b)
    }

    @Test
    fun workout_hashCode_isConsistentWithEquality() {
        val a = Workout(id = 1, name = "Push Day", description = "Chest")
        val b = Workout(id = 1, name = "Push Day", description = "Chest")
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ─── WorkoutExercise ──────────────────────────────────────────────────────

    @Test
    fun workoutExercise_defaultId_isZero() {
        val we = WorkoutExercise(
            workoutId = 1, exerciseId = 1, position = 0,
            sets = 3, minRepetitions = 8, maxRepetitions = 12,
            weight = 60.0, restSeconds = 90,
        )
        assertEquals(0L, we.id)
    }

    @Test
    fun workoutExercise_defaultNotes_isEmpty() {
        val we = WorkoutExercise(
            workoutId = 1, exerciseId = 1, position = 0,
            sets = 3, minRepetitions = 8, maxRepetitions = 12,
            weight = 60.0, restSeconds = 90,
        )
        assertEquals("", we.notes)
    }

    @Test
    fun workoutExercise_allFieldsAreStoredCorrectly() {
        val we = WorkoutExercise(
            id = 5,
            workoutId = 10,
            exerciseId = 20,
            position = 2,
            sets = 4,
            minRepetitions = 6,
            maxRepetitions = 10,
            weight = 100.5,
            restSeconds = 120,
            notes = "Última série até a falha",
        )
        assertEquals(5L, we.id)
        assertEquals(10L, we.workoutId)
        assertEquals(20L, we.exerciseId)
        assertEquals(2, we.position)
        assertEquals(4, we.sets)
        assertEquals(6, we.minRepetitions)
        assertEquals(10, we.maxRepetitions)
        assertEquals(100.5, we.weight, 0.001)
        assertEquals(120, we.restSeconds)
        assertEquals("Última série até a falha", we.notes)
    }

    @Test
    fun workoutExercise_copy_changesOnlyPosition() {
        val original = WorkoutExercise(
            id = 1, workoutId = 1, exerciseId = 1, position = 0,
            sets = 3, minRepetitions = 8, maxRepetitions = 12,
            weight = 60.0, restSeconds = 90,
        )
        val moved = original.copy(position = 3)
        assertEquals(1L, moved.id)
        assertEquals(3, moved.position)
        assertEquals(original.sets, moved.sets)
        assertEquals(original.weight, moved.weight, 0.001)
    }

    @Test
    fun workoutExercises_withSameData_areEqual() {
        val a = WorkoutExercise(
            id = 1, workoutId = 2, exerciseId = 3, position = 0,
            sets = 3, minRepetitions = 8, maxRepetitions = 12,
            weight = 50.0, restSeconds = 60,
        )
        val b = WorkoutExercise(
            id = 1, workoutId = 2, exerciseId = 3, position = 0,
            sets = 3, minRepetitions = 8, maxRepetitions = 12,
            weight = 50.0, restSeconds = 60,
        )
        assertEquals(a, b)
    }

    @Test
    fun workoutExercise_minAndMaxRepetitions_areIndependent() {
        val we = WorkoutExercise(
            workoutId = 1, exerciseId = 1, position = 0,
            sets = 3, minRepetitions = 8, maxRepetitions = 12,
            weight = 0.0, restSeconds = 60,
        )
        assertEquals(8, we.minRepetitions)
        assertEquals(12, we.maxRepetitions)
    }
}

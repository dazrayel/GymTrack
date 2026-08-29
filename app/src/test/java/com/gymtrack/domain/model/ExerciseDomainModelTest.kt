package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ExerciseDomainModelTest {

    @Test
    fun exercise_defaultId_isZero() {
        val exercise = Exercise(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight")
        assertEquals(0L, exercise.id)
    }

    @Test
    fun exercise_fieldsAreStoredCorrectly() {
        val exercise = Exercise(id = 42, name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell")
        assertEquals(42L, exercise.id)
        assertEquals("Squat", exercise.name)
        assertEquals("Legs", exercise.muscleGroup)
        assertEquals("Barbell", exercise.equipmentType)
    }

    @Test
    fun exercise_copy_changesOnlySpecifiedField() {
        val original = Exercise(id = 1, name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight")
        val copy = original.copy(name = "Wide Push-up")
        assertEquals(1L, copy.id)
        assertEquals("Wide Push-up", copy.name)
        assertEquals("Chest", copy.muscleGroup)
        assertEquals("Bodyweight", copy.equipmentType)
    }

    @Test
    fun exercises_withSameData_areEqual() {
        val a = Exercise(id = 1, name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight")
        val b = Exercise(id = 1, name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight")
        assertEquals(a, b)
    }

    @Test
    fun exercises_withDifferentIds_areNotEqual() {
        val a = Exercise(id = 1, name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight")
        val b = Exercise(id = 2, name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight")
        assertNotEquals(a, b)
    }

    @Test
    fun exercises_withDifferentNames_areNotEqual() {
        val a = Exercise(id = 1, name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight")
        val b = Exercise(id = 1, name = "Pull-up", muscleGroup = "Chest", equipmentType = "Bodyweight")
        assertNotEquals(a, b)
    }

    @Test
    fun exercise_hashCode_isConsistentWithEquality() {
        val a = Exercise(id = 1, name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight")
        val b = Exercise(id = 1, name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight")
        assertEquals(a.hashCode(), b.hashCode())
    }
}

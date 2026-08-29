package com.gymtrack.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.data.local.GymTrackDatabase
import com.gymtrack.data.local.dao.ExerciseDao
import com.gymtrack.domain.model.Exercise
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for ExerciseRepositoryImpl. Covers item 8.
 *
 * Verifies that Exercise ↔ ExerciseEntity mapping is correct via round-trips:
 * the functions that convert between domain and entity objects are private,
 * so they are tested indirectly by saving an Exercise and reading it back,
 * comparing every field.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExerciseRepositoryImplTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var db: GymTrackDatabase
    private lateinit var dao: ExerciseDao
    private lateinit var repository: ExerciseRepositoryImpl

    @Before
    fun setUp() {
        hiltRule.inject()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.exerciseDao()
        repository = ExerciseRepositoryImpl(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ─── Item 8: Mapeamento Exercise ↔ ExerciseEntity ────────────────────────

    @Test
    fun save_and_getAll_allFieldsMappedCorrectly() = runBlocking {
        val exercise = Exercise(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight")

        val savedId = repository.save(exercise)

        val all = repository.getAll().first()
        assertEquals(1, all.size)
        val result = all[0]
        assertEquals(savedId, result.id)
        assertEquals("Push-up", result.name)
        assertEquals("Chest", result.muscleGroup)
        assertEquals("Bodyweight", result.equipmentType)
    }

    @Test
    fun save_withIdZero_generatesAutoIncrementedId() = runBlocking {
        val exercise = Exercise(id = 0, name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell")

        val generatedId = repository.save(exercise)

        assertTrue("Generated ID should be positive", generatedId > 0)
    }

    @Test
    fun getById_returnsMappedExerciseWithAllFields() = runBlocking {
        val id = repository.save(Exercise(name = "Deadlift", muscleGroup = "Back", equipmentType = "Barbell"))

        val result = repository.getById(id).first()

        assertNotNull(result)
        assertEquals(id, result!!.id)
        assertEquals("Deadlift", result.name)
        assertEquals("Back", result.muscleGroup)
        assertEquals("Barbell", result.equipmentType)
    }

    @Test
    fun getById_forNonExistentId_returnsNull() = runBlocking {
        val result = repository.getById(9999L).first()
        assertNull(result)
    }

    @Test
    fun search_returnsDomainObjectsWithAllFieldsMapped() = runBlocking {
        repository.save(Exercise(name = "Bench Press", muscleGroup = "Chest", equipmentType = "Barbell"))
        repository.save(Exercise(name = "Incline Press", muscleGroup = "Chest", equipmentType = "Dumbbell"))
        repository.save(Exercise(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"))

        val result = repository.search("Press").first()

        assertEquals(2, result.size)
        val benchPress = result.find { it.name == "Bench Press" }
        val inclinePress = result.find { it.name == "Incline Press" }
        assertNotNull(benchPress)
        assertNotNull(inclinePress)
        assertEquals("Chest", benchPress!!.muscleGroup)
        assertEquals("Barbell", benchPress.equipmentType)
        assertEquals("Chest", inclinePress!!.muscleGroup)
        assertEquals("Dumbbell", inclinePress.equipmentType)
    }

    @Test
    fun save_withExistingId_updatesExercise_preservingId() = runBlocking {
        val id = repository.save(Exercise(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))

        repository.save(Exercise(id = id, name = "Wide Push-up", muscleGroup = "Shoulders", equipmentType = "Bodyweight"))

        val all = repository.getAll().first()
        assertEquals(1, all.size)
        assertEquals(id, all[0].id)
        assertEquals("Wide Push-up", all[0].name)
        assertEquals("Shoulders", all[0].muscleGroup)
    }

    @Test
    fun delete_removesExerciseFromStorage() = runBlocking {
        val id = repository.save(Exercise(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))
        val saved = repository.getById(id).first()!!

        repository.delete(saved)

        assertNull(repository.getById(id).first())
        assertTrue(repository.getAll().first().isEmpty())
    }

    @Test
    fun delete_preservesOtherExercises() = runBlocking {
        val id1 = repository.save(Exercise(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))
        repository.save(Exercise(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"))

        val toDelete = repository.getById(id1).first()!!
        repository.delete(toDelete)

        val remaining = repository.getAll().first()
        assertEquals(1, remaining.size)
        assertEquals("Squat", remaining[0].name)
    }

    @Test
    fun getAll_returnsDomainObjects_notEntities() = runBlocking {
        repository.save(Exercise(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))

        val result = repository.getAll().first()

        assertTrue(result.all { it is Exercise })
    }
}

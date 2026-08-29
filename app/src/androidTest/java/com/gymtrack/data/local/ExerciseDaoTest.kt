package com.gymtrack.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.data.local.dao.ExerciseDao
import com.gymtrack.data.local.entity.ExerciseEntity
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
 * Instrumented tests for ExerciseDao. Covers items 1–7.
 * Uses an in-memory database to ensure isolation between test runs.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExerciseDaoTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var db: GymTrackDatabase
    private lateinit var dao: ExerciseDao

    @Before
    fun setUp() {
        hiltRule.inject()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.exerciseDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ─── Item 1: Inserção ────────────────────────────────────────────────────

    @Test
    fun insert_returnsPositiveRowId() {
        val entity = ExerciseEntity(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight")
        val id = dao.insert(entity)
        assertTrue("Row ID should be positive", id > 0)
    }

    @Test
    fun insert_multipleExercises_eachGetsUniqueId() {
        val id1 = dao.insert(ExerciseEntity(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))
        val id2 = dao.insert(ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"))
        assertTrue(id1 != id2)
    }

    // ─── Item 2: Busca de todos ──────────────────────────────────────────────

    @Test
    fun getAll_emptyDatabase_returnsEmptyList() = runBlocking {
        val result = dao.getAll().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun getAll_afterInserts_returnsAllExercisesSortedByName() = runBlocking {
        dao.insert(ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"))
        dao.insert(ExerciseEntity(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))
        dao.insert(ExerciseEntity(name = "Deadlift", muscleGroup = "Back", equipmentType = "Barbell"))

        val result = dao.getAll().first()

        assertEquals(3, result.size)
        assertEquals("Deadlift", result[0].name)
        assertEquals("Push-up", result[1].name)
        assertEquals("Squat", result[2].name)
    }

    // ─── Item 3: Busca por ID ────────────────────────────────────────────────

    @Test
    fun getById_returnsCorrectExercise() = runBlocking {
        dao.insert(ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"))
        val id = dao.insert(ExerciseEntity(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))

        val result = dao.getById(id).first()

        assertNotNull(result)
        assertEquals(id, result!!.id)
        assertEquals("Push-up", result.name)
        assertEquals("Chest", result.muscleGroup)
        assertEquals("Bodyweight", result.equipmentType)
    }

    @Test
    fun getById_forNonExistentId_returnsNull() = runBlocking {
        val result = dao.getById(999L).first()
        assertNull(result)
    }

    // ─── Item 4: Busca por nome ──────────────────────────────────────────────

    @Test
    fun search_byExactName_returnsSingleMatch() = runBlocking {
        dao.insert(ExerciseEntity(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))
        dao.insert(ExerciseEntity(name = "Pull-up", muscleGroup = "Back", equipmentType = "Bar"))
        dao.insert(ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"))

        val result = dao.search("Squat").first()

        assertEquals(1, result.size)
        assertEquals("Squat", result[0].name)
    }

    @Test
    fun search_byPartialName_returnsAllMatches() = runBlocking {
        dao.insert(ExerciseEntity(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))
        dao.insert(ExerciseEntity(name = "Pull-up", muscleGroup = "Back", equipmentType = "Bar"))
        dao.insert(ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"))

        val result = dao.search("up").first()

        assertEquals(2, result.size)
        assertTrue(result.all { "up" in it.name.lowercase() })
    }

    @Test
    fun search_caseInsensitive_returnsMatches() = runBlocking {
        dao.insert(ExerciseEntity(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))

        val result = dao.search("PUSH").first()

        assertEquals(1, result.size)
    }

    @Test
    fun search_withNoMatch_returnsEmptyList() = runBlocking {
        dao.insert(ExerciseEntity(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))

        val result = dao.search("xyz_no_match").first()

        assertTrue(result.isEmpty())
    }

    // ─── Item 5: Atualização ─────────────────────────────────────────────────

    @Test
    fun insert_withExistingId_replacesExercise() = runBlocking {
        val id = dao.insert(ExerciseEntity(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))

        dao.insert(ExerciseEntity(id = id, name = "Wide Push-up", muscleGroup = "Shoulders", equipmentType = "Bodyweight"))

        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("Wide Push-up", all[0].name)
        assertEquals("Shoulders", all[0].muscleGroup)
    }

    @Test
    fun insert_withExistingId_doesNotCreateDuplicate() = runBlocking {
        val id = dao.insert(ExerciseEntity(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))
        dao.insert(ExerciseEntity(id = id, name = "Wide Push-up", muscleGroup = "Shoulders", equipmentType = "Bodyweight"))

        val all = dao.getAll().first()
        assertEquals(1, all.size)
    }

    // ─── Item 6: Exclusão ────────────────────────────────────────────────────

    @Test
    fun deleteById_removesExercise() = runBlocking {
        val id = dao.insert(ExerciseEntity(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))

        dao.deleteById(id)

        assertNull(dao.getById(id).first())
        assertTrue(dao.getAll().first().isEmpty())
    }

    @Test
    fun deleteById_returnsAffectedRowCount() {
        val id = dao.insert(ExerciseEntity(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))
        val deletedCount = dao.deleteById(id)
        assertEquals(1, deletedCount)
    }

    @Test
    fun deleteById_forNonExistentId_returnsZero() {
        val deletedCount = dao.deleteById(999L)
        assertEquals(0, deletedCount)
    }

    @Test
    fun deleteById_removesOnlyTargetExercise() = runBlocking {
        val id1 = dao.insert(ExerciseEntity(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))
        dao.insert(ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"))

        dao.deleteById(id1)

        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("Squat", all[0].name)
    }

    // ─── Item 7: Flow reflete alterações ─────────────────────────────────────

    @Test
    fun getAll_flowReflectsInsert() = runBlocking {
        val before = dao.getAll().first()
        assertTrue(before.isEmpty())

        dao.insert(ExerciseEntity(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))

        val after = dao.getAll().first()
        assertEquals(1, after.size)
        assertEquals("Push-up", after[0].name)
    }

    @Test
    fun getAll_flowReflectsDelete() = runBlocking {
        val id = dao.insert(ExerciseEntity(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))
        assertEquals(1, dao.getAll().first().size)

        dao.deleteById(id)

        assertTrue(dao.getAll().first().isEmpty())
    }

    @Test
    fun getAll_flowReflectsUpdate() = runBlocking {
        val id = dao.insert(ExerciseEntity(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))
        assertEquals("Push-up", dao.getAll().first()[0].name)

        dao.insert(ExerciseEntity(id = id, name = "Wide Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"))

        assertEquals("Wide Push-up", dao.getAll().first()[0].name)
    }
}

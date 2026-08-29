package com.gymtrack.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.data.local.dao.WorkoutDao
import com.gymtrack.data.local.entity.WorkoutEntity
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

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WorkoutDaoTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var db: GymTrackDatabase
    private lateinit var dao: WorkoutDao

    @Before
    fun setUp() {
        hiltRule.inject()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.workoutDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ─── Inserção ────────────────────────────────────────────────────────────

    @Test
    fun insert_returnsPositiveRowId() {
        val entity = WorkoutEntity(name = "Push Day", description = "Chest and triceps")
        val id = dao.insert(entity)
        assertTrue("Row ID should be positive", id > 0)
    }

    @Test
    fun insert_multipleWorkouts_eachGetsUniqueId() {
        val id1 = dao.insert(WorkoutEntity(name = "Push Day", description = ""))
        val id2 = dao.insert(WorkoutEntity(name = "Pull Day", description = ""))
        assertTrue(id1 != id2)
    }

    @Test
    fun insert_withDefaultDescription_storesEmptyString() = runBlocking {
        val id = dao.insert(WorkoutEntity(name = "Leg Day"))
        val result = dao.getById(id).first()
        assertNotNull(result)
        assertEquals("", result!!.description)
    }

    // ─── Busca de todos ──────────────────────────────────────────────────────

    @Test
    fun getAll_emptyDatabase_returnsEmptyList() = runBlocking {
        val result = dao.getAll().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun getAll_afterInserts_returnsAllWorkouts() = runBlocking {
        dao.insert(WorkoutEntity(name = "Push Day", description = ""))
        dao.insert(WorkoutEntity(name = "Pull Day", description = ""))

        val result = dao.getAll().first()
        assertEquals(2, result.size)
    }

    @Test
    fun getAll_isSortedByNameAscending() = runBlocking {
        dao.insert(WorkoutEntity(name = "Push Day", description = ""))
        dao.insert(WorkoutEntity(name = "Leg Day", description = ""))
        dao.insert(WorkoutEntity(name = "Pull Day", description = ""))

        val result = dao.getAll().first()
        assertEquals(3, result.size)
        assertEquals("Leg Day", result[0].name)
        assertEquals("Pull Day", result[1].name)
        assertEquals("Push Day", result[2].name)
    }

    // ─── Busca por ID ────────────────────────────────────────────────────────

    @Test
    fun getById_returnsCorrectWorkout() = runBlocking {
        dao.insert(WorkoutEntity(name = "Push Day", description = ""))
        val id = dao.insert(WorkoutEntity(name = "Pull Day", description = "Back and biceps"))

        val result = dao.getById(id).first()

        assertNotNull(result)
        assertEquals(id, result!!.id)
        assertEquals("Pull Day", result.name)
        assertEquals("Back and biceps", result.description)
    }

    @Test
    fun getById_forNonExistentId_returnsNull() = runBlocking {
        val result = dao.getById(999L).first()
        assertNull(result)
    }

    // ─── Atualização ─────────────────────────────────────────────────────────

    @Test
    fun insert_withExistingId_replacesWorkout() = runBlocking {
        val id = dao.insert(WorkoutEntity(name = "Push Day", description = "Original"))

        dao.insert(WorkoutEntity(id = id, name = "Push Day Advanced", description = "Updated"))

        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("Push Day Advanced", all[0].name)
        assertEquals("Updated", all[0].description)
    }

    @Test
    fun insert_withExistingId_doesNotCreateDuplicate() = runBlocking {
        val id = dao.insert(WorkoutEntity(name = "Push Day", description = ""))
        dao.insert(WorkoutEntity(id = id, name = "Push Day Advanced", description = ""))

        val all = dao.getAll().first()
        assertEquals(1, all.size)
    }

    // ─── Exclusão ────────────────────────────────────────────────────────────

    @Test
    fun deleteById_removesWorkout() = runBlocking {
        val id = dao.insert(WorkoutEntity(name = "Push Day", description = ""))

        dao.deleteById(id)

        assertNull(dao.getById(id).first())
        assertTrue(dao.getAll().first().isEmpty())
    }

    @Test
    fun deleteById_returnsAffectedRowCount() {
        val id = dao.insert(WorkoutEntity(name = "Push Day", description = ""))
        val deletedCount = dao.deleteById(id)
        assertEquals(1, deletedCount)
    }

    @Test
    fun deleteById_forNonExistentId_returnsZero() {
        val deletedCount = dao.deleteById(999L)
        assertEquals(0, deletedCount)
    }

    @Test
    fun deleteById_removesOnlyTargetWorkout() = runBlocking {
        val id1 = dao.insert(WorkoutEntity(name = "Push Day", description = ""))
        dao.insert(WorkoutEntity(name = "Pull Day", description = ""))

        dao.deleteById(id1)

        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("Pull Day", all[0].name)
    }

    // ─── Flow reflete alterações ─────────────────────────────────────────────

    @Test
    fun getAll_flowReflectsInsert() = runBlocking {
        assertTrue(dao.getAll().first().isEmpty())

        dao.insert(WorkoutEntity(name = "Push Day", description = ""))

        assertEquals(1, dao.getAll().first().size)
    }

    @Test
    fun getAll_flowReflectsDelete() = runBlocking {
        val id = dao.insert(WorkoutEntity(name = "Push Day", description = ""))
        assertEquals(1, dao.getAll().first().size)

        dao.deleteById(id)

        assertTrue(dao.getAll().first().isEmpty())
    }

    @Test
    fun getAll_flowReflectsUpdate() = runBlocking {
        val id = dao.insert(WorkoutEntity(name = "Push Day", description = ""))
        assertEquals("Push Day", dao.getAll().first()[0].name)

        dao.insert(WorkoutEntity(id = id, name = "Push Day Advanced", description = ""))

        assertEquals("Push Day Advanced", dao.getAll().first()[0].name)
    }
}

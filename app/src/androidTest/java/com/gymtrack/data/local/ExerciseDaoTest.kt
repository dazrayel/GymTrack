package com.gymtrack.data.local

import android.database.sqlite.SQLiteConstraintException
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

    @Test
    fun insert_manualExercise_hasNullExternalIdentity() = runBlocking {
        val id = dao.insert(
            ExerciseEntity(name = "Meu exercício", muscleGroup = "Peitoral", equipmentType = "Barra"),
        )

        val stored = dao.getById(id).first()!!
        assertNull(stored.externalSource)
        assertNull(stored.externalId)
    }

    @Test
    fun insert_importedAndOtherSource_sameExternalId_canCoexist() = runBlocking {
        dao.insert(
            ExerciseEntity(
                name = "Sit-Up FEDB",
                muscleGroup = "Abdômen",
                equipmentType = "Peso corporal",
                externalSource = "free-exercise-db",
                externalId = "abc",
            ),
        )
        dao.insert(
            ExerciseEntity(
                name = "Sit-Up Wger",
                muscleGroup = "Abdômen",
                equipmentType = "Peso corporal",
                externalSource = "wger",
                externalId = "abc",
            ),
        )

        val all = dao.getAll().first()
        assertEquals(2, all.size)
        assertNotNull(dao.getByExternalIdentity("free-exercise-db", "abc"))
        assertNotNull(dao.getByExternalIdentity("wger", "abc"))
    }

    @Test
    fun uniqueIndex_rejectsDuplicateExternalIdentity() {
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO exercises (name, muscleGroup, equipmentType, externalSource, externalId) " +
                "VALUES ('First', 'Abdômen', 'Peso corporal', 'free-exercise-db', 'abc')",
        )
        try {
            db.openHelper.writableDatabase.execSQL(
                "INSERT INTO exercises (name, muscleGroup, equipmentType, externalSource, externalId) " +
                    "VALUES ('Second', 'Abdômen', 'Peso corporal', 'free-exercise-db', 'abc')",
            )
            org.junit.Assert.fail("Duplicate (externalSource, externalId) should be rejected")
        } catch (_: SQLiteConstraintException) {
            // expected — UNIQUE index, default ABORT
        }
    }

    /**
     * ExerciseDao.insert uses OnConflictStrategy.REPLACE. A unique-index clash therefore
     * deletes the previous row (and CASCADE `workout_exercises`) instead of aborting.
     * Etapa 3B must not upsert imported exercises through this insert.
     */
    @Test
    fun insert_replaceOnDuplicateExternalIdentity_deletesPreviousRow() = runBlocking {
        val firstId = dao.insert(
            ExerciseEntity(
                name = "First",
                muscleGroup = "Abdômen",
                equipmentType = "Peso corporal",
                externalSource = "free-exercise-db",
                externalId = "abc",
            ),
        )
        val secondId = dao.insert(
            ExerciseEntity(
                name = "Second",
                muscleGroup = "Abdômen",
                equipmentType = "Peso corporal",
                externalSource = "free-exercise-db",
                externalId = "abc",
            ),
        )

        val all = dao.getAll().first()
        assertEquals(1, all.size)
        assertEquals("Second", all[0].name)
        assertTrue(secondId != firstId)
        assertNull(dao.getById(firstId).first())
    }

    @Test
    fun insert_multipleManualNullIdentities_areAllowed() = runBlocking {
        dao.insert(ExerciseEntity(name = "Manual A", muscleGroup = "Peitoral", equipmentType = "Barra"))
        dao.insert(ExerciseEntity(name = "Manual B", muscleGroup = "Costas", equipmentType = "Barra"))
        dao.insert(ExerciseEntity(name = "Manual C", muscleGroup = "Ombros", equipmentType = "Halteres"))

        val manuals = dao.getAll().first().filter { it.externalSource == null && it.externalId == null }
        assertEquals(3, manuals.size)
    }

    /**
     * SQLite UNIQUE treats each NULL as distinct from every other NULL.
     * Incomplete identities are not produced by the future importer, but the index
     * therefore allows more than one (null, "abc") and more than one ("source", null).
     */
    @Test
    fun insert_partialExternalIdentity_allowsDuplicatesBecauseSqliteTreatsNullAsDistinct() =
        runBlocking {
            dao.insert(
                ExerciseEntity(
                    name = "Partial A",
                    muscleGroup = "Peitoral",
                    equipmentType = "Barra",
                    externalSource = null,
                    externalId = "abc",
                ),
            )
            dao.insert(
                ExerciseEntity(
                    name = "Partial B",
                    muscleGroup = "Peitoral",
                    equipmentType = "Barra",
                    externalSource = null,
                    externalId = "abc",
                ),
            )
            dao.insert(
                ExerciseEntity(
                    name = "Partial C",
                    muscleGroup = "Peitoral",
                    equipmentType = "Barra",
                    externalSource = "free-exercise-db",
                    externalId = null,
                ),
            )
            dao.insert(
                ExerciseEntity(
                    name = "Partial D",
                    muscleGroup = "Peitoral",
                    equipmentType = "Barra",
                    externalSource = "free-exercise-db",
                    externalId = null,
                ),
            )

            val all = dao.getAll().first()
            assertEquals(4, all.size)
        }

    @Test
    fun update_manualExercise_doesNotAssignExternalIdentity() = runBlocking {
        val id = dao.insert(
            ExerciseEntity(name = "Push-up", muscleGroup = "Chest", equipmentType = "Bodyweight"),
        )

        dao.update(
            ExerciseEntity(
                id = id,
                name = "Wide Push-up",
                muscleGroup = "Shoulders",
                equipmentType = "Bodyweight",
            ),
        )

        val stored = dao.getById(id).first()!!
        assertEquals("Wide Push-up", stored.name)
        assertNull(stored.externalSource)
        assertNull(stored.externalId)
    }

    @Test
    fun update_importedExercise_preservesExternalIdentity() = runBlocking {
        val id = dao.insert(
            ExerciseEntity(
                name = "Sit-Up",
                muscleGroup = "Abdômen",
                equipmentType = "Peso corporal",
                externalSource = "free-exercise-db",
                externalId = "3_4_Sit-Up",
            ),
        )

        dao.update(
            ExerciseEntity(
                id = id,
                name = "3/4 Sit-Up",
                muscleGroup = "Abdômen",
                equipmentType = "Peso corporal",
                externalSource = "free-exercise-db",
                externalId = "3_4_Sit-Up",
            ),
        )

        val stored = dao.getById(id).first()!!
        assertEquals("3/4 Sit-Up", stored.name)
        assertEquals(id, stored.id)
        assertEquals("free-exercise-db", stored.externalSource)
        assertEquals("3_4_Sit-Up", stored.externalId)
    }
}

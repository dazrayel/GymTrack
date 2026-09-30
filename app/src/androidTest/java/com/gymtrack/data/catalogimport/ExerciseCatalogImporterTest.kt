package com.gymtrack.data.catalogimport

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.data.local.GymTrackDatabase
import com.gymtrack.data.local.entity.ExerciseEntity
import com.gymtrack.data.local.entity.WorkoutEntity
import com.gymtrack.data.local.entity.WorkoutExerciseEntity
import com.gymtrack.data.local.entity.WorkoutSessionEntity
import com.gymtrack.data.local.entity.WorkoutSessionExerciseEntity
import com.gymtrack.domain.catalogimport.ExerciseCatalogPackException
import com.gymtrack.domain.model.WorkoutSessionExerciseStatus
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExerciseCatalogImporterTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var db: GymTrackDatabase
    private lateinit var importer: ExerciseCatalogImporter

    @Before
    fun setUp() {
        hiltRule.inject()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        importer = ExerciseCatalogImporter(
            context,
            db,
            db.exerciseDao(),
            db.exerciseSecondaryMuscleDao(),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun firstImport_insertsWithoutUpdates() = runBlocking {
        val result = importer.importJson(pack(item("exercise-1", "Nome antigo")))
        assertEquals(1, result.inserted)
        assertEquals(0, result.updated)
        assertEquals(0, result.unchanged)
        assertEquals(0, result.rejected)
        assertEquals(1, db.exerciseDao().countByExternalSource("free-exercise-db"))
    }

    @Test
    fun secondImport_updatesWithoutInsertingDuplicates() = runBlocking {
        importer.importJson(pack(item("exercise-1", "Nome antigo")))
        val result = importer.importJson(pack(item("exercise-1", "Nome antigo")))
        assertEquals(0, result.inserted)
        assertEquals(0, result.updated)
        assertEquals(1, result.unchanged)
        assertEquals(1, db.exerciseDao().countByExternalSource("free-exercise-db"))
    }

    @Test
    fun thirdImport_doesNotDuplicate() = runBlocking {
        val json = pack(item("exercise-1", "Nome"))
        importer.importJson(json)
        importer.importJson(json)
        val third = importer.importJson(json)
        assertEquals(0, third.inserted)
        assertEquals(1, db.exerciseDao().countByExternalSource("free-exercise-db"))
    }

    @Test
    fun updateName_preservesRoomId() = runBlocking {
        importer.importJson(pack(item("exercise-1", "Nome antigo")))
        val before = db.exerciseDao().getByExternalIdentity("free-exercise-db", "exercise-1")!!
        importer.importJson(pack(item("exercise-1", "Nome novo")))
        val after = db.exerciseDao().getByExternalIdentity("free-exercise-db", "exercise-1")!!
        assertEquals(before.id, after.id)
        assertEquals("Nome novo", after.name)
        assertEquals("free-exercise-db", after.externalSource)
        assertEquals("exercise-1", after.externalId)
    }

    @Test
    fun updateMuscleAndEquipment() = runBlocking {
        importer.importJson(
            pack(
                item(
                    "exercise-1",
                    "Supino",
                    muscle = "Peitoral",
                    equipment = "Barra",
                    sourceEq = "barbell",
                ),
            ),
        )
        importer.importJson(
            pack(
                item(
                    "exercise-1",
                    "Supino",
                    muscle = "Ombros",
                    equipment = "Halteres",
                    sourceEq = "dumbbell",
                    primaries = listOf("shoulders"),
                ),
            ),
        )
        val stored = db.exerciseDao().getByExternalIdentity("free-exercise-db", "exercise-1")!!
        assertEquals("Ombros", stored.muscleGroup)
        assertEquals("Halteres", stored.equipmentType)
    }

    @Test
    fun updateSecondaryMuscles() = runBlocking {
        importer.importJson(
            pack(item("exercise-1", "Supino", secondaries = listOf("Ombros"))),
        )
        importer.importJson(
            pack(item("exercise-1", "Supino", secondaries = listOf("Tríceps", "Abdômen"))),
        )
        val muscles = db.exerciseSecondaryMuscleDao().getMusclesOnce(
            db.exerciseDao().getByExternalIdentity("free-exercise-db", "exercise-1")!!.id,
        )
        assertEquals(listOf("Abdômen", "Tríceps"), muscles)
    }

    @Test
    fun manualWithSameName_coexistsAndIsUntouched() = runBlocking {
        val manualId = db.exerciseDao().insert(
            ExerciseEntity(name = "Supino", muscleGroup = "Peitoral", equipmentType = "Barra"),
        )
        importer.importJson(pack(item("abc", "Supino")))
        val manual = db.exerciseDao().getById(manualId).first()!!
        val imported = db.exerciseDao().getByExternalIdentity("free-exercise-db", "abc")!!
        assertEquals("Supino", manual.name)
        assertNull(manual.externalSource)
        assertNull(manual.externalId)
        assertEquals("Supino", imported.name)
        assertTrue(manual.id != imported.id)
        importer.importJson(pack(item("abc", "Bench Press")))
        assertEquals("Supino", db.exerciseDao().getById(manualId).first()!!.name)
        assertEquals("Bench Press", db.exerciseDao().getByExternalIdentity("free-exercise-db", "abc")!!.name)
    }

    @Test
    fun sameExternalId_differentSources_doNotOverwriteEachOther() = runBlocking {
        importer.importJson(
            """
            [
              ${item("abc", "FEDB", source = "free-exercise-db")},
              ${item("abc", "Wger", source = "wger", sourceEq = "barbell")}
            ]
            """.trimIndent(),
        )
        val fedb = db.exerciseDao().getByExternalIdentity("free-exercise-db", "abc")!!
        val wger = db.exerciseDao().getByExternalIdentity("wger", "abc")!!
        assertEquals("FEDB", fedb.name)
        assertEquals("Wger", wger.name)
        importer.importJson("""[${item("abc", "Wger 2", source = "wger", sourceEq = "barbell")}]""")
        assertEquals("FEDB", db.exerciseDao().getByExternalIdentity("free-exercise-db", "abc")!!.name)
        assertEquals("Wger 2", db.exerciseDao().getByExternalIdentity("wger", "abc")!!.name)
    }

    @Test
    fun removedFromPack_isNotDeleted() = runBlocking {
        importer.importJson(
            pack(
                item("keep", "Keep"),
                item("gone", "Gone"),
            ),
        )
        importer.importJson(pack(item("keep", "Keep")))
        assertNotNull(db.exerciseDao().getByExternalIdentity("free-exercise-db", "gone"))
        assertEquals(2, db.exerciseDao().countByExternalSource("free-exercise-db"))
    }

    @Test
    fun invalidRecord_isNotInserted() = runBlocking {
        val result = importer.importJson(
            pack(
                item("ok", "Ok"),
                """
                {
                  "source": "free-exercise-db",
                  "externalId": "bad",
                  "name": "Neck",
                  "muscleGroup": null,
                  "equipmentType": "Peso corporal",
                  "secondaryMuscles": [],
                  "sourceData": { "equipment": "body only", "primaryMuscles": ["neck"] }
                }
                """.trimIndent(),
            ),
        )
        assertEquals(1, result.inserted)
        assertEquals(1, result.rejected)
        assertNull(db.exerciseDao().getByExternalIdentity("free-exercise-db", "bad"))
    }

    @Test
    fun updateImported_preservesWorkoutExerciseRelation() = runBlocking {
        importer.importJson(pack(item("exercise-1", "Nome antigo")))
        val exerciseId = db.exerciseDao().getByExternalIdentity("free-exercise-db", "exercise-1")!!.id
        val workoutId = db.workoutDao().insert(WorkoutEntity(name = "A", description = ""))
        val relationId = db.workoutExerciseDao().insert(
            WorkoutExerciseEntity(
                workoutId = workoutId,
                exerciseId = exerciseId,
                position = 0,
                sets = 3,
                minRepetitions = 8,
                maxRepetitions = 12,
                weight = 60.0,
                restSeconds = 90,
            ),
        )
        importer.importJson(pack(item("exercise-1", "Nome novo")))
        val after = db.exerciseDao().getByExternalIdentity("free-exercise-db", "exercise-1")!!
        val relation = db.workoutExerciseDao().getById(relationId).first()!!
        assertEquals(exerciseId, after.id)
        assertEquals(exerciseId, relation.exerciseId)
        assertEquals(workoutId, relation.workoutId)
    }

    @Test
    fun updateImported_preservesSessionSnapshot() = runBlocking {
        importer.importJson(pack(item("exercise-1", "Nome antigo")))
        val exerciseId = db.exerciseDao().getByExternalIdentity("free-exercise-db", "exercise-1")!!.id
        val workoutId = db.workoutDao().insert(WorkoutEntity(name = "A", description = ""))
        db.workoutExerciseDao().insert(
            WorkoutExerciseEntity(
                workoutId = workoutId,
                exerciseId = exerciseId,
                position = 0,
                sets = 3,
                minRepetitions = 8,
                maxRepetitions = 12,
                weight = 60.0,
                restSeconds = 90,
            ),
        )
        val sessionId = db.workoutSessionDao().insert(
            WorkoutSessionEntity(
                workoutId = workoutId,
                workoutName = "A",
                startedAtMillis = 1_000L,
                status = WorkoutSessionEntity.STATUS_COMPLETED,
            ),
        )
        val snapshotId = db.workoutSessionExerciseDao().insert(
            WorkoutSessionExerciseEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                position = 0,
                exerciseName = "Nome antigo",
                muscleGroup = "Peitoral",
                equipmentType = "Barra",
                plannedSets = 3,
                minRepetitions = 8,
                maxRepetitions = 12,
                plannedWeight = 60.0,
                restSeconds = 90,
                notes = "",
                secondaryMuscles = "",
                status = WorkoutSessionExerciseStatus.PENDING.name,
            ),
        )
        importer.importJson(pack(item("exercise-1", "Nome novo")))
        val snapshot = db.workoutSessionExerciseDao().getByIdOnce(snapshotId)!!
        val session = db.workoutSessionDao().getByIdOnce(sessionId)!!
        assertEquals("Nome antigo", snapshot.exerciseName)
        assertEquals(exerciseId, snapshot.exerciseId)
        assertEquals(WorkoutSessionEntity.STATUS_COMPLETED, session.status)
        assertEquals("Nome novo", db.exerciseDao().getById(exerciseId).first()!!.name)
    }

    @Test
    fun fatalError_rollsBackPartialImport() = runBlocking {
        importer.failAfterPersistedCount = 1
        try {
            importer.importJson(
                pack(
                    item("one", "One"),
                    item("two", "Two"),
                ),
            )
            fail("Expected forced failure")
        } catch (_: ExerciseCatalogPackException) {
            // expected
        }
        assertEquals(0, db.exerciseDao().countByExternalSource("free-exercise-db"))
        assertTrue(db.exerciseDao().getAll().first().isEmpty())
    }

    @Test
    fun duplicateIdentityInPack_doesNotPersist() = runBlocking {
        try {
            importer.importJson(
                pack(
                    item("abc", "A"),
                    item("abc", "B"),
                ),
            )
            fail("Expected pack failure")
        } catch (_: ExerciseCatalogPackException) {
            // expected
        }
        assertTrue(db.exerciseDao().getAll().first().isEmpty())
    }

    @Test
    fun manualsRemainWhenImportingRealPackShape() = runBlocking {
        repeat(3) { index ->
            db.exerciseDao().insert(
                ExerciseEntity(
                    name = "Manual $index",
                    muscleGroup = "Peitoral",
                    equipmentType = "Barra",
                ),
            )
        }
        val result = importer.importJson(
            pack(item("ex-1", "Imported"), item("ex-2", "Imported 2")),
        )
        assertEquals(2, result.inserted)
        val all = db.exerciseDao().getAll().first()
        assertEquals(5, all.size)
        assertEquals(3, all.count { it.externalSource == null })
        assertEquals(2, db.exerciseDao().countByExternalSource("free-exercise-db"))
    }

    @Test
    fun realAsset_importIsIdempotentForPublishedV21Catalog() = runBlocking {
        // Stage 4.9: published pack is MAIN_CATALOG_V2_1 (136). Stage 2.5
        // 876/777/99 remains on the full archive, not on the Android asset.
        val first = importer.importDefaultAsset()
        assertEquals(136, first.inputCount)
        assertEquals(136, first.importableCount)
        assertEquals(0, first.rejected)
        assertEquals(136, first.inserted)
        assertEquals(0, first.updated)
        assertEquals(0, first.unchanged)
        assertEquals(0, first.errors)
        assertEquals(136, db.exerciseDao().countByExternalSource("free-exercise-db"))

        val second = importer.importDefaultAsset()
        assertEquals(0, second.inserted)
        assertEquals(0, second.updated)
        assertEquals(136, second.unchanged)
        assertEquals(0, second.rejected)
        assertEquals(136, db.exerciseDao().countByExternalSource("free-exercise-db"))

        val third = importer.importDefaultAsset()
        assertEquals(0, third.inserted)
        assertEquals(136, third.unchanged)
        assertEquals(136, db.exerciseDao().countByExternalSource("free-exercise-db"))
        assertEquals(
            136,
            db.exerciseDao().getAll().first().count { it.externalSource == "free-exercise-db" },
        )
    }

    private fun pack(vararg items: String): String = items.joinToString(prefix = "[", postfix = "]")

    private fun item(
        externalId: String,
        name: String,
        source: String = "free-exercise-db",
        muscle: String = "Peitoral",
        equipment: String? = "Barra",
        sourceEq: String? = "barbell",
        primaries: List<String> = listOf("chest"),
        secondaries: List<String> = emptyList(),
    ): String {
        val equipmentJson = equipment?.let { "\"$it\"" } ?: "null"
        val sourceEqJson = sourceEq?.let { "\"$it\"" } ?: "null"
        val primaryJson = primaries.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        val secondaryJson = secondaries.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        return """
            {
              "source": "$source",
              "externalId": "$externalId",
              "name": "$name",
              "muscleGroup": "$muscle",
              "equipmentType": $equipmentJson,
              "secondaryMuscles": $secondaryJson,
              "sourceData": {
                "equipment": $sourceEqJson,
                "primaryMuscles": $primaryJson
              }
            }
        """.trimIndent()
    }
}

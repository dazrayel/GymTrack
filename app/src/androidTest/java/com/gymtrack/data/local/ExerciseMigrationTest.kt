package com.gymtrack.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for the Room migration 1 → 2. Covers item 9.
 *
 * The [MigrationTestHelper] reads the exported schema JSON files (in app/schemas/)
 * configured as test assets via sourceSets.androidTest.assets in build.gradle.kts.
 * It validates that the resulting schema after migration matches the version-2 schema exactly.
 */
@RunWith(AndroidJUnit4::class)
class ExerciseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GymTrackDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migration1To2_schemValidatesAndExercisesTableIsUsable() {
        // Create database at version 1 (app_metadata table)
        helper.createDatabase(TEST_DB, 1).use { v1 ->
            v1.execSQL("INSERT INTO app_metadata (id, schemaVersion) VALUES (0, 1)")
        }

        // Apply MIGRATION_1_2 and validate resulting schema against schemas/2.json
        val v2 = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            GymTrackDatabase.MIGRATION_1_2,
        )

        // exercises table must accept inserts
        v2.execSQL(
            "INSERT INTO exercises (name, muscleGroup, equipmentType) VALUES ('Migrated Exercise', 'Arms', 'Bodyweight')",
        )
        val cursor = v2.query("SELECT * FROM exercises WHERE name = 'Migrated Exercise'")
        assertEquals(1, cursor.count)
        cursor.close()
    }

    @Test
    fun migration1To2_dropsAppMetadataTable() {
        helper.createDatabase(TEST_DB, 1).use { v1 ->
            v1.execSQL("INSERT INTO app_metadata (id, schemaVersion) VALUES (0, 1)")
        }

        val v2 = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            GymTrackDatabase.MIGRATION_1_2,
        )

        val cursor = v2.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='app_metadata'",
        )
        assertEquals("app_metadata table should not exist in v2", 0, cursor.count)
        cursor.close()
    }

    @Test
    fun migration1To2_exercisesTableHasAutoIncrementId() {
        helper.createDatabase(TEST_DB, 1).use { /* empty v1 */ }

        val v2 = helper.runMigrationsAndValidate(
            TEST_DB,
            2,
            true,
            GymTrackDatabase.MIGRATION_1_2,
        )

        // Insert without specifying id — autoincrement should assign one
        v2.execSQL("INSERT INTO exercises (name, muscleGroup, equipmentType) VALUES ('A', 'B', 'C')")
        v2.execSQL("INSERT INTO exercises (name, muscleGroup, equipmentType) VALUES ('D', 'E', 'F')")

        val cursor = v2.query("SELECT id FROM exercises ORDER BY id ASC")
        assertEquals(2, cursor.count)
        cursor.moveToFirst()
        val id1 = cursor.getLong(0)
        cursor.moveToNext()
        val id2 = cursor.getLong(0)
        cursor.close()

        assertTrue("IDs should be distinct and auto-incremented", id2 > id1)
    }

    private fun assertTrue(message: String, value: Boolean) {
        org.junit.Assert.assertTrue(message, value)
    }

    companion object {
        private const val TEST_DB = "exercise-migration-test"
    }
}

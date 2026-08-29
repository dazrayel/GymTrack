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
 * Instrumented test for the Room migration 2 → 3.
 *
 * The [MigrationTestHelper] reads the exported schema JSON files (in app/schemas/)
 * configured as test assets via sourceSets.androidTest.assets in build.gradle.kts.
 * It validates that the resulting schema after migration matches the version-3 schema exactly.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GymTrackDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migration2To3_schemaValidatesAndWorkoutsTableIsUsable() {
        helper.createDatabase(TEST_DB, 2).use { v2 ->
            v2.execSQL(
                "INSERT INTO exercises (name, muscleGroup, equipmentType) VALUES ('Bench Press', 'Chest', 'Barbell')",
            )
        }

        val v3 = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            GymTrackDatabase.MIGRATION_2_3,
        )

        // workouts table must accept inserts
        v3.execSQL("INSERT INTO workouts (name, description) VALUES ('Push Day', 'Chest focus')")
        val cursor = v3.query("SELECT * FROM workouts WHERE name = 'Push Day'")
        assertEquals(1, cursor.count)
        cursor.close()
    }

    @Test
    fun migration2To3_exercisesTableRemainsIntact() {
        helper.createDatabase(TEST_DB, 2).use { v2 ->
            v2.execSQL(
                "INSERT INTO exercises (name, muscleGroup, equipmentType) VALUES ('Squat', 'Legs', 'Barbell')",
            )
        }

        val v3 = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            GymTrackDatabase.MIGRATION_2_3,
        )

        val cursor = v3.query("SELECT * FROM exercises WHERE name = 'Squat'")
        assertEquals("exercises table should still contain pre-migration data", 1, cursor.count)
        cursor.close()
    }

    @Test
    fun migration2To3_workoutExercisesTableIsCreated() {
        helper.createDatabase(TEST_DB, 2).use { /* empty v2 */ }

        val v3 = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            GymTrackDatabase.MIGRATION_2_3,
        )

        v3.execSQL("INSERT INTO workouts (name, description) VALUES ('Leg Day', '')")
        v3.execSQL(
            "INSERT INTO exercises (name, muscleGroup, equipmentType) VALUES ('Squat', 'Legs', 'Barbell')",
        )

        val workoutCursor = v3.query("SELECT id FROM workouts LIMIT 1")
        workoutCursor.moveToFirst()
        val workoutId = workoutCursor.getLong(0)
        workoutCursor.close()

        val exerciseCursor = v3.query("SELECT id FROM exercises LIMIT 1")
        exerciseCursor.moveToFirst()
        val exerciseId = exerciseCursor.getLong(0)
        exerciseCursor.close()

        v3.execSQL(
            "INSERT INTO workout_exercises " +
                "(workoutId, exerciseId, position, sets, minRepetitions, maxRepetitions, weight, restSeconds, notes) " +
                "VALUES ($workoutId, $exerciseId, 0, 3, 8, 12, 60.0, 90, '')",
        )

        val weCursor = v3.query("SELECT * FROM workout_exercises WHERE workoutId = $workoutId")
        assertEquals(1, weCursor.count)
        weCursor.close()
    }

    @Test
    fun migration2To3_indicesExist() {
        helper.createDatabase(TEST_DB, 2).use { /* empty v2 */ }

        val v3 = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            GymTrackDatabase.MIGRATION_2_3,
        )

        val cursor = v3.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='workout_exercises'",
        )
        val indexNames = buildList {
            while (cursor.moveToNext()) add(cursor.getString(0))
        }
        cursor.close()

        assertTrue(
            "index_workout_exercises_workoutId should exist",
            indexNames.contains("index_workout_exercises_workoutId"),
        )
        assertTrue(
            "index_workout_exercises_exerciseId should exist",
            indexNames.contains("index_workout_exercises_exerciseId"),
        )
    }

    @Test
    fun migration2To3_foreignKeyCascadeOnWorkout() {
        helper.createDatabase(TEST_DB, 2).use { /* empty v2 */ }

        val v3 = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            GymTrackDatabase.MIGRATION_2_3,
        )

        v3.execSQL("INSERT INTO workouts (name, description) VALUES ('Push Day', '')")
        v3.execSQL(
            "INSERT INTO exercises (name, muscleGroup, equipmentType) VALUES ('Bench Press', 'Chest', 'Barbell')",
        )

        val workoutCursor = v3.query("SELECT id FROM workouts LIMIT 1")
        workoutCursor.moveToFirst()
        val workoutId = workoutCursor.getLong(0)
        workoutCursor.close()

        val exerciseCursor = v3.query("SELECT id FROM exercises LIMIT 1")
        exerciseCursor.moveToFirst()
        val exerciseId = exerciseCursor.getLong(0)
        exerciseCursor.close()

        v3.execSQL(
            "INSERT INTO workout_exercises " +
                "(workoutId, exerciseId, position, sets, minRepetitions, maxRepetitions, weight, restSeconds, notes) " +
                "VALUES ($workoutId, $exerciseId, 0, 3, 8, 12, 60.0, 90, '')",
        )

        val countBefore = v3.query("SELECT * FROM workout_exercises").count

        v3.execSQL("PRAGMA foreign_keys = ON")
        v3.execSQL("DELETE FROM workouts WHERE id = $workoutId")

        val countAfter = v3.query("SELECT * FROM workout_exercises WHERE workoutId = $workoutId").count
        assertEquals("CASCADE should remove workout_exercises when workout is deleted", 0, countAfter)
        assertEquals("One workout_exercise was present before delete", 1, countBefore)
    }

    @Test
    fun migration2To3_workoutsTableHasAutoIncrementId() {
        helper.createDatabase(TEST_DB, 2).use { /* empty v2 */ }

        val v3 = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            GymTrackDatabase.MIGRATION_2_3,
        )

        v3.execSQL("INSERT INTO workouts (name, description) VALUES ('A', '')")
        v3.execSQL("INSERT INTO workouts (name, description) VALUES ('B', '')")

        val cursor = v3.query("SELECT id FROM workouts ORDER BY id ASC")
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
        private const val TEST_DB = "workout-migration-test"
    }
}

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

    @Test
    fun migration6To7_schemaValidates() {
        helper.createDatabase(TEST_DB_6_7, 6).use { /* empty v6 */ }

        helper.runMigrationsAndValidate(
            TEST_DB_6_7,
            7,
            true,
            GymTrackDatabase.MIGRATION_6_7,
        )
    }

    @Test
    fun migration6To7_keepsExistingExercisesAndNormalizesKnownValues() {
        helper.createDatabase(TEST_DB_6_7, 6).use { v6 ->
            v6.execSQL(
                "INSERT INTO exercises (name, muscleGroup, equipmentType) " +
                    "VALUES ('Peito Syn', 'Peito', 'barbell')",
            )
            v6.execSQL(
                "INSERT INTO exercises (name, muscleGroup, equipmentType) " +
                    "VALUES ('Legs Keep', 'Legs', 'xyz')",
            )
            v6.execSQL(
                "INSERT INTO exercises (name, muscleGroup, equipmentType) " +
                    "VALUES ('Mystery Keep', 'Mystery', 'foo')",
            )
            v6.execSQL(
                "INSERT INTO exercises (name, muscleGroup, equipmentType) " +
                    "VALUES ('Chest Syn', 'Chest', 'dumbbell')",
            )
            v6.execSQL(
                "INSERT INTO workout_sessions " +
                    "(workoutName, workoutDescription, startedAtMillis, status) " +
                    "VALUES ('Old session', '', 1000, 'COMPLETED')",
            )
            val sessionIdCursor = v6.query("SELECT id FROM workout_sessions LIMIT 1")
            sessionIdCursor.moveToFirst()
            val sessionId = sessionIdCursor.getLong(0)
            sessionIdCursor.close()
            v6.execSQL(
                "INSERT INTO workout_session_exercises " +
                    "(sessionId, position, exerciseName, muscleGroup, equipmentType, " +
                    "plannedSets, minRepetitions, maxRepetitions, plannedWeight, restSeconds, notes) " +
                    "VALUES ($sessionId, 0, 'Peito Syn', 'Peito', 'barbell', 3, 8, 12, 60.0, 90, '')",
            )
        }

        val v7 = helper.runMigrationsAndValidate(
            TEST_DB_6_7,
            7,
            true,
            GymTrackDatabase.MIGRATION_6_7,
        )

        val exercises = v7.query(
            "SELECT name, muscleGroup, equipmentType FROM exercises ORDER BY name ASC",
        )
        val byName = mutableMapOf<String, Pair<String, String>>()
        while (exercises.moveToNext()) {
            byName[exercises.getString(0)] = exercises.getString(1) to exercises.getString(2)
        }
        exercises.close()

        assertEquals("Peitoral" to "Barra", byName["Peito Syn"])
        assertEquals("Legs" to "xyz", byName["Legs Keep"])
        assertEquals("Mystery" to "foo", byName["Mystery Keep"])
        assertEquals("Peitoral" to "Halteres", byName["Chest Syn"])

        val snapshot = v7.query("SELECT secondaryMuscles FROM workout_session_exercises")
        snapshot.moveToFirst()
        assertEquals("", snapshot.getString(0))
        snapshot.close()

        val secondaryTable = v7.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='exercise_secondary_muscles'",
        )
        assertEquals(1, secondaryTable.count)
        secondaryTable.close()
    }

    @Test
    fun migration8To9_schemaValidates() {
        helper.createDatabase(TEST_DB_8_9, 8).use { /* empty v8 */ }

        helper.runMigrationsAndValidate(
            TEST_DB_8_9,
            9,
            true,
            GymTrackDatabase.MIGRATION_8_9,
        )
    }

    @Test
    fun migration8To9_preservesExistingExerciseAndSetsExternalIdentityNull() {
        helper.createDatabase(TEST_DB_8_9, 8).use { v8 ->
            v8.execSQL(
                "INSERT INTO exercises (id, name, muscleGroup, equipmentType) " +
                    "VALUES (10, 'Supino', 'Peitoral', 'Barra')",
            )
        }

        val v9 = helper.runMigrationsAndValidate(
            TEST_DB_8_9,
            9,
            true,
            GymTrackDatabase.MIGRATION_8_9,
        )

        val cursor = v9.query(
            "SELECT id, name, muscleGroup, equipmentType, externalSource, externalId " +
                "FROM exercises WHERE id = 10",
        )
        assertEquals(1, cursor.count)
        cursor.moveToFirst()
        assertEquals(10L, cursor.getLong(0))
        assertEquals("Supino", cursor.getString(1))
        assertEquals("Peitoral", cursor.getString(2))
        assertEquals("Barra", cursor.getString(3))
        assertTrue("externalSource should be NULL", cursor.isNull(4))
        assertTrue("externalId should be NULL", cursor.isNull(5))
        cursor.close()
    }

    @Test
    fun migration8To9_preservesWorkoutSessionAndSecondaryMuscleRelations() {
        helper.createDatabase(TEST_DB_8_9, 8).use { v8 ->
            v8.execSQL(
                "INSERT INTO exercises (id, name, muscleGroup, equipmentType) " +
                    "VALUES (10, 'Supino', 'Peitoral', 'Barra')",
            )
            v8.execSQL(
                "INSERT INTO exercise_secondary_muscles (exerciseId, muscle) " +
                    "VALUES (10, 'Ombros')",
            )
            v8.execSQL(
                "INSERT INTO workouts (id, name, description) VALUES (1, 'Push A', '')",
            )
            v8.execSQL(
                "INSERT INTO workout_exercises " +
                    "(id, workoutId, exerciseId, position, sets, minRepetitions, maxRepetitions, " +
                    "weight, restSeconds, notes) " +
                    "VALUES (100, 1, 10, 0, 3, 8, 12, 60.0, 90, '')",
            )
            v8.execSQL(
                "INSERT INTO workout_sessions " +
                    "(id, workoutId, workoutName, workoutDescription, startedAtMillis, status) " +
                    "VALUES (5, 1, 'Push A', '', 1000, 'COMPLETED')",
            )
            v8.execSQL(
                "INSERT INTO workout_session_exercises " +
                    "(id, sessionId, exerciseId, position, exerciseName, muscleGroup, equipmentType, " +
                    "plannedSets, minRepetitions, maxRepetitions, plannedWeight, restSeconds, notes, " +
                    "secondaryMuscles, status) " +
                    "VALUES (50, 5, 10, 0, 'Supino', 'Peitoral', 'Barra', 3, 8, 12, 60.0, 90, '', " +
                    "'Ombros', 'PENDING')",
            )
            v8.execSQL(
                "INSERT INTO workout_sets " +
                    "(id, sessionExerciseId, setIndex, reps, weight, completedAtMillis) " +
                    "VALUES (200, 50, 0, 10, 60.0, 2000)",
            )
        }

        val v9 = helper.runMigrationsAndValidate(
            TEST_DB_8_9,
            9,
            true,
            GymTrackDatabase.MIGRATION_8_9,
        )

        val exercise = v9.query("SELECT id, name FROM exercises WHERE id = 10")
        assertEquals(1, exercise.count)
        exercise.close()

        val secondary = v9.query(
            "SELECT muscle FROM exercise_secondary_muscles WHERE exerciseId = 10",
        )
        assertEquals(1, secondary.count)
        secondary.moveToFirst()
        assertEquals("Ombros", secondary.getString(0))
        secondary.close()

        val workoutExercise = v9.query(
            "SELECT workoutId, exerciseId FROM workout_exercises WHERE id = 100",
        )
        assertEquals(1, workoutExercise.count)
        workoutExercise.moveToFirst()
        assertEquals(1L, workoutExercise.getLong(0))
        assertEquals(10L, workoutExercise.getLong(1))
        workoutExercise.close()

        val session = v9.query("SELECT workoutId, status FROM workout_sessions WHERE id = 5")
        assertEquals(1, session.count)
        session.moveToFirst()
        assertEquals(1L, session.getLong(0))
        assertEquals("COMPLETED", session.getString(1))
        session.close()

        val sessionExercise = v9.query(
            "SELECT exerciseId, exerciseName FROM workout_session_exercises WHERE id = 50",
        )
        assertEquals(1, sessionExercise.count)
        sessionExercise.moveToFirst()
        assertEquals(10L, sessionExercise.getLong(0))
        assertEquals("Supino", sessionExercise.getString(1))
        sessionExercise.close()

        val set = v9.query("SELECT reps FROM workout_sets WHERE id = 200")
        assertEquals(1, set.count)
        set.moveToFirst()
        assertEquals(10, set.getInt(0))
        set.close()
    }

    private fun assertTrue(message: String, value: Boolean) {
        org.junit.Assert.assertTrue(message, value)
    }

    companion object {
        private const val TEST_DB = "exercise-migration-test"
        private const val TEST_DB_6_7 = "exercise-migration-6-7"
        private const val TEST_DB_8_9 = "exercise-migration-8-9"
    }
}

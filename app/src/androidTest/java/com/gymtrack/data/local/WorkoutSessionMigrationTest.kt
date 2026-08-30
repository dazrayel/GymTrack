package com.gymtrack.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the Room migration 3 → 4 (workout session persistence).
 *
 * [MigrationTestHelper] validates the migrated schema against schemas/4.json.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutSessionMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GymTrackDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migration3To4_schemaValidates() {
        helper.createDatabase(TEST_DB, 3).use { /* empty v3 */ }

        helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            GymTrackDatabase.MIGRATION_3_4,
        )
    }

    @Test
    fun migration3To4_existingV3DataRemainsIntact() {
        helper.createDatabase(TEST_DB, 3).use { v3 ->
            v3.execSQL(
                "INSERT INTO exercises (name, muscleGroup, equipmentType) VALUES ('Bench Press', 'Chest', 'Barbell')",
            )
            v3.execSQL("INSERT INTO workouts (name, description) VALUES ('Push Day', 'Chest focus')")
            val workoutId = queryId(v3, "SELECT id FROM workouts LIMIT 1")
            val exerciseId = queryId(v3, "SELECT id FROM exercises LIMIT 1")
            v3.execSQL(
                "INSERT INTO workout_exercises " +
                    "(workoutId, exerciseId, position, sets, minRepetitions, maxRepetitions, weight, restSeconds, notes) " +
                    "VALUES ($workoutId, $exerciseId, 0, 3, 8, 12, 60.0, 90, 'notes')",
            )
        }

        val v4 = helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            GymTrackDatabase.MIGRATION_3_4,
        )

        assertEquals(1, v4.query("SELECT * FROM exercises WHERE name = 'Bench Press'").use { it.count })
        assertEquals(1, v4.query("SELECT * FROM workouts WHERE name = 'Push Day'").use { it.count })
        val we = v4.query("SELECT notes, weight FROM workout_exercises")
        assertEquals(1, we.count)
        we.moveToFirst()
        assertEquals("notes", we.getString(0))
        assertEquals(60.0, we.getDouble(1), 0.001)
        we.close()
    }

    @Test
    fun migration3To4_sessionTablesExistAndAreUsable() {
        helper.createDatabase(TEST_DB, 3).use { /* empty v3 */ }

        val v4 = helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            GymTrackDatabase.MIGRATION_3_4,
        )

        v4.execSQL(
            "INSERT INTO workout_sessions " +
                "(workoutName, workoutDescription, startedAtMillis, status) " +
                "VALUES ('Push Day', '', 1000, 'IN_PROGRESS')",
        )
        val sessionId = queryId(v4, "SELECT id FROM workout_sessions LIMIT 1")
        v4.execSQL(
            "INSERT INTO workout_session_exercises " +
                "(sessionId, position, exerciseName, muscleGroup, equipmentType, " +
                "plannedSets, minRepetitions, maxRepetitions, plannedWeight, restSeconds, notes) " +
                "VALUES ($sessionId, 0, 'Bench', 'Chest', 'Barbell', 3, 8, 12, 60.0, 90, '')",
        )
        val sessionExerciseId = queryId(v4, "SELECT id FROM workout_session_exercises LIMIT 1")
        v4.execSQL(
            "INSERT INTO workout_sets " +
                "(sessionExerciseId, setIndex, reps, weight, completedAtMillis) " +
                "VALUES ($sessionExerciseId, 0, 10, 60.0, 2000)",
        )

        assertEquals(1, v4.query("SELECT * FROM workout_sessions").use { it.count })
        assertEquals(1, v4.query("SELECT * FROM workout_session_exercises").use { it.count })
        assertEquals(1, v4.query("SELECT * FROM workout_sets").use { it.count })
    }

    @Test
    fun migration3To4_deletingSessionCascadesExercisesAndSets() {
        helper.createDatabase(TEST_DB, 3).use { /* empty v3 */ }

        val v4 = helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            GymTrackDatabase.MIGRATION_3_4,
        )

        v4.execSQL(
            "INSERT INTO workout_sessions " +
                "(workoutName, workoutDescription, startedAtMillis, status) " +
                "VALUES ('Push Day', '', 1000, 'IN_PROGRESS')",
        )
        val sessionId = queryId(v4, "SELECT id FROM workout_sessions LIMIT 1")
        v4.execSQL(
            "INSERT INTO workout_session_exercises " +
                "(sessionId, position, exerciseName, muscleGroup, equipmentType, " +
                "plannedSets, minRepetitions, maxRepetitions, plannedWeight, restSeconds, notes) " +
                "VALUES ($sessionId, 0, 'Bench', 'Chest', 'Barbell', 3, 8, 12, 60.0, 90, '')",
        )
        val sessionExerciseId = queryId(v4, "SELECT id FROM workout_session_exercises LIMIT 1")
        v4.execSQL(
            "INSERT INTO workout_sets " +
                "(sessionExerciseId, setIndex, reps, weight, completedAtMillis) " +
                "VALUES ($sessionExerciseId, 0, 10, 60.0, 2000)",
        )

        v4.execSQL("PRAGMA foreign_keys = ON")
        v4.execSQL("DELETE FROM workout_sessions WHERE id = $sessionId")

        assertEquals(0, v4.query("SELECT * FROM workout_session_exercises").use { it.count })
        assertEquals(0, v4.query("SELECT * FROM workout_sets").use { it.count })
    }

    @Test
    fun migration3To4_deletingExerciseSetsSessionExerciseIdNull() {
        helper.createDatabase(TEST_DB, 3).use { v3 ->
            v3.execSQL(
                "INSERT INTO exercises (name, muscleGroup, equipmentType) VALUES ('Bench Press', 'Chest', 'Barbell')",
            )
        }

        val v4 = helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            GymTrackDatabase.MIGRATION_3_4,
        )

        val exerciseId = queryId(v4, "SELECT id FROM exercises LIMIT 1")
        v4.execSQL(
            "INSERT INTO workout_sessions " +
                "(workoutName, workoutDescription, startedAtMillis, status) " +
                "VALUES ('Push Day', '', 1000, 'IN_PROGRESS')",
        )
        val sessionId = queryId(v4, "SELECT id FROM workout_sessions LIMIT 1")
        v4.execSQL(
            "INSERT INTO workout_session_exercises " +
                "(sessionId, exerciseId, position, exerciseName, muscleGroup, equipmentType, " +
                "plannedSets, minRepetitions, maxRepetitions, plannedWeight, restSeconds, notes) " +
                "VALUES ($sessionId, $exerciseId, 0, 'Bench Press', 'Chest', 'Barbell', 3, 8, 12, 60.0, 90, '')",
        )

        v4.execSQL("PRAGMA foreign_keys = ON")
        v4.execSQL("DELETE FROM exercises WHERE id = $exerciseId")

        val cursor = v4.query("SELECT exerciseId, exerciseName FROM workout_session_exercises")
        assertEquals(1, cursor.count)
        cursor.moveToFirst()
        assertTrue(cursor.isNull(0))
        assertEquals("Bench Press", cursor.getString(1))
        cursor.close()
        assertEquals(0, v4.query("SELECT * FROM exercises").use { it.count })
    }

    @Test
    fun migration3To4_uniqueSessionExerciseIdAndSetIndex() {
        helper.createDatabase(TEST_DB, 3).use { /* empty v3 */ }

        val v4 = helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            GymTrackDatabase.MIGRATION_3_4,
        )

        v4.execSQL(
            "INSERT INTO workout_sessions " +
                "(workoutName, workoutDescription, startedAtMillis, status) " +
                "VALUES ('Push Day', '', 1000, 'IN_PROGRESS')",
        )
        val sessionId = queryId(v4, "SELECT id FROM workout_sessions LIMIT 1")
        v4.execSQL(
            "INSERT INTO workout_session_exercises " +
                "(sessionId, position, exerciseName, muscleGroup, equipmentType, " +
                "plannedSets, minRepetitions, maxRepetitions, plannedWeight, restSeconds, notes) " +
                "VALUES ($sessionId, 0, 'Bench', 'Chest', 'Barbell', 3, 8, 12, 60.0, 90, '')",
        )
        val sessionExerciseId = queryId(v4, "SELECT id FROM workout_session_exercises LIMIT 1")
        v4.execSQL(
            "INSERT INTO workout_sets " +
                "(sessionExerciseId, setIndex, reps, weight, completedAtMillis) " +
                "VALUES ($sessionExerciseId, 0, 10, 60.0, 2000)",
        )

        try {
            v4.execSQL(
                "INSERT INTO workout_sets " +
                    "(sessionExerciseId, setIndex, reps, weight, completedAtMillis) " +
                    "VALUES ($sessionExerciseId, 0, 8, 62.5, 3000)",
            )
            fail("Duplicate (sessionExerciseId, setIndex) should be rejected")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            // expected
        }
    }

    @Test
    fun migration3To4_indicesExist() {
        helper.createDatabase(TEST_DB, 3).use { /* empty v3 */ }

        val v4 = helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            GymTrackDatabase.MIGRATION_3_4,
        )

        val sessionIndices = indexNames(v4, "workout_sessions")
        assertTrue(sessionIndices.contains("index_workout_sessions_status"))
        assertTrue(sessionIndices.contains("index_workout_sessions_workoutId"))

        val sessionExerciseIndices = indexNames(v4, "workout_session_exercises")
        assertTrue(sessionExerciseIndices.contains("index_workout_session_exercises_sessionId"))
        assertTrue(sessionExerciseIndices.contains("index_workout_session_exercises_exerciseId"))

        val setIndices = indexNames(v4, "workout_sets")
        assertTrue(setIndices.contains("index_workout_sets_sessionExerciseId"))
        assertTrue(setIndices.contains("index_workout_sets_sessionExerciseId_setIndex"))
    }

    @Test
    fun migration4To5_schemaValidates() {
        helper.createDatabase(TEST_DB_4_5, 4).use { /* empty v4 */ }

        helper.runMigrationsAndValidate(
            TEST_DB_4_5,
            5,
            true,
            GymTrackDatabase.MIGRATION_4_5,
        )
    }

    @Test
    fun migration4To5_existingSessionDataRemainsIntact() {
        helper.createDatabase(TEST_DB_4_5, 4).use { v4 ->
            v4.execSQL(
                "INSERT INTO workout_sessions " +
                    "(workoutName, workoutDescription, startedAtMillis, status) " +
                    "VALUES ('Push Day', 'Chest', 1000, 'IN_PROGRESS')",
            )
            val sessionId = queryId(v4, "SELECT id FROM workout_sessions LIMIT 1")
            v4.execSQL(
                "INSERT INTO workout_session_exercises " +
                    "(sessionId, position, exerciseName, muscleGroup, equipmentType, " +
                    "plannedSets, minRepetitions, maxRepetitions, plannedWeight, restSeconds, notes) " +
                    "VALUES ($sessionId, 0, 'Bench', 'Chest', 'Barbell', 3, 8, 12, 60.0, 90, 'keep')",
            )
        }

        val v5 = helper.runMigrationsAndValidate(
            TEST_DB_4_5,
            5,
            true,
            GymTrackDatabase.MIGRATION_4_5,
        )

        val session = v5.query(
            "SELECT workoutName, workoutDescription, startedAtMillis, status FROM workout_sessions",
        )
        assertEquals(1, session.count)
        session.moveToFirst()
        assertEquals("Push Day", session.getString(0))
        assertEquals("Chest", session.getString(1))
        assertEquals(1000L, session.getLong(2))
        assertEquals("IN_PROGRESS", session.getString(3))
        session.close()
        assertEquals(
            1,
            v5.query("SELECT * FROM workout_session_exercises WHERE notes = 'keep'").use { it.count },
        )
    }

    @Test
    fun migration4To5_newRestColumnsAcceptNull() {
        helper.createDatabase(TEST_DB_4_5, 4).use { /* empty v4 */ }

        val v5 = helper.runMigrationsAndValidate(
            TEST_DB_4_5,
            5,
            true,
            GymTrackDatabase.MIGRATION_4_5,
        )

        v5.execSQL(
            "INSERT INTO workout_sessions " +
                "(workoutName, workoutDescription, startedAtMillis, status) " +
                "VALUES ('Push Day', '', 1000, 'IN_PROGRESS')",
        )
        val cursor = v5.query(
            "SELECT restEndsAtMillis, restPausedRemainingMillis, restSessionExerciseId, restAfterSetIndex " +
                "FROM workout_sessions",
        )
        assertEquals(1, cursor.count)
        cursor.moveToFirst()
        assertTrue(cursor.isNull(0))
        assertTrue(cursor.isNull(1))
        assertTrue(cursor.isNull(2))
        assertTrue(cursor.isNull(3))
        cursor.close()
    }

    @Test
    fun migration4To5_existingSessionRemainsUsable() {
        helper.createDatabase(TEST_DB_4_5, 4).use { v4 ->
            v4.execSQL(
                "INSERT INTO workout_sessions " +
                    "(workoutName, workoutDescription, startedAtMillis, status) " +
                    "VALUES ('Push Day', '', 1000, 'IN_PROGRESS')",
            )
        }

        val v5 = helper.runMigrationsAndValidate(
            TEST_DB_4_5,
            5,
            true,
            GymTrackDatabase.MIGRATION_4_5,
        )

        val sessionId = queryId(v5, "SELECT id FROM workout_sessions LIMIT 1")
        v5.execSQL(
            "UPDATE workout_sessions SET restEndsAtMillis = 5000, restSessionExerciseId = 9, restAfterSetIndex = 0 " +
                "WHERE id = $sessionId",
        )
        val cursor = v5.query(
            "SELECT restEndsAtMillis, restSessionExerciseId, status FROM workout_sessions WHERE id = $sessionId",
        )
        cursor.moveToFirst()
        assertEquals(5000L, cursor.getLong(0))
        assertEquals(9L, cursor.getLong(1))
        assertEquals("IN_PROGRESS", cursor.getString(2))
        cursor.close()
    }

    private fun indexNames(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String): List<String> {
        val cursor = db.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='$table'",
        )
        val names = buildList {
            while (cursor.moveToNext()) add(cursor.getString(0))
        }
        cursor.close()
        return names
    }

    private fun queryId(db: androidx.sqlite.db.SupportSQLiteDatabase, sql: String): Long {
        val cursor = db.query(sql)
        cursor.moveToFirst()
        val id = cursor.getLong(0)
        cursor.close()
        return id
    }

    companion object {
        private const val TEST_DB = "session-migration-test"
        private const val TEST_DB_4_5 = "session-migration-test-4-5"
    }
}

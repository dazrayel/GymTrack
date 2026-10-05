package com.gymtrack.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutPositionMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GymTrackDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migration12To13_addsPositionAndBackfillsByIdAsc() {
        helper.createDatabase(TEST_DB, 12).use { v12 ->
            // Insert out of alphabetical order; ids define creation order.
            v12.execSQL("INSERT INTO workouts (name, description) VALUES ('Zebra', '')")
            v12.execSQL("INSERT INTO workouts (name, description) VALUES ('Alpha', '')")
            v12.execSQL("INSERT INTO workouts (name, description) VALUES ('Middle', '')")
        }

        val v13 = helper.runMigrationsAndValidate(
            TEST_DB,
            13,
            true,
            GymTrackDatabase.MIGRATION_12_13,
        )

        val cursor = v13.query("SELECT name, position FROM workouts ORDER BY position ASC")
        assertEquals(3, cursor.count)

        cursor.moveToFirst()
        assertEquals("Zebra", cursor.getString(0))
        assertEquals(0, cursor.getInt(1))

        cursor.moveToNext()
        assertEquals("Alpha", cursor.getString(0))
        assertEquals(1, cursor.getInt(1))

        cursor.moveToNext()
        assertEquals("Middle", cursor.getString(0))
        assertEquals(2, cursor.getInt(1))
        cursor.close()
    }

    @Test
    fun migration12To13_emptyWorkouts_stillValidates() {
        helper.createDatabase(TEST_DB_EMPTY, 12).use { /* empty */ }

        helper.runMigrationsAndValidate(
            TEST_DB_EMPTY,
            13,
            true,
            GymTrackDatabase.MIGRATION_12_13,
        )
    }

    companion object {
        private const val TEST_DB = "workout-position-migration-test"
        private const val TEST_DB_EMPTY = "workout-position-migration-empty"
    }
}

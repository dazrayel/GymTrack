package com.gymtrack.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeeklyWorkoutPlanMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GymTrackDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migration11To12_schemaValidatesAndPlanTableIsUsable() {
        helper.createDatabase(TEST_DB, 11).use { v11 ->
            v11.execSQL("INSERT INTO workouts (name, description) VALUES ('Push Day', '')")
        }

        val v12 = helper.runMigrationsAndValidate(
            TEST_DB,
            12,
            true,
            GymTrackDatabase.MIGRATION_11_12,
        )

        val workoutId = v12.query("SELECT id FROM workouts LIMIT 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getLong(0)
        }

        v12.execSQL(
            "INSERT INTO weekly_workout_plans (dayOfWeek, workoutId) VALUES (1, $workoutId)",
        )
        val plans = v12.query("SELECT dayOfWeek, workoutId FROM weekly_workout_plans")
        assertEquals(1, plans.count)
        plans.moveToFirst()
        assertEquals(1, plans.getInt(0))
        assertEquals(workoutId, plans.getLong(1))
        plans.close()

        // Existing workouts remain intact.
        assertEquals(1, v12.query("SELECT * FROM workouts WHERE name = 'Push Day'").use { it.count })
    }

    companion object {
        private const val TEST_DB = "weekly-plan-migration-test"
    }
}

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
class MissionInstanceMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GymTrackDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migration14To15_createsMissionInstancesTable() {
        helper.createDatabase(TEST_DB, 14).use { /* empty v14 */ }

        val v15 = helper.runMigrationsAndValidate(
            TEST_DB,
            15,
            true,
            GymTrackDatabase.MIGRATION_14_15,
        )

        v15.execSQL(
            "INSERT INTO mission_instances " +
                "(definitionId, periodKey, status, startedAtMillis, completedAtMillis) " +
                "VALUES ('FIRST_STEP', '2026-10-07', 'ACTIVE', 1, NULL)",
        )
        val cursor = v15.query(
            "SELECT definitionId, periodKey, status FROM mission_instances",
        )
        assertEquals(1, cursor.count)
        assertTrue(cursor.moveToFirst())
        assertEquals("FIRST_STEP", cursor.getString(0))
        assertEquals("2026-10-07", cursor.getString(1))
        assertEquals("ACTIVE", cursor.getString(2))
        cursor.close()
    }

    companion object {
        private const val TEST_DB = "mission-instance-migration-test"
    }
}

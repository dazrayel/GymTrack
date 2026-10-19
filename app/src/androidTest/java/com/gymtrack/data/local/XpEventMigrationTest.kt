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
class XpEventMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GymTrackDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migration13To14_createsXpEventsTable() {
        helper.createDatabase(TEST_DB, 13).use { /* empty v13 */ }

        val v14 = helper.runMigrationsAndValidate(
            TEST_DB,
            14,
            true,
            GymTrackDatabase.MIGRATION_13_14,
        )

        v14.execSQL(
            "INSERT INTO xp_events (type, amount, sourceKey, createdAtMillis) " +
                "VALUES ('WORKOUT_COMPLETED', 100, 'WORKOUT_COMPLETED:session:1', 1)",
        )
        val cursor = v14.query("SELECT type, amount, sourceKey FROM xp_events")
        assertEquals(1, cursor.count)
        assertTrue(cursor.moveToFirst())
        assertEquals("WORKOUT_COMPLETED", cursor.getString(0))
        assertEquals(100, cursor.getInt(1))
        assertEquals("WORKOUT_COMPLETED:session:1", cursor.getString(2))
        cursor.close()
    }

    companion object {
        private const val TEST_DB = "xp-event-migration-test"
    }
}

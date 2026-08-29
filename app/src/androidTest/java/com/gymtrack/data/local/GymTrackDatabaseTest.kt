package com.gymtrack.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class GymTrackDatabaseTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var database: GymTrackDatabase

    @Before
    fun setUp() {
        hiltRule.inject()
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            GymTrackDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun database_isCreatedSuccessfully() {
        assertNotNull(database.openHelper.writableDatabase)
    }
}

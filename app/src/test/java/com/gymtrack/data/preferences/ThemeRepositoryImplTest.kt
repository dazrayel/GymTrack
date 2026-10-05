package com.gymtrack.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.gymtrack.domain.model.ThemeMode
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class ThemeRepositoryImplTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    @Test
    fun default_whenUnset_isDark() = runTest(testDispatcher) {
        val repository = ThemeRepositoryImpl(createDataStore())
        assertEquals(ThemeMode.DARK, repository.themeMode.first())
    }

    @Test
    fun setThemeMode_light_isPersistedAndRecovered() = runTest(testDispatcher) {
        val dataStore = createDataStore()
        val repository = ThemeRepositoryImpl(dataStore)

        repository.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repository.themeMode.first())

        val reopened = ThemeRepositoryImpl(dataStore)
        assertEquals(ThemeMode.LIGHT, reopened.themeMode.first())
    }

    @Test
    fun setThemeMode_dark_isPersistedAndRecovered() = runTest(testDispatcher) {
        val dataStore = createDataStore()
        val repository = ThemeRepositoryImpl(dataStore)

        repository.setThemeMode(ThemeMode.LIGHT)
        repository.setThemeMode(ThemeMode.DARK)

        assertEquals(ThemeMode.DARK, repository.themeMode.first())
        assertEquals(ThemeMode.DARK, ThemeRepositoryImpl(dataStore).themeMode.first())
    }

    @Test
    fun toggle_lightToDarkAndBack() = runTest(testDispatcher) {
        val repository = ThemeRepositoryImpl(createDataStore())

        repository.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repository.themeMode.first())

        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.themeMode.first())

        repository.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repository.themeMode.first())
    }

    @Test
    fun corruptStoredValue_fallsBackToDark() = runTest(testDispatcher) {
        val dataStore = createDataStore()
        dataStore.edit { it[ThemeRepositoryImpl.THEME_MODE_KEY] = "NOT_A_THEME" }

        val repository = ThemeRepositoryImpl(dataStore)
        assertEquals(ThemeMode.DARK, repository.themeMode.first())
    }

    private fun createDataStore(): DataStore<Preferences> {
        val file = File(temporaryFolder.newFolder(), "theme_prefs.preferences_pb")
        return PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { file },
        )
    }
}

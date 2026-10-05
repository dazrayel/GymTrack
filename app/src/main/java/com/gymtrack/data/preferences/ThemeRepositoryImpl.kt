package com.gymtrack.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gymtrack.domain.model.ThemeMode
import com.gymtrack.domain.repository.ThemeRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ThemeRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ThemeRepository {

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        ThemeMode.fromStorageKey(prefs[THEME_MODE_KEY])
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }

    companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }
}

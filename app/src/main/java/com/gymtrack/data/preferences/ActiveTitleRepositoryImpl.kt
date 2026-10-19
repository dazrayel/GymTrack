package com.gymtrack.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gymtrack.domain.repository.ActiveTitleRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ActiveTitleRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ActiveTitleRepository {

    override val activeTitleId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[ACTIVE_TITLE_ID_KEY]?.takeIf { it.isNotBlank() }
    }

    override suspend fun setActiveTitleId(titleId: String?) {
        dataStore.edit { prefs ->
            if (titleId.isNullOrBlank()) {
                prefs.remove(ACTIVE_TITLE_ID_KEY)
            } else {
                prefs[ACTIVE_TITLE_ID_KEY] = titleId
            }
        }
    }

    companion object {
        val ACTIVE_TITLE_ID_KEY = stringPreferencesKey("active_title_id")
    }
}

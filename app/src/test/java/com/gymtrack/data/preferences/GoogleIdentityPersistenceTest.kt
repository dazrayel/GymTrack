package com.gymtrack.data.preferences

import com.gymtrack.data.repository.GoogleIdentityRepositoryImpl
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GoogleIdentityPersistenceTest {

    @Test
    fun userPreferencesDataStore_doesNotDefineGoogleIdentityKeys() {
        val themeKey = ThemeRepositoryImpl.THEME_MODE_KEY.name
        assertFalse(themeKey.contains("google", ignoreCase = true))
        assertFalse(themeKey.contains("account", ignoreCase = true))
        assertFalse(themeKey.contains("user", ignoreCase = true))
    }

    @Test
    fun googleIdentityRepository_hasNoPersistenceFields() {
        val persistenceHints = listOf("DataStore", "SharedPreferences", "Room", "database")
        val fieldTypes = GoogleIdentityRepositoryImpl::class.java.declaredFields.map { it.type.name }
        persistenceHints.forEach { hint ->
            assertFalse(
                "Unexpected persistence type containing $hint in $fieldTypes",
                fieldTypes.any { it.contains(hint, ignoreCase = true) },
            )
        }
    }
}

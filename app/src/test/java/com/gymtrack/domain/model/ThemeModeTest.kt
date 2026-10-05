package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun default_isDark() {
        assertEquals(ThemeMode.DARK, ThemeMode.Default)
        assertTrue(ThemeMode.Default.isDark)
    }

    @Test
    fun light_isNotDark() {
        assertFalse(ThemeMode.LIGHT.isDark)
    }

    @Test
    fun fromStorageKey_knownValues() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromStorageKey("LIGHT"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageKey("DARK"))
    }

    @Test
    fun fromStorageKey_nullOrUnknown_fallsBackToDark() {
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageKey(null))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageKey(""))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageKey("SYSTEM"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStorageKey("light"))
    }
}

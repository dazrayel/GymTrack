package com.gymtrack.domain.repository

import com.gymtrack.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {

    /** Emits the persisted theme preference. Defaults to [ThemeMode.DARK] when unset. */
    val themeMode: Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}

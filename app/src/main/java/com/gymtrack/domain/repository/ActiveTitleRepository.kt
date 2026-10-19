package com.gymtrack.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Persists only the user's preferred active RPG title id (DataStore).
 * Unlock eligibility is never stored here.
 */
interface ActiveTitleRepository {

    /** Emits the preferred title id, or null when unset / "Sem título". */
    val activeTitleId: Flow<String?>

    suspend fun setActiveTitleId(titleId: String?)
}

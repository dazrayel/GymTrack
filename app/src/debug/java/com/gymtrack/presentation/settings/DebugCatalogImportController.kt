package com.gymtrack.presentation.settings

import com.gymtrack.domain.catalogimport.ExerciseCatalogImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface DebugCatalogImportUiState {
    data object Idle : DebugCatalogImportUiState
    data object Importing : DebugCatalogImportUiState
    data class Success(val result: ExerciseCatalogImportResult) : DebugCatalogImportUiState
    data class Error(val message: String) : DebugCatalogImportUiState
}

/**
 * Debug-only orchestration around [com.gymtrack.data.catalogimport.ExerciseCatalogImporter].
 * Does not own import rules — only UI state transitions.
 */
class DebugCatalogImportController(
    private val importDefaultAsset: suspend () -> ExerciseCatalogImportResult,
) {
    private val _state = MutableStateFlow<DebugCatalogImportUiState>(DebugCatalogImportUiState.Idle)
    val state: StateFlow<DebugCatalogImportUiState> = _state.asStateFlow()

    suspend fun importCatalog() {
        if (_state.value is DebugCatalogImportUiState.Importing) return
        _state.value = DebugCatalogImportUiState.Importing
        try {
            val result = importDefaultAsset()
            _state.value = DebugCatalogImportUiState.Success(result)
        } catch (e: Exception) {
            _state.value = DebugCatalogImportUiState.Error(
                e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName,
            )
        }
    }
}

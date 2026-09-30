package com.gymtrack.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.data.catalogimport.ExerciseCatalogImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugCatalogImportViewModel @Inject constructor(
    importer: ExerciseCatalogImporter,
) : ViewModel() {

    private val controller = DebugCatalogImportController(
        importDefaultAsset = importer::importDefaultAsset,
    )

    val uiState: StateFlow<DebugCatalogImportUiState> = controller.state

    fun importCatalog() {
        viewModelScope.launch {
            controller.importCatalog()
        }
    }
}

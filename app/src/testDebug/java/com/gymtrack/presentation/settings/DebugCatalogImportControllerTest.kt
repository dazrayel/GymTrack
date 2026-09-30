package com.gymtrack.presentation.settings

import com.gymtrack.domain.catalogimport.ExerciseCatalogImportResult
import com.gymtrack.domain.catalogimport.RejectedCatalogRecord
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugCatalogImportControllerTest {

    @Test
    fun importCatalog_setsImportingThenSuccess() = runTest {
        val result = sampleResult(inserted = 136, rejected = 0)
        lateinit var ctrl: DebugCatalogImportController
        val midStates = mutableListOf<DebugCatalogImportUiState>()
        ctrl = DebugCatalogImportController {
            midStates.add(ctrl.state.value)
            result
        }

        assertEquals(DebugCatalogImportUiState.Idle, ctrl.state.value)
        ctrl.importCatalog()

        assertEquals(listOf(DebugCatalogImportUiState.Importing), midStates)
        val success = ctrl.state.value as DebugCatalogImportUiState.Success
        assertEquals(136, success.result.inserted)
        assertEquals(0, success.result.rejected)
        assertEquals(136, success.result.importedCount)
    }

    @Test
    fun importCatalog_setsErrorOnFailure() = runTest {
        val ctrl = DebugCatalogImportController { error("boom") }
        ctrl.importCatalog()
        val error = ctrl.state.value as DebugCatalogImportUiState.Error
        assertEquals("boom", error.message)
    }

    @Test
    fun importCatalog_ignoresConcurrentCallWhileImporting() = runTest {
        var calls = 0
        lateinit var ctrl: DebugCatalogImportController
        ctrl = DebugCatalogImportController {
            calls++
            ctrl.importCatalog()
            sampleResult()
        }
        ctrl.importCatalog()
        assertEquals(1, calls)
        assertTrue(ctrl.state.value is DebugCatalogImportUiState.Success)
    }

    private fun sampleResult(
        inserted: Int = 0,
        updated: Int = 0,
        unchanged: Int = 0,
        rejected: Int = 0,
    ) = ExerciseCatalogImportResult(
        source = "free-exercise-db",
        inputCount = inserted + rejected,
        importableCount = inserted + updated + unchanged,
        inserted = inserted,
        updated = updated,
        unchanged = unchanged,
        rejected = rejected,
        errors = 0,
        rejections = List(rejected) {
            RejectedCatalogRecord(null, "x$it", "test")
        },
    )
}

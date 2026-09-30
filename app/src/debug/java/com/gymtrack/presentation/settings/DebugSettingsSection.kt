package com.gymtrack.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DebugSettingsSection(
    modifier: Modifier = Modifier,
    viewModel: DebugCatalogImportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DebugCatalogImportSection(
        uiState = uiState,
        onImportClick = viewModel::importCatalog,
        modifier = modifier,
    )
}

@Composable
internal fun DebugCatalogImportSection(
    uiState: DebugCatalogImportUiState,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .testTag("debug_catalog_import_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Desenvolvimento",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Button(
            onClick = onImportClick,
            enabled = uiState !is DebugCatalogImportUiState.Importing,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("debug_import_catalog_button"),
        ) {
            Text("Importar catálogo de exercícios")
        }
        when (val state = uiState) {
            DebugCatalogImportUiState.Idle -> Unit
            DebugCatalogImportUiState.Importing -> {
                CircularProgressIndicator(modifier = Modifier.testTag("debug_import_progress"))
                Text(
                    text = "Importando...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("debug_import_status"),
                )
            }
            is DebugCatalogImportUiState.Success -> {
                val result = state.result
                Text(
                    text = "Importação concluída",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("debug_import_status"),
                )
                Text(
                    text = buildString {
                        appendLine("${result.importedCount} exercícios disponíveis")
                        appendLine("${result.rejected} rejeitados")
                        appendLine("inseridos=${result.inserted}")
                        appendLine("atualizados=${result.updated}")
                        appendLine("inalterados=${result.unchanged}")
                        append("erros=${result.errors}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("debug_import_result"),
                )
            }
            is DebugCatalogImportUiState.Error -> {
                Text(
                    text = "Falha na importação",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("debug_import_status"),
                )
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("debug_import_error"),
                )
            }
        }
    }
}

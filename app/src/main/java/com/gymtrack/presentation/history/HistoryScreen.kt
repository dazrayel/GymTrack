package com.gymtrack.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.R
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.model.formatVolumeKg
import com.gymtrack.domain.time.formatElapsedMillis
import com.gymtrack.domain.time.formatHistoryDate
import com.gymtrack.presentation.components.GymCard
import com.gymtrack.presentation.components.GymIconButton
import com.gymtrack.presentation.components.GymTopAppBar
import com.gymtrack.presentation.theme.GymShapeTokens
import com.gymtrack.presentation.theme.GymSpacing
import com.gymtrack.presentation.theme.GymTrackTheme

@Composable
fun HistoryScreen(
    contentPadding: PaddingValues = PaddingValues(),
    onSessionClick: (Long) -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HistoryScreen(
        uiState = uiState,
        onSessionClick = onSessionClick,
        onNavigateToCalendar = onNavigateToCalendar,
        onDeleteClick = viewModel::showDeleteConfirmation,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDelete = viewModel::dismissDeleteConfirmation,
        onErrorShown = viewModel::clearError,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onSessionClick: (Long) -> Unit,
    onErrorShown: () -> Unit,
    onNavigateToCalendar: () -> Unit = {},
    onDeleteClick: (WorkoutHistoryItem) -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onDismissDelete: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val sessionCount = uiState.items.size
    val eyebrow = when {
        uiState.isLoading || sessionCount == 0 -> null
        else -> stringResource(R.string.history_sessions_eyebrow, sessionCount)
    }

    LaunchedEffect(uiState.error) {
        val message = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = message)
        onErrorShown()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier.padding(contentPadding),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GymTopAppBar(
                title = stringResource(R.string.history_title),
                eyebrow = eyebrow,
                actions = {
                    GymIconButton(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = stringResource(R.string.history_open_calendar_cd),
                        onClick = onNavigateToCalendar,
                        modifier = Modifier.testTag("history_open_calendar"),
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.testTag("history_loading"),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            uiState.items.isEmpty() -> {
                EmptyHistoryContent(modifier = Modifier.padding(innerPadding))
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("history_list"),
                    contentPadding = PaddingValues(
                        start = GymSpacing.ScreenPadding,
                        end = GymSpacing.ScreenPadding,
                        top = GymSpacing.Sm,
                        bottom = GymSpacing.Lg,
                    ),
                    verticalArrangement = Arrangement.spacedBy(GymSpacing.CardSpacing),
                ) {
                    items(uiState.items, key = { it.sessionId }) { item ->
                        HistoryItem(
                            item = item,
                            onClick = { onSessionClick(item.sessionId) },
                            onDeleteClick = { onDeleteClick(item) },
                        )
                    }
                }
            }
        }
    }

    val sessionToDelete = uiState.sessionToDelete
    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(GymShapeTokens.Dialog),
            title = { Text(stringResource(R.string.delete_history_session)) },
            text = { Text(stringResource(R.string.delete_history_session_message)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.testTag("confirm_delete_history_button"),
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissDelete,
                    modifier = Modifier.testTag("cancel_delete_history_button"),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun HistoryItem(
    item: WorkoutHistoryItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val date = formatHistoryDate(item.endedAtMillis ?: item.startedAtMillis)
    val duration = formatElapsedMillis(item.durationMillis)
    val volume = formatVolumeKg(item.volume)
    val sessionDescription = stringResource(
        R.string.history_session_cd,
        item.workoutName,
        date,
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GymCard(
            onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .testTag("history_item_${item.sessionId}")
                .semantics { contentDescription = sessionDescription },
            contentPadding = PaddingValues(GymSpacing.CardPadding),
        ) {
            Text(
                text = item.workoutName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = GymSpacing.Xxs),
            )
            Text(
                text = stringResource(
                    R.string.history_item_meta,
                    duration,
                    item.exerciseCount,
                    item.completedSetCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = GymSpacing.Sm),
            )
            Text(
                text = stringResource(R.string.history_volume, volume),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = GymSpacing.Xxs),
            )
        }
        GymIconButton(
            imageVector = Icons.Outlined.Delete,
            contentDescription = stringResource(
                R.string.delete_history_session_cd,
                item.workoutName,
            ),
            onClick = onDeleteClick,
            modifier = Modifier.testTag("history_delete_${item.sessionId}"),
            contentColor = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun EmptyHistoryContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(GymSpacing.Xxxl)
            .testTag("history_empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
        )
        Spacer(Modifier.height(GymSpacing.Lg))
        Text(
            text = stringResource(R.string.empty_history),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(GymSpacing.Sm))
        Text(
            text = stringResource(R.string.empty_history_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    GymTrackTheme(darkTheme = true) {
        HistoryScreen(
            uiState = HistoryUiState(
                isLoading = false,
                items = listOf(
                    WorkoutHistoryItem(
                        sessionId = 1L,
                        workoutName = "Treino A",
                        startedAtMillis = 1_756_339_200_000L,
                        endedAtMillis = 1_756_341_720_000L,
                        durationMillis = 2_520_000L,
                        volume = 4_320.0,
                        exerciseCount = 3,
                        completedSetCount = 12,
                        plannedSetCount = 12,
                    ),
                ),
            ),
            onSessionClick = {},
            onErrorShown = {},
        )
    }
}

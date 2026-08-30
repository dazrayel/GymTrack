package com.gymtrack.presentation.stats

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.R
import com.gymtrack.domain.model.ExercisePerformancePoint
import com.gymtrack.domain.model.ExercisePersonalRecords
import com.gymtrack.domain.model.formatVolumeKg
import com.gymtrack.domain.time.formatLocalDate
import com.gymtrack.domain.time.formatLocalTime
import java.time.ZoneId

@Composable
fun ExerciseStatsScreen(
    onNavigateBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    viewModel: ExerciseStatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExerciseStatsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onErrorShown = viewModel::clearError,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseStatsScreen(
    uiState: ExerciseStatsUiState,
    onNavigateBack: () -> Unit,
    onErrorShown: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        val message = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = message)
        onErrorShown()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier.padding(contentPadding),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.exerciseName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
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
                        modifier = Modifier.testTag("exercise_stats_loading"),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            uiState.showEmpty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("exercise_stats_empty"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.empty_exercise_stats),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            }

            else -> {
                ExerciseStatsContent(
                    uiState = uiState,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun ExerciseStatsContent(
    uiState: ExerciseStatsUiState,
    modifier: Modifier = Modifier,
) {
    val record = uiState.personalRecord ?: return
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .testTag("exercise_stats"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        RecordsSummaryCard(record = record)
        HistoryCard(points = uiState.performanceHistory)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RecordsSummaryCard(
    record: ExercisePersonalRecords,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.dashboard_records),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("exercise_stats_records"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag("exercise_stats_record_${record.exerciseName}"),
            ) {
                Text(
                    text = stringResource(R.string.home_record_weight, formatVolumeKg(record.bestWeight)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.home_record_reps, record.bestReps),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = stringResource(R.string.home_record_volume, formatVolumeKg(record.bestVolume)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(
    points: List<ExercisePerformancePoint>,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.exercise_stats_history),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("exercise_stats_history"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                points.forEach { point ->
                    val dateTime = stringResource(
                        R.string.dashboard_datetime,
                        formatLocalDate(point.occurredAtMillis, zone),
                        formatLocalTime(point.occurredAtMillis, zone),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exercise_stats_point_${point.sessionId}"),
                    ) {
                        Text(
                            text = dateTime,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.home_record_weight,
                                    formatVolumeKg(point.bestWeight),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            )
                            Text(
                                text = stringResource(R.string.home_record_reps, point.bestReps),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            )
                            Text(
                                text = stringResource(
                                    R.string.home_record_volume,
                                    formatVolumeKg(point.volume),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
        }
    }
}

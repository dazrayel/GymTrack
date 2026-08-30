package com.gymtrack.presentation.workouts.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.R
import com.gymtrack.domain.model.formatVolumeKg
import com.gymtrack.domain.time.formatElapsedMillis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionSummaryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWorkout: (workoutId: Long) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    viewModel: WorkoutSessionSummaryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WorkoutSessionSummaryContent(
        uiState = uiState,
        contentPadding = contentPadding,
        onNavigateBack = {
            val workoutId = uiState.session?.workoutId
            if (workoutId != null && uiState.canNavigateToWorkout) {
                onNavigateToWorkout(workoutId)
            } else {
                onNavigateBack()
            }
        },
        onErrorShown = viewModel::clearError,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutSessionSummaryContent(
    uiState: WorkoutSessionSummaryUiState,
    contentPadding: PaddingValues,
    onNavigateBack: () -> Unit,
    onErrorShown: () -> Unit,
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
                        text = stringResource(R.string.session_summary_title),
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val progress = uiState.progress
            val duration = formatElapsedMillis(uiState.durationMillis)
            val volumeText = formatVolumeKg(uiState.volume)
            val durationDescription = stringResource(R.string.duration_description, duration)
            val volumeDescription = stringResource(R.string.volume_description, volumeText)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .testTag("session_summary"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = uiState.session?.workoutName.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("summary_workout_name"),
                )
                Text(
                    text = stringResource(R.string.duration_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Text(
                    text = duration,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .testTag("summary_duration")
                        .semantics { contentDescription = durationDescription },
                )
                Text(
                    text = stringResource(
                        R.string.exercises_progress,
                        progress.completedExercises,
                        progress.totalExercises,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("summary_exercises"),
                )
                Text(
                    text = stringResource(
                        R.string.sets_progress,
                        progress.completedSets,
                        progress.plannedSets,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.testTag("summary_sets"),
                )
                Text(
                    text = stringResource(R.string.workout_progress_percent, progress.progressPercent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("summary_percent"),
                )
                Text(
                    text = stringResource(R.string.volume_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                Text(
                    text = volumeText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .testTag("summary_volume")
                        .semantics { contentDescription = volumeDescription },
                )
                Spacer(Modifier.height(8.dp))
                uiState.exerciseSummaries.forEach { exercise ->
                    ExerciseSummaryCard(exercise)
                }
            }
        }
    }
}

@Composable
private fun ExerciseSummaryCard(
    exercise: SessionExerciseSummary,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = exercise.exerciseName,
                style = MaterialTheme.typography.titleMedium,
            )
            if (exercise.muscleGroup.isNotBlank()) {
                Text(
                    text = exercise.muscleGroup,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Text(
                text = stringResource(
                    R.string.exercise_sets_progress,
                    exercise.completedSets,
                    exercise.plannedSets,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            exercise.sets.forEach { set ->
                Text(
                    text = stringResource(
                        R.string.set_performed,
                        set.reps,
                        formatWeight(set.weight),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = formatVolumeKg(exercise.volume),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun formatWeight(weight: Double): String =
    if (weight == weight.toLong().toDouble()) weight.toLong().toString() else weight.toString()

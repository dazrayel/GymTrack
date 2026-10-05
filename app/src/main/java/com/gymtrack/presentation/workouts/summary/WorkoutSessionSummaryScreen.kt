package com.gymtrack.presentation.workouts.summary

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.R
import com.gymtrack.domain.model.formatSignedInt
import com.gymtrack.domain.model.formatSignedKg
import com.gymtrack.domain.model.formatVolumeKg
import com.gymtrack.domain.time.formatElapsedMillis
import com.gymtrack.presentation.components.GymCard
import com.gymtrack.presentation.components.GymCardTone
import com.gymtrack.presentation.components.GymIconButton
import com.gymtrack.presentation.components.GymLabel
import com.gymtrack.presentation.components.GymSectionHeader
import com.gymtrack.presentation.components.GymTopAppBar
import com.gymtrack.presentation.theme.GymSpacing
import com.gymtrack.presentation.theme.GymTheme

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
        onNavigateBack = onNavigateBack,
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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GymTopAppBar(
                title = stringResource(R.string.session_summary_title),
                navigationIcon = {
                    GymIconButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        onClick = onNavigateBack,
                    )
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
        } else if (uiState.sessionNotFound || uiState.session == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(GymSpacing.Xxxl)
                    .testTag("summary_not_found"),
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
                    text = stringResource(R.string.session_not_found),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            val progress = uiState.progress
            val duration = formatElapsedMillis(uiState.durationMillis)
            val volumeText = formatVolumeKg(uiState.volume)
            val durationDescription = stringResource(R.string.duration_description, duration)
            val volumeDescription = stringResource(R.string.volume_description, volumeText)
            val improvedExercises = uiState.exerciseSummaries.filter { summary ->
                val weightDelta = summary.progress.weightDelta
                weightDelta != null && weightDelta > 0.0
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(GymSpacing.ScreenPadding)
                    .testTag("session_summary"),
                verticalArrangement = Arrangement.spacedBy(GymSpacing.Md),
            ) {
                GymLabel(
                    text = stringResource(
                        R.string.summary_completed_eyebrow,
                        progress.progressPercent,
                    ),
                )
                Text(
                    text = uiState.session.workoutName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("summary_workout_name"),
                )
                Text(
                    text = uiState.startedDate,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("summary_date"),
                )
                Text(
                    text = stringResource(R.string.session_start, uiState.startedTime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("summary_start_time"),
                )
                if (uiState.endedTime.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.session_end, uiState.endedTime),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("summary_end_time"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
                ) {
                    SummaryMetricCard(
                        label = stringResource(R.string.duration_label),
                        value = duration,
                        contentDescription = durationDescription,
                        valueTestTag = "summary_duration",
                        modifier = Modifier.weight(1f),
                    )
                    SummaryMetricCard(
                        label = stringResource(R.string.volume_label),
                        value = volumeText,
                        contentDescription = volumeDescription,
                        valueTestTag = "summary_volume",
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
                ) {
                    GymCard(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(GymSpacing.Md),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.exercises_progress,
                                progress.completedExercises,
                                progress.totalExercises,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("summary_exercises"),
                        )
                    }
                    GymCard(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(GymSpacing.Md),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.sets_progress,
                                progress.completedSets,
                                progress.plannedSets,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("summary_sets"),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.workout_progress_percent, progress.progressPercent),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("summary_percent"),
                )

                if (improvedExercises.isNotEmpty()) {
                    GymCard(
                        tone = GymCardTone.Warning,
                        contentPadding = PaddingValues(GymSpacing.CardPadding),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GymSpacing.Md),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EmojiEvents,
                                contentDescription = null,
                                tint = GymTheme.extendedColors.warning,
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.summary_pr_highlight_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = improvedExercises.joinToString(" · ") { it.exerciseName },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = GymSpacing.Xxs),
                                )
                            }
                        }
                    }
                }

                GymSectionHeader(title = stringResource(R.string.summary_exercises_section))

                uiState.exerciseSummaries.forEach { exercise ->
                    ExerciseSummaryCard(exercise)
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(
    label: String,
    value: String,
    contentDescription: String,
    valueTestTag: String,
    modifier: Modifier = Modifier,
) {
    GymCard(
        modifier = modifier,
        contentPadding = PaddingValues(GymSpacing.Md),
    ) {
        GymLabel(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = GymTheme.gymTypography.metric,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(top = GymSpacing.Xs)
                .testTag(valueTestTag)
                .semantics { this.contentDescription = contentDescription },
        )
    }
}

@Composable
private fun ExerciseSummaryCard(
    exercise: SessionExerciseSummary,
    modifier: Modifier = Modifier,
) {
    val hasWeightPr = (exercise.progress.weightDelta ?: 0.0) > 0.0
    val tone = if (hasWeightPr) GymCardTone.Warning else GymCardTone.Surface
    val prBadge = stringResource(R.string.summary_pr_badge)

    GymCard(
        modifier = modifier.fillMaxWidth(),
        tone = tone,
        contentPadding = PaddingValues(GymSpacing.CardPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (exercise.muscleGroup.isNotBlank()) {
                    Text(
                        text = exercise.muscleGroup,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = GymSpacing.Xxs),
                    )
                }
            }
            if (hasWeightPr) {
                Text(
                    text = prBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = GymTheme.extendedColors.warning,
                    modifier = Modifier.padding(start = GymSpacing.Sm),
                )
            }
        }
        Text(
            text = stringResource(
                R.string.exercise_sets_progress,
                exercise.completedSets,
                exercise.plannedSets,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = GymSpacing.Sm),
        )
        Text(
            text = stringResource(
                when {
                    exercise.isComplete -> R.string.exercise_status_completed
                    exercise.isSkipped -> R.string.exercise_status_skipped
                    else -> R.string.exercise_status_pending
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = GymSpacing.Xxs),
        )
        exercise.sets.forEach { set ->
            Text(
                text = stringResource(
                    R.string.set_performed,
                    set.reps,
                    formatWeight(set.weight),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = GymSpacing.Xxs),
            )
        }
        Text(
            text = formatVolumeKg(exercise.volume),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = GymSpacing.Sm),
        )
        val best = exercise.progress.historicalBest
        if (best != null) {
            Spacer(Modifier.height(GymSpacing.Sm))
            Text(
                text = stringResource(R.string.progress_best_marks),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("progress_best_${exercise.exerciseName}"),
            )
            Text(
                text = stringResource(
                    R.string.progress_best_values,
                    formatVolumeKg(best.bestWeight),
                    best.bestReps,
                    formatVolumeKg(best.bestVolume),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        val weightDelta = exercise.progress.weightDelta
        val repsDelta = exercise.progress.repsDelta
        val volumeDelta = exercise.progress.volumeDelta
        if (weightDelta != null && repsDelta != null && volumeDelta != null) {
            Text(
                text = stringResource(R.string.progress_evolution),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = GymSpacing.Sm)
                    .testTag("progress_evolution_${exercise.exerciseName}"),
            )
            Text(
                text = stringResource(
                    R.string.progress_evolution_values,
                    formatSignedKg(weightDelta),
                    formatSignedInt(repsDelta),
                    formatSignedKg(volumeDelta),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun formatWeight(weight: Double): String =
    if (weight == weight.toLong().toDouble()) weight.toLong().toString() else weight.toString()

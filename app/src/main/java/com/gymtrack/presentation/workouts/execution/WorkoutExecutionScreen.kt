package com.gymtrack.presentation.workouts.execution

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.R
import com.gymtrack.domain.model.formatRepetitionTarget
import com.gymtrack.domain.time.formatElapsedMillis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutExecutionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSummary: (sessionId: Long) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    viewModel: WorkoutExecutionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.sessionFinishedEvent) {
        val sessionId = uiState.sessionFinishedEvent ?: return@LaunchedEffect
        onNavigateToSummary(sessionId)
        viewModel.consumeSessionFinishedEvent()
    }

    WorkoutExecutionContent(
        uiState = uiState,
        contentPadding = contentPadding,
        onNavigateBack = onNavigateBack,
        onRepsChanged = viewModel::onRepsChanged,
        onWeightChanged = viewModel::onWeightChanged,
        onCompleteSet = viewModel::completeCurrentSet,
        onPauseRest = viewModel::pauseRest,
        onResumeRest = viewModel::resumeRest,
        onSkipRest = viewModel::skipRest,
        onRequestSkip = viewModel::requestSkip,
        onDismissSkipConfirmation = viewModel::dismissSkipConfirmation,
        onConfirmSkip = viewModel::confirmSkip,
        onResumeExercise = viewModel::resumeExercise,
        onRequestFinish = viewModel::requestFinish,
        onDismissFinishConfirmation = viewModel::dismissFinishConfirmation,
        onConfirmFinish = viewModel::confirmFinish,
        onErrorShown = viewModel::clearError,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutExecutionContent(
    uiState: WorkoutExecutionUiState,
    contentPadding: PaddingValues,
    onNavigateBack: () -> Unit,
    onRepsChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onCompleteSet: () -> Unit,
    onPauseRest: () -> Unit,
    onResumeRest: () -> Unit,
    onSkipRest: () -> Unit,
    onRequestSkip: () -> Unit,
    onDismissSkipConfirmation: () -> Unit,
    onConfirmSkip: () -> Unit,
    onResumeExercise: (Long) -> Unit,
    onRequestFinish: () -> Unit,
    onDismissFinishConfirmation: () -> Unit,
    onConfirmFinish: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showSessionExercises by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        val message = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = message)
        onErrorShown()
    }

    val title = stringResource(R.string.workout_in_progress)

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier.padding(contentPadding),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
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
                actions = {
                    TextButton(
                        onClick = onRequestFinish,
                        modifier = Modifier.testTag("finish_workout_button"),
                    ) {
                        Text(stringResource(R.string.finish_workout))
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    ExecutionProgressSummary(
                        elapsedMillis = uiState.elapsedMillis,
                        completedExercises = uiState.completedExercises,
                        totalExercises = uiState.totalExercises,
                        completedSets = uiState.completedSets,
                        plannedSets = uiState.plannedSets,
                        progressPercent = uiState.progressPercent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                    when {
                        uiState.phase == WorkoutExecutionPhase.FINISHED ||
                            (uiState.isWorkoutComplete && uiState.phase != WorkoutExecutionPhase.RESTING) -> {
                            CompletedWorkoutContent(
                                workoutName = uiState.session?.workoutName.orEmpty(),
                                modifier = Modifier.weight(1f),
                            )
                        }

                        uiState.phase == WorkoutExecutionPhase.RESTING -> {
                            RestContent(
                                exerciseName = uiState.restExercise?.exerciseName.orEmpty(),
                                remainingMillis = uiState.restRemainingMillis,
                                restDurationMillis = (uiState.restExercise?.restSeconds ?: 0) * 1_000L,
                                isPaused = uiState.isRestPaused,
                                onPauseRest = onPauseRest,
                                onResumeRest = onResumeRest,
                                onSkipRest = onSkipRest,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        else -> {
                            val exercise = uiState.currentExercise
                            if (exercise == null) {
                                if (uiState.hasIncompleteExercises) {
                                    PendingExercisesPanel(
                                        workoutName = uiState.session?.workoutName.orEmpty(),
                                        rows = uiState.sessionExerciseRows,
                                        onResumeExercise = onResumeExercise,
                                        modifier = Modifier.weight(1f),
                                    )
                                } else {
                                    CompletedWorkoutContent(
                                        workoutName = uiState.session?.workoutName.orEmpty(),
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            } else {
                                ActiveSetContent(
                                    workoutName = uiState.session?.workoutName.orEmpty(),
                                    exerciseName = exercise.exerciseName,
                                    muscleGroup = exercise.muscleGroup,
                                    notes = exercise.notes,
                                    currentSetNumber = uiState.currentSetIndex + 1,
                                    plannedSets = exercise.plannedSets,
                                    minRepetitions = exercise.minRepetitions,
                                    maxRepetitions = exercise.maxRepetitions,
                                    repsInput = uiState.repsInput,
                                    weightInput = uiState.weightInput,
                                    repsError = uiState.repsError,
                                    weightError = uiState.weightError,
                                    onRepsChanged = onRepsChanged,
                                    onWeightChanged = onWeightChanged,
                                    onCompleteSet = onCompleteSet,
                                    onRequestSkip = onRequestSkip,
                                    onOpenSessionExercises = { showSessionExercises = true },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showFinishConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissFinishConfirmation,
            title = { Text(stringResource(R.string.finish_workout_title)) },
            text = {
                Text(
                    stringResource(
                        if (uiState.finishHasPendingExercises) {
                            R.string.finish_workout_pending_message
                        } else {
                            R.string.finish_workout_message
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmFinish,
                    modifier = Modifier.testTag("confirm_finish_button"),
                ) {
                    Text(stringResource(R.string.finish_workout))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissFinishConfirmation,
                    modifier = Modifier.testTag("cancel_finish_button"),
                ) {
                    Text(
                        stringResource(
                            if (uiState.finishHasPendingExercises) {
                                R.string.continue_workout
                            } else {
                                R.string.cancel
                            },
                        ),
                    )
                }
            },
        )
    }

    if (showSessionExercises && uiState.phase == WorkoutExecutionPhase.WORKING) {
        AlertDialog(
            onDismissRequest = { showSessionExercises = false },
            title = { Text(stringResource(R.string.session_exercises)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    SessionExercisesList(
                        rows = uiState.sessionExerciseRows,
                        onResumeExercise = { id ->
                            onResumeExercise(id)
                            showSessionExercises = false
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showSessionExercises = false },
                    modifier = Modifier.testTag("close_session_exercises_button"),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (uiState.showSkipConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissSkipConfirmation,
            title = { Text(stringResource(R.string.skip_exercise_title)) },
            text = { Text(stringResource(R.string.skip_exercise_message)) },
            confirmButton = {
                TextButton(
                    onClick = onConfirmSkip,
                    modifier = Modifier.testTag("confirm_skip_button"),
                ) {
                    Text(stringResource(R.string.skip))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissSkipConfirmation,
                    modifier = Modifier.testTag("cancel_skip_button"),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ExecutionProgressSummary(
    elapsedMillis: Long,
    completedExercises: Int,
    totalExercises: Int,
    completedSets: Int,
    plannedSets: Int,
    progressPercent: Int,
    modifier: Modifier = Modifier,
) {
    val elapsedClock = formatElapsedMillis(elapsedMillis)
    val elapsedDescription = stringResource(R.string.elapsed_time_description, elapsedClock)
    val progressFraction = if (plannedSets <= 0) {
        0f
    } else {
        (completedSets.toFloat() / plannedSets.toFloat()).coerceIn(0f, 1f)
    }
    val progressBarDescription = stringResource(R.string.workout_progress_bar, progressPercent)

    Column(
        modifier = modifier.testTag("execution_progress"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.elapsed_time_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Text(
            text = elapsedClock,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .testTag("elapsed_timer")
                .semantics { contentDescription = elapsedDescription },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.exercises_progress, completedExercises, totalExercises),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.sets_progress, completedSets, plannedSets),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = stringResource(R.string.workout_progress_percent, progressPercent),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("workout_progress_bar")
                .semantics { contentDescription = progressBarDescription },
        )
    }
}

@Composable
private fun CompletedWorkoutContent(
    workoutName: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.workout_basic_complete),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (workoutName.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = workoutName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ActiveSetContent(
    workoutName: String,
    exerciseName: String,
    muscleGroup: String,
    notes: String,
    currentSetNumber: Int,
    plannedSets: Int,
    minRepetitions: Int,
    maxRepetitions: Int,
    repsInput: String,
    weightInput: String,
    repsError: Int?,
    weightError: Int?,
    onRepsChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onCompleteSet: () -> Unit,
    onRequestSkip: () -> Unit,
    onOpenSessionExercises: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (workoutName.isNotBlank()) {
            Text(
                text = workoutName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        Text(
            text = exerciseName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (muscleGroup.isNotBlank()) {
            Text(
                text = muscleGroup,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        Text(
            text = stringResource(R.string.current_set, currentSetNumber, plannedSets),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        formatRepetitionTarget(minRepetitions, maxRepetitions)?.let { target ->
            Text(
                text = stringResource(R.string.execution_reps_target, target),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.testTag("reps_target"),
            )
        }
        if (notes.isNotBlank()) {
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        OutlinedTextField(
            value = repsInput,
            onValueChange = onRepsChanged,
            label = { Text(stringResource(R.string.reps_label)) },
            isError = repsError != null,
            supportingText = repsError?.let { resId -> { Text(stringResource(resId)) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reps_field"),
        )
        OutlinedTextField(
            value = weightInput,
            onValueChange = onWeightChanged,
            label = { Text(stringResource(R.string.weight_label)) },
            isError = weightError != null,
            supportingText = weightError?.let { resId -> { Text(stringResource(resId)) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("weight_field"),
        )
        Button(
            onClick = onCompleteSet,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("complete_set_button"),
        ) {
            Text(stringResource(R.string.complete_set))
        }
        OutlinedButton(
            onClick = onOpenSessionExercises,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("session_exercises_button"),
        ) {
            Text(stringResource(R.string.session_exercises))
        }
        OutlinedButton(
            onClick = onRequestSkip,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("skip_exercise_button"),
        ) {
            Text(stringResource(R.string.skip_exercise))
        }
    }
}

@Composable
private fun PendingExercisesPanel(
    workoutName: String,
    rows: List<SessionExerciseRow>,
    onResumeExercise: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (workoutName.isNotBlank()) {
            Text(
                text = workoutName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        Text(
            text = stringResource(R.string.session_exercises),
            style = MaterialTheme.typography.titleMedium,
        )
        SessionExercisesList(
            rows = rows,
            onResumeExercise = onResumeExercise,
        )
    }
}

@Composable
private fun SessionExercisesList(
    rows: List<SessionExerciseRow>,
    onResumeExercise: (Long) -> Unit,
) {
    if (rows.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("session_exercises_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        rows.forEach { row ->
            val name = row.exercise.exerciseName
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("session_exercise_row_$name"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(statusLabelRes(row.listStatus)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.testTag("session_exercise_status_$name"),
                )
                Text(
                    text = stringResource(
                        R.string.pending_exercise_sets,
                        row.completedSets,
                        row.exercise.plannedSets,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
                if (row.canSelectNow) {
                    OutlinedButton(
                        onClick = { onResumeExercise(row.exercise.id) },
                        modifier = Modifier.testTag("resume_exercise_$name"),
                    ) {
                        Text(stringResource(R.string.do_exercise_now))
                    }
                }
            }
        }
    }
}

private fun statusLabelRes(status: SessionExerciseListStatus): Int = when (status) {
    SessionExerciseListStatus.COMPLETED -> R.string.exercise_status_completed
    SessionExerciseListStatus.SKIPPED -> R.string.exercise_status_skipped
    SessionExerciseListStatus.IN_PROGRESS -> R.string.exercise_status_in_progress
    SessionExerciseListStatus.PENDING -> R.string.exercise_status_pending
}

internal fun restRemainingFraction(remainingMillis: Long, durationMillis: Long): Float {
    if (durationMillis <= 0L) return 0f
    return (remainingMillis.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
}

@Composable
private fun RestContent(
    exerciseName: String,
    remainingMillis: Long,
    restDurationMillis: Long,
    isPaused: Boolean,
    onPauseRest: () -> Unit,
    onResumeRest: () -> Unit,
    onSkipRest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clock = formatRestClock(remainingMillis)
    val remainingDescription = stringResource(R.string.rest_remaining, clock)
    val targetFraction = restRemainingFraction(remainingMillis, restDurationMillis)
    var hasShownProgress by remember { mutableStateOf(false) }
    val progressFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = if (hasShownProgress) {
            tween(durationMillis = 1_000, easing = LinearEasing)
        } else {
            snap()
        },
        label = "restRemainingArc",
    )
    LaunchedEffect(Unit) {
        hasShownProgress = true
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (exerciseName.isNotBlank()) {
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
        }
        Text(
            text = stringResource(if (isPaused) R.string.rest_paused else R.string.rest_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val diameter = min(maxWidth * 0.72f, 280.dp).coerceAtLeast(168.dp)
            val stroke = (diameter * 0.08f).coerceIn(8.dp, 14.dp)
            Box(
                modifier = Modifier.size(diameter),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("rest_progress"),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = stroke,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                )
                Text(
                    text = clock,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .testTag("rest_timer")
                        .semantics { contentDescription = remainingDescription },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        if (isPaused) {
            Button(
                onClick = onResumeRest,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("resume_rest_button"),
            ) {
                Text(stringResource(R.string.resume_rest))
            }
        } else {
            Button(
                onClick = onPauseRest,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pause_rest_button"),
            ) {
                Text(stringResource(R.string.pause_rest))
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onSkipRest,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("skip_rest_button"),
        ) {
            Text(stringResource(R.string.skip_rest))
        }
    }
}

private fun formatRestClock(millis: Long): String {
    val totalSeconds = (millis / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

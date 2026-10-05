package com.gymtrack.presentation.workouts.execution

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.ProgressionAction
import com.gymtrack.domain.model.ProgressionSuggestion
import com.gymtrack.domain.model.WorkoutBlockType
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.formatProgressionRepsSequence
import com.gymtrack.domain.model.formatRepetitionTarget
import com.gymtrack.domain.model.formatVolumeKg
import com.gymtrack.domain.time.formatElapsedMillis
import com.gymtrack.presentation.components.GymCard
import com.gymtrack.presentation.components.GymCardTone
import com.gymtrack.presentation.components.GymIconButton
import com.gymtrack.presentation.components.GymLabel
import com.gymtrack.presentation.components.GymPrimaryButton
import com.gymtrack.presentation.components.GymSecondaryButton
import com.gymtrack.presentation.components.GymTextButton
import com.gymtrack.presentation.components.GymTypeBadge
import com.gymtrack.presentation.exercises.ExerciseAnimation
import com.gymtrack.presentation.theme.GymShapeTokens
import com.gymtrack.presentation.theme.GymSpacing
import com.gymtrack.presentation.theme.GymTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    LaunchedEffect(uiState.restBeepEvent) {
        if (uiState.restBeepEvent == null) return@LaunchedEffect
        withContext(Dispatchers.Default) {
            RestCompletionBeep.play()
        }
        viewModel.consumeRestBeepEvent()
    }

    WorkoutExecutionScreen(
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
        onRequestApplyProgressionSuggestion = viewModel::requestApplyProgressionSuggestion,
        onDismissApplyProgressionConfirmation = viewModel::dismissApplyProgressionConfirmation,
        onConfirmApplyProgressionSuggestion = viewModel::confirmApplyProgressionSuggestion,
        onErrorShown = viewModel::clearError,
        onInfoMessageShown = viewModel::clearInfoMessage,
        modifier = modifier,
    )
}

@Composable
fun WorkoutExecutionScreen(
    uiState: WorkoutExecutionUiState,
    onNavigateBack: () -> Unit,
    onRepsChanged: (String) -> Unit = {},
    onWeightChanged: (String) -> Unit = {},
    onCompleteSet: () -> Unit = {},
    onPauseRest: () -> Unit = {},
    onResumeRest: () -> Unit = {},
    onSkipRest: () -> Unit = {},
    onRequestSkip: () -> Unit = {},
    onDismissSkipConfirmation: () -> Unit = {},
    onConfirmSkip: () -> Unit = {},
    onResumeExercise: (Long) -> Unit = {},
    onRequestFinish: () -> Unit = {},
    onDismissFinishConfirmation: () -> Unit = {},
    onConfirmFinish: () -> Unit = {},
    onRequestApplyProgressionSuggestion: () -> Unit = {},
    onDismissApplyProgressionConfirmation: () -> Unit = {},
    onConfirmApplyProgressionSuggestion: () -> Unit = {},
    onErrorShown: () -> Unit = {},
    onInfoMessageShown: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    WorkoutExecutionContent(
        uiState = uiState,
        contentPadding = contentPadding,
        onNavigateBack = onNavigateBack,
        onRepsChanged = onRepsChanged,
        onWeightChanged = onWeightChanged,
        onCompleteSet = onCompleteSet,
        onPauseRest = onPauseRest,
        onResumeRest = onResumeRest,
        onSkipRest = onSkipRest,
        onRequestSkip = onRequestSkip,
        onDismissSkipConfirmation = onDismissSkipConfirmation,
        onConfirmSkip = onConfirmSkip,
        onResumeExercise = onResumeExercise,
        onRequestFinish = onRequestFinish,
        onDismissFinishConfirmation = onDismissFinishConfirmation,
        onConfirmFinish = onConfirmFinish,
        onRequestApplyProgressionSuggestion = onRequestApplyProgressionSuggestion,
        onDismissApplyProgressionConfirmation = onDismissApplyProgressionConfirmation,
        onConfirmApplyProgressionSuggestion = onConfirmApplyProgressionSuggestion,
        onErrorShown = onErrorShown,
        onInfoMessageShown = onInfoMessageShown,
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
    onRequestApplyProgressionSuggestion: () -> Unit,
    onDismissApplyProgressionConfirmation: () -> Unit,
    onConfirmApplyProgressionSuggestion: () -> Unit,
    onErrorShown: () -> Unit,
    onInfoMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showSessionExercises by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        val message = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = message)
        onErrorShown()
    }

    val infoMessage = uiState.infoMessageResId?.let { stringResource(it) }
    LaunchedEffect(infoMessage) {
        val message = infoMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = message)
        onInfoMessageShown()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier.padding(contentPadding),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Back + finish only; status bar inset already comes from parent contentPadding.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(GymSpacing.TouchTarget)
                        .padding(horizontal = GymSpacing.Xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GymIconButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("execution_back_button"),
                    )
                    Spacer(Modifier.weight(1f))
                    GymTextButton(
                        text = stringResource(R.string.finish_workout),
                        onClick = onRequestFinish,
                        modifier = Modifier.testTag("finish_workout_button"),
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                )
            }
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
                            .padding(horizontal = GymSpacing.ScreenPadding, vertical = GymSpacing.Xs),
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
                                    blockType = exercise.blockType,
                                    blockLabel = blockExecutionLabel(
                                        exercise = exercise,
                                        exercises = uiState.exercises,
                                    ),
                                    exerciseName = exercise.exerciseName,
                                    muscleGroup = exercise.muscleGroup,
                                    notes = exercise.notes,
                                    mediaExercise = uiState.mediaExercise,
                                    progressionSuggestion = uiState.progressionSuggestion,
                                    canApplyProgressionToTemplate = uiState.canApplyProgressionToTemplate,
                                    currentSetNumber = uiState.currentSetIndex + 1,
                                    plannedSets = exercise.plannedSets,
                                    slotIndex = exercise.positionInBlock + 1,
                                    slotCount = uiState.exercises.count { it.blockPosition == exercise.blockPosition },
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
                                    onRequestApplyProgressionSuggestion = onRequestApplyProgressionSuggestion,
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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(GymShapeTokens.Dialog),
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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(GymShapeTokens.Dialog),
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
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(GymShapeTokens.Dialog),
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

    if (uiState.showApplyProgressionConfirmation) {
        val fromWeight = uiState.applyProgressionFromWeight
        val toWeight = uiState.applyProgressionToWeight
        AlertDialog(
            onDismissRequest = onDismissApplyProgressionConfirmation,
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = RoundedCornerShape(GymShapeTokens.Dialog),
            title = { Text(stringResource(R.string.apply_progression_title)) },
            text = {
                if (fromWeight != null && toWeight != null) {
                    Text(
                        stringResource(
                            R.string.apply_progression_message,
                            formatVolumeKg(fromWeight),
                            formatVolumeKg(toWeight),
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmApplyProgressionSuggestion,
                    modifier = Modifier.testTag("confirm_apply_progression_button"),
                ) {
                    Text(stringResource(R.string.apply_progression))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissApplyProgressionConfirmation,
                    modifier = Modifier.testTag("cancel_apply_progression_button"),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            modifier = Modifier.testTag("apply_progression_dialog"),
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
        verticalArrangement = Arrangement.spacedBy(GymSpacing.Xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = elapsedClock,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .testTag("elapsed_timer")
                    .semantics { contentDescription = elapsedDescription },
            )
            Text(
                text = stringResource(
                    R.string.execution_progress_compact,
                    completedExercises,
                    totalExercises,
                    completedSets,
                    plannedSets,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(GymShapeTokens.Badge))
                .testTag("workout_progress_bar")
                .semantics { contentDescription = progressBarDescription },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
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
            .padding(GymSpacing.ScreenPadding),
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
            Spacer(Modifier.height(GymSpacing.Sm))
            Text(
                text = workoutName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ActiveSetContent(
    workoutName: String,
    blockType: WorkoutBlockType,
    blockLabel: String?,
    exerciseName: String,
    muscleGroup: String,
    notes: String,
    mediaExercise: Exercise?,
    progressionSuggestion: ProgressionSuggestion?,
    canApplyProgressionToTemplate: Boolean,
    currentSetNumber: Int,
    plannedSets: Int,
    slotIndex: Int,
    slotCount: Int,
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
    onRequestApplyProgressionSuggestion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = GymSpacing.ScreenPadding)
                .padding(top = GymSpacing.Sm, bottom = GymSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
        ) {
            if (workoutName.isNotBlank()) {
                GymLabel(
                    text = workoutName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!blockLabel.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
                ) {
                    GymTypeBadge(type = blockType)
                    Text(
                        text = blockLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("execution_block_label"),
                    )
                }
                if (slotCount > 1) {
                    Text(
                        text = stringResource(R.string.execution_block_slot, slotIndex, slotCount),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            GymCard(
                tone = GymCardTone.Elevated,
                contentPadding = PaddingValues(GymSpacing.Md),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exerciseName,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (muscleGroup.isNotBlank()) {
                            Text(
                                text = muscleGroup,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = GymSpacing.Xxs),
                            )
                        }
                    }
                    if (canApplyProgressionToTemplate) {
                        Box {
                            GymIconButton(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.exercise_actions_menu),
                                onClick = { menuExpanded = true },
                                modifier = Modifier.testTag("exercise_actions_menu"),
                            )
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.apply_progression_menu)) },
                                    onClick = {
                                        menuExpanded = false
                                        onRequestApplyProgressionSuggestion()
                                    },
                                    modifier = Modifier.testTag("apply_progression_menu_item"),
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = GymSpacing.Sm)
                        .clip(RoundedCornerShape(GymShapeTokens.Medium))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(GymShapeTokens.Medium),
                        ),
                ) {
                    ExerciseAnimation(
                        exercise = mediaExercise,
                        height = 112.dp,
                        modifier = Modifier.testTag("execution_exercise_animation"),
                    )
                }

                Text(
                    text = stringResource(R.string.current_set, currentSetNumber, plannedSets),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = GymSpacing.Md),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = GymSpacing.Sm),
                    horizontalArrangement = Arrangement.spacedBy(GymSpacing.Xs),
                ) {
                    repeat(plannedSets.coerceAtLeast(0)) { index ->
                        val color = when {
                            index < currentSetNumber - 1 -> MaterialTheme.colorScheme.primary
                            index == currentSetNumber - 1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(GymShapeTokens.Badge))
                                .background(color),
                        )
                    }
                }
                formatRepetitionTarget(minRepetitions, maxRepetitions)?.let { target ->
                    Text(
                        text = stringResource(R.string.execution_reps_target, target),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = GymSpacing.Sm)
                            .testTag("reps_target"),
                    )
                }
                if (notes.isNotBlank()) {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = GymSpacing.Xs),
                    )
                }
            }

            ProgressionSuggestionInfo(
                suggestion = progressionSuggestion,
                canApplyProgressionToTemplate = canApplyProgressionToTemplate,
                onRequestApplyProgressionSuggestion = onRequestApplyProgressionSuggestion,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GymSpacing.Md),
            ) {
                OutlinedTextField(
                    value = repsInput,
                    onValueChange = onRepsChanged,
                    label = { Text(stringResource(R.string.reps_label)) },
                    isError = repsError != null,
                    supportingText = repsError?.let { resId -> { Text(stringResource(resId)) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(GymShapeTokens.Medium),
                    modifier = Modifier
                        .weight(1f)
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
                    shape = RoundedCornerShape(GymShapeTokens.Medium),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("weight_field"),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                .imePadding()
                .padding(horizontal = GymSpacing.ScreenPadding, vertical = GymSpacing.Sm)
                .testTag("execution_actions"),
            verticalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
        ) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            )
            GymPrimaryButton(
                text = stringResource(R.string.complete_set),
                onClick = onCompleteSet,
                glow = true,
                leadingIcon = Icons.Filled.Check,
                modifier = Modifier.testTag("complete_set_button"),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
            ) {
                GymSecondaryButton(
                    text = stringResource(R.string.session_exercises),
                    onClick = onOpenSessionExercises,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("session_exercises_button"),
                )
                GymSecondaryButton(
                    text = stringResource(R.string.skip_exercise),
                    onClick = onRequestSkip,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("skip_exercise_button"),
                )
            }
        }
    }
}

@Composable
private fun ProgressionSuggestionInfo(
    suggestion: ProgressionSuggestion?,
    canApplyProgressionToTemplate: Boolean,
    onRequestApplyProgressionSuggestion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (suggestion == null) return

    val title = when (suggestion.action) {
        ProgressionAction.NO_HISTORY -> stringResource(R.string.progression_title)
        else -> stringResource(R.string.progression_suggested_title)
    }
    val lastSessionLine = suggestion.lastSessionSets.takeIf { it.isNotEmpty() }?.let { sets ->
        stringResource(
            R.string.progression_last_session,
            formatProgressionRepsSequence(sets),
            formatVolumeKg(sets.first().weight),
        )
    }
    val guidance = when (suggestion.action) {
        ProgressionAction.INCREASE_WEIGHT -> {
            val weight = suggestion.suggestedWeight
            if (weight != null) {
                stringResource(R.string.progression_try_weight, formatVolumeKg(weight))
            } else {
                stringResource(R.string.progression_maintain)
            }
        }
        ProgressionAction.INCREASE_REPS -> stringResource(R.string.progression_increase_reps)
        ProgressionAction.MAINTAIN -> stringResource(R.string.progression_maintain)
        ProgressionAction.NO_HISTORY -> stringResource(R.string.progression_no_history)
    }
    val tone = when (suggestion.action) {
        ProgressionAction.INCREASE_WEIGHT, ProgressionAction.INCREASE_REPS -> GymCardTone.Highlight
        else -> GymCardTone.Surface
    }

    GymCard(
        modifier = modifier.testTag("progression_suggestion"),
        tone = tone,
        contentPadding = PaddingValues(GymSpacing.Md),
    ) {
        GymLabel(text = title)
        if (lastSessionLine != null) {
            Text(
                text = lastSessionLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = GymSpacing.Xs)
                    .testTag("progression_last_session"),
            )
        }
        Text(
            text = guidance,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(top = GymSpacing.Xs)
                .testTag("progression_guidance"),
        )
        if (canApplyProgressionToTemplate) {
            GymTextButton(
                text = stringResource(R.string.apply_progression_to_workout),
                onClick = onRequestApplyProgressionSuggestion,
                modifier = Modifier.testTag("apply_progression_button"),
            )
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
            .padding(GymSpacing.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(GymSpacing.Md),
    ) {
        if (workoutName.isNotBlank()) {
            GymLabel(
                text = workoutName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        verticalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
    ) {
        rows.forEach { row ->
            val name = row.exercise.exerciseName
            val tone = when (row.listStatus) {
                SessionExerciseListStatus.IN_PROGRESS -> GymCardTone.Highlight
                else -> GymCardTone.Surface
            }
            GymCard(
                modifier = Modifier.testTag("session_exercise_row_$name"),
                tone = tone,
                contentPadding = PaddingValues(GymSpacing.Md),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(statusLabelRes(row.listStatus)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = GymSpacing.Xxs)
                        .testTag("session_exercise_status_$name"),
                )
                Text(
                    text = stringResource(
                        R.string.pending_exercise_sets,
                        row.completedSets,
                        row.exercise.plannedSets,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = GymSpacing.Xxs),
                )
                if (row.canSelectNow) {
                    GymSecondaryButton(
                        text = stringResource(R.string.do_exercise_now),
                        onClick = { onResumeExercise(row.exercise.id) },
                        modifier = Modifier
                            .padding(top = GymSpacing.Sm)
                            .testTag("resume_exercise_$name"),
                    )
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
            .padding(horizontal = GymSpacing.ScreenPadding, vertical = GymSpacing.Lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GymLabel(text = stringResource(if (isPaused) R.string.rest_paused else R.string.rest_title))
        if (exerciseName.isNotBlank()) {
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = GymSpacing.Sm),
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            val diameter = min(
                min(maxWidth * 0.72f, 280.dp),
                maxHeight * 0.92f,
            ).coerceAtLeast(min(120.dp, maxHeight).coerceAtLeast(48.dp))
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
                    style = GymTheme.gymTypography.metric,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .testTag("rest_timer")
                        .semantics { contentDescription = remainingDescription },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val restActionModifier = Modifier.weight(1f)
            if (isPaused) {
                GymPrimaryButton(
                    text = stringResource(R.string.resume_rest),
                    onClick = onResumeRest,
                    modifier = restActionModifier.testTag("resume_rest_button"),
                )
            } else {
                GymPrimaryButton(
                    text = stringResource(R.string.pause_rest),
                    onClick = onPauseRest,
                    modifier = restActionModifier.testTag("pause_rest_button"),
                )
            }
            GymSecondaryButton(
                text = stringResource(R.string.skip_rest),
                onClick = onSkipRest,
                modifier = restActionModifier.testTag("skip_rest_button"),
            )
        }
    }
}

private fun formatRestClock(millis: Long): String {
    val totalSeconds = (millis / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun blockExecutionLabel(
    exercise: WorkoutSessionExercise,
    exercises: List<WorkoutSessionExercise>,
): String? {
    if (exercise.blockType == WorkoutBlockType.SINGLE) return null
    val typeLabel = when (exercise.blockType) {
        WorkoutBlockType.BI_SET -> "BI-SET"
        WorkoutBlockType.TRI_SET -> "TRI-SET"
        WorkoutBlockType.SINGLE -> return null
    }
    val sameTypeOrdered = exercises
        .map { it.blockPosition to it.blockType }
        .distinct()
        .filter { it.second == exercise.blockType }
        .sortedBy { it.first }
    val ordinal = sameTypeOrdered.indexOfFirst { it.first == exercise.blockPosition }.let {
        if (it >= 0) it + 1 else exercise.blockPosition + 1
    }
    return "$typeLabel $ordinal"
}

package com.gymtrack.presentation.workouts.detail

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.R
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.model.WorkoutExerciseDetail
import com.gymtrack.presentation.theme.GymTrackTheme

@Composable
fun WorkoutDetailScreen(
    onNavigateBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    viewModel: WorkoutDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableExercises by viewModel.availableExercises.collectAsStateWithLifecycle()

    WorkoutDetailContent(
        uiState = uiState,
        availableExercises = availableExercises,
        contentPadding = contentPadding,
        onNavigateBack = onNavigateBack,
        onShowEditWorkoutDialog = viewModel::showEditWorkoutDialog,
        onDismissEditWorkoutDialog = viewModel::dismissEditWorkoutDialog,
        onSaveWorkout = viewModel::saveWorkout,
        onShowExercisePicker = viewModel::showExercisePicker,
        onDismissExercisePicker = viewModel::dismissExercisePicker,
        onSelectExercise = viewModel::selectExercise,
        onDismissExerciseConfiguration = viewModel::dismissExerciseConfiguration,
        onSaveExerciseConfiguration = viewModel::saveExerciseConfiguration,
        onShowEditExercise = viewModel::showEditExercise,
        onShowDeleteConfirmation = viewModel::showDeleteConfirmation,
        onDismissDeleteConfirmation = viewModel::dismissDeleteConfirmation,
        onConfirmDelete = viewModel::confirmDelete,
        onErrorShown = viewModel::clearError,
        modifier = modifier,
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// Content
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutDetailContent(
    uiState: WorkoutDetailUiState,
    availableExercises: List<Exercise>,
    contentPadding: PaddingValues,
    onNavigateBack: () -> Unit,
    onShowEditWorkoutDialog: () -> Unit,
    onDismissEditWorkoutDialog: () -> Unit,
    onSaveWorkout: (name: String, description: String) -> Unit,
    onShowExercisePicker: () -> Unit,
    onDismissExercisePicker: () -> Unit,
    onSelectExercise: (Exercise) -> Unit,
    onDismissExerciseConfiguration: () -> Unit,
    onSaveExerciseConfiguration: (sets: Int, minReps: Int, maxReps: Int, weight: Double, restSeconds: Int, notes: String) -> Unit,
    onShowEditExercise: (WorkoutExerciseDetail) -> Unit,
    onShowDeleteConfirmation: (WorkoutExerciseDetail) -> Unit,
    onDismissDeleteConfirmation: () -> Unit,
    onConfirmDelete: () -> Unit,
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
                        text = uiState.workout?.name
                            ?: stringResource(R.string.workout_detail_title),
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
                    IconButton(onClick = onShowEditWorkoutDialog) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.edit_workout),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onShowExercisePicker,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_exercise_to_workout),
                    tint = MaterialTheme.colorScheme.onPrimary,
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

            uiState.exercises.isEmpty() -> {
                EmptyExercisesContent(
                    onAddClick = onShowExercisePicker,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 88.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    uiState.workout?.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        item {
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                        }
                    }

                    items(uiState.exercises, key = { it.id }) { detail ->
                        WorkoutExerciseItem(
                            detail = detail,
                            onEditClick = { onShowEditExercise(detail) },
                            onDeleteClick = { onShowDeleteConfirmation(detail) },
                        )
                    }
                }
            }
        }
    }

    // Exercise picker dialog
    if (uiState.showExercisePicker) {
        ExercisePickerDialog(
            exercises = availableExercises,
            onSelect = onSelectExercise,
            onDismiss = onDismissExercisePicker,
        )
    }

    // Exercise configuration dialog (add or edit)
    uiState.exerciseToConfigure?.let { detail ->
        ExerciseConfigurationDialog(
            detail = detail,
            onConfirm = onSaveExerciseConfiguration,
            onDismiss = onDismissExerciseConfiguration,
        )
    }

    // Delete exercise confirmation dialog
    if (uiState.showDeleteConfirmation) {
        uiState.exerciseToDelete?.let { detail ->
            DeleteExerciseDialog(
                detail = detail,
                onConfirm = onConfirmDelete,
                onDismiss = onDismissDeleteConfirmation,
            )
        }
    }

    // Edit workout dialog
    if (uiState.showEditWorkoutDialog) {
        uiState.workout?.let { workout ->
            EditWorkoutDialog(
                workout = workout,
                onSave = onSaveWorkout,
                onDismiss = onDismissEditWorkoutDialog,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Exercise list item
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun WorkoutExerciseItem(
    detail: WorkoutExerciseDetail,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: name + actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = detail.exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val meta = buildList {
                        if (detail.exercise.muscleGroup.isNotBlank()) add(detail.exercise.muscleGroup)
                        if (detail.exercise.equipmentType.isNotBlank()) add(detail.exercise.equipmentType)
                    }.joinToString(" • ")
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.edit_exercise),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.remove_exercise_from_workout),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Stats row
            Text(
                text = "${detail.sets}× • ${detail.minRepetitions}–${detail.maxRepetitions} reps • ${formatWeight(detail.weight)} kg",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${detail.restSeconds}s ${stringResource(R.string.rest_seconds_display)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp),
            )

            if (detail.notes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = detail.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

private fun formatWeight(weight: Double): String =
    if (weight == weight.toLong().toDouble()) weight.toLong().toString() else weight.toString()

// ──────────────────────────────────────────────────────────────────────────────
// Empty state
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyExercisesContent(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.FitnessCenter,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.workout_detail_empty),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.workout_detail_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onAddClick) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(stringResource(R.string.add_exercise_to_workout))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Exercise picker dialog
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExercisePickerDialog(
    exercises: List<Exercise>,
    onSelect: (Exercise) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_exercise_title)) },
        text = {
            if (exercises.isEmpty()) {
                Text(
                    text = stringResource(R.string.pick_exercise_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(exercises, key = { it.id }) { exercise ->
                        Card(
                            onClick = { onSelect(exercise) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                val meta = buildList {
                                    if (exercise.muscleGroup.isNotBlank()) add(exercise.muscleGroup)
                                    if (exercise.equipmentType.isNotBlank()) add(exercise.equipmentType)
                                }.joinToString(" • ")
                                if (meta.isNotBlank()) {
                                    Text(
                                        text = meta,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// Exercise configuration dialog
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExerciseConfigurationDialog(
    detail: WorkoutExerciseDetail,
    onConfirm: (sets: Int, minReps: Int, maxReps: Int, weight: Double, restSeconds: Int, notes: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val isNew = detail.workoutExercise.id == 0L

    var sets by remember(detail) { mutableStateOf(detail.sets.toString()) }
    var minReps by remember(detail) { mutableStateOf(detail.minRepetitions.toString()) }
    var maxReps by remember(detail) { mutableStateOf(detail.maxRepetitions.toString()) }
    var weight by remember(detail) { mutableStateOf(formatWeight(detail.weight)) }
    var restSeconds by remember(detail) { mutableStateOf(detail.restSeconds.toString()) }
    var notes by remember(detail) { mutableStateOf(detail.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isNew) stringResource(R.string.configure_exercise_title_new)
                else stringResource(R.string.configure_exercise_title_edit),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = detail.exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        value = sets,
                        onValueChange = { sets = it },
                        label = stringResource(R.string.sets_label),
                        modifier = Modifier.weight(1f),
                    )
                    NumberField(
                        value = restSeconds,
                        onValueChange = { restSeconds = it },
                        label = stringResource(R.string.rest_seconds_label),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        value = minReps,
                        onValueChange = { minReps = it },
                        label = stringResource(R.string.min_reps_label),
                        modifier = Modifier.weight(1f),
                    )
                    NumberField(
                        value = maxReps,
                        onValueChange = { maxReps = it },
                        label = stringResource(R.string.max_reps_label),
                        modifier = Modifier.weight(1f),
                    )
                }
                NumberField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = stringResource(R.string.weight_label),
                    isDecimal = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes_label)) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        sets.toIntOrNull() ?: detail.sets,
                        minReps.toIntOrNull() ?: detail.minRepetitions,
                        maxReps.toIntOrNull() ?: detail.maxRepetitions,
                        weight.toDoubleOrNull() ?: detail.weight,
                        restSeconds.toIntOrNull() ?: detail.restSeconds,
                        notes,
                    )
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isDecimal: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number,
        ),
        singleLine = true,
        modifier = modifier,
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// Delete exercise confirmation dialog
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeleteExerciseDialog(
    detail: WorkoutExerciseDetail,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.remove_exercise_from_workout)) },
        text = {
            Text(stringResource(R.string.remove_exercise_confirmation, detail.exercise.name))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// Edit workout dialog
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun EditWorkoutDialog(
    workout: Workout,
    onSave: (name: String, description: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(workout) { mutableStateOf(workout.name) }
    var description by remember(workout) { mutableStateOf(workout.description) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_workout)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text(stringResource(R.string.workout_name)) },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(stringResource(R.string.field_required)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.workout_description)) },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) nameError = true else onSave(name, description)
                },
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// Previews
// ──────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun WorkoutDetailContentLoadingPreview() {
    GymTrackTheme {
        WorkoutDetailContent(
            uiState = WorkoutDetailUiState(isLoading = true),
            availableExercises = emptyList(),
            contentPadding = PaddingValues(),
            onNavigateBack = {},
            onShowEditWorkoutDialog = {},
            onDismissEditWorkoutDialog = {},
            onSaveWorkout = { _, _ -> },
            onShowExercisePicker = {},
            onDismissExercisePicker = {},
            onSelectExercise = {},
            onDismissExerciseConfiguration = {},
            onSaveExerciseConfiguration = { _, _, _, _, _, _ -> },
            onShowEditExercise = {},
            onShowDeleteConfirmation = {},
            onDismissDeleteConfirmation = {},
            onConfirmDelete = {},
            onErrorShown = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutDetailContentEmptyPreview() {
    GymTrackTheme {
        WorkoutDetailContent(
            uiState = WorkoutDetailUiState(
                workout = Workout(1L, "Push Day", "Peitoral e tríceps"),
                isLoading = false,
            ),
            availableExercises = emptyList(),
            contentPadding = PaddingValues(),
            onNavigateBack = {},
            onShowEditWorkoutDialog = {},
            onDismissEditWorkoutDialog = {},
            onSaveWorkout = { _, _ -> },
            onShowExercisePicker = {},
            onDismissExercisePicker = {},
            onSelectExercise = {},
            onDismissExerciseConfiguration = {},
            onSaveExerciseConfiguration = { _, _, _, _, _, _ -> },
            onShowEditExercise = {},
            onShowDeleteConfirmation = {},
            onDismissDeleteConfirmation = {},
            onConfirmDelete = {},
            onErrorShown = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutDetailContentListPreview() {
    val exercise = Exercise(1L, "Supino reto", "Peitoral", "Barra")
    val we = WorkoutExercise(
        id = 1L, workoutId = 1L, exerciseId = 1L,
        position = 0, sets = 4, minRepetitions = 8, maxRepetitions = 12,
        weight = 60.0, restSeconds = 90, notes = "Controlar a descida",
    )
    GymTrackTheme {
        WorkoutDetailContent(
            uiState = WorkoutDetailUiState(
                workout = Workout(1L, "Push Day", "Peitoral e tríceps"),
                exercises = listOf(WorkoutExerciseDetail(we, exercise)),
                isLoading = false,
            ),
            availableExercises = listOf(exercise),
            contentPadding = PaddingValues(),
            onNavigateBack = {},
            onShowEditWorkoutDialog = {},
            onDismissEditWorkoutDialog = {},
            onSaveWorkout = { _, _ -> },
            onShowExercisePicker = {},
            onDismissExercisePicker = {},
            onSelectExercise = {},
            onDismissExerciseConfiguration = {},
            onSaveExerciseConfiguration = { _, _, _, _, _, _ -> },
            onShowEditExercise = {},
            onShowDeleteConfirmation = {},
            onDismissDeleteConfirmation = {},
            onConfirmDelete = {},
            onErrorShown = {},
        )
    }
}

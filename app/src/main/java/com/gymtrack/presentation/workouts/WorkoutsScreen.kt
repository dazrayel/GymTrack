package com.gymtrack.presentation.workouts

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.R
import com.gymtrack.domain.model.Workout
import com.gymtrack.presentation.theme.GymTrackTheme

@Composable
fun WorkoutsScreen(
    onNavigateToDetail: (workoutId: Long) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    viewModel: WorkoutViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    WorkoutsContent(
        uiState = uiState,
        contentPadding = contentPadding,
        onAddClick = viewModel::showAddDialog,
        onWorkoutClick = { workout -> onNavigateToDetail(workout.id) },
        onDeleteClick = viewModel::showDeleteConfirmation,
        onSaveWorkout = viewModel::saveWorkout,
        onDismissDialog = viewModel::dismissDialog,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDelete = viewModel::dismissDeleteConfirmation,
        onErrorShown = viewModel::clearError,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutsContent(
    uiState: WorkoutUiState,
    contentPadding: PaddingValues,
    onAddClick: () -> Unit,
    onWorkoutClick: (Workout) -> Unit,
    onDeleteClick: (Workout) -> Unit,
    onSaveWorkout: (name: String, description: String) -> Unit,
    onDismissDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.workouts_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_workout),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
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
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            uiState.workouts.isEmpty() -> {
                EmptyWorkoutsContent(modifier = Modifier.padding(innerPadding))
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 88.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.workouts, key = { it.id }) { workout ->
                        WorkoutItem(
                            workout = workout,
                            onClick = { onWorkoutClick(workout) },
                            onDeleteClick = { onDeleteClick(workout) },
                        )
                    }
                }
            }
        }
    }

    if (uiState.showAddEditDialog) {
        AddEditWorkoutDialog(
            workout = uiState.workoutToEdit,
            onSave = onSaveWorkout,
            onDismiss = onDismissDialog,
        )
    }

    if (uiState.showDeleteConfirmation) {
        uiState.workoutToDelete?.let { workout ->
            DeleteWorkoutDialog(
                workout = workout,
                onConfirm = onConfirmDelete,
                onDismiss = onDismissDelete,
            )
        }
    }
}

@Composable
private fun WorkoutItem(
    workout: Workout,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (workout.description.isNotBlank()) {
                    Text(
                        text = workout.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete_workout),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun EmptyWorkoutsContent(modifier: Modifier = Modifier) {
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
            text = stringResource(R.string.empty_workouts),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_workouts_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AddEditWorkoutDialog(
    workout: Workout?,
    onSave: (name: String, description: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(workout) { mutableStateOf(workout?.name ?: "") }
    var description by remember(workout) { mutableStateOf(workout?.description ?: "") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (workout != null) {
                    stringResource(R.string.edit_workout)
                } else {
                    stringResource(R.string.add_workout)
                },
            )
        },
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
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.workout_description)) },
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        onSave(name, description)
                    }
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
private fun DeleteWorkoutDialog(
    workout: Workout,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_workout)) },
        text = {
            Text(
                text = stringResource(R.string.delete_workout_confirmation, workout.name),
            )
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

// ─── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun WorkoutsContentLoadingPreview() {
    GymTrackTheme {
        WorkoutsContent(
            uiState = WorkoutUiState(isLoading = true),
            contentPadding = PaddingValues(),
            onAddClick = {},
            onWorkoutClick = {},
            onDeleteClick = {},
            onSaveWorkout = { _, _ -> },
            onDismissDialog = {},
            onConfirmDelete = {},
            onDismissDelete = {},
            onErrorShown = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutsContentEmptyPreview() {
    GymTrackTheme {
        WorkoutsContent(
            uiState = WorkoutUiState(isLoading = false),
            contentPadding = PaddingValues(),
            onAddClick = {},
            onWorkoutClick = {},
            onDeleteClick = {},
            onSaveWorkout = { _, _ -> },
            onDismissDialog = {},
            onConfirmDelete = {},
            onDismissDelete = {},
            onErrorShown = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutsContentListPreview() {
    GymTrackTheme {
        WorkoutsContent(
            uiState = WorkoutUiState(
                isLoading = false,
                workouts = listOf(
                    Workout(1, "Push Day", "Peitoral, ombros e tríceps"),
                    Workout(2, "Pull Day", "Costas e bíceps"),
                    Workout(3, "Leg Day", ""),
                ),
            ),
            contentPadding = PaddingValues(),
            onAddClick = {},
            onWorkoutClick = {},
            onDeleteClick = {},
            onSaveWorkout = { _, _ -> },
            onDismissDialog = {},
            onConfirmDelete = {},
            onDismissDelete = {},
            onErrorShown = {},
        )
    }
}

package com.gymtrack.presentation.exercises

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.R
import com.gymtrack.domain.model.Exercise
import com.gymtrack.presentation.theme.GymTrackTheme

@Composable
fun ExercisesScreen(
    outerPadding: PaddingValues,
    onNavigateBack: () -> Unit,
    onExerciseStatsClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ExerciseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ExercisesScreen(
        uiState = uiState,
        outerPadding = outerPadding,
        onNavigateBack = onNavigateBack,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onAddClick = viewModel::showAddDialog,
        onExerciseClick = viewModel::showEditDialog,
        onExerciseStatsClick = onExerciseStatsClick,
        onDeleteClick = viewModel::showDeleteConfirmation,
        onSaveExercise = viewModel::saveExercise,
        onDismissDialog = viewModel::dismissDialog,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDelete = viewModel::dismissDeleteConfirmation,
        onErrorShown = viewModel::clearError,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    uiState: ExerciseUiState,
    outerPadding: PaddingValues,
    onNavigateBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onExerciseClick: (Exercise) -> Unit,
    onExerciseStatsClick: (String) -> Unit,
    onDeleteClick: (Exercise) -> Unit,
    onSaveExercise: (name: String, muscleGroup: String, equipmentType: String) -> Unit,
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
        modifier = modifier.padding(outerPadding),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.exercises_title),
                        style = MaterialTheme.typography.titleLarge,
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_exercise),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("exercise_search"),
                placeholder = { Text(stringResource(R.string.search_exercises)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.clear_search),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
            )

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                uiState.exercises.isEmpty() -> {
                    EmptyExercisesContent(isSearchEmpty = uiState.searchQuery.isNotEmpty())
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 88.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(uiState.exercises, key = { it.id }) { exercise ->
                            ExerciseItem(
                                exercise = exercise,
                                onClick = { onExerciseClick(exercise) },
                                onStatsClick = { onExerciseStatsClick(exercise.name) },
                                onDeleteClick = { onDeleteClick(exercise) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showAddEditDialog) {
        AddEditExerciseDialog(
            exercise = uiState.exerciseToEdit,
            onSave = onSaveExercise,
            onDismiss = onDismissDialog,
        )
    }

    if (uiState.showDeleteConfirmation) {
        uiState.exerciseToDelete?.let { exercise ->
            DeleteExerciseDialog(
                exercise = exercise,
                onConfirm = onConfirmDelete,
                onDismiss = onDismissDelete,
            )
        }
    }
}

@Composable
private fun ExerciseItem(
    exercise: Exercise,
    onClick: () -> Unit,
    onStatsClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("exercise_card_${exercise.name}"),
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
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (exercise.muscleGroup.isNotBlank() || exercise.equipmentType.isNotBlank()) {
                    val subtitle = listOf(exercise.muscleGroup, exercise.equipmentType)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            TextButton(
                onClick = onStatsClick,
                modifier = Modifier.testTag("exercise_view_performance_${exercise.name}"),
            ) {
                Text(text = stringResource(R.string.exercise_view_performance))
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.testTag("exercise_delete_${exercise.name}"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete_exercise),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun EmptyExercisesContent(
    modifier: Modifier = Modifier,
    isSearchEmpty: Boolean = false,
) {
    val titleRes = if (isSearchEmpty) {
        R.string.empty_exercises_search
    } else {
        R.string.empty_exercises
    }
    val hintRes = if (isSearchEmpty) {
        R.string.empty_exercises_search_hint
    } else {
        R.string.empty_exercises_hint
    }
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
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(hintRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AddEditExerciseDialog(
    exercise: Exercise?,
    onSave: (name: String, muscleGroup: String, equipmentType: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(exercise) { mutableStateOf(exercise?.name ?: "") }
    var muscleGroup by remember(exercise) { mutableStateOf(exercise?.muscleGroup ?: "") }
    var equipmentType by remember(exercise) { mutableStateOf(exercise?.equipmentType ?: "") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (exercise != null) {
                    stringResource(R.string.edit_exercise)
                } else {
                    stringResource(R.string.add_exercise)
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
                    label = { Text(stringResource(R.string.exercise_name)) },
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
                    value = muscleGroup,
                    onValueChange = { muscleGroup = it },
                    label = { Text(stringResource(R.string.exercise_muscle_group)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = equipmentType,
                    onValueChange = { equipmentType = it },
                    label = { Text(stringResource(R.string.exercise_equipment_type)) },
                    singleLine = true,
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
                        onSave(name, muscleGroup, equipmentType)
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
private fun DeleteExerciseDialog(
    exercise: Exercise,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_exercise)) },
        text = {
            Text(
                text = stringResource(R.string.delete_exercise_confirmation, exercise.name),
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

@Preview(showBackground = true)
@Composable
private fun ExercisesContentLoadingPreview() {
    GymTrackTheme {
        ExercisesScreen(
            uiState = ExerciseUiState(isLoading = true),
            outerPadding = PaddingValues(),
            onNavigateBack = {},
            onSearchQueryChange = {},
            onAddClick = {},
            onExerciseClick = {},
            onExerciseStatsClick = {},
            onDeleteClick = {},
            onSaveExercise = { _, _, _ -> },
            onDismissDialog = {},
            onConfirmDelete = {},
            onDismissDelete = {},
            onErrorShown = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExercisesContentEmptyPreview() {
    GymTrackTheme {
        ExercisesScreen(
            uiState = ExerciseUiState(isLoading = false),
            outerPadding = PaddingValues(),
            onNavigateBack = {},
            onSearchQueryChange = {},
            onAddClick = {},
            onExerciseClick = {},
            onExerciseStatsClick = {},
            onDeleteClick = {},
            onSaveExercise = { _, _, _ -> },
            onDismissDialog = {},
            onConfirmDelete = {},
            onDismissDelete = {},
            onErrorShown = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ExercisesContentListPreview() {
    GymTrackTheme {
        ExercisesScreen(
            uiState = ExerciseUiState(
                isLoading = false,
                exercises = listOf(
                    Exercise(1, "Supino reto", "Peitoral", "Barra"),
                    Exercise(2, "Agachamento", "Pernas", "Barra"),
                    Exercise(3, "Pull-up", "Costas", "Barra fixa"),
                ),
            ),
            outerPadding = PaddingValues(),
            onNavigateBack = {},
            onSearchQueryChange = {},
            onAddClick = {},
            onExerciseClick = {},
            onExerciseStatsClick = {},
            onDeleteClick = {},
            onSaveExercise = { _, _, _ -> },
            onDismissDialog = {},
            onConfirmDelete = {},
            onDismissDelete = {},
            onErrorShown = {},
        )
    }
}

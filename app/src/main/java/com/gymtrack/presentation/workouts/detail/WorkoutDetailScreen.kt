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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.R
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutBlockDetail
import com.gymtrack.domain.model.WorkoutBlockType
import com.gymtrack.domain.model.WorkoutExerciseDetail
import com.gymtrack.presentation.exercises.ExerciseAnimation

@Composable
fun WorkoutDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExecution: (sessionId: Long) -> Unit = {},
    onNavigateToExercises: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    viewModel: WorkoutDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val availableExercises by viewModel.availableExercises.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.sessionStartedEvent) {
        val sessionId = uiState.sessionStartedEvent ?: return@LaunchedEffect
        onNavigateToExecution(sessionId)
        viewModel.consumeSessionStartedEvent()
    }

    WorkoutDetailContent(
        uiState = uiState,
        availableExercises = availableExercises,
        contentPadding = contentPadding,
        onNavigateBack = onNavigateBack,
        onShowEditWorkoutDialog = viewModel::showEditWorkoutDialog,
        onDismissEditWorkoutDialog = viewModel::dismissEditWorkoutDialog,
        onSaveWorkout = viewModel::saveWorkout,
        onShowAddTypeDialog = viewModel::showAddTypeDialog,
        onDismissAddTypeDialog = viewModel::dismissAddTypeDialog,
        onChooseAddType = viewModel::chooseAddType,
        onDismissBlockBuilder = viewModel::dismissBlockBuilder,
        onPickSlot = viewModel::pickSlot,
        onClearSlot = viewModel::clearSlot,
        onConfirmBlockDraft = viewModel::confirmBlockDraft,
        onDismissExercisePicker = viewModel::dismissExercisePicker,
        onSelectExercise = viewModel::selectExercise,
        onNavigateToExercises = onNavigateToExercises,
        onDismissExerciseConfiguration = viewModel::dismissExerciseConfiguration,
        onSaveExerciseConfiguration = viewModel::saveExerciseConfiguration,
        onShowEditExercise = viewModel::showEditExercise,
        onShowDeleteConfirmation = viewModel::showDeleteConfirmation,
        onDismissDeleteConfirmation = viewModel::dismissDeleteConfirmation,
        onConfirmDelete = viewModel::confirmDelete,
        onMoveUp = { index -> viewModel.reorderBlocks(index, index - 1) },
        onMoveDown = { index -> viewModel.reorderBlocks(index, index + 1) },
        onStartWorkout = viewModel::startWorkout,
        onContinueInProgressSession = viewModel::continueInProgressSession,
        onDismissInProgressConflict = viewModel::dismissInProgressConflict,
        onErrorShown = viewModel::clearError,
        modifier = modifier,
    )
}

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
    onShowAddTypeDialog: () -> Unit,
    onDismissAddTypeDialog: () -> Unit,
    onChooseAddType: (WorkoutBlockType) -> Unit,
    onDismissBlockBuilder: () -> Unit,
    onPickSlot: (Int) -> Unit,
    onClearSlot: (Int) -> Unit,
    onConfirmBlockDraft: (rounds: Int, restSeconds: Int) -> Unit,
    onDismissExercisePicker: () -> Unit,
    onSelectExercise: (Exercise) -> Unit,
    onNavigateToExercises: () -> Unit,
    onDismissExerciseConfiguration: () -> Unit,
    onSaveExerciseConfiguration: (sets: Int, minReps: Int, maxReps: Int, weight: Double, restSeconds: Int, notes: String) -> Unit,
    onShowEditExercise: (WorkoutBlockDetail, WorkoutExerciseDetail) -> Unit,
    onShowDeleteConfirmation: (WorkoutBlockDetail) -> Unit,
    onDismissDeleteConfirmation: () -> Unit,
    onConfirmDelete: () -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onStartWorkout: () -> Unit,
    onContinueInProgressSession: () -> Unit,
    onDismissInProgressConflict: () -> Unit,
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
                        text = uiState.workout?.name ?: stringResource(R.string.workout_detail_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = onStartWorkout, enabled = uiState.blocks.isNotEmpty()) {
                        Text(stringResource(R.string.start_workout))
                    }
                    IconButton(onClick = onShowEditWorkoutDialog) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_workout))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onShowAddTypeDialog, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_exercise_to_workout), tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            uiState.blocks.isEmpty() -> EmptyExercisesContent(onAddClick = onShowAddTypeDialog, modifier = Modifier.padding(innerPadding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.workout?.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    item {
                        Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                itemsIndexed(uiState.blocks, key = { _, b -> b.id }) { index, block ->
                    WorkoutBlockCard(
                        block = block,
                        biSetIndex = uiState.blocks.filter { it.type == WorkoutBlockType.BI_SET }.indexOfFirst { it.id == block.id }.let { if (it >= 0) it + 1 else 0 },
                        triSetIndex = uiState.blocks.filter { it.type == WorkoutBlockType.TRI_SET }.indexOfFirst { it.id == block.id }.let { if (it >= 0) it + 1 else 0 },
                        canMoveUp = index > 0,
                        canMoveDown = index < uiState.blocks.lastIndex,
                        onMoveUp = { onMoveUp(index) },
                        onMoveDown = { onMoveDown(index) },
                        onEditItem = { item -> onShowEditExercise(block, item) },
                        onDelete = { onShowDeleteConfirmation(block) },
                    )
                }
            }
        }
    }
    if (uiState.showAddTypeDialog) AddTypeDialog(onChoose = onChooseAddType, onDismiss = onDismissAddTypeDialog)
    if (uiState.showBlockBuilder) {
        BlockBuilderDialog(
            type = uiState.pendingBlockType ?: WorkoutBlockType.BI_SET,
            slots = uiState.blockDraftSlots,
            onPickSlot = onPickSlot,
            onClearSlot = onClearSlot,
            onConfirm = onConfirmBlockDraft,
            onDismiss = onDismissBlockBuilder,
        )
    }
    if (uiState.showExercisePicker) {
        ExercisePickerDialog(
            exercises = availableExercises,
            onSelect = onSelectExercise,
            onDismiss = onDismissExercisePicker,
            onNavigateToExercises = onNavigateToExercises,
        )
    }
    uiState.exerciseToConfigure?.let { detail ->
        ExerciseConfigurationDialog(
            detail = detail,
            initialRounds = uiState.configureRounds,
            initialRestSeconds = uiState.configureRestSeconds,
            onConfirm = onSaveExerciseConfiguration,
            onDismiss = onDismissExerciseConfiguration,
        )
    }
    if (uiState.showDeleteConfirmation) DeleteBlockDialog(onConfirm = onConfirmDelete, onDismiss = onDismissDeleteConfirmation)
    if (uiState.showEditWorkoutDialog) {
        uiState.workout?.let { EditWorkoutDialog(workout = it, onSave = onSaveWorkout, onDismiss = onDismissEditWorkoutDialog) }
    }
    uiState.inProgressConflict?.let { conflict ->
        AlertDialog(
            onDismissRequest = onDismissInProgressConflict,
            title = { Text(stringResource(R.string.in_progress_conflict_title)) },
            text = { Text(stringResource(R.string.in_progress_conflict_message, conflict.workoutName)) },
            confirmButton = {
                TextButton(onClick = onContinueInProgressSession) { Text(stringResource(R.string.continue_in_progress_workout)) }
            },
            dismissButton = {
                TextButton(onClick = onDismissInProgressConflict) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun WorkoutBlockCard(
    block: WorkoutBlockDetail,
    biSetIndex: Int,
    triSetIndex: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEditItem: (WorkoutExerciseDetail) -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("workout_block_card_${block.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    val title = when (block.type) {
                        WorkoutBlockType.SINGLE -> block.items.firstOrNull()?.exercise?.name ?: stringResource(R.string.add_normal_exercise)
                        WorkoutBlockType.BI_SET -> stringResource(R.string.bi_set_label, biSetIndex)
                        WorkoutBlockType.TRI_SET -> stringResource(R.string.tri_set_label, triSetIndex)
                    }
                    Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${block.rounds}× · ${block.restSeconds}s ${stringResource(R.string.rest_seconds_display)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
                IconButton(onClick = onMoveUp, enabled = canMoveUp) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null) }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null) }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete_block), tint = MaterialTheme.colorScheme.error)
                }
            }
            block.items.forEachIndexed { index, item ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (block.type != WorkoutBlockType.SINGLE) {
                            Text(text = "${index + 1}. ${item.exercise.name}", style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(
                            text = "${item.minRepetitions}–${item.maxRepetitions} reps · ${formatWeight(item.weight)} kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                    IconButton(onClick = { onEditItem(item) }) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_exercise), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddTypeDialog(onChoose: (WorkoutBlockType) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_workout_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onChoose(WorkoutBlockType.SINGLE) }, modifier = Modifier.fillMaxWidth().testTag("add_type_single")) {
                    Text(stringResource(R.string.add_normal_exercise))
                }
                Button(onClick = { onChoose(WorkoutBlockType.BI_SET) }, modifier = Modifier.fillMaxWidth().testTag("add_type_bi_set")) {
                    Text(stringResource(R.string.add_bi_set))
                }
                Button(onClick = { onChoose(WorkoutBlockType.TRI_SET) }, modifier = Modifier.fillMaxWidth().testTag("add_type_tri_set")) {
                    Text(stringResource(R.string.add_tri_set))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun BlockBuilderDialog(
    type: WorkoutBlockType,
    slots: List<Exercise?>,
    onPickSlot: (Int) -> Unit,
    onClearSlot: (Int) -> Unit,
    onConfirm: (rounds: Int, restSeconds: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var rounds by remember { mutableStateOf("3") }
    var rest by remember { mutableStateOf("60") }
    val ready = slots.size == type.requiredExerciseCount && slots.all { it != null }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (type == WorkoutBlockType.BI_SET) stringResource(R.string.add_bi_set) else stringResource(R.string.add_tri_set))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                slots.forEachIndexed { index, exercise ->
                    Text(stringResource(R.string.block_slot_label, index + 1))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = exercise?.name ?: stringResource(R.string.block_slot_select),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        TextButton(onClick = { onPickSlot(index) }) { Text(stringResource(R.string.block_slot_select)) }
                        if (exercise != null) {
                            IconButton(onClick = { onClearSlot(index) }) { Icon(Icons.Filled.Clear, contentDescription = null) }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rounds,
                        onValueChange = { rounds = it },
                        label = { Text(stringResource(R.string.block_rounds_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = rest,
                        onValueChange = { rest = it },
                        label = { Text(stringResource(R.string.rest_seconds_display)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = ready,
                onClick = { onConfirm(rounds.toIntOrNull() ?: 3, rest.toIntOrNull() ?: 60) },
                modifier = Modifier.testTag("block_builder_confirm"),
            ) { Text(stringResource(R.string.block_builder_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun EmptyExercisesContent(onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.FitnessCenter, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.workout_detail_empty), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.workout_detail_empty_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onAddClick) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text(stringResource(R.string.add_exercise_to_workout))
        }
    }
}

private fun formatWeight(weight: Double): String =
    if (weight == weight.toLong().toDouble()) weight.toLong().toString() else weight.toString()


internal fun filterExercisesByName(exercises: List<Exercise>, query: String): List<Exercise> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return exercises
    return exercises.filter { it.name.contains(trimmed, ignoreCase = true) }
}

@Composable
private fun ExercisePickerDialog(
    exercises: List<Exercise>,
    onSelect: (Exercise) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToExercises: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var previewExercise by remember { mutableStateOf<Exercise?>(null) }
    val filteredExercises = remember(exercises, searchQuery) {
        filterExercisesByName(exercises, searchQuery)
    }
    LaunchedEffect(filteredExercises) {
        val preview = previewExercise
        if (preview != null && filteredExercises.none { it.id == preview.id }) {
            previewExercise = null
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_exercise_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("picker_exercise_search"),
                    placeholder = { Text(stringResource(R.string.pick_exercise_search)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = stringResource(R.string.clear_search),
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                    ),
                    shape = RoundedCornerShape(50),
                )
                Spacer(Modifier.height(12.dp))
                when {
                    exercises.isEmpty() && searchQuery.isBlank() -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.pick_exercise_empty),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = stringResource(R.string.pick_exercise_empty_hint),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    filteredExercises.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.empty_exercises_search),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("picker_exercise_search_empty"),
                        )
                    }
                    else -> {
                        ExerciseAnimation(
                            exercise = previewExercise,
                            height = 140.dp,
                            modifier = Modifier.testTag("picker_exercise_animation"),
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(filteredExercises, key = { it.id }) { exercise ->
                                val selected = previewExercise?.id == exercise.id
                                Card(
                                    onClick = { previewExercise = exercise },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("picker_exercise_${exercise.id}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 10.dp,
                                        ),
                                    ) {
                                        Text(
                                            text = exercise.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        val meta = buildList {
                                            if (exercise.muscleGroup.isNotBlank()) {
                                                add(exercise.muscleGroup)
                                            }
                                            if (exercise.equipmentType.isNotBlank()) {
                                                add(exercise.equipmentType)
                                            }
                                        }.joinToString(" • ")
                                        if (meta.isNotBlank()) {
                                            Text(
                                                text = meta,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.6f,
                                                ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val preview = previewExercise
            if (preview != null) {
                TextButton(
                    onClick = { onSelect(preview) },
                    modifier = Modifier.testTag("picker_confirm_select"),
                ) {
                    Text(stringResource(R.string.pick_exercise_confirm))
                }
            } else {
                TextButton(
                    onClick = {
                        onDismiss()
                        onNavigateToExercises()
                    },
                    modifier = Modifier.testTag("go_to_exercises"),
                ) {
                    Text(stringResource(R.string.go_to_exercises))
                }
            }
        },
        dismissButton = {
            Row {
                if (previewExercise != null) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onNavigateToExercises()
                        },
                        modifier = Modifier.testTag("go_to_exercises"),
                    ) {
                        Text(stringResource(R.string.go_to_exercises))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
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
    initialRounds: Int,
    initialRestSeconds: Int,
    onConfirm: (sets: Int, minReps: Int, maxReps: Int, weight: Double, restSeconds: Int, notes: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val isNew = detail.workoutExercise.id == 0L

    var sets by remember(detail, initialRounds) { mutableStateOf(initialRounds.toString()) }
    var minReps by remember(detail) { mutableStateOf(detail.minRepetitions.toString()) }
    var maxReps by remember(detail) { mutableStateOf(detail.maxRepetitions.toString()) }
    var weight by remember(detail) { mutableStateOf(formatWeight(detail.weight)) }
    var restSeconds by remember(detail, initialRestSeconds) { mutableStateOf(initialRestSeconds.toString()) }
    var notes by remember(detail) { mutableStateOf(detail.notes) }

    var setsError by remember { mutableStateOf<Int?>(null) }
    var minRepsError by remember { mutableStateOf<Int?>(null) }
    var maxRepsError by remember { mutableStateOf<Int?>(null) }
    var weightError by remember { mutableStateOf<Int?>(null) }
    var restSecondsError by remember { mutableStateOf<Int?>(null) }

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
                        onValueChange = {
                            sets = it
                            setsError = null
                        },
                        label = stringResource(R.string.sets_label),
                        isError = setsError != null,
                        errorMessage = setsError?.let { stringResource(it) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sets_field"),
                    )
                    NumberField(
                        value = restSeconds,
                        onValueChange = {
                            restSeconds = it
                            restSecondsError = null
                        },
                        label = stringResource(R.string.rest_seconds_label),
                        isError = restSecondsError != null,
                        errorMessage = restSecondsError?.let { stringResource(it) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rest_seconds_field"),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField(
                        value = minReps,
                        onValueChange = {
                            minReps = it
                            minRepsError = null
                            maxRepsError = null
                        },
                        label = stringResource(R.string.min_reps_label),
                        isError = minRepsError != null,
                        errorMessage = minRepsError?.let { stringResource(it) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("min_reps_field"),
                    )
                    NumberField(
                        value = maxReps,
                        onValueChange = {
                            maxReps = it
                            maxRepsError = null
                        },
                        label = stringResource(R.string.max_reps_label),
                        isError = maxRepsError != null,
                        errorMessage = maxRepsError?.let { stringResource(it) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("max_reps_field"),
                    )
                }
                NumberField(
                    value = weight,
                    onValueChange = {
                        weight = it
                        weightError = null
                    },
                    label = stringResource(R.string.weight_label),
                    isDecimal = true,
                    isError = weightError != null,
                    errorMessage = weightError?.let { stringResource(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weight_field"),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes_label)) },
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
                    setsError = integerFieldError(sets, minValue = 1, belowMinRes = R.string.validation_sets_min)
                    minRepsError = integerFieldError(minReps, minValue = 1, belowMinRes = R.string.validation_min_reps_min)
                    maxRepsError = integerFieldError(maxReps, minValue = 1, belowMinRes = R.string.validation_max_reps_min)
                    weightError = doubleFieldError(weight)
                    restSecondsError = integerFieldError(restSeconds, minValue = 0, belowMinRes = R.string.validation_rest_min)

                    val setsValue = parseRequiredIntAtLeast(sets, minValue = 1)
                    val minValue = parseRequiredIntAtLeast(minReps, minValue = 1)
                    val maxValue = parseRequiredIntAtLeast(maxReps, minValue = 1)
                    val weightValue = parseRequiredNonNegativeDouble(weight)
                    val restValue = parseRequiredIntAtLeast(restSeconds, minValue = 0)

                    val rangeInvalid = minValue != null && maxValue != null && maxValue < minValue
                    if (rangeInvalid) {
                        maxRepsError = R.string.validation_max_reps_range
                    }

                    if (setsValue == null || minValue == null || maxValue == null ||
                        weightValue == null || restValue == null || rangeInvalid
                    ) {
                        return@TextButton
                    }

                    onConfirm(setsValue, minValue, maxValue, weightValue, restValue, notes)
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

private fun parseRequiredIntAtLeast(raw: String, minValue: Int): Int? {
    val trimmed = raw.trim()
    if (!trimmed.matches(Regex("-?\\d+"))) return null
    val parsed = trimmed.toIntOrNull() ?: return null
    return parsed.takeIf { it >= minValue }
}

private fun parseRequiredNonNegativeDouble(raw: String): Double? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val parsed = trimmed.toDoubleOrNull() ?: return null
    return parsed.takeIf { it >= 0.0 }
}

private fun integerFieldError(raw: String, minValue: Int, belowMinRes: Int): Int? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return R.string.field_required
    if (!trimmed.matches(Regex("-?\\d+"))) return R.string.validation_invalid_value
    val parsed = trimmed.toIntOrNull() ?: return R.string.validation_invalid_value
    if (parsed < minValue) return belowMinRes
    return null
}

private fun doubleFieldError(raw: String): Int? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return R.string.field_required
    val parsed = trimmed.toDoubleOrNull() ?: return R.string.validation_invalid_value
    if (parsed < 0.0) return R.string.validation_weight_min
    return null
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isDecimal: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage) }
        } else {
            null
        },
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
private fun DeleteBlockDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_block)) },
        text = {
            Text(stringResource(R.string.delete_block_confirmation))
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

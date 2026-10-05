package com.gymtrack.presentation.workouts

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.R
import com.gymtrack.domain.model.Workout
import com.gymtrack.presentation.components.GymCard
import com.gymtrack.presentation.components.GymIconButton
import com.gymtrack.presentation.components.GymTopAppBar
import com.gymtrack.presentation.theme.GymShapeTokens
import com.gymtrack.presentation.theme.GymSpacing
import com.gymtrack.presentation.theme.GymTrackTheme
import com.gymtrack.presentation.theme.gymPrimaryGlow

@Composable
fun WorkoutsScreen(
    onNavigateToDetail: (workoutId: Long) -> Unit = {},
    onNavigateToWeeklyPlanning: () -> Unit = {},
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
        onNavigateToWeeklyPlanning = onNavigateToWeeklyPlanning,
        onDeleteClick = viewModel::showDeleteConfirmation,
        onReorder = viewModel::reorderWorkouts,
        onSaveWorkout = viewModel::saveWorkout,
        onDismissDialog = viewModel::dismissDialog,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDelete = viewModel::dismissDeleteConfirmation,
        onErrorShown = viewModel::clearError,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun WorkoutsContent(
    uiState: WorkoutUiState,
    contentPadding: PaddingValues,
    onAddClick: () -> Unit,
    onWorkoutClick: (Workout) -> Unit,
    onNavigateToWeeklyPlanning: () -> Unit,
    onDeleteClick: (Workout) -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onSaveWorkout: (name: String, description: String) -> Unit,
    onDismissDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val workoutCount = uiState.workouts.size
    val eyebrow = if (!uiState.isLoading && workoutCount > 0) {
        pluralStringResource(R.plurals.workouts_count_eyebrow, workoutCount, workoutCount)
    } else {
        null
    }

    val listState = rememberLazyListState()
    var displayedWorkouts by remember { mutableStateOf(uiState.workouts) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragStartIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(uiState.workouts) {
        if (draggingIndex == null) {
            displayedWorkouts = uiState.workouts
        }
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
                title = stringResource(R.string.workouts_title),
                eyebrow = eyebrow,
                actions = {
                    GymIconButton(
                        imageVector = Icons.AutoMirrored.Outlined.EventNote,
                        contentDescription = stringResource(R.string.workouts_open_planning_cd),
                        onClick = onNavigateToWeeklyPlanning,
                        modifier = Modifier.testTag("workouts_open_planning"),
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(GymShapeTokens.Button),
                modifier = Modifier.gymPrimaryGlow(),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_workout),
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
                val latestWorkouts = rememberUpdatedState(displayedWorkouts)
                val latestDraggingIndex = rememberUpdatedState(draggingIndex)
                val latestDragOffset = rememberUpdatedState(dragOffsetY)
                val latestUiWorkouts = rememberUpdatedState(uiState.workouts)
                val latestOnReorder = rememberUpdatedState(onReorder)

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(
                        start = GymSpacing.ScreenPadding,
                        end = GymSpacing.ScreenPadding,
                        top = GymSpacing.Sm,
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(GymSpacing.CardSpacing),
                ) {
                    itemsIndexed(
                        items = displayedWorkouts,
                        key = { _, workout -> workout.id },
                    ) { index, workout ->
                        val isDragging = draggingIndex == index
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 8.dp else 0.dp,
                            label = "workoutDragElevation",
                        )
                        WorkoutItem(
                            workout = workout,
                            isDragging = isDragging,
                            dragOffsetY = if (isDragging) dragOffsetY else 0f,
                            elevation = elevation,
                            onClick = { onWorkoutClick(workout) },
                            onDeleteClick = { onDeleteClick(workout) },
                            modifier = Modifier
                                .animateItem()
                                .zIndex(if (isDragging) 1f else 0f)
                                .pointerInput(workout.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            val start = latestWorkouts.value
                                                .indexOfFirst { it.id == workout.id }
                                            if (start < 0) return@detectDragGesturesAfterLongPress
                                            draggingIndex = start
                                            dragStartIndex = start
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggingIndex = null
                                            dragStartIndex = null
                                            dragOffsetY = 0f
                                            displayedWorkouts = latestUiWorkouts.value
                                        },
                                        onDragEnd = {
                                            val start = dragStartIndex
                                            val end = draggingIndex
                                            draggingIndex = null
                                            dragStartIndex = null
                                            dragOffsetY = 0f
                                            if (start != null && end != null && start != end) {
                                                latestOnReorder.value(start, end)
                                            } else {
                                                displayedWorkouts = latestUiWorkouts.value
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY = latestDragOffset.value + dragAmount.y
                                            val current = latestDraggingIndex.value
                                                ?: return@detectDragGesturesAfterLongPress
                                            val items = latestWorkouts.value
                                            val itemInfo = listState.layoutInfo.visibleItemsInfo
                                                .find { it.index == current }
                                                ?: return@detectDragGesturesAfterLongPress
                                            val threshold = itemInfo.size / 2f
                                            when {
                                                dragOffsetY > threshold &&
                                                    current < items.lastIndex -> {
                                                    dragOffsetY -= itemInfo.size.toFloat()
                                                    displayedWorkouts = items.toMutableList().apply {
                                                        add(current + 1, removeAt(current))
                                                    }
                                                    draggingIndex = current + 1
                                                }
                                                dragOffsetY < -threshold && current > 0 -> {
                                                    dragOffsetY += itemInfo.size.toFloat()
                                                    displayedWorkouts = items.toMutableList().apply {
                                                        add(current - 1, removeAt(current))
                                                    }
                                                    draggingIndex = current - 1
                                                }
                                            }
                                        },
                                    )
                                },
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
    isDragging: Boolean,
    dragOffsetY: Float,
    elevation: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initial = workout.name.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    val reorderCd = stringResource(R.string.reorder_workout_cd, workout.name)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = dragOffsetY
                shadowElevation = elevation.toPx()
                alpha = if (isDragging) 0.96f else 1f
                scaleX = if (isDragging) 1.02f else 1f
                scaleY = if (isDragging) 1.02f else 1f
            }
            .semantics { contentDescription = reorderCd },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GymCard(
            onClick = if (isDragging) null else onClick,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(GymSpacing.CardPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(GymShapeTokens.Medium))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (initial.isNotEmpty()) initial else "•",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = GymSpacing.Md),
                ) {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (workout.description.isNotBlank()) {
                        Text(
                            text = workout.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = GymSpacing.Xs),
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        GymIconButton(
            imageVector = Icons.Outlined.Delete,
            contentDescription = stringResource(R.string.delete_workout),
            onClick = onDeleteClick,
            contentColor = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun EmptyWorkoutsContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(GymSpacing.Xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.FitnessCenter,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
        )
        Spacer(Modifier.height(GymSpacing.Lg))
        Text(
            text = stringResource(R.string.empty_workouts),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(GymSpacing.Sm))
        Text(
            text = stringResource(R.string.empty_workouts_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(GymShapeTokens.Dialog),
        title = {
            Text(
                text = if (workout != null) {
                    stringResource(R.string.edit_workout)
                } else {
                    stringResource(R.string.add_workout)
                },
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(GymSpacing.Md)) {
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
                    shape = RoundedCornerShape(GymShapeTokens.Medium),
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
                    shape = RoundedCornerShape(GymShapeTokens.Medium),
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
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(GymShapeTokens.Dialog),
        title = {
            Text(
                text = stringResource(R.string.delete_workout),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.delete_workout_confirmation, workout.name),
                style = MaterialTheme.typography.bodyMedium,
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
private fun WorkoutsContentLoadingPreview() {
    GymTrackTheme(darkTheme = true) {
        WorkoutsContent(
            uiState = WorkoutUiState(isLoading = true),
            contentPadding = PaddingValues(),
            onAddClick = {},
            onWorkoutClick = {},
            onNavigateToWeeklyPlanning = {},
            onDeleteClick = {},
            onReorder = { _, _ -> },
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
    GymTrackTheme(darkTheme = true) {
        WorkoutsContent(
            uiState = WorkoutUiState(isLoading = false),
            contentPadding = PaddingValues(),
            onAddClick = {},
            onWorkoutClick = {},
            onNavigateToWeeklyPlanning = {},
            onDeleteClick = {},
            onReorder = { _, _ -> },
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
    GymTrackTheme(darkTheme = true) {
        WorkoutsContent(
            uiState = WorkoutUiState(
                isLoading = false,
                workouts = listOf(
                    Workout(1, "Push Day", "Peitoral, ombros e tríceps", position = 0),
                    Workout(2, "Pull Day", "Costas e bíceps", position = 1),
                    Workout(3, "Leg Day", "", position = 2),
                ),
            ),
            contentPadding = PaddingValues(),
            onAddClick = {},
            onWorkoutClick = {},
            onNavigateToWeeklyPlanning = {},
            onDeleteClick = {},
            onReorder = { _, _ -> },
            onSaveWorkout = { _, _ -> },
            onDismissDialog = {},
            onConfirmDelete = {},
            onDismissDelete = {},
            onErrorShown = {},
        )
    }
}

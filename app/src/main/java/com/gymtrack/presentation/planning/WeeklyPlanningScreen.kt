package com.gymtrack.presentation.planning

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
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.R
import com.gymtrack.domain.model.WeeklyPlanDaySlot
import com.gymtrack.domain.model.Workout
import com.gymtrack.presentation.components.GymCard
import com.gymtrack.presentation.components.GymCardTone
import com.gymtrack.presentation.components.GymIconButton
import com.gymtrack.presentation.components.GymLabel
import com.gymtrack.presentation.components.GymPrimaryButton
import com.gymtrack.presentation.components.GymSecondaryButton
import com.gymtrack.presentation.components.GymTextButton
import com.gymtrack.presentation.components.GymTopAppBar
import com.gymtrack.presentation.theme.GymShapeTokens
import com.gymtrack.presentation.theme.GymSpacing
import com.gymtrack.presentation.theme.GymTrackTheme
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun WeeklyPlanningScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWorkouts: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    viewModel: WeeklyPlanningViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WeeklyPlanningScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToWorkouts = onNavigateToWorkouts,
        onAddClick = viewModel::openAddPicker,
        onPlannedDayClick = viewModel::openDayActions,
        onDismissPicker = viewModel::dismissPicker,
        onDismissDayActions = viewModel::dismissDayActions,
        onChangeWorkout = viewModel::openChangeFromActions,
        onRemovePlan = viewModel::removePlan,
        onSelectWorkout = viewModel::assignWorkout,
        onErrorShown = viewModel::clearError,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlanningScreen(
    uiState: WeeklyPlanningUiState,
    onNavigateBack: () -> Unit,
    onNavigateToWorkouts: () -> Unit,
    onAddClick: (DayOfWeek) -> Unit,
    onPlannedDayClick: (DayOfWeek) -> Unit,
    onDismissPicker: () -> Unit,
    onDismissDayActions: () -> Unit,
    onChangeWorkout: () -> Unit,
    onRemovePlan: () -> Unit,
    onSelectWorkout: (Long) -> Unit,
    onErrorShown: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val locale = remember { Locale.getDefault() }
    val eyebrow = when {
        uiState.isLoading -> null
        uiState.plannedDayCount == 0 -> stringResource(R.string.weekly_planning_eyebrow)
        else -> pluralStringResource(
            R.plurals.weekly_planning_days_eyebrow,
            uiState.plannedDayCount,
            uiState.plannedDayCount,
        )
    }

    LaunchedEffect(uiState.error) {
        val message = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = message)
        onErrorShown()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier
            .padding(contentPadding)
            .testTag("weekly_planning_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GymTopAppBar(
                title = stringResource(R.string.weekly_planning_title),
                eyebrow = eyebrow,
                navigationIcon = {
                    GymIconButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("weekly_planning_back"),
                    )
                },
            )
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
                    CircularProgressIndicator(
                        modifier = Modifier.testTag("weekly_planning_loading"),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            !uiState.hasWorkouts -> {
                EmptyWorkoutsForPlanning(
                    onNavigateToWorkouts = onNavigateToWorkouts,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("weekly_planning_list"),
                    contentPadding = PaddingValues(
                        start = GymSpacing.ScreenPadding,
                        end = GymSpacing.ScreenPadding,
                        top = GymSpacing.Sm,
                        bottom = GymSpacing.Lg,
                    ),
                    verticalArrangement = Arrangement.spacedBy(GymSpacing.CardSpacing),
                ) {
                    item(key = "intro") {
                        Text(
                            text = stringResource(R.string.weekly_planning_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(GymSpacing.Sm))
                    }
                    items(uiState.days, key = { it.dayOfWeek }) { slot ->
                        WeeklyPlanDayCard(
                            slot = slot,
                            locale = locale,
                            onAddClick = { onAddClick(slot.dayOfWeek) },
                            onPlannedClick = { onPlannedDayClick(slot.dayOfWeek) },
                        )
                    }
                }
            }
        }
    }

    if (uiState.isPickerOpen) {
        WorkoutPickerSheet(
            workouts = uiState.availableWorkouts,
            onSelect = onSelectWorkout,
            onDismiss = onDismissPicker,
        )
    }

    if (uiState.isDayActionsOpen) {
        DayActionsSheet(
            onChange = onChangeWorkout,
            onRemove = onRemovePlan,
            onDismiss = onDismissDayActions,
        )
    }
}

@Composable
private fun WeeklyPlanDayCard(
    slot: WeeklyPlanDaySlot,
    locale: Locale,
    onAddClick: () -> Unit,
    onPlannedClick: () -> Unit,
) {
    val dayLabel = remember(slot.dayOfWeek, locale) {
        slot.dayOfWeek
            .getDisplayName(TextStyle.FULL_STANDALONE, locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
    val dayTag = "weekly_planning_day_${slot.dayOfWeek.name.lowercase(Locale.ROOT)}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(dayTag),
        verticalArrangement = Arrangement.spacedBy(GymSpacing.Xs),
    ) {
        GymLabel(text = dayLabel.uppercase(locale))
        if (slot.hasPlan) {
            val name = slot.plannedWorkoutName.orEmpty()
            GymCard(
                onClick = onPlannedClick,
                tone = GymCardTone.Highlight,
                contentPadding = PaddingValues(GymSpacing.CardPadding),
                modifier = Modifier.semantics {
                    contentDescription = "$dayLabel, $name"
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.weekly_planning_planned_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = GymSpacing.Xxs),
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            GymCard(
                onClick = onAddClick,
                tone = GymCardTone.Surface,
                contentPadding = PaddingValues(GymSpacing.CardPadding),
                modifier = Modifier
                    .testTag("weekly_planning_add")
                    .semantics {
                        contentDescription = "$dayLabel, adicionar treino"
                    },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.weekly_planning_add),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutPickerSheet(
    workouts: List<Workout>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(
            topStart = GymShapeTokens.BottomSheet,
            topEnd = GymShapeTokens.BottomSheet,
        ),
        modifier = Modifier.testTag("weekly_planning_picker"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GymSpacing.ScreenPadding)
                .padding(bottom = GymSpacing.Xxl),
            verticalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
        ) {
            Text(
                text = stringResource(R.string.weekly_planning_picker_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.weekly_planning_picker_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(GymSpacing.Xs))
            workouts.forEach { workout ->
                GymCard(
                    onClick = { onSelect(workout.id) },
                    tone = GymCardTone.Surface,
                    contentPadding = PaddingValues(GymSpacing.CardPadding),
                    modifier = Modifier.testTag("weekly_planning_picker_item_${workout.id}"),
                ) {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.titleMedium,
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
                            modifier = Modifier.padding(top = GymSpacing.Xxs),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayActionsSheet(
    onChange: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(
            topStart = GymShapeTokens.BottomSheet,
            topEnd = GymShapeTokens.BottomSheet,
        ),
        modifier = Modifier.testTag("weekly_planning_actions"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = GymSpacing.ScreenPadding)
                .padding(bottom = GymSpacing.Xxl),
            verticalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
        ) {
            Text(
                text = stringResource(R.string.weekly_planning_actions_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            GymPrimaryButton(
                text = stringResource(R.string.weekly_planning_change),
                onClick = onChange,
                modifier = Modifier.testTag("weekly_planning_change"),
            )
            GymSecondaryButton(
                text = stringResource(R.string.weekly_planning_remove),
                onClick = onRemove,
                modifier = Modifier.testTag("weekly_planning_remove"),
            )
            GymTextButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun EmptyWorkoutsForPlanning(
    onNavigateToWorkouts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(GymSpacing.Xxxl)
            .testTag("weekly_planning_empty_workouts"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.EventNote,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
        )
        Spacer(Modifier.height(GymSpacing.Lg))
        Text(
            text = stringResource(R.string.weekly_planning_no_workouts_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(GymSpacing.Sm))
        Text(
            text = stringResource(R.string.weekly_planning_no_workouts_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(GymSpacing.Xxl))
        GymPrimaryButton(
            text = stringResource(R.string.weekly_planning_go_to_workouts),
            onClick = onNavigateToWorkouts,
            modifier = Modifier.testTag("weekly_planning_go_to_workouts"),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WeeklyPlanningScreenPreview() {
    GymTrackTheme(darkTheme = true) {
        WeeklyPlanningScreen(
            uiState = WeeklyPlanningUiState(
                isLoading = false,
                days = listOf(
                    WeeklyPlanDaySlot(DayOfWeek.MONDAY, 1L, "Peito + Tríceps"),
                    WeeklyPlanDaySlot(DayOfWeek.TUESDAY, 2L, "Costas + Bíceps"),
                    WeeklyPlanDaySlot(DayOfWeek.WEDNESDAY),
                    WeeklyPlanDaySlot(DayOfWeek.THURSDAY, 3L, "Pernas"),
                    WeeklyPlanDaySlot(DayOfWeek.FRIDAY, 4L, "Ombros"),
                    WeeklyPlanDaySlot(DayOfWeek.SATURDAY),
                    WeeklyPlanDaySlot(DayOfWeek.SUNDAY),
                ),
                availableWorkouts = listOf(
                    Workout(id = 1L, name = "Peito + Tríceps"),
                    Workout(id = 2L, name = "Costas + Bíceps"),
                ),
                plannedDayCount = 4,
            ),
            onNavigateBack = {},
            onNavigateToWorkouts = {},
            onAddClick = {},
            onPlannedDayClick = {},
            onDismissPicker = {},
            onDismissDayActions = {},
            onChangeWorkout = {},
            onRemovePlan = {},
            onSelectWorkout = {},
            onErrorShown = {},
        )
    }
}

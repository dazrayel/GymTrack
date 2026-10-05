package com.gymtrack.presentation.dashboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.R
import com.gymtrack.domain.model.ExercisePersonalRecords
import com.gymtrack.domain.model.MetricChange
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.formatVolumeKg
import com.gymtrack.presentation.components.GymCard
import com.gymtrack.presentation.components.GymMetric
import com.gymtrack.presentation.components.GymPrimaryButton
import com.gymtrack.presentation.components.GymSecondaryButton
import com.gymtrack.presentation.components.GymSectionHeader
import com.gymtrack.presentation.components.GymTopAppBar
import com.gymtrack.presentation.theme.GymShapeTokens
import com.gymtrack.presentation.theme.GymSpacing
import com.gymtrack.presentation.theme.GymTrackTheme

@Composable
fun DashboardScreen(
    onNavigateToWorkouts: () -> Unit,
    onNavigateToWorkoutDetail: (Long) -> Unit,
    onNavigateToAchievements: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(
        uiState = uiState,
        onNavigateToWorkouts = onNavigateToWorkouts,
        onNavigateToWorkoutDetail = onNavigateToWorkoutDetail,
        onNavigateToAchievements = onNavigateToAchievements,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onNavigateToWorkouts: () -> Unit,
    onNavigateToWorkoutDetail: (Long) -> Unit,
    onNavigateToAchievements: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.padding(contentPadding),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GymTopAppBar(
                title = stringResource(R.string.dashboard_screen_title),
                eyebrow = stringResource(R.string.dashboard_screen_eyebrow),
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("dashboard_loading"),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.isEmpty -> {
                EmptyDashboardContent(
                    displayName = uiState.displayName,
                    nextWorkout = uiState.nextWorkout,
                    onNavigateToWorkouts = onNavigateToWorkouts,
                    onNavigateToWorkoutDetail = onNavigateToWorkoutDetail,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("dashboard_content"),
                    contentPadding = PaddingValues(GymSpacing.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(GymSpacing.SectionSpacing),
                ) {
                    item { DashboardHeader(displayName = uiState.displayName) }
                    item {
                        WeeklySummaryCard(
                            sessionCount = uiState.weeklySessionCount,
                            setCount = uiState.weeklySetCount,
                            volume = uiState.weeklyVolume,
                        )
                    }
                    item {
                        WeekComparisonCard(
                            hasPreviousWeekData = uiState.hasPreviousWeekData,
                            sessionChange = uiState.sessionChange,
                            setChange = uiState.setChange,
                            volumeChange = uiState.volumeChange,
                        )
                    }
                    item { FrequencyCard(trainedDays = uiState.trainedDaysLast30) }
                    item {
                        VolumeCard(
                            volume = uiState.volumeLast30,
                            volumePeriodChange = uiState.volumePeriodChange,
                            hasPreviousPeriod = uiState.hasPreviousVolumePeriod,
                        )
                    }
                    item { HighlightCard(record = uiState.highlightRecord) }
                    item {
                        AchievementsSummaryCard(
                            unlocked = uiState.unlockedAchievementCount,
                            total = uiState.totalAchievementCount,
                            onOpen = onNavigateToAchievements,
                        )
                    }
                    item {
                        NextWorkoutCard(
                            workout = uiState.nextWorkout,
                            onOpen = onNavigateToWorkoutDetail,
                        )
                    }
                    if (uiState.hasWeeklyPlan) {
                        item {
                            WeeklyPlanSummaryCard(
                                todayName = uiState.todayPlanWorkoutName,
                                tomorrowName = uiState.tomorrowPlanWorkoutName,
                            )
                        }
                    }
                    item { Spacer(Modifier.height(GymSpacing.Sm)) }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    displayName: String?,
    modifier: Modifier = Modifier,
) {
    val name = displayName?.trim()?.takeIf { it.isNotEmpty() }
    val title = if (name == null) {
        stringResource(R.string.dashboard_greeting)
    } else {
        stringResource(R.string.dashboard_greeting_with_name, name)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_header"),
        verticalArrangement = Arrangement.spacedBy(GymSpacing.Xs),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag("dashboard_greeting"),
        )
        Text(
            text = stringResource(R.string.dashboard_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyDashboardContent(
    displayName: String?,
    nextWorkout: Workout?,
    onNavigateToWorkouts: () -> Unit,
    onNavigateToWorkoutDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.testTag("dashboard_empty"),
        contentPadding = PaddingValues(GymSpacing.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(GymSpacing.SectionSpacing),
    ) {
        item { DashboardHeader(displayName = displayName) }
        item {
            GymCard(contentPadding = PaddingValues(GymSpacing.CardPadding)) {
                Column(verticalArrangement = Arrangement.spacedBy(GymSpacing.Md)) {
                    Text(
                        text = stringResource(R.string.dashboard_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.dashboard_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.dashboard_empty_bullets),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    GymPrimaryButton(
                        text = stringResource(R.string.dashboard_empty_cta),
                        onClick = onNavigateToWorkouts,
                        modifier = Modifier.testTag("dashboard_empty_cta"),
                    )
                }
            }
        }
        if (nextWorkout != null) {
            item {
                NextWorkoutCard(
                    workout = nextWorkout,
                    onOpen = onNavigateToWorkoutDetail,
                )
            }
        }
    }
}

@Composable
private fun WeeklySummaryCard(
    sessionCount: Int,
    setCount: Int,
    volume: Double,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GymSpacing.Md)) {
        GymSectionHeader(title = stringResource(R.string.dashboard_this_week))
        GymCard(
            modifier = Modifier.testTag("dashboard_week_summary"),
            contentPadding = PaddingValues(GymSpacing.CardPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
            ) {
                GymMetric(
                    label = stringResource(R.string.dashboard_workouts),
                    value = sessionCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                GymMetric(
                    label = stringResource(R.string.dashboard_sets_label),
                    value = setCount.toString(),
                    modifier = Modifier.weight(1f),
                )
                GymMetric(
                    label = stringResource(R.string.dashboard_volume_label),
                    value = formatVolumeKg(volume),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WeekComparisonCard(
    hasPreviousWeekData: Boolean,
    sessionChange: MetricChange?,
    setChange: MetricChange?,
    volumeChange: MetricChange?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GymSpacing.Md)) {
        GymSectionHeader(title = stringResource(R.string.dashboard_comparison_title))
        GymCard(
            modifier = Modifier.testTag("dashboard_week_comparison"),
            contentPadding = PaddingValues(GymSpacing.CardPadding),
        ) {
            if (!hasPreviousWeekData) {
                Text(
                    text = stringResource(R.string.dashboard_comparison_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(GymSpacing.Sm)) {
                    ComparisonRow(
                        label = stringResource(R.string.dashboard_workouts),
                        change = sessionChange,
                        absolute = true,
                    )
                    ComparisonRow(
                        label = stringResource(R.string.dashboard_sets_label),
                        change = setChange,
                        absolute = false,
                    )
                    ComparisonRow(
                        label = stringResource(R.string.dashboard_volume_label),
                        change = volumeChange,
                        absolute = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    change: MetricChange?,
    absolute: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatMetricChange(change, absolute),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun formatMetricChange(change: MetricChange?, absolute: Boolean): String {
    return when (change) {
        null -> stringResource(R.string.dashboard_change_unavailable)
        MetricChange.New -> stringResource(R.string.dashboard_change_new)
        MetricChange.Unchanged -> if (absolute) "0" else "0%"
        is MetricChange.Absolute -> {
            val sign = if (change.delta > 0) "+" else ""
            "$sign${change.delta}"
        }
        is MetricChange.Percent -> {
            val sign = if (change.percent > 0) "+" else ""
            "$sign${change.percent}%"
        }
    }
}

@Composable
private fun FrequencyCard(trainedDays: Int) {
    val progress = (trainedDays / 30f).coerceIn(0f, 1f)
    val description = stringResource(R.string.dashboard_frequency_cd, trainedDays)
    Column(verticalArrangement = Arrangement.spacedBy(GymSpacing.Md)) {
        GymSectionHeader(title = stringResource(R.string.dashboard_frequency))
        GymCard(
            modifier = Modifier
                .testTag("dashboard_frequency")
                .semantics { contentDescription = description },
            contentPadding = PaddingValues(GymSpacing.CardPadding),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GymSpacing.Md)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(GymSpacing.Sm)
                        .clip(RoundedCornerShape(GymShapeTokens.Chip)),
                )
                Text(
                    text = stringResource(R.string.dashboard_frequency_days, trainedDays),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.dashboard_frequency_window),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VolumeCard(
    volume: Double,
    volumePeriodChange: MetricChange?,
    hasPreviousPeriod: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GymSpacing.Md)) {
        GymSectionHeader(title = stringResource(R.string.dashboard_volume_label))
        GymCard(
            modifier = Modifier.testTag("dashboard_volume"),
            contentPadding = PaddingValues(GymSpacing.CardPadding),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GymSpacing.Sm)) {
                Text(
                    text = stringResource(R.string.dashboard_volume_window),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatVolumeKg(volume),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (hasPreviousPeriod) {
                    Text(
                        text = stringResource(
                            R.string.dashboard_volume_vs_previous,
                            formatMetricChange(volumePeriodChange, absolute = false),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightCard(record: ExercisePersonalRecords?) {
    Column(verticalArrangement = Arrangement.spacedBy(GymSpacing.Md)) {
        GymSectionHeader(title = stringResource(R.string.dashboard_highlight_title))
        GymCard(
            modifier = Modifier.testTag("dashboard_highlight"),
            contentPadding = PaddingValues(GymSpacing.CardPadding),
        ) {
            if (record == null) {
                Text(
                    text = stringResource(R.string.dashboard_highlight_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(GymSpacing.Xs)) {
                    Text(
                        text = record.exerciseName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            R.string.dashboard_highlight_lift,
                            formatVolumeKg(record.bestWeight),
                            record.bestWeightReps,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.dashboard_highlight_best),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementsSummaryCard(
    unlocked: Int,
    total: Int,
    onOpen: () -> Unit,
) {
    val progress = if (total == 0) 0f else unlocked / total.toFloat()
    val description = stringResource(R.string.dashboard_achievements_cd, unlocked, total)
    Column(verticalArrangement = Arrangement.spacedBy(GymSpacing.Md)) {
        GymSectionHeader(title = stringResource(R.string.dashboard_achievements_title))
        GymCard(
            modifier = Modifier
                .testTag("dashboard_achievements")
                .semantics { contentDescription = description },
            contentPadding = PaddingValues(GymSpacing.CardPadding),
            onClick = onOpen,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GymSpacing.Md)) {
                Text(
                    text = stringResource(R.string.dashboard_achievements_progress, unlocked, total),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(GymSpacing.Sm)
                        .clip(RoundedCornerShape(GymShapeTokens.Chip)),
                )
            }
        }
    }
}

@Composable
private fun NextWorkoutCard(
    workout: Workout?,
    onOpen: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GymSpacing.Md)) {
        GymSectionHeader(title = stringResource(R.string.dashboard_next_workout_title))
        GymCard(
            modifier = Modifier.testTag("dashboard_next_workout"),
            contentPadding = PaddingValues(GymSpacing.CardPadding),
        ) {
            if (workout == null) {
                Text(
                    text = stringResource(R.string.dashboard_next_workout_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(GymSpacing.Md)) {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    workout.description.takeIf { it.isNotBlank() }?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    GymSecondaryButton(
                        text = stringResource(R.string.dashboard_next_workout_cta),
                        onClick = { onOpen(workout.id) },
                        modifier = Modifier.testTag("dashboard_next_workout_cta"),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyPlanSummaryCard(
    todayName: String?,
    tomorrowName: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(GymSpacing.Md)) {
        GymSectionHeader(title = stringResource(R.string.dashboard_plan_title))
        GymCard(
            modifier = Modifier.testTag("dashboard_weekly_plan"),
            contentPadding = PaddingValues(GymSpacing.CardPadding),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(GymSpacing.Sm)) {
                PlanDayRow(
                    label = stringResource(R.string.dashboard_plan_today),
                    workoutName = todayName,
                )
                PlanDayRow(
                    label = stringResource(R.string.dashboard_plan_tomorrow),
                    workoutName = tomorrowName,
                )
            }
        }
    }
}

@Composable
private fun PlanDayRow(
    label: String,
    workoutName: String?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = workoutName ?: stringResource(R.string.dashboard_plan_rest),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    GymTrackTheme(darkTheme = true) {
        DashboardScreen(
            uiState = DashboardUiState(
                displayName = "Danilo",
                isLoading = false,
                weeklySessionCount = 4,
                weeklySetCount = 38,
                weeklyVolume = 12450.0,
                hasPreviousWeekData = true,
                sessionChange = MetricChange.Absolute(1),
                setChange = MetricChange.Percent(12),
                volumeChange = MetricChange.Percent(8),
                trainedDaysLast30 = 18,
                volumeLast30 = 48200.0,
                hasPreviousVolumePeriod = true,
                volumePeriodChange = MetricChange.Percent(5),
                highlightRecord = ExercisePersonalRecords(
                    exerciseName = "Supino reto",
                    bestWeight = 80.0,
                    bestWeightReps = 8,
                    bestWeightSessionId = 1L,
                    bestReps = 12,
                    bestRepsWeight = 60.0,
                    bestRepsSessionId = 1L,
                    bestVolume = 1200.0,
                    bestVolumeSessionId = 1L,
                ),
                unlockedAchievementCount = 12,
                totalAchievementCount = 18,
                nextWorkout = Workout(id = 1L, name = "Treino A", description = "Peito + Tríceps"),
            ),
            onNavigateToWorkouts = {},
            onNavigateToWorkoutDetail = {},
            onNavigateToAchievements = {},
        )
    }
}

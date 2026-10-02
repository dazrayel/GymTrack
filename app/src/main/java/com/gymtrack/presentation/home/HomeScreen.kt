package com.gymtrack.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.R
import com.gymtrack.domain.model.AchievementCatalog
import com.gymtrack.domain.model.DailyVolume
import com.gymtrack.domain.model.DashboardPeriod
import com.gymtrack.domain.model.ExercisePersonalRecords
import com.gymtrack.domain.model.PeriodDashboardStats
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.model.formatVolumeKg
import com.gymtrack.domain.model.occurredAtMillis
import com.gymtrack.domain.time.formatDashboardDuration
import com.gymtrack.domain.time.formatLocalDate
import com.gymtrack.domain.time.formatLocalDayOfMonth
import com.gymtrack.domain.time.formatLocalTime
import com.gymtrack.presentation.theme.GymTrackTheme
import java.time.ZoneId

@Composable
fun HomeScreen(
    onNavigateToExercises: () -> Unit = {},
    onNavigateToWorkouts: () -> Unit = {},
    onNavigateToWorkoutDetail: (workoutId: Long) -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onSessionClick: (Long) -> Unit = {},
    onRecordClick: (String) -> Unit = {},
    onContinueInProgress: (sessionId: Long) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onNavigateToExercises = onNavigateToExercises,
        onNavigateToWorkouts = onNavigateToWorkouts,
        onNavigateToWorkoutDetail = onNavigateToWorkoutDetail,
        onNavigateToAchievements = onNavigateToAchievements,
        onSessionClick = onSessionClick,
        onRecordClick = onRecordClick,
        onContinueInProgress = onContinueInProgress,
        onPeriodSelected = viewModel::selectPeriod,
        onErrorShown = viewModel::clearError,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onNavigateToExercises: () -> Unit,
    onNavigateToWorkouts: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onErrorShown: () -> Unit,
    onNavigateToWorkoutDetail: (workoutId: Long) -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onPeriodSelected: (DashboardPeriod) -> Unit = {},
    onRecordClick: (String) -> Unit = {},
    onContinueInProgress: (sessionId: Long) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
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
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                    )
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
                    CircularProgressIndicator(
                        modifier = Modifier.testTag("home_loading"),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    uiState.inProgressSession?.let { session ->
                        InProgressResumeCard(
                            workoutName = session.workoutName,
                            onContinue = { onContinueInProgress(session.id) },
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
                        )
                    }
                    uiState.nextWorkout?.let { workout ->
                        NextWorkoutCard(
                            workout = workout,
                            onOpen = { onNavigateToWorkoutDetail(workout.id) },
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
                        )
                    }
                    AchievementsCard(
                        unlockedCount = uiState.unlockedAchievementCount,
                        totalCount = uiState.totalAchievementCount,
                        onOpen = onNavigateToAchievements,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
                    )
                    if (uiState.showEmpty) {
                        EmptyDashboardContent(
                            onNavigateToWorkouts = onNavigateToWorkouts,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        DashboardContent(
                            uiState = uiState,
                            onSessionClick = onSessionClick,
                            onNavigateToExercises = onNavigateToExercises,
                            onPeriodSelected = onPeriodSelected,
                            onRecordClick = onRecordClick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    uiState: HomeUiState,
    onSessionClick: (Long) -> Unit,
    onNavigateToExercises: () -> Unit,
    onPeriodSelected: (DashboardPeriod) -> Unit,
    onRecordClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .testTag("home_dashboard"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        uiState.recentWorkout?.let { item ->
            RecentWorkoutCard(item = item, onClick = { onSessionClick(item.sessionId) })
        }
        PeriodStatsCard(
            selectedPeriod = uiState.selectedPeriod,
            stats = uiState.periodStats,
            onPeriodSelected = onPeriodSelected,
        )
        FrequencyCard(trainedDayCount = uiState.trainedDayCount)
        TrendCard(points = uiState.dailyVolumeTrend)
        if (uiState.records.isNotEmpty()) {
            RecordsCard(records = uiState.records, onRecordClick = onRecordClick)
        }
        QuickAccessCard(onNavigateToExercises = onNavigateToExercises)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun InProgressResumeCard(
    workoutName: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_in_progress"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = stringResource(R.string.workout_in_progress),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Text(
                text = workoutName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .testTag("home_continue_in_progress"),
            ) {
                Text(text = stringResource(R.string.continue_in_progress_workout))
            }
        }
    }
}

@Composable
private fun NextWorkoutCard(
    workout: Workout,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.dashboard_next_workout_cd, workout.name)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.dashboard_next_workout_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            onClick = onOpen,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("next_workout_card")
                .semantics { contentDescription = description },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("next_workout_title"),
                    )
                    Text(
                        text = stringResource(R.string.dashboard_next_workout_cta),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .testTag("next_workout_open"),
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RecentWorkoutCard(
    item: WorkoutHistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val occurredAt = occurredAtMillis(item)
    val date = formatLocalDate(occurredAt, zone)
    val time = formatLocalTime(occurredAt, zone)
    val dateTime = stringResource(R.string.dashboard_datetime, date, time)
    val description = stringResource(R.string.dashboard_recent_cd, item.workoutName, dateTime)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.dashboard_recent_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_recent_${item.sessionId}")
                .semantics { contentDescription = description },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = item.workoutName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = dateTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = stringResource(
                        R.string.dashboard_duration,
                        formatDashboardDuration(item.durationMillis),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = stringResource(R.string.dashboard_volume, formatVolumeKg(item.volume)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun PeriodStatsCard(
    selectedPeriod: DashboardPeriod,
    stats: PeriodDashboardStats,
    onPeriodSelected: (DashboardPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleRes = when (selectedPeriod) {
        DashboardPeriod.WEEK -> R.string.dashboard_this_week
        DashboardPeriod.MONTH -> R.string.dashboard_this_month
        DashboardPeriod.ALL -> R.string.dashboard_all_time
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_period"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_period_selector"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PeriodChip(
                        label = stringResource(R.string.dashboard_period_week),
                        selected = selectedPeriod == DashboardPeriod.WEEK,
                        testTag = "home_period_week",
                        onClick = { onPeriodSelected(DashboardPeriod.WEEK) },
                        modifier = Modifier.weight(1f),
                    )
                    PeriodChip(
                        label = stringResource(R.string.dashboard_period_month),
                        selected = selectedPeriod == DashboardPeriod.MONTH,
                        testTag = "home_period_month",
                        onClick = { onPeriodSelected(DashboardPeriod.MONTH) },
                        modifier = Modifier.weight(1f),
                    )
                    PeriodChip(
                        label = stringResource(R.string.dashboard_period_all),
                        selected = selectedPeriod == DashboardPeriod.ALL,
                        testTag = "home_period_all",
                        onClick = { onPeriodSelected(DashboardPeriod.ALL) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    PeriodStat(
                        label = stringResource(R.string.dashboard_workouts),
                        value = stats.sessionCount.toString(),
                        testTag = "home_period_sessions",
                    )
                    PeriodStat(
                        label = stringResource(R.string.dashboard_volume_label),
                        value = formatVolumeKg(stats.volume),
                        testTag = "home_period_volume",
                    )
                    PeriodStat(
                        label = stringResource(R.string.dashboard_time_label),
                        value = formatDashboardDuration(stats.durationMillis),
                        testTag = "home_period_duration",
                    )
                    PeriodStat(
                        label = stringResource(R.string.dashboard_exercises_label),
                        value = stats.distinctExerciseCount.toString(),
                        testTag = "home_period_exercises",
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodChip(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        modifier = modifier.testTag(testTag),
    )
}

@Composable
private fun FrequencyCard(
    trainedDayCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.dashboard_frequency),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_frequency"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Text(
                text = stringResource(R.string.dashboard_trained_days, trainedDayCount),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun TrendCard(
    points: List<DailyVolume>,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return
    val zone = ZoneId.systemDefault()
    val maxVolume = points.maxOf { it.volume }
    val trendDescription = stringResource(R.string.dashboard_trend_cd)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.dashboard_last_seven_days),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_trend")
                .semantics { contentDescription = trendDescription },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 14.dp)
                    .height(96.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                points.forEachIndexed { index, point ->
                    val fraction = if (maxVolume <= 0.0) {
                        0.2f
                    } else {
                        (point.volume / maxVolume).toFloat().coerceIn(0.08f, 1f)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("home_trend_bar_$index"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                                    ),
                            )
                        }
                        Text(
                            text = formatLocalDayOfMonth(point.dayStartMillis, zone),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordsCard(
    records: List<ExercisePersonalRecords>,
    onRecordClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.dashboard_records),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_records"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                records.forEach { record ->
                    val description = stringResource(R.string.home_record_cd, record.exerciseName)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_record_${record.exerciseName}")
                            .clickable { onRecordClick(record.exerciseName) }
                            .semantics { contentDescription = description },
                    ) {
                        Text(
                            text = record.exerciseName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.home_record_weight,
                                    formatVolumeKg(record.bestWeight),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            )
                            Text(
                                text = stringResource(R.string.home_record_reps, record.bestReps),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            )
                            Text(
                                text = stringResource(
                                    R.string.home_record_volume,
                                    formatVolumeKg(record.bestVolume),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodStat(
    label: String,
    value: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 4.dp)
                .testTag(testTag),
        )
    }
}

@Composable
private fun AchievementsCard(
    unlockedCount: Int,
    totalCount: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = stringResource(R.string.home_achievements_summary, unlockedCount, totalCount)
    val description = stringResource(R.string.home_achievements_cd, unlockedCount, totalCount)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.achievements_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            onClick = onOpen,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_achievements")
                .semantics { contentDescription = description },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.achievements_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.testTag("home_achievements_summary"),
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    onNavigateToExercises: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.home_quick_access),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            onClick = onNavigateToExercises,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_exercises"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.exercises_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.home_exercises_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun EmptyDashboardContent(
    onNavigateToWorkouts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .testTag("home_empty"),
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
            text = stringResource(R.string.empty_dashboard),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_dashboard_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNavigateToWorkouts,
            modifier = Modifier.testTag("home_workouts_cta"),
        ) {
            Text(text = stringResource(R.string.empty_dashboard_cta))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    GymTrackTheme {
        HomeScreen(
            uiState = HomeUiState(
                isLoading = false,
                recentWorkout = WorkoutHistoryItem(
                    sessionId = 1L,
                    workoutName = "Peito e Tríceps",
                    startedAtMillis = 1_756_540_320_000L,
                    endedAtMillis = 1_756_543_200_000L,
                    durationMillis = 2_880_000L,
                    volume = 6_250.0,
                    exerciseCount = 4,
                    completedSetCount = 12,
                    plannedSetCount = 12,
                ),
                periodStats = PeriodDashboardStats(
                    sessionCount = 3,
                    volume = 18_450.0,
                    durationMillis = 8_220_000L,
                    distinctExerciseCount = 8,
                ),
                trainedDayCount = 3,
                dailyVolumeTrend = listOf(
                    DailyVolume(1_756_166_400_000L, 100.0),
                    DailyVolume(1_756_252_800_000L, 0.0),
                    DailyVolume(1_756_339_200_000L, 200.0),
                    DailyVolume(1_756_425_600_000L, 0.0),
                    DailyVolume(1_756_512_000_000L, 150.0),
                    DailyVolume(1_756_598_400_000L, 0.0),
                    DailyVolume(1_756_684_800_000L, 80.0),
                ),
                unlockedAchievementCount = 1,
                totalAchievementCount = AchievementCatalog.size,
            ),
            onNavigateToExercises = {},
            onNavigateToWorkouts = {},
            onSessionClick = {},
            onErrorShown = {},
        )
    }
}

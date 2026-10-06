package com.gymtrack.presentation.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.gymtrack.domain.model.DashboardPeriod
import com.gymtrack.domain.model.ExercisePersonalRecords
import com.gymtrack.domain.model.HomeActivityDay
import com.gymtrack.domain.model.PeriodDashboardStats
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.model.formatVolumeKg
import com.gymtrack.domain.model.occurredAtMillis
import com.gymtrack.domain.time.formatDashboardDuration
import com.gymtrack.domain.time.formatLocalDate
import com.gymtrack.domain.time.formatLocalTime
import com.gymtrack.presentation.components.GymCard
import com.gymtrack.presentation.components.GymCardTone
import com.gymtrack.presentation.components.GymLabel
import com.gymtrack.presentation.components.GymPrimaryButton
import com.gymtrack.presentation.components.GymSecondaryButton
import com.gymtrack.presentation.theme.GymShapeTokens
import com.gymtrack.presentation.theme.GymSpacing
import com.gymtrack.presentation.theme.GymTheme
import com.gymtrack.presentation.theme.GymTrackTheme
import java.time.DayOfWeek
import java.time.LocalTime
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
        containerColor = MaterialTheme.colorScheme.background,
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
                    if (uiState.showEmpty) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = GymSpacing.ScreenPadding),
                            verticalArrangement = Arrangement.spacedBy(GymSpacing.CardSpacing),
                        ) {
                            HomeHeroHeader(
                                trainedDayCount = uiState.trainedDayCount,
                                userDisplayName = uiState.userDisplayName,
                            )
                            HomeActivityWeekStrip(days = uiState.activityWeekDays)
                            uiState.inProgressSession?.let { session ->
                                InProgressResumeCard(
                                    workoutName = session.workoutName,
                                    onContinue = { onContinueInProgress(session.id) },
                                )
                            }
                            uiState.nextWorkout?.let { workout ->
                                NextWorkoutCard(
                                    workout = workout,
                                    onOpen = { onNavigateToWorkoutDetail(workout.id) },
                                )
                            }
                            AchievementsCard(
                                unlockedCount = uiState.unlockedAchievementCount,
                                totalCount = uiState.totalAchievementCount,
                                onOpen = onNavigateToAchievements,
                            )
                            EmptyDashboardContent(
                                onNavigateToWorkouts = onNavigateToWorkouts,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = GymSpacing.Xxl),
                            )
                            Spacer(Modifier.height(GymSpacing.Sm))
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = GymSpacing.ScreenPadding)
                                .testTag("home_dashboard"),
                            verticalArrangement = Arrangement.spacedBy(GymSpacing.SectionSpacing),
                        ) {
                            HomeHeroHeader(
                                trainedDayCount = uiState.trainedDayCount,
                                userDisplayName = uiState.userDisplayName,
                            )
                            HomeActivityWeekStrip(days = uiState.activityWeekDays)
                            uiState.inProgressSession?.let { session ->
                                InProgressResumeCard(
                                    workoutName = session.workoutName,
                                    onContinue = { onContinueInProgress(session.id) },
                                )
                            }
                            uiState.nextWorkout?.let { workout ->
                                NextWorkoutCard(
                                    workout = workout,
                                    onOpen = { onNavigateToWorkoutDetail(workout.id) },
                                )
                            }
                            AchievementsCard(
                                unlockedCount = uiState.unlockedAchievementCount,
                                totalCount = uiState.totalAchievementCount,
                                onOpen = onNavigateToAchievements,
                            )
                            uiState.recentWorkout?.let { item ->
                                RecentWorkoutCard(
                                    item = item,
                                    onClick = { onSessionClick(item.sessionId) },
                                )
                            }
                            PeriodStatsCard(
                                selectedPeriod = uiState.selectedPeriod,
                                stats = uiState.periodStats,
                                onPeriodSelected = onPeriodSelected,
                            )
                            FrequencyCard(trainedDayCount = uiState.trainedDayCount)
                            if (uiState.records.isNotEmpty()) {
                                RecordsCard(
                                    records = uiState.records,
                                    onRecordClick = onRecordClick,
                                )
                            }
                            QuickAccessCard(onNavigateToExercises = onNavigateToExercises)
                            Spacer(Modifier.height(GymSpacing.Sm))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeroHeader(
    trainedDayCount: Int,
    userDisplayName: String? = null,
    modifier: Modifier = Modifier,
) {
    val hour = LocalTime.now().hour
    val period = homeGreetingStringResForHour(hour)
    val (greetingRes, greetingWithNameRes) = when (period) {
        HomeGreetingPeriod.MORNING -> R.string.home_greeting_morning to R.string.home_greeting_morning_with_name
        HomeGreetingPeriod.AFTERNOON -> R.string.home_greeting_afternoon to R.string.home_greeting_afternoon_with_name
        HomeGreetingPeriod.EVENING -> R.string.home_greeting_evening to R.string.home_greeting_evening_with_name
    }
    val greetingText = formatHomeGreeting(
        baseGreeting = stringResource(greetingRes),
        greetingWithName = stringResource(greetingWithNameRes),
        displayName = userDisplayName,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = GymSpacing.Lg, bottom = GymSpacing.Sm),
        verticalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GymLabel(text = stringResource(R.string.app_name))
            if (trainedDayCount > 0) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(GymShapeTokens.Chip))
                        .background(MaterialTheme.colorScheme.secondary)
                        .padding(horizontal = GymSpacing.Md, vertical = GymSpacing.Xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(GymSpacing.Xs),
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = GymTheme.extendedColors.warning,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.home_trained_days_chip, trainedDayCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondary,
                    )
                }
            }
        }
        Text(
            text = greetingText,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag("home_greeting"),
        )
        Text(
            text = stringResource(R.string.home_welcome_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InProgressResumeCard(
    workoutName: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GymCard(
        modifier = modifier.testTag("home_in_progress"),
        tone = GymCardTone.Highlight,
    ) {
        GymLabel(text = stringResource(R.string.workout_in_progress))
        Text(
            text = workoutName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = GymSpacing.Xs),
        )
        GymPrimaryButton(
            text = stringResource(R.string.continue_in_progress_workout),
            onClick = onContinue,
            modifier = Modifier
                .padding(top = GymSpacing.Md)
                .testTag("home_continue_in_progress"),
            glow = true,
            leadingIcon = Icons.Filled.PlayArrow,
        )
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
        GymLabel(
            text = stringResource(R.string.dashboard_next_workout_title),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = GymSpacing.Sm),
        )
        GymCard(
            onClick = onOpen,
            tone = GymCardTone.Highlight,
            modifier = Modifier
                .testTag("next_workout_card")
                .semantics { contentDescription = description },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("next_workout_title"),
                    )
                    Text(
                        text = stringResource(R.string.dashboard_next_workout_cta),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = GymSpacing.Sm)
                            .testTag("next_workout_open"),
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
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
        GymLabel(
            text = stringResource(R.string.dashboard_recent_title),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = GymSpacing.Sm),
        )
        GymCard(
            onClick = onClick,
            modifier = Modifier
                .testTag("home_recent_${item.sessionId}")
                .semantics { contentDescription = description },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.workoutName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = dateTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = GymSpacing.Xs),
                    )
                    Text(
                        text = stringResource(
                            R.string.dashboard_duration,
                            formatDashboardDuration(item.durationMillis),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = GymSpacing.Sm),
                    )
                    Text(
                        text = stringResource(R.string.dashboard_volume, formatVolumeKg(item.volume)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = GymSpacing.Sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GymLabel(
                text = stringResource(titleRes),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        GymCard(
            modifier = Modifier.testTag("home_period"),
            contentPadding = PaddingValues(GymSpacing.Md),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("home_period_selector"),
                horizontalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
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
            Spacer(Modifier.height(GymSpacing.Md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
            ) {
                PeriodStatTile(
                    label = stringResource(R.string.dashboard_workouts),
                    value = stats.sessionCount.toString(),
                    testTag = "home_period_sessions",
                    modifier = Modifier.weight(1f),
                )
                PeriodStatTile(
                    label = stringResource(R.string.dashboard_volume_label),
                    value = formatVolumeKg(stats.volume),
                    testTag = "home_period_volume",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(GymSpacing.Sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
            ) {
                PeriodStatTile(
                    label = stringResource(R.string.dashboard_time_label),
                    value = formatDashboardDuration(stats.durationMillis),
                    testTag = "home_period_duration",
                    modifier = Modifier.weight(1f),
                )
                PeriodStatTile(
                    label = stringResource(R.string.dashboard_exercises_label),
                    value = stats.distinctExerciseCount.toString(),
                    testTag = "home_period_exercises",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PeriodStatTile(
    label: String,
    value: String,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GymShapeTokens.Medium))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(GymSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(GymSpacing.Xs),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.testTag(testTag),
        )
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
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        },
        modifier = modifier.testTag(testTag),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.surface,
            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
        ),
    )
}

@Composable
private fun FrequencyCard(
    trainedDayCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        GymLabel(
            text = stringResource(R.string.dashboard_frequency),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = GymSpacing.Sm),
        )
        GymCard(modifier = Modifier.testTag("home_frequency")) {
            Text(
                text = stringResource(R.string.dashboard_trained_days, trainedDayCount),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun HomeActivityWeekStrip(
    days: List<HomeActivityDay>,
    modifier: Modifier = Modifier,
) {
    if (days.isEmpty()) return
    val weekDescription = stringResource(R.string.home_activity_week_cd)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_activity_week")
            .semantics { contentDescription = weekDescription },
        horizontalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
    ) {
        days.forEachIndexed { index, day ->
            HomeActivityDayCell(
                day = day,
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_activity_day_$index"),
            )
        }
    }
}

@Composable
private fun HomeActivityDayCell(
    day: HomeActivityDay,
    modifier: Modifier = Modifier,
) {
    val weekdayFull = stringResource(weekdayFullRes(day.dayOfWeek))
    val weekdayShort = stringResource(weekdayShortRes(day.dayOfWeek))
    val dayDescription = stringResource(
        if (day.trained) {
            R.string.home_activity_day_trained_cd
        } else {
            R.string.home_activity_day_rest_cd
        },
        weekdayFull,
    )
    val cellColor = if (day.trained) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val shape = RoundedCornerShape(GymShapeTokens.ExtraSmall)
    val showTodayBorder = day.isToday && day.trained
    val shouldPulse = day.isToday && !day.trained

    Column(
        modifier = modifier.semantics { contentDescription = dayDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(GymSpacing.Xs),
    ) {
        if (shouldPulse) {
            PulsingHomeActivitySquare(
                color = cellColor,
                shape = shape,
            )
        } else {
            HomeActivitySquare(
                color = cellColor,
                shape = shape,
                showBorder = showTodayBorder,
            )
        }
        Text(
            text = weekdayShort,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HomeActivitySquare(
    color: Color,
    shape: RoundedCornerShape,
    showBorder: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(
                if (showBorder) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = shape,
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(color, shape),
    )
}

@Composable
private fun PulsingHomeActivitySquare(
    color: Color,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "home_activity_today_pulse")
    val pulseSpec = infiniteRepeatable<Float>(
        animation = tween(
            durationMillis = HomeActivityTodayPulseDurationMs,
            easing = FastOutSlowInEasing,
        ),
        repeatMode = RepeatMode.Reverse,
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = HomeActivityTodayPulseMaxScale,
        animationSpec = pulseSpec,
        label = "home_activity_today_pulse_scale",
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = HomeActivityTodayPulseMinAlpha,
        targetValue = 1f,
        animationSpec = pulseSpec,
        label = "home_activity_today_pulse_alpha",
    )
    HomeActivitySquare(
        color = color,
        shape = shape,
        showBorder = true,
        modifier = modifier
            .testTag("home_activity_today_pulse")
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
    )
}

private const val HomeActivityTodayPulseMaxScale = 1.07f
private const val HomeActivityTodayPulseMinAlpha = 0.75f
private const val HomeActivityTodayPulseDurationMs = 900

private fun weekdayShortRes(dayOfWeek: DayOfWeek): Int = when (dayOfWeek) {
    DayOfWeek.MONDAY -> R.string.home_weekday_mon
    DayOfWeek.TUESDAY -> R.string.home_weekday_tue
    DayOfWeek.WEDNESDAY -> R.string.home_weekday_wed
    DayOfWeek.THURSDAY -> R.string.home_weekday_thu
    DayOfWeek.FRIDAY -> R.string.home_weekday_fri
    DayOfWeek.SATURDAY -> R.string.home_weekday_sat
    DayOfWeek.SUNDAY -> R.string.home_weekday_sun
}

private fun weekdayFullRes(dayOfWeek: DayOfWeek): Int = when (dayOfWeek) {
    DayOfWeek.MONDAY -> R.string.home_weekday_mon_full
    DayOfWeek.TUESDAY -> R.string.home_weekday_tue_full
    DayOfWeek.WEDNESDAY -> R.string.home_weekday_wed_full
    DayOfWeek.THURSDAY -> R.string.home_weekday_thu_full
    DayOfWeek.FRIDAY -> R.string.home_weekday_fri_full
    DayOfWeek.SATURDAY -> R.string.home_weekday_sat_full
    DayOfWeek.SUNDAY -> R.string.home_weekday_sun_full
}

@Composable
private fun RecordsCard(
    records: List<ExercisePersonalRecords>,
    onRecordClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        GymLabel(
            text = stringResource(R.string.dashboard_records),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = GymSpacing.Sm),
        )
        GymCard(modifier = Modifier.testTag("home_records")) {
            Column(verticalArrangement = Arrangement.spacedBy(GymSpacing.Md)) {
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(
                            modifier = Modifier.padding(top = GymSpacing.Xs),
                            horizontalArrangement = Arrangement.spacedBy(GymSpacing.Md),
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.home_record_weight,
                                    formatVolumeKg(record.bestWeight),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(R.string.home_record_reps, record.bestReps),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(
                                    R.string.home_record_volume,
                                    formatVolumeKg(record.bestVolume),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
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
    val progress = if (totalCount > 0) {
        unlockedCount.toFloat() / totalCount.toFloat()
    } else {
        0f
    }
    Column(modifier = modifier.fillMaxWidth()) {
        GymLabel(
            text = stringResource(R.string.achievements_title),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = GymSpacing.Sm),
        )
        GymCard(
            onClick = onOpen,
            modifier = Modifier
                .testTag("home_achievements")
                .semantics { contentDescription = description },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(GymShapeTokens.Medium))
                        .background(GymTheme.extendedColors.warning.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        tint = GymTheme.extendedColors.warning,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(GymSpacing.Md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("home_achievements_summary"),
                    )
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = GymSpacing.Sm)
                            .height(6.dp)
                            .clip(RoundedCornerShape(GymShapeTokens.ExtraSmall)),
                        color = GymTheme.extendedColors.warning,
                        trackColor = MaterialTheme.colorScheme.secondary,
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
        GymLabel(
            text = stringResource(R.string.home_quick_access),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = GymSpacing.Sm),
        )
        GymCard(
            onClick = onNavigateToExercises,
            tone = GymCardTone.Elevated,
            modifier = Modifier.testTag("home_exercises"),
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
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(GymSpacing.Md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.exercises_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.home_exercises_description),
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
    }
}

@Composable
private fun EmptyDashboardContent(
    onNavigateToWorkouts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
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
        Spacer(Modifier.height(GymSpacing.Lg))
        Text(
            text = stringResource(R.string.empty_dashboard),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(GymSpacing.Sm))
        Text(
            text = stringResource(R.string.empty_dashboard_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(GymSpacing.Xxl))
        GymSecondaryButton(
            text = stringResource(R.string.empty_dashboard_cta),
            onClick = onNavigateToWorkouts,
            modifier = Modifier.testTag("home_workouts_cta"),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    GymTrackTheme(darkTheme = true) {
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
                activityWeekDays = listOf(
                    HomeActivityDay(1_756_512_000_000L, DayOfWeek.MONDAY, trained = true, isToday = false),
                    HomeActivityDay(1_756_598_400_000L, DayOfWeek.TUESDAY, trained = true, isToday = false),
                    HomeActivityDay(1_756_684_800_000L, DayOfWeek.WEDNESDAY, trained = false, isToday = true),
                    HomeActivityDay(1_756_425_600_000L, DayOfWeek.THURSDAY, trained = true, isToday = false),
                    HomeActivityDay(1_756_252_800_000L, DayOfWeek.FRIDAY, trained = false, isToday = false),
                    HomeActivityDay(1_756_339_200_000L, DayOfWeek.SATURDAY, trained = false, isToday = false),
                    HomeActivityDay(1_756_166_400_000L, DayOfWeek.SUNDAY, trained = true, isToday = false),
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

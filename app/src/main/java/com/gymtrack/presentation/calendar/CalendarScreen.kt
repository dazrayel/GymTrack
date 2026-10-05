package com.gymtrack.presentation.calendar

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.gymtrack.domain.model.CalendarDayCell
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.model.formatVolumeKg
import com.gymtrack.domain.time.formatElapsedMillis
import com.gymtrack.domain.time.formatLocalTime
import com.gymtrack.presentation.components.GymCard
import com.gymtrack.presentation.components.GymCardTone
import com.gymtrack.presentation.components.GymIconButton
import com.gymtrack.presentation.components.GymSectionHeader
import com.gymtrack.presentation.components.GymTopAppBar
import com.gymtrack.presentation.theme.GymShapeTokens
import com.gymtrack.presentation.theme.GymSpacing
import com.gymtrack.presentation.theme.GymTrackTheme
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    onSessionClick: (Long) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CalendarScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSessionClick = onSessionClick,
        onPreviousMonth = viewModel::goToPreviousMonth,
        onNextMonth = viewModel::goToNextMonth,
        onSelectDay = viewModel::selectDay,
        onErrorShown = viewModel::clearError,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onNavigateBack: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onErrorShown: () -> Unit,
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
        modifier = modifier
            .padding(contentPadding)
            .testTag("calendar_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GymTopAppBar(
                title = stringResource(R.string.calendar_title),
                eyebrow = stringResource(R.string.calendar_eyebrow),
                navigationIcon = {
                    GymIconButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("calendar_back"),
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
                        modifier = Modifier.testTag("calendar_loading"),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("calendar_content"),
                    contentPadding = PaddingValues(
                        start = GymSpacing.ScreenPadding,
                        end = GymSpacing.ScreenPadding,
                        top = GymSpacing.Sm,
                        bottom = GymSpacing.Lg,
                    ),
                    verticalArrangement = Arrangement.spacedBy(GymSpacing.SectionSpacing),
                ) {
                    item(key = "month_header") {
                        CalendarMonthHeader(
                            monthTitle = uiState.monthTitle,
                            canGoPrevious = uiState.canGoPrevious,
                            canGoNext = uiState.canGoNext,
                            onPrevious = onPreviousMonth,
                            onNext = onNextMonth,
                        )
                    }

                    item(key = "month_summary") {
                        CalendarMonthSummaryRow(
                            sessionCount = uiState.monthSessionCount,
                            trainedDayCount = uiState.monthTrainedDayCount,
                            hasAnyHistory = uiState.hasAnyHistory,
                        )
                    }

                    item(key = "grid") {
                        CalendarMonthGrid(
                            weekdayLabels = uiState.weekdayLabels,
                            days = uiState.days,
                            selectedDate = uiState.selectedDate,
                            onSelectDay = onSelectDay,
                        )
                    }

                    item(key = "selected_section") {
                        SelectedDaySection(
                            selectedDate = uiState.selectedDate,
                            sessions = uiState.selectedDaySessions,
                            onSessionClick = onSessionClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthHeader(
    monthTitle: String,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calendar_month"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        GymIconButton(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = stringResource(R.string.calendar_previous_month_cd),
            onClick = onPrevious,
            enabled = canGoPrevious,
            modifier = Modifier.testTag("calendar_prev_month"),
        )
        Text(
            text = monthTitle,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .testTag("calendar_month_title"),
        )
        GymIconButton(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.calendar_next_month_cd),
            onClick = onNext,
            enabled = canGoNext,
            modifier = Modifier.testTag("calendar_next_month"),
        )
    }
}

@Composable
private fun CalendarMonthSummaryRow(
    sessionCount: Int,
    trainedDayCount: Int,
    hasAnyHistory: Boolean,
) {
    GymCard(
        tone = GymCardTone.Surface,
        contentPadding = PaddingValues(GymSpacing.CardPadding),
        modifier = Modifier.testTag("calendar_month_summary"),
    ) {
        when {
            !hasAnyHistory -> {
                Text(
                    text = stringResource(R.string.calendar_empty_history),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            sessionCount == 0 -> {
                Text(
                    text = stringResource(R.string.calendar_month_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                Text(
                    text = pluralStringResource(
                        R.plurals.calendar_month_sessions,
                        sessionCount,
                        sessionCount,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(GymSpacing.Xxs))
                Text(
                    text = pluralStringResource(
                        R.plurals.calendar_month_trained_days,
                        trainedDayCount,
                        trainedDayCount,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    weekdayLabels: List<String>,
    days: List<CalendarDayCell>,
    selectedDate: LocalDate?,
    onSelectDay: (LocalDate) -> Unit,
) {
    GymCard(
        tone = GymCardTone.Elevated,
        contentPadding = PaddingValues(GymSpacing.Md),
        modifier = Modifier.testTag("calendar_grid"),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(GymSpacing.Sm))
        days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GymSpacing.Xxs),
            ) {
                week.forEach { cell ->
                    CalendarDayCellView(
                        cell = cell,
                        selected = cell.date != null && cell.date == selectedDate,
                        onClick = { date -> onSelectDay(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCellView(
    cell: CalendarDayCell,
    selected: Boolean,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val date = cell.date
    val dayTag = date?.let { "calendar_day_${it}" } ?: "calendar_day_empty"
    val shape = RoundedCornerShape(GymShapeTokens.Small)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(GymSpacing.Xxs)
            .clip(shape)
            .then(
                if (date != null) {
                    Modifier
                        .clickable { onClick(date) }
                        .semantics {
                            contentDescription = buildString {
                                append(date.dayOfMonth)
                                if (cell.hasWorkouts) append(", treinado")
                                if (cell.isToday) append(", hoje")
                                if (selected) append(", selecionado")
                            }
                        }
                } else {
                    Modifier
                },
            )
            .then(
                when {
                    selected -> Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                        .border(1.dp, MaterialTheme.colorScheme.primary, shape)
                    cell.isToday -> Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        shape,
                    )
                    else -> Modifier
                },
            )
            .testTag(dayTag),
        contentAlignment = Alignment.Center,
    ) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (selected) {
                    Box(modifier = Modifier.testTag("calendar_selected_day"))
                }
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        selected -> MaterialTheme.colorScheme.primary
                        cell.hasWorkouts -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Box(
                    modifier = Modifier
                        .padding(top = GymSpacing.Xxs)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            if (cell.hasWorkouts) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun SelectedDaySection(
    selectedDate: LocalDate?,
    sessions: List<WorkoutHistoryItem>,
    onSessionClick: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calendar_sessions"),
        verticalArrangement = Arrangement.spacedBy(GymSpacing.CardSpacing),
    ) {
        when {
            selectedDate == null -> {
                Text(
                    text = stringResource(R.string.calendar_select_day_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            sessions.isEmpty() -> {
                GymSectionHeader(
                    title = formatSelectedDayTitle(selectedDate),
                    eyebrow = stringResource(R.string.calendar_selected_day_eyebrow),
                )
                GymCard(
                    tone = GymCardTone.Surface,
                    contentPadding = PaddingValues(GymSpacing.CardPadding),
                ) {
                    Text(
                        text = stringResource(R.string.calendar_day_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> {
                GymSectionHeader(
                    title = formatSelectedDayTitle(selectedDate),
                    eyebrow = pluralStringResource(
                        R.plurals.calendar_day_sessions_eyebrow,
                        sessions.size,
                        sessions.size,
                    ),
                )
                sessions.forEach { item ->
                    CalendarSessionItem(
                        item = item,
                        onClick = { onSessionClick(item.sessionId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarSessionItem(
    item: WorkoutHistoryItem,
    onClick: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val occurredAt = item.endedAtMillis ?: item.startedAtMillis
    val time = formatLocalTime(occurredAt, zone)
    val duration = formatElapsedMillis(item.durationMillis)
    val volume = formatVolumeKg(item.volume)

    GymCard(
        onClick = onClick,
        tone = GymCardTone.Surface,
        contentPadding = PaddingValues(GymSpacing.CardPadding),
        modifier = Modifier
            .testTag("calendar_session_item")
            .semantics {
                contentDescription = "${item.workoutName}, $time"
            },
    ) {
        Text(
            text = item.workoutName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = time,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = GymSpacing.Xxs),
        )
        Text(
            text = stringResource(
                R.string.history_item_meta,
                duration,
                item.exerciseCount,
                item.completedSetCount,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = GymSpacing.Sm),
        )
        Text(
            text = stringResource(R.string.history_volume, volume),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = GymSpacing.Xxs),
        )
    }
}

@Composable
private fun formatSelectedDayTitle(date: LocalDate): String {
    val formatter = remember {
        DateTimeFormatter.ofPattern("d MMM yyyy")
    }
    return date.format(formatter)
}

@Preview(showBackground = true)
@Composable
private fun CalendarScreenPreview() {
    val day = LocalDate.of(2026, 10, 5)
    GymTrackTheme(darkTheme = true) {
        CalendarScreen(
            uiState = CalendarUiState(
                isLoading = false,
                visibleYearMonth = YearMonth.of(2026, 10),
                monthTitle = "Outubro 2026",
                weekdayLabels = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"),
                days = listOf(
                    CalendarDayCell(null, false, false, false, 0),
                    CalendarDayCell(null, false, false, false, 0),
                    CalendarDayCell(null, false, false, false, 0),
                    CalendarDayCell(null, false, false, false, 0),
                    CalendarDayCell(day, true, true, true, 2),
                ),
                monthSessionCount = 2,
                monthTrainedDayCount = 1,
                canGoPrevious = true,
                canGoNext = false,
                selectedDate = day,
                selectedDaySessions = listOf(
                    WorkoutHistoryItem(
                        sessionId = 1L,
                        workoutName = "Treino A",
                        startedAtMillis = 1_756_339_200_000L,
                        endedAtMillis = 1_756_341_720_000L,
                        durationMillis = 2_520_000L,
                        volume = 4_320.0,
                        exerciseCount = 3,
                        completedSetCount = 12,
                        plannedSetCount = 12,
                    ),
                ),
                hasAnyHistory = true,
            ),
            onNavigateBack = {},
            onSessionClick = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onSelectDay = {},
            onErrorShown = {},
        )
    }
}

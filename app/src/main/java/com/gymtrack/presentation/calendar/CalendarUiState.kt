package com.gymtrack.presentation.calendar

import com.gymtrack.domain.model.CalendarDayCell
import com.gymtrack.domain.model.WorkoutHistoryItem
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val visibleYearMonth: YearMonth = YearMonth.now(),
    val monthTitle: String = "",
    val weekdayLabels: List<String> = emptyList(),
    val days: List<CalendarDayCell> = emptyList(),
    val monthSessionCount: Int = 0,
    val monthTrainedDayCount: Int = 0,
    val canGoPrevious: Boolean = false,
    val canGoNext: Boolean = false,
    val selectedDate: LocalDate? = null,
    val selectedDaySessions: List<WorkoutHistoryItem> = emptyList(),
    val hasAnyHistory: Boolean = false,
)

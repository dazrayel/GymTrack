package com.gymtrack.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.domain.model.buildCalendarMonthCells
import com.gymtrack.domain.model.calendarMonthSummary
import com.gymtrack.domain.model.calendarWeekdayLabels
import com.gymtrack.domain.model.canNavigateToNextMonth
import com.gymtrack.domain.model.canNavigateToPreviousMonth
import com.gymtrack.domain.model.earliestSessionYearMonth
import com.gymtrack.domain.model.formatCalendarMonthTitle
import com.gymtrack.domain.model.groupSessionsByLocalDay
import com.gymtrack.domain.model.sessionsForLocalDate
import com.gymtrack.domain.repository.WorkoutSessionRepository
import com.gymtrack.domain.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val sessionRepository: WorkoutSessionRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val locale: Locale = Locale.getDefault()
    private val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek

    private val visibleYearMonth = MutableStateFlow(currentYearMonth())
    private val selectedDate = MutableStateFlow<LocalDate?>(null)

    private val _uiState = MutableStateFlow(
        CalendarUiState(
            visibleYearMonth = visibleYearMonth.value,
            monthTitle = formatCalendarMonthTitle(visibleYearMonth.value, locale),
            weekdayLabels = calendarWeekdayLabels(firstDayOfWeek, locale),
        ),
    )
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                sessionRepository.observeCompletedSessions(),
                visibleYearMonth,
                selectedDate,
            ) { sessions, yearMonth, selected ->
                Triple(sessions, yearMonth, selected)
            }
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message)
                    }
                }
                .collect { (sessions, yearMonth, selected) ->
                    val today = currentLocalDate()
                    val currentMonth = YearMonth.from(today)
                    val sessionsByDay = groupSessionsByLocalDay(sessions, zoneId)
                    val summary = calendarMonthSummary(yearMonth, sessionsByDay)
                    val earliest = earliestSessionYearMonth(sessions, zoneId)
                    val resolvedSelected = selected?.takeIf { YearMonth.from(it) == yearMonth }

                    _uiState.value = CalendarUiState(
                        isLoading = false,
                        error = null,
                        visibleYearMonth = yearMonth,
                        monthTitle = formatCalendarMonthTitle(yearMonth, locale),
                        weekdayLabels = calendarWeekdayLabels(firstDayOfWeek, locale),
                        days = buildCalendarMonthCells(
                            yearMonth = yearMonth,
                            sessionsByDay = sessionsByDay,
                            today = today,
                            firstDayOfWeek = firstDayOfWeek,
                        ),
                        monthSessionCount = summary.sessionCount,
                        monthTrainedDayCount = summary.trainedDayCount,
                        canGoPrevious = canNavigateToPreviousMonth(yearMonth, earliest, currentMonth),
                        canGoNext = canNavigateToNextMonth(yearMonth, currentMonth),
                        selectedDate = resolvedSelected,
                        selectedDaySessions = sessionsForLocalDate(resolvedSelected, sessionsByDay),
                        hasAnyHistory = sessions.isNotEmpty(),
                    )
                }
        }
    }

    fun goToPreviousMonth() {
        val current = visibleYearMonth.value
        if (!_uiState.value.canGoPrevious) return
        selectedDate.value = null
        visibleYearMonth.value = current.minusMonths(1)
    }

    fun goToNextMonth() {
        val current = visibleYearMonth.value
        if (!_uiState.value.canGoNext) return
        selectedDate.value = null
        visibleYearMonth.value = current.plusMonths(1)
    }

    fun selectDay(date: LocalDate?) {
        if (date == null) return
        if (YearMonth.from(date) != visibleYearMonth.value) return
        selectedDate.value = date
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun currentLocalDate(): LocalDate =
        Instant.ofEpochMilli(timeProvider.nowMillis()).atZone(zoneId).toLocalDate()

    private fun currentYearMonth(): YearMonth = YearMonth.from(currentLocalDate())
}

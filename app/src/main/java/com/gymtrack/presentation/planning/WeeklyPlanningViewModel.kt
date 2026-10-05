package com.gymtrack.presentation.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.domain.model.buildWeeklyPlanSlots
import com.gymtrack.domain.repository.WeeklyPlanRepository
import com.gymtrack.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class WeeklyPlanningViewModel @Inject constructor(
    private val weeklyPlanRepository: WeeklyPlanRepository,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val firstDayOfWeek: DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    private val _uiState = MutableStateFlow(WeeklyPlanningUiState())
    val uiState: StateFlow<WeeklyPlanningUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                weeklyPlanRepository.observePlans(),
                workoutRepository.getAll(),
            ) { plans, workouts ->
                plans to workouts
            }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { (plans, workouts) ->
                    val days = buildWeeklyPlanSlots(plans, firstDayOfWeek)
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            error = null,
                            days = days,
                            availableWorkouts = workouts,
                            plannedDayCount = days.count { it.hasPlan },
                        )
                    }
                }
        }
    }

    fun openAddPicker(dayOfWeek: DayOfWeek) {
        _uiState.update {
            it.copy(pickerDay = dayOfWeek, dayActionsDay = null)
        }
    }

    fun openDayActions(dayOfWeek: DayOfWeek) {
        _uiState.update {
            it.copy(dayActionsDay = dayOfWeek, pickerDay = null)
        }
    }

    fun dismissPicker() {
        _uiState.update { it.copy(pickerDay = null) }
    }

    fun dismissDayActions() {
        _uiState.update { it.copy(dayActionsDay = null) }
    }

    fun openChangeFromActions() {
        val day = _uiState.value.dayActionsDay ?: return
        _uiState.update {
            it.copy(pickerDay = day, dayActionsDay = null)
        }
    }

    fun assignWorkout(workoutId: Long) {
        val day = _uiState.value.pickerDay ?: return
        viewModelScope.launch {
            try {
                weeklyPlanRepository.assignWorkout(day, workoutId)
                _uiState.update { it.copy(pickerDay = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun removePlan() {
        val day = _uiState.value.dayActionsDay ?: return
        viewModelScope.launch {
            try {
                weeklyPlanRepository.clearDay(day)
                _uiState.update { it.copy(dayActionsDay = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

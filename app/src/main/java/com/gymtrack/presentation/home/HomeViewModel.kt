package com.gymtrack.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.domain.model.DashboardPeriod
import com.gymtrack.domain.model.PeriodDashboardStats
import com.gymtrack.domain.model.dailyVolumeTrend
import com.gymtrack.domain.model.dashboardPeriodStats
import com.gymtrack.domain.model.historicalPersonalRecords
import com.gymtrack.domain.model.periodBounds
import com.gymtrack.domain.model.trainedDayCount
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
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionRepository: WorkoutSessionRepository,
    private val timeProvider: TimeProvider,
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(DashboardPeriod.WEEK)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeDashboard()
        observeInProgressSession()
    }

    fun selectPeriod(period: DashboardPeriod) {
        selectedPeriod.value = period
    }

    private fun observeDashboard() {
        viewModelScope.launch {
            combine(
                sessionRepository.observeCompletedSessions(),
                sessionRepository.observeCompletedSetHistory(),
                selectedPeriod,
            ) { sessions, sets, period ->
                Triple(sessions, sets, period)
            }
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message)
                    }
                }
                .collect { (sessions, sets, period) ->
                    val nowMillis = timeProvider.nowMillis()
                    val zoneId = ZoneId.systemDefault()
                    val bounds = periodBounds(period, nowMillis, zoneId)
                    val emptyHistory = sessions.isEmpty()
                    _uiState.update {
                        it.copy(
                            recentWorkout = sessions.firstOrNull(),
                            selectedPeriod = period,
                            periodStats = if (emptyHistory) {
                                PeriodDashboardStats.Empty
                            } else {
                                dashboardPeriodStats(sessions, sets, bounds)
                            },
                            trainedDayCount = if (emptyHistory) {
                                0
                            } else {
                                trainedDayCount(sessions, bounds, zoneId)
                            },
                            dailyVolumeTrend = if (emptyHistory) {
                                emptyList()
                            } else {
                                dailyVolumeTrend(sessions, nowMillis, zoneId)
                            },
                            records = if (emptyHistory) {
                                emptyList()
                            } else {
                                historicalPersonalRecords(sets)
                            },
                            isLoading = false,
                            error = null,
                        )
                    }
                }
        }
    }

    private fun observeInProgressSession() {
        viewModelScope.launch {
            sessionRepository.observeInProgress()
                .catch { e ->
                    _uiState.update {
                        it.copy(error = e.message)
                    }
                }
                .collect { session ->
                    _uiState.update { it.copy(inProgressSession = session) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

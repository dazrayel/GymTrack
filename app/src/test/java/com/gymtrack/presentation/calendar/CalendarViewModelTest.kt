package com.gymtrack.presentation.calendar

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.gymtrack.domain.model.CompletedSetRecord
import com.gymtrack.domain.model.StartSessionResult
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSet
import com.gymtrack.domain.repository.WorkoutSessionRepository
import com.gymtrack.domain.time.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class CalendarViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val zone: ZoneId = ZoneId.systemDefault()
    private val fixedNow = LocalDateTime.of(2026, 10, 15, 12, 0)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

    private lateinit var fakeRepository: FakeCalendarSessionRepository
    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeCalendarSessionRepository()
        viewModel = CalendarViewModel(
            sessionRepository = fakeRepository,
            timeProvider = TimeProvider { fixedNow },
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isLoadingTrue_beforeFirstEmission() {
        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(YearMonth.of(2026, 10), viewModel.uiState.value.visibleYearMonth)
    }

    @Test
    fun afterLoad_marksTrainedDaysInVisibleMonth() = runTest(testDispatcher) {
        val trained = item(sessionId = 1L, occurredAt = local("2026-10-05T18:00:00"))
        fakeRepository.emit(listOf(trained))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.hasAnyHistory)
        assertEquals(1, state.monthSessionCount)
        assertEquals(1, state.monthTrainedDayCount)
        assertTrue(state.days.any { it.date == LocalDate.of(2026, 10, 5) && it.hasWorkouts })
    }

    @Test
    fun emptyHistory_monthSummaryReflectsNoActivity() = runTest(testDispatcher) {
        fakeRepository.emit(emptyList())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.hasAnyHistory)
        assertEquals(0, state.monthSessionCount)
        assertEquals(0, state.monthTrainedDayCount)
        assertTrue(state.days.none { it.hasWorkouts })
    }

    @Test
    fun selectDayWithMultipleSessions_exposesAllNewestFirst() = runTest(testDispatcher) {
        val morning = item(sessionId = 1L, occurredAt = local("2026-10-05T08:00:00"))
        val evening = item(sessionId = 2L, occurredAt = local("2026-10-05T20:00:00"))
        fakeRepository.emit(listOf(morning, evening))
        advanceUntilIdle()

        viewModel.selectDay(LocalDate.of(2026, 10, 5))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LocalDate.of(2026, 10, 5), state.selectedDate)
        assertEquals(listOf(2L, 1L), state.selectedDaySessions.map { it.sessionId })
    }

    @Test
    fun selectDayWithoutWorkouts_exposesEmptySessions() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(item(sessionId = 1L, occurredAt = local("2026-10-05T10:00:00"))))
        advanceUntilIdle()

        viewModel.selectDay(LocalDate.of(2026, 10, 6))
        advanceUntilIdle()

        assertEquals(LocalDate.of(2026, 10, 6), viewModel.uiState.value.selectedDate)
        assertTrue(viewModel.uiState.value.selectedDaySessions.isEmpty())
    }

    @Test
    fun goToPreviousMonth_updatesVisibleMonthAndClearsSelection() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(item(sessionId = 1L, occurredAt = local("2026-09-10T10:00:00"))))
        advanceUntilIdle()
        viewModel.selectDay(LocalDate.of(2026, 10, 5))
        advanceUntilIdle()

        viewModel.goToPreviousMonth()
        advanceUntilIdle()

        assertEquals(YearMonth.of(2026, 9), viewModel.uiState.value.visibleYearMonth)
        assertNull(viewModel.uiState.value.selectedDate)
        assertEquals(1, viewModel.uiState.value.monthSessionCount)
        assertTrue(viewModel.uiState.value.canGoNext)
    }

    @Test
    fun goToNextMonth_blockedAtCurrentMonth() = runTest(testDispatcher) {
        fakeRepository.emit(emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.canGoNext)
        viewModel.goToNextMonth()
        advanceUntilIdle()
        assertEquals(YearMonth.of(2026, 10), viewModel.uiState.value.visibleYearMonth)
    }

    @Test
    fun sessionOutsideVisibleMonth_notCountedInSummary() = runTest(testDispatcher) {
        val october = item(sessionId = 1L, occurredAt = local("2026-10-05T10:00:00"))
        val september = item(sessionId = 2L, occurredAt = local("2026-09-05T10:00:00"))
        fakeRepository.emit(listOf(october, september))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.monthSessionCount)
        assertEquals(1, viewModel.uiState.value.monthTrainedDayCount)
    }

    @Test
    fun selectedSessionIds_areAvailableForSummaryNavigation() = runTest(testDispatcher) {
        val session = item(sessionId = 42L, occurredAt = local("2026-10-05T10:00:00"))
        fakeRepository.emit(listOf(session))
        advanceUntilIdle()

        viewModel.selectDay(LocalDate.of(2026, 10, 5))
        advanceUntilIdle()

        assertEquals(42L, viewModel.uiState.value.selectedDaySessions.single().sessionId)
    }

    @Test
    fun whenObserveThrows_errorIsSet() = runTest(testDispatcher) {
        fakeRepository.shouldThrowOnObserve = true
        val vm = CalendarViewModel(fakeRepository, TimeProvider { fixedNow })
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("Simulated calendar error", vm.uiState.value.error)
    }

    @Test
    fun clearError_clearsMessage() = runTest(testDispatcher) {
        fakeRepository.shouldThrowOnObserve = true
        val vm = CalendarViewModel(fakeRepository, TimeProvider { fixedNow })
        advanceUntilIdle()

        vm.clearError()
        assertNull(vm.uiState.value.error)
    }

    private fun local(dateTime: String): Long =
        LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()

    private fun item(
        sessionId: Long,
        occurredAt: Long,
    ) = WorkoutHistoryItem(
        sessionId = sessionId,
        workoutName = "Treino $sessionId",
        startedAtMillis = occurredAt,
        endedAtMillis = occurredAt,
        durationMillis = 1_800_000L,
        volume = 500.0,
        exerciseCount = 3,
        completedSetCount = 9,
        plannedSetCount = 9,
    )
}

private class FakeCalendarSessionRepository : WorkoutSessionRepository {

    private val completed = MutableSharedFlow<List<WorkoutHistoryItem>>(extraBufferCapacity = 1)
    var shouldThrowOnObserve = false

    suspend fun emit(items: List<WorkoutHistoryItem>) {
        completed.emit(items)
    }

    override fun observeCompletedSessions(): Flow<List<WorkoutHistoryItem>> {
        if (shouldThrowOnObserve) {
            return flow { throw RuntimeException("Simulated calendar error") }
        }
        return completed
    }

    override fun observeCompletedSetHistory(): Flow<List<CompletedSetRecord>> = emptyFlow()

    override suspend fun startSession(workoutId: Long): StartSessionResult =
        error("Not used")

    override suspend fun getSession(id: Long): WorkoutSession? = null

    override fun observeSession(id: Long): Flow<WorkoutSession?> = emptyFlow()

    override fun observeInProgress(): Flow<WorkoutSession?> = emptyFlow()

    override fun observeSessionExercises(sessionId: Long): Flow<List<WorkoutSessionExercise>> =
        emptyFlow()

    override fun observeSets(sessionExerciseId: Long): Flow<List<WorkoutSet>> = emptyFlow()

    override suspend fun completeSet(
        sessionExerciseId: Long,
        setIndex: Int,
        reps: Int,
        weight: Double,
    ): Long = 0L

    override suspend fun startRest(
        sessionId: Long,
        sessionExerciseId: Long,
        afterSetIndex: Int,
        restSeconds: Int,
    ) = Unit

    override suspend fun pauseRest(sessionId: Long) = Unit

    override suspend fun resumeRest(sessionId: Long) = Unit

    override suspend fun skipRest(sessionId: Long) = Unit

    override suspend fun skipSessionExercise(sessionExerciseId: Long) = Unit

    override suspend fun resumeSessionExercise(sessionExerciseId: Long) = Unit

    override suspend fun finishSession(sessionId: Long) = Unit

    override suspend fun deleteCompletedSession(sessionId: Long) = Unit
}

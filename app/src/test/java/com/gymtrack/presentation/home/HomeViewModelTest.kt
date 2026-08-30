package com.gymtrack.presentation.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.gymtrack.domain.model.CompletedSetRecord
import com.gymtrack.domain.model.DashboardPeriod
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
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class HomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val zone: ZoneId = ZoneId.systemDefault()
    private lateinit var fakeRepository: FakeHomeSessionRepository
    private lateinit var clock: FakeTimeProvider

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeHomeSessionRepository()
        clock = FakeTimeProvider(now = local("2026-08-26T12:00:00"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isLoadingTrue_beforeFirstEmission() {
        val viewModel = HomeViewModel(fakeRepository, clock)
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.recentWorkout)
        assertFalse(viewModel.uiState.value.showEmpty)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun afterLoad_emptySessions_showEmpty() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(fakeRepository, clock)
        fakeRepository.emitSessions(emptyList())
        fakeRepository.emitSets(emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.showEmpty)
        assertNull(viewModel.uiState.value.recentWorkout)
        assertEquals(DashboardPeriod.WEEK, viewModel.uiState.value.selectedPeriod)
        assertEquals(0, viewModel.uiState.value.periodStats.sessionCount)
        assertTrue(viewModel.uiState.value.dailyVolumeTrend.isEmpty())
        assertEquals(0, viewModel.uiState.value.trainedDayCount)
        assertTrue(viewModel.uiState.value.records.isEmpty())
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun afterLoad_exposesRecentWorkoutAndWeeklyStats() = runTest(testDispatcher) {
        val recent = item(sessionId = 20L, name = "Mais recente", occurredAt = local("2026-08-26T10:00:00"), volume = 200.0)
        val older = item(sessionId = 10L, name = "Mais antigo", occurredAt = local("2026-08-25T10:00:00"), volume = 100.0)
        val sets = listOf(
            CompletedSetRecord(20L, local("2026-08-26T10:00:00"), "Supino", 8, 60.0),
            CompletedSetRecord(10L, local("2026-08-25T10:00:00"), "Agachamento", 5, 80.0),
        )
        val viewModel = HomeViewModel(fakeRepository, clock)
        fakeRepository.emitSessions(listOf(recent, older))
        fakeRepository.emitSets(sets)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(20L, viewModel.uiState.value.recentWorkout?.sessionId)
        assertEquals("Mais recente", viewModel.uiState.value.recentWorkout?.workoutName)
        assertEquals(2, viewModel.uiState.value.periodStats.sessionCount)
        assertEquals(300.0, viewModel.uiState.value.periodStats.volume, 0.001)
        assertEquals(2, viewModel.uiState.value.periodStats.distinctExerciseCount)
        assertEquals(DashboardPeriod.WEEK, viewModel.uiState.value.selectedPeriod)
        assertEquals(2, viewModel.uiState.value.trainedDayCount)
        assertEquals(7, viewModel.uiState.value.dailyVolumeTrend.size)
        assertEquals(
            listOf("Agachamento", "Supino"),
            viewModel.uiState.value.records.map { it.exerciseName },
        )
        assertEquals(80.0, viewModel.uiState.value.records.single { it.exerciseName == "Agachamento" }.bestWeight, 0.001)
        assertEquals(60.0, viewModel.uiState.value.records.single { it.exerciseName == "Supino" }.bestWeight, 0.001)
        assertEquals(clock.now, clock.lastReadNow)
    }

    @Test
    fun sessionOutsideCurrentWeek_isExcludedFromWeeklyStats() = runTest(testDispatcher) {
        val inWeek = item(sessionId = 1L, occurredAt = local("2026-08-26T10:00:00"), volume = 50.0)
        val outside = item(sessionId = 2L, occurredAt = local("2026-08-10T10:00:00"), volume = 9_000.0)
        val viewModel = HomeViewModel(fakeRepository, clock)
        fakeRepository.emitSessions(listOf(inWeek, outside))
        fakeRepository.emitSets(
            listOf(CompletedSetRecord(1L, local("2026-08-26T10:00:00"), "Supino", 8, 50.0)),
        )
        advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.recentWorkout?.sessionId)
        assertEquals(1, viewModel.uiState.value.periodStats.sessionCount)
        assertEquals(50.0, viewModel.uiState.value.periodStats.volume, 0.001)
        assertEquals(1, viewModel.uiState.value.trainedDayCount)
    }

    @Test
    fun sessionInRollingTrend_butOutsideIsoWeek_isNotCountedInFrequency() = runTest(testDispatcher) {
        clock.now = local("2026-08-31T12:00:00")
        val inTrendOutsideWeek = item(sessionId = 1L, occurredAt = local("2026-08-25T10:00:00"), volume = 40.0)
        val viewModel = HomeViewModel(fakeRepository, clock)
        fakeRepository.emitSessions(listOf(inTrendOutsideWeek))
        fakeRepository.emitSets(emptyList())
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.periodStats.sessionCount)
        assertEquals(0, viewModel.uiState.value.trainedDayCount)
        assertEquals(7, viewModel.uiState.value.dailyVolumeTrend.size)
        assertEquals(40.0, viewModel.uiState.value.dailyVolumeTrend.first().volume, 0.001)
        assertEquals(local("2026-08-25T00:00:00"), viewModel.uiState.value.dailyVolumeTrend.first().dayStartMillis)
        assertTrue(viewModel.uiState.value.records.isEmpty())
    }

    @Test
    fun records_collapseSameExerciseAndKeepZeroWeight() = runTest(testDispatcher) {
        val first = item(sessionId = 1L, occurredAt = local("2026-08-26T10:00:00"), volume = 10.0)
        val second = item(sessionId = 2L, occurredAt = local("2026-08-25T10:00:00"), volume = 20.0)
        val viewModel = HomeViewModel(fakeRepository, clock)
        fakeRepository.emitSessions(listOf(first, second))
        fakeRepository.emitSets(
            listOf(
                CompletedSetRecord(1L, local("2026-08-26T10:00:00"), "Supino Snapshot", 8, 80.0),
                CompletedSetRecord(2L, local("2026-08-25T10:00:00"), "Supino Snapshot", 8, 85.0),
                CompletedSetRecord(1L, local("2026-08-26T10:00:00"), "Abdominal", 20, 0.0),
            ),
        )
        advanceUntilIdle()

        assertEquals(
            listOf("Abdominal", "Supino Snapshot"),
            viewModel.uiState.value.records.map { it.exerciseName },
        )
        assertEquals(
            85.0,
            viewModel.uiState.value.records.single { it.exerciseName == "Supino Snapshot" }.bestWeight,
            0.001,
        )
        assertEquals(0.0, viewModel.uiState.value.records.single { it.exerciseName == "Abdominal" }.bestWeight, 0.001)
        assertEquals(20, viewModel.uiState.value.records.single { it.exerciseName == "Abdominal" }.bestReps)
        assertEquals(2, viewModel.uiState.value.trainedDayCount)
        assertEquals(7, viewModel.uiState.value.dailyVolumeTrend.size)
    }

    @Test
    fun reemittingSets_updatesRecords() = runTest(testDispatcher) {
        val session = item(sessionId = 1L, occurredAt = local("2026-08-26T10:00:00"), volume = 10.0)
        val viewModel = HomeViewModel(fakeRepository, clock)
        fakeRepository.emitSessions(listOf(session))
        fakeRepository.emitSets(emptyList())
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.records.isEmpty())

        fakeRepository.emitSets(
            listOf(CompletedSetRecord(1L, local("2026-08-26T10:00:00"), "Supino", 8, 80.0)),
        )
        advanceUntilIdle()
        assertEquals(listOf("Supino"), viewModel.uiState.value.records.map { it.exerciseName })
    }

    @Test
    fun reemittingSessions_updatesFrequencyAndTrend() = runTest(testDispatcher) {
        val first = item(sessionId = 1L, occurredAt = local("2026-08-26T10:00:00"), volume = 10.0)
        val viewModel = HomeViewModel(fakeRepository, clock)
        fakeRepository.emitSessions(listOf(first))
        fakeRepository.emitSets(emptyList())
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.trainedDayCount)

        val second = item(sessionId = 2L, occurredAt = local("2026-08-25T10:00:00"), volume = 20.0)
        fakeRepository.emitSessions(listOf(first, second))
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.trainedDayCount)
        assertEquals(7, viewModel.uiState.value.dailyVolumeTrend.size)
    }

    @Test
    fun emptySessions_doNotExposeSevenZeroTrendBars() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(fakeRepository, clock)
        fakeRepository.emitSessions(emptyList())
        fakeRepository.emitSets(emptyList())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showEmpty)
        assertTrue(viewModel.uiState.value.dailyVolumeTrend.isEmpty())
        assertEquals(0, viewModel.uiState.value.trainedDayCount)
        assertTrue(viewModel.uiState.value.records.isEmpty())
    }

    @Test
    fun whenObserveThrows_errorIsSet() = runTest(testDispatcher) {
        fakeRepository.shouldThrowOnObserve = true
        val viewModel = HomeViewModel(fakeRepository, clock)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Simulated home error", viewModel.uiState.value.error)
    }

    @Test
    fun clearError_clearsMessage() = runTest(testDispatcher) {
        fakeRepository.shouldThrowOnObserve = true
        val viewModel = HomeViewModel(fakeRepository, clock)
        advanceUntilIdle()

        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun startSession_isNotCalledWhenObservingDashboard() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(fakeRepository, clock)
        fakeRepository.emitSessions(emptyList())
        fakeRepository.emitSets(emptyList())
        advanceUntilIdle()

        assertEquals(0, fakeRepository.startSessionCalls)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun initialSelectedPeriod_isWeek() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(fakeRepository, clock)
        fakeRepository.emitSessions(
            listOf(item(sessionId = 1L, occurredAt = local("2026-08-26T10:00:00"), volume = 10.0)),
        )
        fakeRepository.emitSets(emptyList())
        advanceUntilIdle()
        assertEquals(DashboardPeriod.WEEK, viewModel.uiState.value.selectedPeriod)
        assertEquals(1, viewModel.uiState.value.periodStats.sessionCount)
    }

    @Test
    fun selectMonth_includesSessionOutsideWeekButInMonth() = runTest(testDispatcher) {
        val inWeek = item(sessionId = 1L, occurredAt = local("2026-08-26T10:00:00"), volume = 10.0)
        val inMonth = item(sessionId = 2L, occurredAt = local("2026-08-10T10:00:00"), volume = 20.0)
        val viewModel = HomeViewModel(fakeRepository, clock)
        fakeRepository.emitSessions(listOf(inWeek, inMonth))
        fakeRepository.emitSets(
            listOf(
                CompletedSetRecord(1L, local("2026-08-26T10:00:00"), "Supino", 8, 10.0),
                CompletedSetRecord(2L, local("2026-08-10T10:00:00"), "Agachamento", 5, 20.0),
            ),
        )
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.periodStats.sessionCount)
        assertEquals(1, viewModel.uiState.value.trainedDayCount)

        viewModel.selectPeriod(DashboardPeriod.MONTH)
        advanceUntilIdle()

        assertEquals(DashboardPeriod.MONTH, viewModel.uiState.value.selectedPeriod)
        assertEquals(2, viewModel.uiState.value.periodStats.sessionCount)
        assertEquals(30.0, viewModel.uiState.value.periodStats.volume, 0.001)
        assertEquals(2, viewModel.uiState.value.periodStats.distinctExerciseCount)
        assertEquals(2, viewModel.uiState.value.trainedDayCount)
        assertEquals(1L, viewModel.uiState.value.recentWorkout?.sessionId)
        assertEquals(7, viewModel.uiState.value.dailyVolumeTrend.size)
        assertEquals(
            listOf("Agachamento", "Supino"),
            viewModel.uiState.value.records.map { it.exerciseName },
        )
        assertEquals(clock.now, clock.lastReadNow)
    }

    @Test
    fun selectAll_includesSessionsOutsideMonth() = runTest(testDispatcher) {
        val inWeek = item(sessionId = 1L, occurredAt = local("2026-08-26T10:00:00"), volume = 10.0)
        val outsideMonth = item(sessionId = 2L, occurredAt = local("2026-07-01T10:00:00"), volume = 40.0)
        val viewModel = HomeViewModel(fakeRepository, clock)
        fakeRepository.emitSessions(listOf(inWeek, outsideMonth))
        fakeRepository.emitSets(emptyList())
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.periodStats.sessionCount)

        viewModel.selectPeriod(DashboardPeriod.ALL)
        advanceUntilIdle()

        assertEquals(DashboardPeriod.ALL, viewModel.uiState.value.selectedPeriod)
        assertEquals(2, viewModel.uiState.value.periodStats.sessionCount)
        assertEquals(50.0, viewModel.uiState.value.periodStats.volume, 0.001)
        assertEquals(2, viewModel.uiState.value.trainedDayCount)
        assertEquals(1L, viewModel.uiState.value.recentWorkout?.sessionId)
        assertEquals(7, viewModel.uiState.value.dailyVolumeTrend.size)
    }

    @Test
    fun emptyMonth_withHistory_doesNotShowEmpty() = runTest(testDispatcher) {
        clock.now = local("2026-09-15T12:00:00")
        val augustOnly = item(sessionId = 1L, occurredAt = local("2026-08-10T10:00:00"), volume = 80.0)
        val viewModel = HomeViewModel(fakeRepository, clock)
        fakeRepository.emitSessions(listOf(augustOnly))
        fakeRepository.emitSets(
            listOf(CompletedSetRecord(1L, local("2026-08-10T10:00:00"), "Supino", 8, 80.0)),
        )
        advanceUntilIdle()
        viewModel.selectPeriod(DashboardPeriod.MONTH)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showEmpty)
        assertEquals(1L, viewModel.uiState.value.recentWorkout?.sessionId)
        assertEquals(0, viewModel.uiState.value.periodStats.sessionCount)
        assertEquals(0, viewModel.uiState.value.trainedDayCount)
        assertEquals(7, viewModel.uiState.value.dailyVolumeTrend.size)
        assertEquals(listOf("Supino"), viewModel.uiState.value.records.map { it.exerciseName })
    }

    private fun local(dateTime: String): Long =
        LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()

    private fun item(
        sessionId: Long,
        name: String = "Treino",
        occurredAt: Long,
        volume: Double = 100.0,
    ) = WorkoutHistoryItem(
        sessionId = sessionId,
        workoutName = name,
        startedAtMillis = occurredAt,
        endedAtMillis = occurredAt,
        durationMillis = 60_000L,
        volume = volume,
        exerciseCount = 1,
        completedSetCount = 1,
        plannedSetCount = 1,
    )
}

private class FakeTimeProvider(var now: Long) : TimeProvider {
    var lastReadNow: Long? = null
    override fun nowMillis(): Long {
        lastReadNow = now
        return now
    }
}

private class FakeHomeSessionRepository : WorkoutSessionRepository {

    private val sessions = MutableSharedFlow<List<WorkoutHistoryItem>>(extraBufferCapacity = 1)
    private val sets = MutableSharedFlow<List<CompletedSetRecord>>(extraBufferCapacity = 1)
    var shouldThrowOnObserve = false
    var startSessionCalls = 0

    suspend fun emitSessions(items: List<WorkoutHistoryItem>) {
        sessions.emit(items)
    }

    suspend fun emitSets(items: List<CompletedSetRecord>) {
        sets.emit(items)
    }

    override fun observeCompletedSessions(): Flow<List<WorkoutHistoryItem>> {
        if (shouldThrowOnObserve) {
            return flow { throw RuntimeException("Simulated home error") }
        }
        return sessions
    }

    override fun observeCompletedSetHistory(): Flow<List<CompletedSetRecord>> = sets

    override suspend fun startSession(workoutId: Long): StartSessionResult {
        startSessionCalls += 1
        return StartSessionResult.Created(0L)
    }

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
    override suspend fun finishSession(sessionId: Long) = Unit
}

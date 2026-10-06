package com.gymtrack.presentation.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.gymtrack.domain.model.CompletedSetRecord
import com.gymtrack.domain.model.DashboardPeriod
import com.gymtrack.domain.model.StartSessionResult
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutBlock
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSet
import android.content.Context
import com.gymtrack.domain.identity.GoogleSignInResult
import com.gymtrack.domain.model.GoogleUser
import com.gymtrack.domain.repository.GoogleIdentityRepository
import com.gymtrack.domain.repository.WorkoutRepository
import com.gymtrack.domain.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.StateFlow
import com.gymtrack.domain.time.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
    private lateinit var workoutRepository: FakeHomeWorkoutRepository
    private lateinit var sessionRepository: FakeHomeSessionRepository
    private lateinit var clock: FakeTimeProvider
    private lateinit var identityRepository: FakeHomeIdentityRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        workoutRepository = FakeHomeWorkoutRepository()
        sessionRepository = FakeHomeSessionRepository()
        identityRepository = FakeHomeIdentityRepository()
        clock = FakeTimeProvider(now = local("2026-08-26T12:00:00"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HomeViewModel(
        workoutRepository,
        sessionRepository,
        identityRepository,
        clock,
    )

    @Test
    fun initialState_isLoadingTrue_beforeFirstEmission() {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.recentWorkout)
        assertNull(viewModel.uiState.value.nextWorkout)
        assertFalse(viewModel.uiState.value.showEmpty)
        assertNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.userDisplayName)
    }

    @Test
    fun identity_whenSignedIn_exposesFirstNameOnly() = runTest(testDispatcher) {
        identityRepository.user.value = GoogleUser("Danilo Barros")
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals("Danilo", viewModel.uiState.value.userDisplayName)
    }

    @Test
    fun afterLoad_emptySessions_showEmpty() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        sessionRepository.emitSessions(emptyList())
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.showEmpty)
        assertNull(viewModel.uiState.value.recentWorkout)
        assertNull(viewModel.uiState.value.nextWorkout)
        assertEquals(DashboardPeriod.WEEK, viewModel.uiState.value.selectedPeriod)
        assertEquals(0, viewModel.uiState.value.periodStats.sessionCount)
        assertEquals(7, viewModel.uiState.value.activityWeekDays.size)
        assertTrue(viewModel.uiState.value.activityWeekDays.none { it.trained })
        assertEquals(0, viewModel.uiState.value.trainedDayCount)
        assertTrue(viewModel.uiState.value.records.isEmpty())
        assertNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.inProgressSession)
        assertEquals(0, sessionRepository.startSessionCalls)
        assertEquals(0, viewModel.uiState.value.unlockedAchievementCount)
        assertEquals(18, viewModel.uiState.value.totalAchievementCount)
    }

    @Test
    fun afterLoad_exposesRecentWorkoutAndWeeklyStats() = runTest(testDispatcher) {
        val recent = item(sessionId = 20L, name = "Mais recente", occurredAt = local("2026-08-26T10:00:00"), volume = 200.0)
        val older = item(sessionId = 10L, name = "Mais antigo", occurredAt = local("2026-08-25T10:00:00"), volume = 100.0)
        val sets = listOf(
            CompletedSetRecord(20L, local("2026-08-26T10:00:00"), "Supino", 8, 60.0),
            CompletedSetRecord(10L, local("2026-08-25T10:00:00"), "Agachamento", 5, 80.0),
        )
        val viewModel = createViewModel()
        sessionRepository.emitSessions(listOf(recent, older))
        sessionRepository.emitSets(sets)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(20L, viewModel.uiState.value.recentWorkout?.sessionId)
        assertEquals("Mais recente", viewModel.uiState.value.recentWorkout?.workoutName)
        assertEquals(2, viewModel.uiState.value.periodStats.sessionCount)
        assertEquals(300.0, viewModel.uiState.value.periodStats.volume, 0.001)
        assertEquals(2, viewModel.uiState.value.periodStats.distinctExerciseCount)
        assertEquals(DashboardPeriod.WEEK, viewModel.uiState.value.selectedPeriod)
        assertEquals(2, viewModel.uiState.value.trainedDayCount)
        assertEquals(7, viewModel.uiState.value.activityWeekDays.size)
        assertEquals(2, viewModel.uiState.value.activityWeekDays.count { it.trained })
        assertEquals(
            listOf("Agachamento", "Supino"),
            viewModel.uiState.value.records.map { it.exerciseName },
        )
        assertEquals(80.0, viewModel.uiState.value.records.single { it.exerciseName == "Agachamento" }.bestWeight, 0.001)
        assertEquals(60.0, viewModel.uiState.value.records.single { it.exerciseName == "Supino" }.bestWeight, 0.001)
        assertEquals(clock.now, clock.lastReadNow)
        assertTrue(viewModel.uiState.value.unlockedAchievementCount >= 1)
        assertEquals(18, viewModel.uiState.value.totalAchievementCount)
    }

    @Test
    fun achievementsSummary_unlocksFirstWorkoutWithOneSession() = runTest(testDispatcher) {
        val session = item(sessionId = 1L, occurredAt = local("2026-08-26T10:00:00"), volume = 100.0)
        val sets = listOf(CompletedSetRecord(1L, local("2026-08-26T10:00:00"), "Supino", 8, 40.0))
        val viewModel = createViewModel()
        sessionRepository.emitSessions(listOf(session))
        sessionRepository.emitSets(sets)
        advanceUntilIdle()

        assertEquals(18, viewModel.uiState.value.totalAchievementCount)
        assertTrue(viewModel.uiState.value.unlockedAchievementCount >= 1)
    }

    @Test
    fun sessionOutsideCurrentWeek_isExcludedFromWeeklyStats() = runTest(testDispatcher) {
        val inWeek = item(sessionId = 1L, occurredAt = local("2026-08-26T10:00:00"), volume = 50.0)
        val outside = item(sessionId = 2L, occurredAt = local("2026-08-10T10:00:00"), volume = 9_000.0)
        val viewModel = createViewModel()
        sessionRepository.emitSessions(listOf(inWeek, outside))
        sessionRepository.emitSets(
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
        val viewModel = createViewModel()
        sessionRepository.emitSessions(listOf(inTrendOutsideWeek))
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.periodStats.sessionCount)
        assertEquals(0, viewModel.uiState.value.trainedDayCount)
        assertEquals(7, viewModel.uiState.value.activityWeekDays.size)
        assertEquals(1, viewModel.uiState.value.activityWeekDays.count { it.trained })
        assertTrue(
            viewModel.uiState.value.activityWeekDays.any {
                it.trained && it.dayStartMillis == local("2026-08-25T00:00:00")
            },
        )
        assertTrue(viewModel.uiState.value.records.isEmpty())
    }

    @Test
    fun records_collapseSameExerciseAndKeepZeroWeight() = runTest(testDispatcher) {
        val first = item(sessionId = 1L, occurredAt = local("2026-08-26T10:00:00"), volume = 10.0)
        val second = item(sessionId = 2L, occurredAt = local("2026-08-25T10:00:00"), volume = 20.0)
        val viewModel = createViewModel()
        sessionRepository.emitSessions(listOf(first, second))
        sessionRepository.emitSets(
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
        assertEquals(7, viewModel.uiState.value.activityWeekDays.size)
    }

    @Test
    fun reemittingSets_updatesRecords() = runTest(testDispatcher) {
        val session = item(sessionId = 1L, occurredAt = local("2026-08-26T10:00:00"), volume = 10.0)
        val viewModel = createViewModel()
        sessionRepository.emitSessions(listOf(session))
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.records.isEmpty())

        sessionRepository.emitSets(
            listOf(CompletedSetRecord(1L, local("2026-08-26T10:00:00"), "Supino", 8, 80.0)),
        )
        advanceUntilIdle()
        assertEquals(listOf("Supino"), viewModel.uiState.value.records.map { it.exerciseName })
    }

    @Test
    fun reemittingSessions_updatesFrequencyAndTrend() = runTest(testDispatcher) {
        val first = item(sessionId = 1L, occurredAt = local("2026-08-26T10:00:00"), volume = 10.0)
        val viewModel = createViewModel()
        sessionRepository.emitSessions(listOf(first))
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.trainedDayCount)

        val second = item(sessionId = 2L, occurredAt = local("2026-08-25T10:00:00"), volume = 20.0)
        sessionRepository.emitSessions(listOf(first, second))
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.trainedDayCount)
        assertEquals(7, viewModel.uiState.value.activityWeekDays.size)
    }

    @Test
    fun activityWeek_multipleSessionsSameDay_countAsOneTrainedDay() = runTest(testDispatcher) {
        val morning = item(sessionId = 1L, occurredAt = local("2026-08-26T08:00:00"), volume = 10.0)
        val evening = item(sessionId = 2L, occurredAt = local("2026-08-26T18:00:00"), volume = 20.0)
        val viewModel = createViewModel()
        sessionRepository.emitSessions(listOf(morning, evening))
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()

        assertEquals(7, viewModel.uiState.value.activityWeekDays.size)
        assertEquals(1, viewModel.uiState.value.activityWeekDays.count { it.trained })
        assertTrue(viewModel.uiState.value.activityWeekDays.single { it.isToday }.trained)
    }

    @Test
    fun activityWeek_onlyCompletedHistoryIsObserved_inProgressNeverAppears() = runTest(testDispatcher) {
        // observeCompletedSessions never emits IN_PROGRESS rows; in-progress is a separate flow.
        val completed = item(sessionId = 1L, occurredAt = local("2026-08-25T10:00:00"), volume = 10.0)
        val viewModel = createViewModel()
        sessionRepository.emitSessions(listOf(completed))
        sessionRepository.emitSets(emptyList())
        sessionRepository.emitInProgress(inProgressSession(id = 99L, name = "Ativo"))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.activityWeekDays.count { it.trained })
        assertFalse(viewModel.uiState.value.activityWeekDays.single { it.isToday }.trained)
        assertEquals(99L, viewModel.uiState.value.inProgressSession?.id)
    }

    @Test
    fun emptySessions_stillExposeSevenActivityDays() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        sessionRepository.emitSessions(emptyList())
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showEmpty)
        assertEquals(7, viewModel.uiState.value.activityWeekDays.size)
        assertTrue(viewModel.uiState.value.activityWeekDays.none { it.trained })
        assertEquals(1, viewModel.uiState.value.activityWeekDays.count { it.isToday })
        assertEquals(0, viewModel.uiState.value.trainedDayCount)
        assertTrue(viewModel.uiState.value.records.isEmpty())
    }

    @Test
    fun whenObserveThrows_errorIsSet() = runTest(testDispatcher) {
        sessionRepository.shouldThrowOnObserve = true
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Simulated home error", viewModel.uiState.value.error)
    }

    @Test
    fun clearError_clearsMessage() = runTest(testDispatcher) {
        sessionRepository.shouldThrowOnObserve = true
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun startSession_isNotCalledWhenObservingDashboard() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        sessionRepository.emitSessions(emptyList())
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()

        assertEquals(0, sessionRepository.startSessionCalls)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.inProgressSession)
    }

    @Test
    fun inProgressSession_isExposedWhenObserved() = runTest(testDispatcher) {
        val active = inProgressSession(id = 77L, name = "Push Day")
        val viewModel = createViewModel()
        sessionRepository.emitSessions(emptyList())
        sessionRepository.emitSets(emptyList())
        sessionRepository.emitInProgress(active)
        advanceUntilIdle()

        assertEquals(77L, viewModel.uiState.value.inProgressSession?.id)
        assertEquals("Push Day", viewModel.uiState.value.inProgressSession?.workoutName)
        assertTrue(viewModel.uiState.value.showEmpty)
        assertEquals(0, sessionRepository.startSessionCalls)
    }

    @Test
    fun inProgressSession_clearsWhenFlowEmitsNull() = runTest(testDispatcher) {
        val active = inProgressSession(id = 77L, name = "Push Day")
        val viewModel = createViewModel()
        sessionRepository.emitSessions(emptyList())
        sessionRepository.emitSets(emptyList())
        sessionRepository.emitInProgress(active)
        advanceUntilIdle()
        assertEquals(77L, viewModel.uiState.value.inProgressSession?.id)

        sessionRepository.emitInProgress(null)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.inProgressSession)
        assertEquals(0, sessionRepository.startSessionCalls)
    }

    @Test
    fun initialSelectedPeriod_isWeek() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        sessionRepository.emitSessions(
            listOf(item(sessionId = 1L, occurredAt = local("2026-08-26T10:00:00"), volume = 10.0)),
        )
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()
        assertEquals(DashboardPeriod.WEEK, viewModel.uiState.value.selectedPeriod)
        assertEquals(1, viewModel.uiState.value.periodStats.sessionCount)
    }

    @Test
    fun selectMonth_includesSessionOutsideWeekButInMonth() = runTest(testDispatcher) {
        val inWeek = item(sessionId = 1L, occurredAt = local("2026-08-26T10:00:00"), volume = 10.0)
        val inMonth = item(sessionId = 2L, occurredAt = local("2026-08-10T10:00:00"), volume = 20.0)
        val viewModel = createViewModel()
        sessionRepository.emitSessions(listOf(inWeek, inMonth))
        sessionRepository.emitSets(
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
        assertEquals(7, viewModel.uiState.value.activityWeekDays.size)
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
        val viewModel = createViewModel()
        sessionRepository.emitSessions(listOf(inWeek, outsideMonth))
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.periodStats.sessionCount)

        viewModel.selectPeriod(DashboardPeriod.ALL)
        advanceUntilIdle()

        assertEquals(DashboardPeriod.ALL, viewModel.uiState.value.selectedPeriod)
        assertEquals(2, viewModel.uiState.value.periodStats.sessionCount)
        assertEquals(50.0, viewModel.uiState.value.periodStats.volume, 0.001)
        assertEquals(2, viewModel.uiState.value.trainedDayCount)
        assertEquals(1L, viewModel.uiState.value.recentWorkout?.sessionId)
        assertEquals(7, viewModel.uiState.value.activityWeekDays.size)
    }

    @Test
    fun emptyMonth_withHistory_doesNotShowEmpty() = runTest(testDispatcher) {
        clock.now = local("2026-09-15T12:00:00")
        val augustOnly = item(sessionId = 1L, occurredAt = local("2026-08-10T10:00:00"), volume = 80.0)
        val viewModel = createViewModel()
        sessionRepository.emitSessions(listOf(augustOnly))
        sessionRepository.emitSets(
            listOf(CompletedSetRecord(1L, local("2026-08-10T10:00:00"), "Supino", 8, 80.0)),
        )
        advanceUntilIdle()
        viewModel.selectPeriod(DashboardPeriod.MONTH)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showEmpty)
        assertEquals(1L, viewModel.uiState.value.recentWorkout?.sessionId)
        assertEquals(0, viewModel.uiState.value.periodStats.sessionCount)
        assertEquals(0, viewModel.uiState.value.trainedDayCount)
        assertEquals(7, viewModel.uiState.value.activityWeekDays.size)
        assertEquals(listOf("Supino"), viewModel.uiState.value.records.map { it.exerciseName })
    }

    @Test
    fun nextWorkout_noWorkouts_isNull() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        sessionRepository.emitSessions(emptyList())
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.nextWorkout)
    }

    @Test
    fun nextWorkout_singleWorkoutWithoutHistory_returnsThatWorkout() = runTest(testDispatcher) {
        val workoutA = Workout(id = 1, name = "A")
        workoutRepository.emit(listOf(workoutA))
        val viewModel = createViewModel()
        sessionRepository.emitSessions(emptyList())
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()
        assertEquals(workoutA, viewModel.uiState.value.nextWorkout)
    }

    @Test
    fun nextWorkout_multipleWithoutHistory_returnsFirstByIdAsc() = runTest(testDispatcher) {
        val workoutA = Workout(id = 1, name = "A")
        val workoutB = Workout(id = 2, name = "B")
        val workoutC = Workout(id = 3, name = "C")
        workoutRepository.emit(listOf(workoutC, workoutA, workoutB))
        val viewModel = createViewModel()
        sessionRepository.emitSessions(emptyList())
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()
        assertEquals(workoutA, viewModel.uiState.value.nextWorkout)
    }

    @Test
    fun nextWorkout_lastCompletedIsA_returnsB() = runTest(testDispatcher) {
        val workoutA = Workout(id = 1, name = "A")
        val workoutB = Workout(id = 2, name = "B")
        val workoutC = Workout(id = 3, name = "C")
        workoutRepository.emit(listOf(workoutA, workoutB, workoutC))
        val viewModel = createViewModel()
        sessionRepository.emitSessions(
            listOf(item(sessionId = 10, workoutId = 1, occurredAt = local("2026-08-26T10:00:00"))),
        )
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()
        assertEquals(workoutB, viewModel.uiState.value.nextWorkout)
    }

    @Test
    fun nextWorkout_lastCompletedIsC_rotatesToA() = runTest(testDispatcher) {
        val workoutA = Workout(id = 1, name = "A")
        val workoutB = Workout(id = 2, name = "B")
        val workoutC = Workout(id = 3, name = "C")
        workoutRepository.emit(listOf(workoutA, workoutB, workoutC))
        val viewModel = createViewModel()
        sessionRepository.emitSessions(
            listOf(
                item(sessionId = 30, workoutId = 3, occurredAt = local("2026-08-26T12:00:00")),
                item(sessionId = 20, workoutId = 2, occurredAt = local("2026-08-26T11:00:00")),
                item(sessionId = 10, workoutId = 1, occurredAt = local("2026-08-26T10:00:00")),
            ),
        )
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()
        assertEquals(workoutA, viewModel.uiState.value.nextWorkout)
    }

    @Test
    fun nextWorkout_neverCompletedPreferred() = runTest(testDispatcher) {
        val workoutA = Workout(id = 1, name = "A")
        val workoutB = Workout(id = 2, name = "B")
        val workoutC = Workout(id = 3, name = "C")
        workoutRepository.emit(listOf(workoutA, workoutB, workoutC))
        val viewModel = createViewModel()
        sessionRepository.emitSessions(
            listOf(
                item(sessionId = 20, workoutId = 2, occurredAt = local("2026-08-26T11:00:00")),
                item(sessionId = 10, workoutId = 1, occurredAt = local("2026-08-26T10:00:00")),
            ),
        )
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()
        assertEquals(workoutC, viewModel.uiState.value.nextWorkout)
    }

    @Test
    fun nextWorkout_inProgressDoesNotChangeRecommendation() = runTest(testDispatcher) {
        val workoutA = Workout(id = 1, name = "A")
        val workoutB = Workout(id = 2, name = "B")
        workoutRepository.emit(listOf(workoutA, workoutB))
        val viewModel = createViewModel()
        sessionRepository.emitSessions(
            listOf(item(sessionId = 10, workoutId = 1, occurredAt = local("2026-08-26T10:00:00"))),
        )
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()
        assertEquals(workoutB, viewModel.uiState.value.nextWorkout)

        sessionRepository.emitInProgress(inProgressSession(id = 99, name = "A"))
        advanceUntilIdle()
        assertEquals(workoutB, viewModel.uiState.value.nextWorkout)
        assertEquals(99L, viewModel.uiState.value.inProgressSession?.id)
    }

    @Test
    fun nextWorkout_nullOrMissingWorkoutIdDoesNotBreak() = runTest(testDispatcher) {
        val workoutA = Workout(id = 1, name = "A")
        val workoutB = Workout(id = 2, name = "B")
        workoutRepository.emit(listOf(workoutA, workoutB))
        val viewModel = createViewModel()
        sessionRepository.emitSessions(
            listOf(
                item(sessionId = 30, workoutId = null, occurredAt = local("2026-08-26T12:00:00")),
                item(sessionId = 20, workoutId = 99, occurredAt = local("2026-08-26T11:00:00")),
            ),
        )
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()
        assertEquals(workoutA, viewModel.uiState.value.nextWorkout)
    }

    @Test
    fun nextWorkout_preservesExistingDashboardFields() = runTest(testDispatcher) {
        val workoutA = Workout(id = 1, name = "A")
        workoutRepository.emit(listOf(workoutA))
        val recent = item(
            sessionId = 20L,
            workoutId = 1L,
            name = "A",
            occurredAt = local("2026-08-26T10:00:00"),
            volume = 200.0,
        )
        val viewModel = createViewModel()
        sessionRepository.emitSessions(listOf(recent))
        sessionRepository.emitSets(
            listOf(CompletedSetRecord(20L, local("2026-08-26T10:00:00"), "Supino", 8, 60.0)),
        )
        advanceUntilIdle()

        assertEquals(workoutA, viewModel.uiState.value.nextWorkout)
        assertEquals(20L, viewModel.uiState.value.recentWorkout?.sessionId)
        assertEquals(1, viewModel.uiState.value.periodStats.sessionCount)
        assertEquals(listOf("Supino"), viewModel.uiState.value.records.map { it.exerciseName })
        assertEquals(7, viewModel.uiState.value.activityWeekDays.size)
    }

    private fun local(dateTime: String): Long =
        LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()

    private fun item(
        sessionId: Long,
        name: String = "Treino",
        occurredAt: Long,
        volume: Double = 100.0,
        workoutId: Long? = null,
    ) = WorkoutHistoryItem(
        sessionId = sessionId,
        workoutId = workoutId,
        workoutName = name,
        startedAtMillis = occurredAt,
        endedAtMillis = occurredAt,
        durationMillis = 60_000L,
        volume = volume,
        exerciseCount = 1,
        completedSetCount = 1,
        plannedSetCount = 1,
    )

    private fun inProgressSession(id: Long, name: String) = WorkoutSession(
        id = id,
        workoutId = 1L,
        workoutName = name,
        startedAtMillis = local("2026-08-26T10:00:00"),
        status = com.gymtrack.domain.model.WorkoutSessionStatus.IN_PROGRESS,
    )
}

private class FakeTimeProvider(var now: Long) : TimeProvider {
    var lastReadNow: Long? = null
    override fun nowMillis(): Long {
        lastReadNow = now
        return now
    }
}

private class FakeHomeWorkoutRepository : WorkoutRepository {
    private val workouts = MutableStateFlow<List<Workout>>(emptyList())
    private val executableIds = MutableStateFlow<Set<Long>>(emptySet())

    fun emit(list: List<Workout>, withExercises: Set<Long> = list.map { it.id }.toSet()) {
        executableIds.value = withExercises
        workouts.value = list
    }

    override fun getAll(): Flow<List<Workout>> = workouts
    override fun getById(id: Long): Flow<Workout?> =
        flowOf(workouts.value.find { it.id == id })
    override fun observeWorkoutIdsWithExercises(): Flow<Set<Long>> = executableIds
    override suspend fun save(workout: Workout): Long = workout.id
    override suspend fun update(workout: Workout) = Unit
    override suspend fun delete(workout: Workout) = Unit
    override fun getBlocks(workoutId: Long) = emptyFlow<List<WorkoutBlock>>()
    override fun getExercisesForWorkout(workoutId: Long) = emptyFlow<List<WorkoutExercise>>()
    override suspend fun addBlock(block: WorkoutBlock, exercises: List<WorkoutExercise>): Long = 0L
    override suspend fun duplicateBlock(blockId: Long): Long = 0L
    override suspend fun updateBlock(block: WorkoutBlock) = Unit
    override suspend fun updateBlockExercise(exercise: WorkoutExercise) = Unit
    override suspend fun replaceBlockExercise(exerciseRowId: Long, newCatalogueExerciseId: Long) = Unit
    override suspend fun removeBlock(blockId: Long) = Unit
    override suspend fun updateBlockPositions(positions: Map<Long, Int>) = Unit
    override suspend fun updateWorkoutPositions(positions: Map<Long, Int>) = Unit
    override suspend fun addExercise(workoutExercise: WorkoutExercise): Long = 0L
    override suspend fun updateExercise(workoutExercise: WorkoutExercise) = Unit
    override suspend fun removeExercise(workoutExercise: WorkoutExercise) = Unit
    override suspend fun removeExerciseById(id: Long) = Unit
    override suspend fun updateExercisePositions(positions: Map<Long, Int>) = Unit
}

private class FakeHomeSessionRepository : WorkoutSessionRepository {

    private val sessions = MutableSharedFlow<List<WorkoutHistoryItem>>(extraBufferCapacity = 1)
    private val sets = MutableSharedFlow<List<CompletedSetRecord>>(extraBufferCapacity = 1)
    private val inProgress = MutableStateFlow<WorkoutSession?>(null)
    var shouldThrowOnObserve = false
    var startSessionCalls = 0

    suspend fun emitSessions(items: List<WorkoutHistoryItem>) {
        sessions.emit(items)
    }

    suspend fun emitSets(items: List<CompletedSetRecord>) {
        sets.emit(items)
    }

    fun emitInProgress(session: WorkoutSession?) {
        inProgress.value = session
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
    override fun observeInProgress(): Flow<WorkoutSession?> = inProgress
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

private class FakeHomeIdentityRepository : GoogleIdentityRepository {
    private val state = MutableStateFlow<GoogleUser?>(null)
    val user = state
    override val currentUser: StateFlow<GoogleUser?> = state.asStateFlow()
    override suspend fun tryRestoreSilentSignIn(hostContext: Context) = Unit
    override suspend fun signIn(hostContext: Context) = GoogleSignInResult.Cancelled
    override suspend fun signOut(hostContext: Context) {
        state.value = null
    }
}

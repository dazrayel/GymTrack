package com.gymtrack.presentation.dashboard

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.gymtrack.domain.identity.GoogleSignInResult
import com.gymtrack.domain.model.CompletedSetRecord
import com.gymtrack.domain.model.GoogleUser
import com.gymtrack.domain.model.MetricChange
import com.gymtrack.domain.model.StartSessionResult
import com.gymtrack.domain.model.WeeklyWorkoutPlan
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutBlock
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSet
import com.gymtrack.domain.repository.GoogleIdentityRepository
import com.gymtrack.domain.repository.WeeklyPlanRepository
import com.gymtrack.domain.repository.WorkoutRepository
import com.gymtrack.domain.repository.WorkoutSessionRepository
import com.gymtrack.domain.time.TimeProvider
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class DashboardViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val zone: ZoneId = ZoneId.systemDefault()
    private lateinit var workoutRepository: FakeDashboardWorkoutRepository
    private lateinit var sessionRepository: FakeDashboardSessionRepository
    private lateinit var weeklyPlanRepository: FakeWeeklyPlanRepository
    private lateinit var identityRepository: FakeDashboardIdentityRepository
    private lateinit var clock: FakeTimeProvider

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        workoutRepository = FakeDashboardWorkoutRepository()
        sessionRepository = FakeDashboardSessionRepository()
        weeklyPlanRepository = FakeWeeklyPlanRepository()
        identityRepository = FakeDashboardIdentityRepository()
        clock = FakeTimeProvider(now = local("2026-08-26T12:00:00"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = DashboardViewModel(
        workoutRepository,
        sessionRepository,
        weeklyPlanRepository,
        identityRepository,
        clock,
    )

    @Test
    fun initialState_isLoading() {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun emptyHistory_showsEmptyStateWithoutZeroMetricNoise() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        sessionRepository.emitSessions(emptyList())
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
        assertEquals(0, state.weeklySessionCount)
        assertNull(state.sessionChange)
    }

    @Test
    fun signedInUser_exposesDisplayName() = runTest(testDispatcher) {
        identityRepository.user.value = GoogleUser("Danilo")
        val viewModel = createViewModel()
        sessionRepository.emitSessions(emptyList())
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()

        assertEquals("Danilo", viewModel.uiState.value.displayName)
    }

    @Test
    fun signedOutUser_hasNullDisplayName() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        sessionRepository.emitSessions(emptyList())
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.displayName)
    }

    @Test
    fun withCompletedSessions_populatesWeeklySummary() = runTest(testDispatcher) {
        workoutRepository.emitWorkouts(listOf(Workout(id = 1L, name = "Treino A")))
        workoutRepository.emitExecutableIds(setOf(1L))
        val viewModel = createViewModel()
        sessionRepository.emitSessions(
            listOf(
                historyItem(
                    sessionId = 1L,
                    workoutId = 1L,
                    occurredAt = local("2026-08-25T10:00:00"),
                    volume = 400.0,
                    completedSetCount = 8,
                ),
            ),
        )
        sessionRepository.emitSets(
            listOf(
                CompletedSetRecord(
                    sessionId = 1L,
                    occurredAtMillis = local("2026-08-25T10:00:00"),
                    exerciseName = "Supino",
                    reps = 8,
                    weight = 50.0,
                ),
            ),
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isEmpty)
        assertEquals(1, state.weeklySessionCount)
        assertEquals(8, state.weeklySetCount)
        assertEquals(400.0, state.weeklyVolume, 0.001)
        assertFalse(state.hasPreviousWeekData)
        assertEquals("Treino A", state.nextWorkout?.name)
        assertEquals("Supino", state.highlightRecord?.exerciseName)
        assertTrue(state.unlockedAchievementCount >= 1)
    }

    @Test
    fun comparison_withPreviousWeek_exposesDeltas() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        sessionRepository.emitSessions(
            listOf(
                historyItem(
                    sessionId = 1L,
                    occurredAt = local("2026-08-25T10:00:00"),
                    volume = 220.0,
                    completedSetCount = 10,
                ),
                historyItem(
                    sessionId = 2L,
                    occurredAt = local("2026-08-18T10:00:00"),
                    volume = 200.0,
                    completedSetCount = 8,
                ),
            ),
        )
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasPreviousWeekData)
        assertEquals(MetricChange.Unchanged, state.sessionChange)
        assertEquals(MetricChange.Percent(25), state.setChange)
        assertEquals(MetricChange.Percent(10), state.volumeChange)
    }

    @Test
    fun weeklyPlan_exposesTodayAndTomorrowNames() = runTest(testDispatcher) {
        val today = Instant.ofEpochMilli(clock.nowMillis()).atZone(zone).toLocalDate().dayOfWeek
        weeklyPlanRepository.plans.value = listOf(
            WeeklyWorkoutPlan(dayOfWeek = today, workoutId = 1L, workoutName = "Treino A"),
            WeeklyWorkoutPlan(dayOfWeek = today.plus(1), workoutId = 2L, workoutName = "Treino B"),
        )
        val viewModel = createViewModel()
        sessionRepository.emitSessions(emptyList())
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasWeeklyPlan)
        assertEquals("Treino A", state.todayPlanWorkoutName)
        assertEquals("Treino B", state.tomorrowPlanWorkoutName)
    }

    @Test
    fun error_setsErrorMessage() = runTest(testDispatcher) {
        sessionRepository.failSessions = true
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("boom", viewModel.uiState.value.error)
    }

    private fun local(dateTime: String): Long =
        LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()

    private fun historyItem(
        sessionId: Long,
        workoutId: Long? = null,
        occurredAt: Long,
        volume: Double,
        completedSetCount: Int,
    ) = WorkoutHistoryItem(
        sessionId = sessionId,
        workoutId = workoutId,
        workoutName = "Treino",
        startedAtMillis = occurredAt,
        endedAtMillis = occurredAt,
        durationMillis = 1_000L,
        volume = volume,
        exerciseCount = 1,
        completedSetCount = completedSetCount,
        plannedSetCount = completedSetCount,
    )
}

private class FakeTimeProvider(private var now: Long) : TimeProvider {
    override fun nowMillis(): Long = now
}

private class FakeDashboardIdentityRepository : GoogleIdentityRepository {
    val user = MutableStateFlow<GoogleUser?>(null)
    override val currentUser: StateFlow<GoogleUser?> = user.asStateFlow()
    override suspend fun tryRestoreSilentSignIn(hostContext: Context) = Unit
    override suspend fun signIn(hostContext: Context) = GoogleSignInResult.Cancelled
    override suspend fun signOut(hostContext: Context) {
        user.value = null
    }
}

private class FakeWeeklyPlanRepository : WeeklyPlanRepository {
    val plans = MutableStateFlow<List<WeeklyWorkoutPlan>>(emptyList())
    override fun observePlans(): Flow<List<WeeklyWorkoutPlan>> = plans.asStateFlow()
    override suspend fun assignWorkout(dayOfWeek: DayOfWeek, workoutId: Long) = Unit
    override suspend fun clearDay(dayOfWeek: DayOfWeek) = Unit
}

private class FakeDashboardWorkoutRepository : WorkoutRepository {
    private val workouts = MutableStateFlow<List<Workout>>(emptyList())
    private val executable = MutableStateFlow<Set<Long>>(emptySet())

    fun emitWorkouts(value: List<Workout>) {
        workouts.value = value
    }

    fun emitExecutableIds(value: Set<Long>) {
        executable.value = value
    }

    override fun getAll(): Flow<List<Workout>> = workouts.asStateFlow()
    override fun getById(id: Long): Flow<Workout?> = flowOf(workouts.value.find { it.id == id })
    override fun observeWorkoutIdsWithExercises(): Flow<Set<Long>> = executable.asStateFlow()
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
    override suspend fun addExercise(workoutExercise: WorkoutExercise): Long = 0L
    override suspend fun updateExercise(workoutExercise: WorkoutExercise) = Unit
    override suspend fun removeExercise(workoutExercise: WorkoutExercise) = Unit
    override suspend fun removeExerciseById(id: Long) = Unit
    override suspend fun updateExercisePositions(positions: Map<Long, Int>) = Unit
}

private class FakeDashboardSessionRepository : WorkoutSessionRepository {
    private val sessions = MutableSharedFlow<List<WorkoutHistoryItem>>(extraBufferCapacity = 1)
    private val sets = MutableSharedFlow<List<CompletedSetRecord>>(extraBufferCapacity = 1)
    var failSessions = false

    suspend fun emitSessions(value: List<WorkoutHistoryItem>) {
        sessions.emit(value)
    }

    suspend fun emitSets(value: List<CompletedSetRecord>) {
        sets.emit(value)
    }

    override fun observeCompletedSessions(): Flow<List<WorkoutHistoryItem>> =
        if (failSessions) flow { error("boom") } else sessions

    override fun observeCompletedSetHistory(): Flow<List<CompletedSetRecord>> = sets
    override suspend fun startSession(workoutId: Long): StartSessionResult =
        StartSessionResult.Created(0L)
    override suspend fun getSession(id: Long): WorkoutSession? = null
    override fun observeSession(id: Long): Flow<WorkoutSession?> = flowOf(null)
    override fun observeInProgress(): Flow<WorkoutSession?> = flowOf(null)
    override fun observeSessionExercises(sessionId: Long): Flow<List<WorkoutSessionExercise>> = emptyFlow()
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

package com.gymtrack.presentation.achievements

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.gymtrack.domain.model.AchievementCatalog
import com.gymtrack.domain.model.CompletedSetRecord
import com.gymtrack.domain.model.StartSessionResult
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSet
import com.gymtrack.domain.repository.WorkoutSessionRepository
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
class AchievementViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val zone: ZoneId = ZoneId.systemDefault()
    private lateinit var sessionRepository: FakeAchievementSessionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        sessionRepository = FakeAchievementSessionRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AchievementViewModel(sessionRepository)

    @Test
    fun initialState_isLoadingTrue_beforeFirstEmission() {
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.achievements.isEmpty())
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun emptyHistory_allAchievementsLocked() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        sessionRepository.emitSessions(emptyList())
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(AchievementCatalog.size, viewModel.uiState.value.achievements.size)
        assertEquals(
            AchievementCatalog.map { it.id },
            viewModel.uiState.value.achievements.map { it.achievementId },
        )
        assertTrue(viewModel.uiState.value.achievements.all { !it.unlocked })
        assertTrue(viewModel.uiState.value.achievements.all { it.current == 0L })
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun oneSession_unlocksFirstWorkout() = runTest(testDispatcher) {
        val sessions = listOf(session(sessionId = 1L, volume = 100.0))
        val sets = listOf(set(sessionId = 1L, name = "Supino"))
        val viewModel = createViewModel()
        sessionRepository.emitSessions(sessions)
        sessionRepository.emitSets(sets)
        advanceUntilIdle()

        assertUnlocked(viewModel, "FIRST_WORKOUT")
        assertLocked(viewModel, "WORKOUTS_10")
    }

    @Test
    fun tenSessions_unlocksWorkouts10() = runTest(testDispatcher) {
        val sessions = (1..10).map { session(sessionId = it.toLong()) }
        val sets = sessions.map { set(sessionId = it.sessionId) }
        val viewModel = createViewModel()
        sessionRepository.emitSessions(sessions)
        sessionRepository.emitSets(sets)
        advanceUntilIdle()

        assertUnlocked(viewModel, "WORKOUTS_10")
        assertLocked(viewModel, "WORKOUTS_25")
    }

    @Test
    fun sufficientVolume_unlocksVolumeAchievement() = runTest(testDispatcher) {
        val sessions = listOf(session(sessionId = 1L, volume = 10_000.0))
        val sets = listOf(set(sessionId = 1L, reps = 10, weight = 1_000.0))
        val viewModel = createViewModel()
        sessionRepository.emitSessions(sessions)
        sessionRepository.emitSets(sets)
        advanceUntilIdle()

        assertUnlocked(viewModel, "VOLUME_10000")
        assertLocked(viewModel, "VOLUME_50000")
    }

    @Test
    fun sufficientSets_unlocksSetsAchievement() = runTest(testDispatcher) {
        val sessions = listOf(session(sessionId = 1L))
        val sets = (1..100).map { set(sessionId = 1L, setIndex = it) }
        val viewModel = createViewModel()
        sessionRepository.emitSessions(sessions)
        sessionRepository.emitSets(sets)
        advanceUntilIdle()

        assertUnlocked(viewModel, "SETS_100")
        assertLocked(viewModel, "SETS_500")
    }

    @Test
    fun distinctExercises_unlocksDiversityAchievement() = runTest(testDispatcher) {
        val sessions = listOf(session(sessionId = 1L))
        val sets = (1..5).map { set(sessionId = 1L, setIndex = it, name = "Exercício $it") }
        val viewModel = createViewModel()
        sessionRepository.emitSessions(sessions)
        sessionRepository.emitSets(sets)
        advanceUntilIdle()

        assertUnlocked(viewModel, "EXERCISES_5")
        assertLocked(viewModel, "EXERCISES_10")
    }

    @Test
    fun personalRecords_unlockPrAchievements() = runTest(testDispatcher) {
        val sessions = listOf(session(sessionId = 1L))
        val five = (1..5).map { set(sessionId = 1L, setIndex = it, name = "Exercício $it") }
        val viewModel = createViewModel()
        sessionRepository.emitSessions(sessions)
        sessionRepository.emitSets(five)
        advanceUntilIdle()

        assertUnlocked(viewModel, "PR_FIRST")
        assertUnlocked(viewModel, "PR_EXERCISES_5")
        assertLocked(viewModel, "PR_EXERCISES_10")
    }

    @Test
    fun trainedDays_unlockTrainedDaysAchievement() = runTest(testDispatcher) {
        val sessions = (0 until 5).map { offset ->
            session(
                sessionId = offset + 1L,
                occurredAt = local("2026-08-0${offset + 1}T10:00:00"),
            )
        }
        val sets = sessions.map { set(sessionId = it.sessionId, occurredAt = it.startedAtMillis) }
        val viewModel = createViewModel()
        sessionRepository.emitSessions(sessions)
        sessionRepository.emitSets(sets)
        advanceUntilIdle()

        assertUnlocked(viewModel, "TRAINED_DAYS_5")
        assertLocked(viewModel, "TRAINED_DAYS_20")
    }

    @Test
    fun newEmission_recalculatesState() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        sessionRepository.emitSessions(emptyList())
        sessionRepository.emitSets(emptyList())
        advanceUntilIdle()
        assertLocked(viewModel, "FIRST_WORKOUT")

        val sessions = listOf(session(sessionId = 1L))
        val sets = listOf(set(sessionId = 1L))
        sessionRepository.emitSessions(sessions)
        sessionRepository.emitSets(sets)
        advanceUntilIdle()

        assertUnlocked(viewModel, "FIRST_WORKOUT")
        assertEquals(AchievementCatalog.size, viewModel.uiState.value.achievements.size)
    }

    @Test
    fun combineUsesSessionsAndSetsTogether() = runTest(testDispatcher) {
        // Sessions alone would unlock FIRST_WORKOUT but not EXERCISES_5 / SETS_100.
        // Sets alone would unlock diversity/sets but volume/session milestones use sessions.
        val sessions = listOf(session(sessionId = 1L, volume = 10_000.0))
        val sets = (1..5).map { set(sessionId = 1L, setIndex = it, name = "Exercício $it") }
        val viewModel = createViewModel()
        sessionRepository.emitSessions(sessions)
        sessionRepository.emitSets(sets)
        advanceUntilIdle()

        assertUnlocked(viewModel, "FIRST_WORKOUT")
        assertUnlocked(viewModel, "VOLUME_10000")
        assertUnlocked(viewModel, "EXERCISES_5")
        assertUnlocked(viewModel, "PR_EXERCISES_5")
        assertEquals(5L, status(viewModel, "SETS_100").current)
        assertFalse(status(viewModel, "SETS_100").unlocked)
    }

    @Test
    fun observeError_exposesErrorAndStopsLoading() = runTest(testDispatcher) {
        sessionRepository.shouldThrowOnObserve = true
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Simulated achievement error", viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.achievements.isEmpty())
    }

    @Test
    fun clearError_clearsErrorMessage() = runTest(testDispatcher) {
        sessionRepository.shouldThrowOnObserve = true
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals("Simulated achievement error", viewModel.uiState.value.error)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    private fun assertUnlocked(viewModel: AchievementViewModel, id: String) {
        val status = status(viewModel, id)
        assertTrue(id, status.unlocked)
        assertEquals(id, status.target, status.current)
    }

    private fun assertLocked(viewModel: AchievementViewModel, id: String) {
        assertFalse(id, status(viewModel, id).unlocked)
    }

    private fun status(viewModel: AchievementViewModel, id: String) =
        viewModel.uiState.value.achievements.single { it.achievementId == id }

    private fun session(
        sessionId: Long,
        occurredAt: Long = sessionId * 86_400_000L,
        volume: Double = 100.0,
    ) = WorkoutHistoryItem(
        sessionId = sessionId,
        workoutName = "Treino",
        startedAtMillis = occurredAt,
        endedAtMillis = occurredAt,
        durationMillis = 1_000L,
        volume = volume,
        exerciseCount = 1,
        completedSetCount = 1,
        plannedSetCount = 1,
    )

    private fun set(
        sessionId: Long,
        setIndex: Int = 1,
        name: String = "Supino",
        reps: Int = 8,
        weight: Double = 10.0,
        occurredAt: Long = sessionId * 86_400_000L,
    ) = CompletedSetRecord(
        sessionId = sessionId,
        occurredAtMillis = occurredAt,
        exerciseName = name,
        reps = reps,
        weight = weight,
    )

    private fun local(dateTime: String): Long =
        LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()
}

private class FakeAchievementSessionRepository : WorkoutSessionRepository {

    private val sessions = MutableSharedFlow<List<WorkoutHistoryItem>>(extraBufferCapacity = 1)
    private val sets = MutableSharedFlow<List<CompletedSetRecord>>(extraBufferCapacity = 1)
    var shouldThrowOnObserve = false

    suspend fun emitSessions(items: List<WorkoutHistoryItem>) {
        sessions.emit(items)
    }

    suspend fun emitSets(items: List<CompletedSetRecord>) {
        sets.emit(items)
    }

    override fun observeCompletedSessions(): Flow<List<WorkoutHistoryItem>> {
        if (shouldThrowOnObserve) {
            return flow { throw RuntimeException("Simulated achievement error") }
        }
        return sessions
    }

    override fun observeCompletedSetHistory(): Flow<List<CompletedSetRecord>> = sets

    override suspend fun startSession(workoutId: Long): StartSessionResult =
        StartSessionResult.Created(0L)

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

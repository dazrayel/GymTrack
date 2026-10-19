package com.gymtrack.presentation.rpg

import com.gymtrack.domain.model.CompletedSetRecord
import com.gymtrack.domain.model.StartSessionResult
import com.gymtrack.domain.model.TitleIds
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSet
import com.gymtrack.domain.repository.FakeActiveTitleRepository
import com.gymtrack.domain.repository.FakeGamificationRepository
import com.gymtrack.domain.repository.FakeMissionRepository
import com.gymtrack.domain.repository.WorkoutSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RpgProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var gamification: FakeGamificationRepository
    private lateinit var sessions: FakeRpgSessionRepository
    private lateinit var missions: FakeMissionRepository
    private lateinit var activeTitle: FakeActiveTitleRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        gamification = FakeGamificationRepository(initialTotalXp = 0L)
        sessions = FakeRpgSessionRepository()
        missions = FakeMissionRepository()
        activeTitle = FakeActiveTitleRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = RpgProfileViewModel(
        gamificationRepository = gamification,
        sessionRepository = sessions,
        missionRepository = missions,
        activeTitleRepository = activeTitle,
    )

    @Test
    fun emptyHistory_loadsWithNoTitlesUnlocked() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(10, viewModel.uiState.value.titles.size)
        assertTrue(viewModel.uiState.value.titles.none { it.unlocked })
        assertNull(viewModel.uiState.value.activeTitleId)
        assertEquals(0, viewModel.uiState.value.completedWorkoutCount)
    }

    @Test
    fun firstWorkout_unlocksBeginnerAndAllowsSelection() = runTest(testDispatcher) {
        sessions.setHistory(
            listOf(session(1L)),
            listOf(set(1L)),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(
            viewModel.uiState.value.titles.single { it.definition.id == TitleIds.BEGINNER }.unlocked,
        )
        viewModel.selectTitle(TitleIds.BEGINNER)
        advanceUntilIdle()
        assertEquals(TitleIds.BEGINNER, viewModel.uiState.value.activeTitleId)
        assertEquals(listOf(TitleIds.BEGINNER), activeTitle.setCalls)
    }

    @Test
    fun clearActiveTitle_persistsNull() = runTest(testDispatcher) {
        sessions.setHistory(listOf(session(1L)), listOf(set(1L)))
        activeTitle = FakeActiveTitleRepository(initial = TitleIds.BEGINNER)
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(TitleIds.BEGINNER, viewModel.uiState.value.activeTitleId)
        viewModel.clearActiveTitle()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.activeTitleId)
        assertTrue(activeTitle.setCalls.contains(null))
    }

    @Test
    fun invalidPreferredTitle_clearsWithoutFallback() = runTest(testDispatcher) {
        // Prefer VETERAN while only BEGINNER unlocked
        activeTitle = FakeActiveTitleRepository(initial = TitleIds.VETERAN)
        sessions.setHistory(listOf(session(1L)), listOf(set(1L)))
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.activeTitleId)
        assertTrue(activeTitle.setCalls.contains(null))
        assertTrue(
            viewModel.uiState.value.titles.single { it.definition.id == TitleIds.BEGINNER }.unlocked,
        )
    }

    @Test
    fun levelTitle_unlocksFromXpLedger() = runTest(testDispatcher) {
        gamification.setTotalXp(5_609) // level 10
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(
            viewModel.uiState.value.titles.single { it.definition.id == TitleIds.ASCENDANT }.unlocked,
        )
    }

    @Test
    fun cannotSelectLockedTitle() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.selectTitle(TitleIds.VETERAN)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.activeTitleId)
        assertTrue(activeTitle.setCalls.isEmpty())
    }

    private fun session(sessionId: Long) = WorkoutHistoryItem(
        sessionId = sessionId,
        workoutName = "Treino",
        startedAtMillis = sessionId * 86_400_000L,
        endedAtMillis = sessionId * 86_400_000L,
        durationMillis = 1_000L,
        volume = 100.0,
        exerciseCount = 1,
        completedSetCount = 1,
        plannedSetCount = 1,
    )

    private fun set(sessionId: Long) = CompletedSetRecord(
        sessionId = sessionId,
        occurredAtMillis = sessionId * 86_400_000L,
        exerciseName = "Supino",
        reps = 8,
        weight = 10.0,
    )
}

private class FakeRpgSessionRepository : WorkoutSessionRepository {
    private val sessions = MutableStateFlow<List<WorkoutHistoryItem>>(emptyList())
    private val sets = MutableStateFlow<List<CompletedSetRecord>>(emptyList())

    fun setHistory(
        sessionItems: List<WorkoutHistoryItem>,
        setItems: List<CompletedSetRecord>,
    ) {
        sessions.value = sessionItems
        sets.value = setItems
    }

    override fun observeCompletedSessions(): Flow<List<WorkoutHistoryItem>> = sessions.asStateFlow()
    override fun observeCompletedSetHistory(): Flow<List<CompletedSetRecord>> = sets.asStateFlow()
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

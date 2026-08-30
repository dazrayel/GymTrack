package com.gymtrack.presentation.history

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class HistoryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeHistorySessionRepository
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeHistorySessionRepository()
        viewModel = HistoryViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isLoadingTrue_beforeFirstEmission() {
        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun afterLoad_sessions_areExposedInUiState() = runTest(testDispatcher) {
        val item = historyItem(sessionId = 7L, workoutName = "Treino A")
        fakeRepository.emit(listOf(item))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf(item), viewModel.uiState.value.items)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun afterLoad_emptyList_itemsStayEmpty() = runTest(testDispatcher) {
        fakeRepository.emit(emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.items.isEmpty())
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun whenObserveThrows_errorIsSet() = runTest(testDispatcher) {
        fakeRepository.shouldThrowOnObserve = true
        val vm = HistoryViewModel(fakeRepository)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("Simulated history error", vm.uiState.value.error)
        assertTrue(vm.uiState.value.items.isEmpty())
    }

    @Test
    fun afterLoad_preservesRepositoryOrder() = runTest(testDispatcher) {
        val newer = historyItem(sessionId = 20L, workoutName = "Mais recente")
        val older = historyItem(sessionId = 10L, workoutName = "Mais antigo")
        fakeRepository.emit(listOf(newer, older))
        advanceUntilIdle()

        assertEquals(
            listOf(20L, 10L),
            viewModel.uiState.value.items.map { it.sessionId },
        )
        assertEquals(
            listOf("Mais recente", "Mais antigo"),
            viewModel.uiState.value.items.map { it.workoutName },
        )
    }

    @Test
    fun clearError_clearsMessage() = runTest(testDispatcher) {
        fakeRepository.shouldThrowOnObserve = true
        val vm = HistoryViewModel(fakeRepository)
        advanceUntilIdle()

        vm.clearError()
        assertNull(vm.uiState.value.error)
    }

    private fun historyItem(
        sessionId: Long,
        workoutName: String,
    ) = WorkoutHistoryItem(
        sessionId = sessionId,
        workoutName = workoutName,
        startedAtMillis = 1_000L,
        endedAtMillis = 2_000L,
        durationMillis = 1_000L,
        volume = 100.0,
        exerciseCount = 2,
        completedSetCount = 4,
        plannedSetCount = 6,
    )
}

private class FakeHistorySessionRepository : WorkoutSessionRepository {

    private val completed = MutableSharedFlow<List<WorkoutHistoryItem>>(extraBufferCapacity = 1)
    var shouldThrowOnObserve = false

    suspend fun emit(items: List<WorkoutHistoryItem>) {
        completed.emit(items)
    }

    override fun observeCompletedSessions(): Flow<List<WorkoutHistoryItem>> {
        if (shouldThrowOnObserve) {
            return flow { throw RuntimeException("Simulated history error") }
        }
        return completed
    }

    override fun observeCompletedSetHistory(): Flow<List<CompletedSetRecord>> = emptyFlow()

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
    override suspend fun finishSession(sessionId: Long) = Unit
}

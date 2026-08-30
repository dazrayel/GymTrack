package com.gymtrack.presentation.stats

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.gymtrack.domain.model.CompletedSetRecord
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
class ExerciseStatsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeExerciseStatsSessionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeExerciseStatsSessionRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isLoadingTrue_beforeFirstEmission() {
        val viewModel = viewModel("Supino")
        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals("Supino", viewModel.uiState.value.exerciseName)
        assertTrue(viewModel.uiState.value.performanceHistory.isEmpty())
        assertNull(viewModel.uiState.value.personalRecord)
        assertFalse(viewModel.uiState.value.showEmpty)
    }

    @Test
    fun singleSession_exposesPointAndPersonalRecord() = runTest(testDispatcher) {
        val viewModel = viewModel("Supino")
        fakeRepository.emitSets(
            listOf(rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 80.0)),
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.showEmpty)
        assertEquals(1, viewModel.uiState.value.performanceHistory.size)
        val point = viewModel.uiState.value.performanceHistory.single()
        assertEquals(1L, point.sessionId)
        assertEquals("Supino", point.exerciseName)
        assertEquals(80.0, point.bestWeight, 0.001)
        assertEquals(8, point.bestReps)
        assertEquals(640.0, point.volume, 0.001)
        assertEquals(80.0, viewModel.uiState.value.personalRecord!!.bestWeight, 0.001)
        assertEquals(8, viewModel.uiState.value.personalRecord!!.bestReps)
        assertEquals(640.0, viewModel.uiState.value.personalRecord!!.bestVolume, 0.001)
    }

    @Test
    fun multipleSessions_areChronological() = runTest(testDispatcher) {
        val viewModel = viewModel("Supino")
        fakeRepository.emitSets(
            listOf(
                rec(sessionId = 3L, occurredAt = 3_000L, name = "Supino", reps = 8, weight = 90.0),
                rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 70.0),
                rec(sessionId = 2L, occurredAt = 2_000L, name = "Supino", reps = 8, weight = 80.0),
            ),
        )
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L, 3L), viewModel.uiState.value.performanceHistory.map { it.sessionId })
        assertEquals(90.0, viewModel.uiState.value.personalRecord!!.bestWeight, 0.001)
        assertEquals(3L, viewModel.uiState.value.personalRecord!!.bestWeightSessionId)
    }

    @Test
    fun filtersByExerciseName() = runTest(testDispatcher) {
        val viewModel = viewModel("Supino")
        fakeRepository.emitSets(
            listOf(
                rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 80.0),
                rec(sessionId = 1L, occurredAt = 1_000L, name = "Agachamento", reps = 5, weight = 140.0),
                rec(sessionId = 2L, occurredAt = 2_000L, name = "Agachamento", reps = 5, weight = 150.0),
            ),
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.performanceHistory.size)
        assertTrue(viewModel.uiState.value.performanceHistory.all { it.exerciseName == "Supino" })
        assertEquals(80.0, viewModel.uiState.value.personalRecord!!.bestWeight, 0.001)
        assertEquals("Supino", viewModel.uiState.value.personalRecord!!.exerciseName)
    }

    @Test
    fun noCompletedSets_showEmpty() = runTest(testDispatcher) {
        val viewModel = viewModel("Supino")
        fakeRepository.emitSets(emptyList())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showEmpty)
        assertTrue(viewModel.uiState.value.performanceHistory.isEmpty())
        assertNull(viewModel.uiState.value.personalRecord)
    }

    @Test
    fun differentExerciseName_doesNotMix() = runTest(testDispatcher) {
        val viewModel = viewModel("Crucifixo")
        fakeRepository.emitSets(
            listOf(
                rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 80.0),
                rec(sessionId = 2L, occurredAt = 2_000L, name = "Crucifixo", reps = 12, weight = 25.0),
            ),
        )
        advanceUntilIdle()

        assertEquals(listOf(2L), viewModel.uiState.value.performanceHistory.map { it.sessionId })
        assertEquals(25.0, viewModel.uiState.value.personalRecord!!.bestWeight, 0.001)
    }

    @Test
    fun reemittingSets_updatesHistory() = runTest(testDispatcher) {
        val viewModel = viewModel("Supino")
        fakeRepository.emitSets(
            listOf(rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 80.0)),
        )
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.performanceHistory.size)

        fakeRepository.emitSets(
            listOf(
                rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino", reps = 8, weight = 80.0),
                rec(sessionId = 2L, occurredAt = 2_000L, name = "Supino", reps = 8, weight = 85.0),
            ),
        )
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.performanceHistory.size)
        assertEquals(85.0, viewModel.uiState.value.personalRecord!!.bestWeight, 0.001)
    }

    @Test
    fun whenObserveThrows_errorIsSet() = runTest(testDispatcher) {
        fakeRepository.shouldThrowOnObserve = true
        val viewModel = viewModel("Supino")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Simulated stats error", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.showEmpty)
    }

    @Test
    fun snapshotName_isPreserved() = runTest(testDispatcher) {
        val viewModel = viewModel("Supino Snapshot")
        fakeRepository.emitSets(
            listOf(rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino Snapshot", reps = 8, weight = 60.0)),
        )
        advanceUntilIdle()

        assertEquals("Supino Snapshot", viewModel.uiState.value.exerciseName)
        assertEquals("Supino Snapshot", viewModel.uiState.value.performanceHistory.single().exerciseName)
        assertEquals("Supino Snapshot", viewModel.uiState.value.personalRecord!!.exerciseName)
    }

    @Test
    fun zeroWeight_isValidPersonalRecord() = runTest(testDispatcher) {
        val viewModel = viewModel("Abdominal")
        fakeRepository.emitSets(
            listOf(rec(sessionId = 1L, occurredAt = 1_000L, name = "Abdominal", reps = 20, weight = 0.0)),
        )
        advanceUntilIdle()

        assertEquals(0.0, viewModel.uiState.value.personalRecord!!.bestWeight, 0.001)
        assertEquals(20, viewModel.uiState.value.personalRecord!!.bestReps)
        assertEquals(0.0, viewModel.uiState.value.personalRecord!!.bestVolume, 0.001)
        assertEquals(0.0, viewModel.uiState.value.performanceHistory.single().bestWeight, 0.001)
    }

    @Test
    fun encodedRouteArgument_isDecoded() = runTest(testDispatcher) {
        val viewModel = ExerciseStatsViewModel(
            savedStateHandle = SavedStateHandle(mapOf("exerciseName" to "Supino%20Inclinado")),
            sessionRepository = fakeRepository,
        )
        fakeRepository.emitSets(
            listOf(rec(sessionId = 1L, occurredAt = 1_000L, name = "Supino Inclinado", reps = 8, weight = 80.0)),
        )
        advanceUntilIdle()

        assertEquals("Supino Inclinado", viewModel.uiState.value.exerciseName)
        assertEquals(1, viewModel.uiState.value.performanceHistory.size)
    }

    private fun viewModel(exerciseName: String) = ExerciseStatsViewModel(
        savedStateHandle = SavedStateHandle(mapOf("exerciseName" to exerciseName)),
        sessionRepository = fakeRepository,
    )

    private fun rec(
        sessionId: Long,
        occurredAt: Long,
        name: String,
        reps: Int,
        weight: Double,
    ) = CompletedSetRecord(
        sessionId = sessionId,
        occurredAtMillis = occurredAt,
        exerciseName = name,
        reps = reps,
        weight = weight,
    )
}

private class FakeExerciseStatsSessionRepository : WorkoutSessionRepository {

    private val sets = MutableSharedFlow<List<CompletedSetRecord>>(extraBufferCapacity = 1)
    var shouldThrowOnObserve = false

    suspend fun emitSets(items: List<CompletedSetRecord>) {
        sets.emit(items)
    }

    override fun observeCompletedSetHistory(): Flow<List<CompletedSetRecord>> {
        if (shouldThrowOnObserve) {
            return flow { throw RuntimeException("Simulated stats error") }
        }
        return sets
    }

    override fun observeCompletedSessions(): Flow<List<WorkoutHistoryItem>> = emptyFlow()
    override suspend fun startSession(workoutId: Long): Long = 0L
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

package com.gymtrack.presentation.workouts.summary

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.gymtrack.domain.model.CompletedSetRecord
import com.gymtrack.domain.model.StartSessionResult
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSessionStatus
import com.gymtrack.domain.model.WorkoutSet
import com.gymtrack.domain.repository.WorkoutSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private const val SESSION_ID = 50L

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class WorkoutSessionSummaryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepo: FakeSummarySessionRepository
    private lateinit var viewModel: WorkoutSessionSummaryViewModel

    private val session = WorkoutSession(
        id = SESSION_ID,
        workoutId = 7L,
        workoutName = "Push Day",
        workoutDescription = "Peito",
        startedAtMillis = 1_000L,
        endedAtMillis = 61_000L,
        status = WorkoutSessionStatus.COMPLETED,
    )

    private val exerciseA = WorkoutSessionExercise(
        id = 10L,
        sessionId = SESSION_ID,
        exerciseId = 1L,
        position = 0,
        exerciseName = "Supino Snapshot",
        muscleGroup = "Peito",
        equipmentType = "Barra",
        plannedSets = 3,
        minRepetitions = 8,
        maxRepetitions = 12,
        plannedWeight = 60.0,
        restSeconds = 90,
    )

    private val exerciseB = WorkoutSessionExercise(
        id = 20L,
        sessionId = SESSION_ID,
        exerciseId = 2L,
        position = 1,
        exerciseName = "Crucifixo Snapshot",
        muscleGroup = "Peito",
        equipmentType = "Halteres",
        plannedSets = 2,
        minRepetitions = 10,
        maxRepetitions = 12,
        plannedWeight = 14.0,
        restSeconds = 0,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeSummarySessionRepository()
        viewModel = WorkoutSessionSummaryViewModel(
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to SESSION_ID)),
            workoutSessionRepository = fakeRepo,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun duration_usesEndedAtMinusStartedAt() {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA, exerciseB))

        assertEquals(60_000L, viewModel.uiState.value.durationMillis)
    }

    @Test
    fun progress_countsCompletedExercisesAndSets() {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA, exerciseB))
        fakeRepo.emitSets(
            exerciseA.id,
            listOf(set(exerciseA.id, 0), set(exerciseA.id, 1), set(exerciseA.id, 2)),
        )
        fakeRepo.emitSets(exerciseB.id, listOf(set(exerciseB.id, 0)))

        val progress = viewModel.uiState.value.progress
        assertEquals(1, progress.completedExercises)
        assertEquals(2, progress.totalExercises)
        assertEquals(4, progress.completedSets)
        assertEquals(5, progress.plannedSets)
        assertEquals(80, progress.progressPercent)
    }

    @Test
    fun volume_sumsOnlyPerformedSets() {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA, exerciseB))
        fakeRepo.emitSets(
            exerciseA.id,
            listOf(set(exerciseA.id, 0, reps = 8, weight = 60.0)),
        )

        assertEquals(480.0, viewModel.uiState.value.volume, 0.001)
        assertEquals(480.0, viewModel.uiState.value.exerciseSummaries[0].volume, 0.001)
        assertEquals(0.0, viewModel.uiState.value.exerciseSummaries[1].volume, 0.001)
    }

    @Test
    fun snapshotName_isUsedEvenWhenExerciseIdIsNull() {
        fakeRepo.emitSession(session.copy(workoutId = null))
        fakeRepo.emitExercises(listOf(exerciseA.copy(exerciseId = null)))
        fakeRepo.emitSets(exerciseA.id, listOf(set(exerciseA.id, 0)))

        val summary = viewModel.uiState.value.exerciseSummaries.single()
        assertEquals("Supino Snapshot", summary.exerciseName)
        assertEquals("Peito", summary.muscleGroup)
        assertFalse(viewModel.uiState.value.canNavigateToWorkout)
        assertEquals("Push Day", viewModel.uiState.value.session?.workoutName)
    }

    @Test
    fun missingOriginalExercise_doesNotBreakSummary() {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA.copy(exerciseId = 999_999L)))
        fakeRepo.emitSets(exerciseA.id, emptyList())

        assertTrue(viewModel.uiState.value.exerciseSummaries.isNotEmpty())
        assertEquals("Supino Snapshot", viewModel.uiState.value.exerciseSummaries[0].exerciseName)
        assertEquals(0, viewModel.uiState.value.progress.completedSets)
    }

    @Test
    fun missingSession_marksNotFound() {
        fakeRepo.emitSession(null)

        assertTrue(viewModel.uiState.value.sessionNotFound)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(null, viewModel.uiState.value.session)
    }

    @Test
    fun completedSession_exposesUtcDateAndTimes() {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA))

        assertEquals("01/01/1970", viewModel.uiState.value.startedDate)
        assertEquals("00:00", viewModel.uiState.value.startedTime)
        assertEquals("00:01", viewModel.uiState.value.endedTime)
        assertFalse(viewModel.uiState.value.sessionNotFound)
        assertEquals("Push Day", viewModel.uiState.value.session?.workoutName)
    }

    @Test
    fun exercises_preservePositionOrder() {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseB, exerciseA))

        assertEquals(
            listOf("Supino Snapshot", "Crucifixo Snapshot"),
            viewModel.uiState.value.exerciseSummaries.map { it.exerciseName },
        )
    }

    @Test
    fun sets_areOrderedBySetIndex() {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA))
        fakeRepo.emitSets(
            exerciseA.id,
            listOf(set(exerciseA.id, 2), set(exerciseA.id, 0), set(exerciseA.id, 1)),
        )

        assertEquals(
            listOf(0, 1, 2),
            viewModel.uiState.value.exerciseSummaries.single().sets.map { it.setIndex },
        )
    }

    @Test
    fun progress_comparesCurrentSessionAgainstPreviousBest() {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA))
        fakeRepo.emitSets(
            exerciseA.id,
            listOf(set(exerciseA.id, 0, reps = 8, weight = 85.0)),
        )
        fakeRepo.emitHistory(
            listOf(
                CompletedSetRecord(
                    sessionId = 10L,
                    occurredAtMillis = 500L,
                    exerciseName = "Supino Snapshot",
                    reps = 8,
                    weight = 80.0,
                ),
                CompletedSetRecord(
                    sessionId = SESSION_ID,
                    occurredAtMillis = 61_000L,
                    exerciseName = "Supino Snapshot",
                    reps = 8,
                    weight = 85.0,
                ),
            ),
        )

        val progress = viewModel.uiState.value.exerciseSummaries.single().progress
        assertEquals(85.0, progress.sessionBestWeight!!, 0.001)
        assertEquals(80.0, progress.previousBest!!.bestWeight, 0.001)
        assertEquals(5.0, progress.weightDelta!!, 0.001)
        assertEquals(SESSION_ID, progress.historicalBest!!.bestWeightSessionId)
        assertEquals(10L, progress.previousSession!!.sessionId)
    }

    private fun set(
        sessionExerciseId: Long,
        setIndex: Int,
        reps: Int = 8,
        weight: Double = 60.0,
    ) = WorkoutSet(
        id = sessionExerciseId * 10 + setIndex,
        sessionExerciseId = sessionExerciseId,
        setIndex = setIndex,
        reps = reps,
        weight = weight,
        completedAtMillis = 1L,
    )
}

private class FakeSummarySessionRepository : WorkoutSessionRepository {

    private val sessionFlow = MutableStateFlow<WorkoutSession?>(null)
    private val exercisesFlow = MutableStateFlow<List<WorkoutSessionExercise>>(emptyList())
    private val setsFlows = mutableMapOf<Long, MutableStateFlow<List<WorkoutSet>>>()
    private val historyFlow = MutableStateFlow<List<CompletedSetRecord>>(emptyList())

    fun emitSession(session: WorkoutSession?) {
        sessionFlow.value = session
    }

    fun emitExercises(exercises: List<WorkoutSessionExercise>) {
        exercisesFlow.value = exercises
    }

    fun emitSets(sessionExerciseId: Long, sets: List<WorkoutSet>) {
        setsFlows.getOrPut(sessionExerciseId) { MutableStateFlow(emptyList()) }.value = sets
    }

    fun emitHistory(records: List<CompletedSetRecord>) {
        historyFlow.value = records
    }

    override suspend fun startSession(workoutId: Long) = StartSessionResult.Created(0L)
    override suspend fun getSession(id: Long) = sessionFlow.value
    override fun observeSession(id: Long) = sessionFlow
    override fun observeInProgress(): Flow<WorkoutSession?> = emptyFlow()
    override fun observeCompletedSessions(): Flow<List<com.gymtrack.domain.model.WorkoutHistoryItem>> = emptyFlow()
    override fun observeCompletedSetHistory(): Flow<List<CompletedSetRecord>> = historyFlow
    override fun observeSessionExercises(sessionId: Long) = exercisesFlow
    override fun observeSets(sessionExerciseId: Long): Flow<List<WorkoutSet>> =
        setsFlows.getOrPut(sessionExerciseId) { MutableStateFlow(emptyList()) }

    override suspend fun completeSet(sessionExerciseId: Long, setIndex: Int, reps: Int, weight: Double) = 0L
    override suspend fun startRest(sessionId: Long, sessionExerciseId: Long, afterSetIndex: Int, restSeconds: Int) = Unit
    override suspend fun pauseRest(sessionId: Long) = Unit
    override suspend fun resumeRest(sessionId: Long) = Unit
    override suspend fun skipRest(sessionId: Long) = Unit
    override suspend fun finishSession(sessionId: Long) = Unit
}

package com.gymtrack.presentation.workouts.detail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.StartSessionResult
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutBlock
import com.gymtrack.domain.model.WorkoutBlockType
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionStatus
import com.gymtrack.domain.repository.ExerciseRepository
import com.gymtrack.domain.repository.WorkoutRepository
import com.gymtrack.domain.repository.WorkoutSessionRepository
import com.gymtrack.domain.model.CompletedSetRecord
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var workoutRepo: FakeWorkoutRepository
    private lateinit var exerciseRepo: FakeExerciseRepository
    private lateinit var sessionRepo: FakeSessionRepository
    private lateinit var viewModel: WorkoutDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        workoutRepo = FakeWorkoutRepository()
        exerciseRepo = FakeExerciseRepository()
        sessionRepo = FakeSessionRepository()
        workoutRepo.emitWorkout(Workout(id = 1, name = "Push"))
        viewModel = WorkoutDetailViewModel(
            workoutRepo,
            exerciseRepo,
            sessionRepo,
            SavedStateHandle(mapOf("workoutId" to 1L)),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun chooseAddType_single_opensPicker() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.chooseAddType(WorkoutBlockType.SINGLE)
        assertTrue(viewModel.uiState.value.showExercisePicker)
        assertEquals(WorkoutBlockType.SINGLE, viewModel.uiState.value.pendingBlockType)
    }

    @Test
    fun chooseAddType_biSet_opensBuilderWithTwoSlots() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.chooseAddType(WorkoutBlockType.BI_SET)
        assertTrue(viewModel.uiState.value.showBlockBuilder)
        assertEquals(2, viewModel.uiState.value.blockDraftSlots.size)
    }

    @Test
    fun confirmBlockDraft_persistsBiSet() = runTest(testDispatcher) {
        advanceUntilIdle()
        val e1 = Exercise(1, "Rosca", "Bíceps", "Barra")
        val e2 = Exercise(2, "Tríceps", "Tríceps", "Corda")
        exerciseRepo.emit(listOf(e1, e2))
        viewModel.chooseAddType(WorkoutBlockType.BI_SET)
        viewModel.pickSlot(0)
        viewModel.selectExercise(e1)
        viewModel.pickSlot(1)
        viewModel.selectExercise(e2)
        viewModel.confirmBlockDraft(rounds = 3, restSeconds = 60)
        advanceUntilIdle()
        assertEquals(1, workoutRepo.addedBlocks.size)
        assertEquals(WorkoutBlockType.BI_SET, workoutRepo.addedBlocks.first().first.type)
        assertEquals(2, workoutRepo.addedBlocks.first().second.size)
        assertFalse(viewModel.uiState.value.showBlockBuilder)
    }

    @Test
    fun selectExercise_forSingle_opensConfiguration() = runTest(testDispatcher) {
        advanceUntilIdle()
        val exercise = Exercise(1, "Supino", "Peitoral", "Barra")
        viewModel.chooseAddType(WorkoutBlockType.SINGLE)
        viewModel.selectExercise(exercise)
        assertEquals(exercise, viewModel.uiState.value.exerciseToConfigure?.exercise)
        assertEquals(3, viewModel.uiState.value.configureRounds)
    }

    @Test
    fun saveExerciseConfiguration_createsSingleBlock() = runTest(testDispatcher) {
        advanceUntilIdle()
        val exercise = Exercise(1, "Supino", "Peitoral", "Barra")
        viewModel.chooseAddType(WorkoutBlockType.SINGLE)
        viewModel.selectExercise(exercise)
        viewModel.saveExerciseConfiguration(4, 8, 12, 60.0, 90, "nota")
        advanceUntilIdle()
        assertEquals(1, workoutRepo.addedBlocks.size)
        assertEquals(4, workoutRepo.addedBlocks.first().first.rounds)
        assertEquals(90, workoutRepo.addedBlocks.first().first.restSeconds)
        assertNull(viewModel.uiState.value.exerciseToConfigure)
    }

    @Test
    fun confirmDelete_removesBlock() = runTest(testDispatcher) {
        advanceUntilIdle()
        val block = WorkoutBlock(id = 9, workoutId = 1, position = 0, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 60)
        val we = WorkoutExercise(id = 1, blockId = 9, exerciseId = 1, positionInBlock = 0, minRepetitions = 8, maxRepetitions = 12, weight = 0.0)
        exerciseRepo.emit(listOf(Exercise(1, "Supino", "Peitoral", "Barra")))
        workoutRepo.emitBlocks(listOf(block))
        workoutRepo.emitExercises(listOf(we))
        advanceUntilIdle()
        val detail = viewModel.uiState.value.blocks.single()
        viewModel.showDeleteConfirmation(detail)
        viewModel.confirmDelete()
        advanceUntilIdle()
        assertTrue(workoutRepo.removedBlockIds.contains(9L))
        assertNull(viewModel.uiState.value.blockToDelete)
    }
}

private class FakeWorkoutRepository : WorkoutRepository {
    private val workoutFlow = MutableStateFlow<Workout?>(null)
    private val blocksFlow = MutableStateFlow<List<WorkoutBlock>>(emptyList())
    private val exercisesFlow = MutableStateFlow<List<WorkoutExercise>>(emptyList())
    val addedBlocks = mutableListOf<Pair<WorkoutBlock, List<WorkoutExercise>>>()
    val removedBlockIds = mutableListOf<Long>()

    fun emitWorkout(workout: Workout?) { workoutFlow.value = workout }
    fun emitBlocks(blocks: List<WorkoutBlock>) { blocksFlow.value = blocks }
    fun emitExercises(exercises: List<WorkoutExercise>) { exercisesFlow.value = exercises }

    override fun getAll() = flowOf(emptyList<Workout>())
    override fun getById(id: Long) = workoutFlow
    override suspend fun save(workout: Workout) = 1L
    override suspend fun update(workout: Workout) {}
    override suspend fun delete(workout: Workout) {}
    override fun getBlocks(workoutId: Long) = blocksFlow
    override fun getExercisesForWorkout(workoutId: Long) = exercisesFlow
    override suspend fun addBlock(block: WorkoutBlock, exercises: List<WorkoutExercise>): Long {
        addedBlocks += block to exercises
        val id = (addedBlocks.size).toLong()
        blocksFlow.value = blocksFlow.value + block.copy(id = id)
        exercisesFlow.value = exercisesFlow.value + exercises.mapIndexed { i, e -> e.copy(id = i + 1L, blockId = id) }
        return id
    }
    override suspend fun updateBlock(block: WorkoutBlock) {}
    override suspend fun updateBlockExercise(exercise: WorkoutExercise) {}
    override suspend fun replaceBlockExercise(exerciseRowId: Long, newCatalogueExerciseId: Long) {}
    override suspend fun removeBlock(blockId: Long) { removedBlockIds += blockId }
    override suspend fun updateBlockPositions(positions: Map<Long, Int>) {}
    override suspend fun addExercise(workoutExercise: WorkoutExercise) = error("n/a")
    override suspend fun updateExercise(workoutExercise: WorkoutExercise) {}
    override suspend fun removeExercise(workoutExercise: WorkoutExercise) {}
    override suspend fun removeExerciseById(id: Long) {}
    override suspend fun updateExercisePositions(positions: Map<Long, Int>) {}
}

private class FakeExerciseRepository : ExerciseRepository {
    private val flow = MutableStateFlow<List<Exercise>>(emptyList())
    fun emit(list: List<Exercise>) { flow.value = list }
    override fun getAll() = flow
    override fun getById(id: Long) = flow.map { list -> list.firstOrNull { it.id == id } }
    override fun search(query: String) = flow.map { list -> list.filter { it.name.contains(query, true) } }
    override suspend fun save(exercise: Exercise) = exercise.id
    override suspend fun delete(exercise: Exercise) {}
}

private class FakeSessionRepository : WorkoutSessionRepository {
    override suspend fun startSession(workoutId: Long) = StartSessionResult.Created(1L)
    override suspend fun getSession(id: Long): WorkoutSession? = null
    override fun observeSession(id: Long) = flowOf<WorkoutSession?>(null)
    override fun observeInProgress() = flowOf<WorkoutSession?>(null)
    override fun observeCompletedSessions() = flowOf(emptyList<WorkoutHistoryItem>())
    override fun observeCompletedSetHistory() = flowOf(emptyList<CompletedSetRecord>())
    override fun observeSessionExercises(sessionId: Long) = flowOf(emptyList<WorkoutSessionExercise>())
    override fun observeSets(sessionExerciseId: Long) = flowOf(emptyList<WorkoutSet>())
    override suspend fun completeSet(sessionExerciseId: Long, setIndex: Int, reps: Int, weight: Double) = 1L
    override suspend fun skipSessionExercise(sessionExerciseId: Long) {}
    override suspend fun resumeSessionExercise(sessionExerciseId: Long) {}
    override suspend fun startRest(sessionId: Long, sessionExerciseId: Long, afterSetIndex: Int, restSeconds: Int) {}
    override suspend fun pauseRest(sessionId: Long) {}
    override suspend fun resumeRest(sessionId: Long) {}
    override suspend fun skipRest(sessionId: Long) {}
    override suspend fun finishSession(sessionId: Long) {}
    override suspend fun deleteCompletedSession(sessionId: Long) {}
}

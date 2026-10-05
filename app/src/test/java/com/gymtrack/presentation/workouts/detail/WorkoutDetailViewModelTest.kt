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
    fun saveExerciseConfiguration_inBiSet_updatesExerciseWithoutTouchingBlockRounds() = runTest(testDispatcher) {
        advanceUntilIdle()
        val block = WorkoutBlock(
            id = 5,
            workoutId = 1,
            position = 0,
            type = WorkoutBlockType.BI_SET,
            rounds = 3,
            restSeconds = 60,
        )
        val weA = WorkoutExercise(
            id = 11,
            blockId = 5,
            exerciseId = 1,
            positionInBlock = 0,
            minRepetitions = 8,
            maxRepetitions = 12,
            weight = 20.0,
        )
        val weB = WorkoutExercise(
            id = 12,
            blockId = 5,
            exerciseId = 2,
            positionInBlock = 1,
            minRepetitions = 10,
            maxRepetitions = 12,
            weight = 15.0,
        )
        exerciseRepo.emit(
            listOf(
                Exercise(1, "Rosca", "Bíceps", "Barra"),
                Exercise(2, "Tríceps", "Tríceps", "Corda"),
            ),
        )
        workoutRepo.emitBlocks(listOf(block))
        workoutRepo.emitExercises(listOf(weA, weB))
        advanceUntilIdle()

        val detail = viewModel.uiState.value.blocks.single()
        viewModel.showEditExercise(detail, detail.items[0])
        viewModel.saveExerciseConfiguration(9, 6, 10, 30.0, 120, "ignored-for-block")
        advanceUntilIdle()

        assertEquals(1, workoutRepo.updatedExercises.size)
        val updated = workoutRepo.updatedExercises.single()
        assertEquals(11L, updated.id)
        assertEquals(5L, updated.blockId)
        assertEquals(0, updated.positionInBlock)
        assertEquals(6, updated.minRepetitions)
        assertEquals(10, updated.maxRepetitions)
        assertEquals(30.0, updated.weight, 0.0)
        assertTrue(workoutRepo.updatedBlocks.isEmpty())
        assertEquals(2, workoutRepo.exercisesFlowValue().size)
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

    @Test
    fun reorderBlocks_moveMiddleUp_persistsConsecutivePositions() = runTest(testDispatcher) {
        advanceUntilIdle()
        seedThreeSingles()
        advanceUntilIdle()
        assertEquals(listOf(10L, 20L, 30L), viewModel.uiState.value.blocks.map { it.id })

        viewModel.reorderBlocks(fromIndex = 1, toIndex = 0)
        advanceUntilIdle()

        assertEquals(mapOf(20L to 0, 10L to 1, 30L to 2), workoutRepo.lastPositionUpdate)
        assertEquals(listOf(20L, 10L, 30L), viewModel.uiState.value.blocks.map { it.id })
        assertEquals(listOf(0, 1, 2), viewModel.uiState.value.blocks.map { it.position })
    }

    @Test
    fun reorderBlocks_moveMiddleDown_persistsConsecutivePositions() = runTest(testDispatcher) {
        advanceUntilIdle()
        seedThreeSingles()
        advanceUntilIdle()

        viewModel.reorderBlocks(fromIndex = 1, toIndex = 2)
        advanceUntilIdle()

        assertEquals(mapOf(10L to 0, 30L to 1, 20L to 2), workoutRepo.lastPositionUpdate)
        assertEquals(listOf(10L, 30L, 20L), viewModel.uiState.value.blocks.map { it.id })
        assertEquals(listOf(0, 1, 2), viewModel.uiState.value.blocks.map { it.position })
    }

    @Test
    fun reorderBlocks_firstUpAndLastDown_areNoOps() = runTest(testDispatcher) {
        advanceUntilIdle()
        seedThreeSingles()
        advanceUntilIdle()

        viewModel.reorderBlocks(fromIndex = 0, toIndex = -1)
        viewModel.reorderBlocks(fromIndex = 2, toIndex = 3)
        advanceUntilIdle()

        assertNull(workoutRepo.lastPositionUpdate)
        assertEquals(listOf(10L, 20L, 30L), viewModel.uiState.value.blocks.map { it.id })
    }

    @Test
    fun confirmDelete_middleBlock_compactsRemainingPositions() = runTest(testDispatcher) {
        advanceUntilIdle()
        seedThreeSingles()
        advanceUntilIdle()
        val middle = viewModel.uiState.value.blocks[1]
        viewModel.showDeleteConfirmation(middle)
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertEquals(listOf(10L, 30L), viewModel.uiState.value.blocks.map { it.id })
        assertEquals(listOf(0, 1), viewModel.uiState.value.blocks.map { it.position })
    }

    @Test
    fun duplicateBlock_insertsCopyImmediatelyAfterOriginal() = runTest(testDispatcher) {
        advanceUntilIdle()
        seedThreeSingles()
        advanceUntilIdle()

        viewModel.duplicateBlock(20L)
        advanceUntilIdle()

        assertEquals(listOf(20L), workoutRepo.duplicatedBlockIds)
        assertEquals(listOf(10L, 20L, 101L, 30L), viewModel.uiState.value.blocks.map { it.id })
        assertEquals(listOf(0, 1, 2, 3), viewModel.uiState.value.blocks.map { it.position })
    }

    @Test
    fun startReplaceExercise_opensPickerWithRowId() = runTest(testDispatcher) {
        advanceUntilIdle()
        seedSingleWithConfig()
        advanceUntilIdle()

        val item = viewModel.uiState.value.blocks.single().items.single()
        viewModel.startReplaceExercise(item)

        assertTrue(viewModel.uiState.value.showExercisePicker)
        assertEquals(item.id, viewModel.uiState.value.replacingExerciseRowId)
        assertNull(viewModel.uiState.value.blockDraftSlotIndex)
    }

    @Test
    fun selectExercise_whileReplacing_updatesOnlyExerciseIdAndClearsState() = runTest(testDispatcher) {
        advanceUntilIdle()
        seedSingleWithConfig()
        advanceUntilIdle()

        val before = workoutRepo.exercisesFlowValue().single()
        val replacement = Exercise(99, "Supino inclinado", "Peitoral", "Barra")
        exerciseRepo.emit(
            listOf(
                Exercise(1, "Supino reto", "Peitoral", "Barra"),
                replacement,
            ),
        )
        advanceUntilIdle()

        viewModel.startReplaceExercise(viewModel.uiState.value.blocks.single().items.single())
        viewModel.selectExercise(replacement)
        advanceUntilIdle()

        assertEquals(listOf(before.id to 99L), workoutRepo.replacedExercises)
        assertTrue(workoutRepo.updatedBlocks.isEmpty())
        assertTrue(workoutRepo.updatedExercises.isEmpty())
        assertTrue(workoutRepo.addedBlocks.isEmpty())
        assertNull(viewModel.uiState.value.replacingExerciseRowId)
        assertFalse(viewModel.uiState.value.showExercisePicker)

        val after = workoutRepo.exercisesFlowValue().single()
        assertEquals(before.id, after.id)
        assertEquals(before.blockId, after.blockId)
        assertEquals(before.positionInBlock, after.positionInBlock)
        assertEquals(before.minRepetitions, after.minRepetitions)
        assertEquals(before.maxRepetitions, after.maxRepetitions)
        assertEquals(before.weight, after.weight, 0.0)
        assertEquals(before.notes, after.notes)
        assertEquals(99L, after.exerciseId)
        assertEquals(1, workoutRepo.blocksFlowValue().size)
    }

    @Test
    fun selectExercise_replaceFirstInBiSet_keepsSiblingIntact() = runTest(testDispatcher) {
        advanceUntilIdle()
        seedBiSet()
        advanceUntilIdle()

        val before = workoutRepo.exercisesFlowValue().sortedBy { it.positionInBlock }
        val replacement = Exercise(30, "Rosca alternada", "Bíceps", "Halteres")
        exerciseRepo.emit(
            listOf(
                Exercise(1, "Rosca", "Bíceps", "Barra"),
                Exercise(2, "Tríceps", "Tríceps", "Corda"),
                replacement,
            ),
        )
        advanceUntilIdle()

        viewModel.startReplaceExercise(viewModel.uiState.value.blocks.single().items[0])
        viewModel.selectExercise(replacement)
        advanceUntilIdle()

        val after = workoutRepo.exercisesFlowValue().sortedBy { it.positionInBlock }
        assertEquals(2, after.size)
        assertEquals(before[0].id, after[0].id)
        assertEquals(30L, after[0].exerciseId)
        assertEquals(before[0].blockId, after[0].blockId)
        assertEquals(0, after[0].positionInBlock)
        assertEquals(before[0].weight, after[0].weight, 0.0)
        assertEquals(before[0].minRepetitions, after[0].minRepetitions)
        assertEquals(before[0].maxRepetitions, after[0].maxRepetitions)
        assertEquals(before[0].notes, after[0].notes)
        assertEquals(before[1], after[1])
        assertTrue(workoutRepo.updatedBlocks.isEmpty())
    }

    @Test
    fun selectExercise_replaceSecondInBiSet_keepsSiblingIntact() = runTest(testDispatcher) {
        advanceUntilIdle()
        seedBiSet()
        advanceUntilIdle()

        val before = workoutRepo.exercisesFlowValue().sortedBy { it.positionInBlock }
        val replacement = Exercise(40, "Tríceps testa", "Tríceps", "Barra")
        exerciseRepo.emit(
            listOf(
                Exercise(1, "Rosca", "Bíceps", "Barra"),
                Exercise(2, "Tríceps", "Tríceps", "Corda"),
                replacement,
            ),
        )
        advanceUntilIdle()

        viewModel.startReplaceExercise(viewModel.uiState.value.blocks.single().items[1])
        viewModel.selectExercise(replacement)
        advanceUntilIdle()

        val after = workoutRepo.exercisesFlowValue().sortedBy { it.positionInBlock }
        assertEquals(before[0], after[0])
        assertEquals(40L, after[1].exerciseId)
        assertEquals(before[1].id, after[1].id)
        assertEquals(1, after[1].positionInBlock)
        assertEquals(before[1].weight, after[1].weight, 0.0)
        assertEquals(before[1].notes, after[1].notes)
    }

    @Test
    fun selectExercise_replaceEachTriSetPosition_preservesOthers() = runTest(testDispatcher) {
        advanceUntilIdle()
        seedTriSet()
        advanceUntilIdle()

        val replacements = listOf(
            Exercise(10, "X0", "Ombros", "Halteres"),
            Exercise(20, "X1", "Ombros", "Halteres"),
            Exercise(30, "X2", "Ombros", "Halteres"),
        )
        exerciseRepo.emit(
            listOf(
                Exercise(1, "A", "Ombros", "Halteres"),
                Exercise(2, "B", "Ombros", "Halteres"),
                Exercise(3, "C", "Ombros", "Halteres"),
            ) + replacements,
        )
        advanceUntilIdle()

        replacements.forEachIndexed { index, replacement ->
            val before = workoutRepo.exercisesFlowValue().sortedBy { it.positionInBlock }
            viewModel.startReplaceExercise(viewModel.uiState.value.blocks.single().items[index])
            viewModel.selectExercise(replacement)
            advanceUntilIdle()

            val after = workoutRepo.exercisesFlowValue().sortedBy { it.positionInBlock }
            assertEquals(3, after.size)
            assertEquals(replacement.id, after[index].exerciseId)
            assertEquals(before[index].id, after[index].id)
            assertEquals(before[index].blockId, after[index].blockId)
            assertEquals(index, after[index].positionInBlock)
            assertEquals(before[index].weight, after[index].weight, 0.0)
            assertEquals(before[index].minRepetitions, after[index].minRepetitions)
            assertEquals(before[index].maxRepetitions, after[index].maxRepetitions)
            assertEquals(before[index].notes, after[index].notes)
            before.forEachIndexed { otherIndex, other ->
                if (otherIndex != index) {
                    assertEquals(other, after[otherIndex])
                }
            }
        }
        assertTrue(workoutRepo.updatedBlocks.isEmpty())
        assertEquals(1, workoutRepo.blocksFlowValue().size)
        assertEquals(WorkoutBlockType.TRI_SET, workoutRepo.blocksFlowValue().single().type)
    }

    @Test
    fun selectExercise_afterReplaceDismiss_stillCreatesSingle() = runTest(testDispatcher) {
        advanceUntilIdle()
        seedSingleWithConfig()
        advanceUntilIdle()

        viewModel.startReplaceExercise(viewModel.uiState.value.blocks.single().items.single())
        viewModel.dismissExercisePicker()
        assertNull(viewModel.uiState.value.replacingExerciseRowId)

        val exercise = Exercise(50, "Remada", "Costas", "Barra")
        viewModel.chooseAddType(WorkoutBlockType.SINGLE)
        viewModel.selectExercise(exercise)
        assertEquals(exercise, viewModel.uiState.value.exerciseToConfigure?.exercise)
        assertTrue(workoutRepo.replacedExercises.isEmpty())
    }

    @Test
    fun showEditExercise_stillOpensConfigurationAfterReplaceAvailable() = runTest(testDispatcher) {
        advanceUntilIdle()
        seedBiSet()
        advanceUntilIdle()

        val detail = viewModel.uiState.value.blocks.single()
        viewModel.showEditExercise(detail, detail.items[0])
        assertEquals(detail.items[0], viewModel.uiState.value.exerciseToConfigure)
        assertEquals(detail.block, viewModel.uiState.value.configureBlock)
        assertNull(viewModel.uiState.value.replacingExerciseRowId)
    }

    private fun seedSingleWithConfig() {
        exerciseRepo.emit(listOf(Exercise(1, "Supino reto", "Peitoral", "Barra")))
        workoutRepo.emitBlocks(
            listOf(
                WorkoutBlock(
                    id = 5,
                    workoutId = 1,
                    position = 0,
                    type = WorkoutBlockType.SINGLE,
                    rounds = 4,
                    restSeconds = 90,
                ),
            ),
        )
        workoutRepo.emitExercises(
            listOf(
                WorkoutExercise(
                    id = 11,
                    blockId = 5,
                    exerciseId = 1,
                    positionInBlock = 0,
                    minRepetitions = 6,
                    maxRepetitions = 10,
                    weight = 80.0,
                    notes = "controle",
                ),
            ),
        )
    }

    private fun seedBiSet() {
        exerciseRepo.emit(
            listOf(
                Exercise(1, "Rosca", "Bíceps", "Barra"),
                Exercise(2, "Tríceps", "Tríceps", "Corda"),
            ),
        )
        workoutRepo.emitBlocks(
            listOf(
                WorkoutBlock(
                    id = 5,
                    workoutId = 1,
                    position = 0,
                    type = WorkoutBlockType.BI_SET,
                    rounds = 3,
                    restSeconds = 60,
                ),
            ),
        )
        workoutRepo.emitExercises(
            listOf(
                WorkoutExercise(
                    id = 11,
                    blockId = 5,
                    exerciseId = 1,
                    positionInBlock = 0,
                    minRepetitions = 8,
                    maxRepetitions = 12,
                    weight = 20.0,
                    notes = "primeiro",
                ),
                WorkoutExercise(
                    id = 12,
                    blockId = 5,
                    exerciseId = 2,
                    positionInBlock = 1,
                    minRepetitions = 10,
                    maxRepetitions = 12,
                    weight = 15.0,
                    notes = "segundo",
                ),
            ),
        )
    }

    private fun seedTriSet() {
        exerciseRepo.emit(
            listOf(
                Exercise(1, "A", "Ombros", "Halteres"),
                Exercise(2, "B", "Ombros", "Halteres"),
                Exercise(3, "C", "Ombros", "Halteres"),
            ),
        )
        workoutRepo.emitBlocks(
            listOf(
                WorkoutBlock(
                    id = 7,
                    workoutId = 1,
                    position = 0,
                    type = WorkoutBlockType.TRI_SET,
                    rounds = 3,
                    restSeconds = 90,
                ),
            ),
        )
        workoutRepo.emitExercises(
            listOf(
                WorkoutExercise(
                    id = 21,
                    blockId = 7,
                    exerciseId = 1,
                    positionInBlock = 0,
                    minRepetitions = 10,
                    maxRepetitions = 12,
                    weight = 8.0,
                    notes = "a",
                ),
                WorkoutExercise(
                    id = 22,
                    blockId = 7,
                    exerciseId = 2,
                    positionInBlock = 1,
                    minRepetitions = 8,
                    maxRepetitions = 10,
                    weight = 12.0,
                    notes = "b",
                ),
                WorkoutExercise(
                    id = 23,
                    blockId = 7,
                    exerciseId = 3,
                    positionInBlock = 2,
                    minRepetitions = 12,
                    maxRepetitions = 15,
                    weight = 6.0,
                    notes = "c",
                ),
            ),
        )
    }

    private fun seedThreeSingles() {
        exerciseRepo.emit(
            listOf(
                Exercise(1, "A", "Peito", "Barra"),
                Exercise(2, "B", "Costas", "Barra"),
                Exercise(3, "C", "Pernas", "Barra"),
            ),
        )
        workoutRepo.emitBlocks(
            listOf(
                WorkoutBlock(id = 10, workoutId = 1, position = 0, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 60),
                WorkoutBlock(id = 20, workoutId = 1, position = 1, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 60),
                WorkoutBlock(id = 30, workoutId = 1, position = 2, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 60),
            ),
        )
        workoutRepo.emitExercises(
            listOf(
                WorkoutExercise(id = 1, blockId = 10, exerciseId = 1, positionInBlock = 0, minRepetitions = 8, maxRepetitions = 12, weight = 0.0),
                WorkoutExercise(id = 2, blockId = 20, exerciseId = 2, positionInBlock = 0, minRepetitions = 8, maxRepetitions = 12, weight = 0.0),
                WorkoutExercise(id = 3, blockId = 30, exerciseId = 3, positionInBlock = 0, minRepetitions = 8, maxRepetitions = 12, weight = 0.0),
            ),
        )
    }
}

private class FakeWorkoutRepository : WorkoutRepository {
    private val workoutFlow = MutableStateFlow<Workout?>(null)
    private val blocksFlow = MutableStateFlow<List<WorkoutBlock>>(emptyList())
    private val exercisesFlow = MutableStateFlow<List<WorkoutExercise>>(emptyList())
    val addedBlocks = mutableListOf<Pair<WorkoutBlock, List<WorkoutExercise>>>()
    val removedBlockIds = mutableListOf<Long>()
    val updatedExercises = mutableListOf<WorkoutExercise>()
    val updatedBlocks = mutableListOf<WorkoutBlock>()
    val replacedExercises = mutableListOf<Pair<Long, Long>>()
    var lastPositionUpdate: Map<Long, Int>? = null
    val duplicatedBlockIds = mutableListOf<Long>()
    private var nextBlockId = 100L

    fun emitWorkout(workout: Workout?) { workoutFlow.value = workout }
    fun emitBlocks(blocks: List<WorkoutBlock>) { blocksFlow.value = blocks }
    fun emitExercises(exercises: List<WorkoutExercise>) { exercisesFlow.value = exercises }
    fun exercisesFlowValue(): List<WorkoutExercise> = exercisesFlow.value
    fun blocksFlowValue(): List<WorkoutBlock> = blocksFlow.value

    override fun getAll() = flowOf(emptyList<Workout>())
    override fun getById(id: Long) = workoutFlow
    override fun observeWorkoutIdsWithExercises() = exercisesFlow.map { exercises ->
        val blockIds = exercises.map { it.blockId }.toSet()
        blocksFlow.value.filter { it.id in blockIds }.map { it.workoutId }.toSet()
    }
    override suspend fun save(workout: Workout) = 1L
    override suspend fun update(workout: Workout) {}
    override suspend fun delete(workout: Workout) {}
    override suspend fun updateWorkoutPositions(positions: Map<Long, Int>) = Unit
    override fun getBlocks(workoutId: Long) = blocksFlow
    override fun getExercisesForWorkout(workoutId: Long) = exercisesFlow
    override suspend fun addBlock(block: WorkoutBlock, exercises: List<WorkoutExercise>): Long {
        addedBlocks += block to exercises
        val id = (addedBlocks.size).toLong()
        val position = blocksFlow.value.size
        blocksFlow.value = blocksFlow.value + block.copy(id = id, position = position)
        exercisesFlow.value = exercisesFlow.value + exercises.mapIndexed { i, e -> e.copy(id = i + 1L, blockId = id) }
        return id
    }
    override suspend fun duplicateBlock(blockId: Long): Long {
        duplicatedBlockIds += blockId
        val original = blocksFlow.value.first { it.id == blockId }
        val sourceExercises = exercisesFlow.value.filter { it.blockId == blockId }.sortedBy { it.positionInBlock }
        val shifted = blocksFlow.value.map { block ->
            if (block.position > original.position) block.copy(position = block.position + 1) else block
        }
        val newId = ++nextBlockId
        val copy = original.copy(id = newId, position = original.position + 1)
        blocksFlow.value = (shifted + copy).sortedBy { it.position }
        val baseExerciseId = (exercisesFlow.value.maxOfOrNull { it.id } ?: 0L) + 1
        exercisesFlow.value = exercisesFlow.value + sourceExercises.mapIndexed { index, exercise ->
            exercise.copy(id = baseExerciseId + index, blockId = newId)
        }
        return newId
    }
    override suspend fun updateBlock(block: WorkoutBlock) {
        updatedBlocks += block
        blocksFlow.value = blocksFlow.value.map { if (it.id == block.id) block else it }
    }
    override suspend fun updateBlockExercise(exercise: WorkoutExercise) {
        updatedExercises += exercise
        exercisesFlow.value = exercisesFlow.value.map { if (it.id == exercise.id) exercise else it }
    }
    override suspend fun replaceBlockExercise(exerciseRowId: Long, newCatalogueExerciseId: Long) {
        replacedExercises += exerciseRowId to newCatalogueExerciseId
        exercisesFlow.value = exercisesFlow.value.map { exercise ->
            if (exercise.id == exerciseRowId) exercise.copy(exerciseId = newCatalogueExerciseId) else exercise
        }
    }
    override suspend fun removeBlock(blockId: Long) {
        removedBlockIds += blockId
        val remaining = blocksFlow.value.filter { it.id != blockId }.sortedBy { it.position }
        blocksFlow.value = remaining.mapIndexed { index, block -> block.copy(position = index) }
        exercisesFlow.value = exercisesFlow.value.filter { it.blockId != blockId }
    }
    override suspend fun updateBlockPositions(positions: Map<Long, Int>) {
        lastPositionUpdate = positions
        blocksFlow.value = blocksFlow.value
            .map { block -> positions[block.id]?.let { block.copy(position = it) } ?: block }
            .sortedBy { it.position }
    }
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

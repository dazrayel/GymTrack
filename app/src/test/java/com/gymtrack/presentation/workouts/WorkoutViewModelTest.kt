package com.gymtrack.presentation.workouts

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutBlock
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class WorkoutViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeWorkoutRepository
    private lateinit var viewModel: WorkoutViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeWorkoutRepository()
        viewModel = WorkoutViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Initial state ────────────────────────────────────────────────────────

    // Note: initialState_isLoading_isTrue is not tested here because WorkoutViewModel has no
    // debounce — with UnconfinedTestDispatcher the init coroutine completes synchronously
    // before the test assertion runs. Loading transition is covered by afterLoad_* tests.

    @Test
    fun initialState_workouts_isEmpty() {
        assertTrue(viewModel.uiState.value.workouts.isEmpty())
    }

    @Test
    fun initialState_error_isNull() {
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun initialState_dialogs_areHidden() {
        val state = viewModel.uiState.value
        assertFalse(state.showAddEditDialog)
        assertFalse(state.showDeleteConfirmation)
        assertNull(state.workoutToEdit)
        assertNull(state.workoutToDelete)
    }

    // ─── Loading ──────────────────────────────────────────────────────────────

    @Test
    fun afterLoad_emptyRepository_isLoadingFalse() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun afterLoad_emptyRepository_workoutsIsEmpty() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.workouts.isEmpty())
    }

    @Test
    fun afterLoad_repositoryHasWorkouts_workoutsPopulated() = runTest(testDispatcher) {
        fakeRepository.emit(
            listOf(
                Workout(1, "Push Day", "Chest and shoulders"),
                Workout(2, "Pull Day", "Back and biceps"),
            ),
        )
        advanceUntilIdle()

        val workouts = viewModel.uiState.value.workouts
        assertEquals(2, workouts.size)
        assertEquals("Pull Day", workouts[0].name)
        assertEquals("Push Day", workouts[1].name)
    }

    @Test
    fun afterLoad_repositoryEmits_isLoadingFalse() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(Workout(1, "Leg Day", "")))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun whenRepositoryThrows_errorIsSet() = runTest(testDispatcher) {
        fakeRepository.shouldThrowOnGetAll = true
        val vm = WorkoutViewModel(fakeRepository)
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    // ─── Create workout ───────────────────────────────────────────────────────

    @Test
    fun saveWorkout_newWorkout_addsToList() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.saveWorkout("Leg Day", "Squats and lunges")
        advanceUntilIdle()

        val workouts = viewModel.uiState.value.workouts
        assertEquals(1, workouts.size)
        assertEquals("Leg Day", workouts[0].name)
        assertEquals("Squats and lunges", workouts[0].description)
    }

    @Test
    fun saveWorkout_newWorkout_dismissesDialog() = runTest(testDispatcher) {
        viewModel.showAddDialog()
        viewModel.saveWorkout("Leg Day", "")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showAddEditDialog)
        assertNull(viewModel.uiState.value.workoutToEdit)
    }

    @Test
    fun saveWorkout_trimsWhitespace() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.saveWorkout("  Push Day  ", "  Chest and shoulders  ")
        advanceUntilIdle()

        val workout = viewModel.uiState.value.workouts.first()
        assertEquals("Push Day", workout.name)
        assertEquals("Chest and shoulders", workout.description)
    }

    @Test
    fun saveWorkout_emptyDescription_savesCorrectly() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.saveWorkout("Full Body", "")
        advanceUntilIdle()

        val workout = viewModel.uiState.value.workouts.first()
        assertEquals("Full Body", workout.name)
        assertEquals("", workout.description)
    }

    @Test
    fun saveWorkout_repositoryThrows_setsError() = runTest(testDispatcher) {
        fakeRepository.shouldThrowOnSave = true
        advanceUntilIdle()

        viewModel.showAddDialog()
        viewModel.saveWorkout("Leg Day", "")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }

    // ─── Edit workout ─────────────────────────────────────────────────────────

    @Test
    fun saveWorkout_editExisting_updatesWorkout() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(Workout(1, "Push Day", "Chest")))
        advanceUntilIdle()

        val original = viewModel.uiState.value.workouts.first()
        viewModel.showEditDialog(original)
        viewModel.saveWorkout("Push Day Updated", "Chest and shoulders")
        advanceUntilIdle()

        val workouts = viewModel.uiState.value.workouts
        assertEquals(1, workouts.size)
        assertEquals("Push Day Updated", workouts[0].name)
        assertEquals("Chest and shoulders", workouts[0].description)
    }

    @Test
    fun saveWorkout_editExisting_doesNotRemoveExercises() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(Workout(1, "Push Day", "Chest")))
        advanceUntilIdle()

        val original = viewModel.uiState.value.workouts.first()
        viewModel.showEditDialog(original)
        viewModel.saveWorkout("Push Day Updated", "Chest")
        advanceUntilIdle()

        assertEquals(0, fakeRepository.removeExerciseCalls)
        assertEquals(0, fakeRepository.removeExerciseByIdCalls)
    }

    @Test
    fun saveWorkout_editExisting_preservesId() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(Workout(42, "Pull Day", "Back")))
        advanceUntilIdle()

        val original = viewModel.uiState.value.workouts.first()
        viewModel.showEditDialog(original)
        viewModel.saveWorkout("Pull Day v2", "Back and biceps")
        advanceUntilIdle()

        assertEquals(42L, viewModel.uiState.value.workouts.first().id)
    }

    @Test
    fun saveWorkout_editExisting_dismissesDialog() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(Workout(1, "Push Day", "")))
        advanceUntilIdle()

        val original = viewModel.uiState.value.workouts.first()
        viewModel.showEditDialog(original)
        viewModel.saveWorkout("Push Day Updated", "")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showAddEditDialog)
        assertNull(viewModel.uiState.value.workoutToEdit)
    }

    @Test
    fun saveWorkout_editRepositoryThrows_setsError() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(Workout(1, "Push Day", "")))
        advanceUntilIdle()
        fakeRepository.shouldThrowOnUpdate = true

        val original = viewModel.uiState.value.workouts.first()
        viewModel.showEditDialog(original)
        viewModel.saveWorkout("Push Day Updated", "")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }

    // ─── Delete workout ───────────────────────────────────────────────────────

    @Test
    fun confirmDelete_removesWorkoutFromList() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(Workout(1, "Push Day", "")))
        advanceUntilIdle()

        val workout = viewModel.uiState.value.workouts.first()
        viewModel.showDeleteConfirmation(workout)
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.workouts.isEmpty())
    }

    @Test
    fun confirmDelete_dismissesConfirmationDialog() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(Workout(1, "Push Day", "")))
        advanceUntilIdle()

        val workout = viewModel.uiState.value.workouts.first()
        viewModel.showDeleteConfirmation(workout)
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertNull(viewModel.uiState.value.workoutToDelete)
    }

    @Test
    fun confirmDelete_noWorkoutSelected_doesNothing() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(Workout(1, "Push Day", "")))
        advanceUntilIdle()

        viewModel.confirmDelete()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.workouts.size)
    }

    @Test
    fun confirmDelete_repositoryThrows_setsErrorAndDismissesDialog() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(Workout(1, "Push Day", "")))
        advanceUntilIdle()
        fakeRepository.shouldThrowOnDelete = true

        val workout = viewModel.uiState.value.workouts.first()
        viewModel.showDeleteConfirmation(workout)
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
    }

    // ─── Dialog state management ──────────────────────────────────────────────

    @Test
    fun showAddDialog_setsShowAddEditDialogTrue() {
        viewModel.showAddDialog()
        assertTrue(viewModel.uiState.value.showAddEditDialog)
    }

    @Test
    fun showAddDialog_setsWorkoutToEditNull() {
        viewModel.showAddDialog()
        assertNull(viewModel.uiState.value.workoutToEdit)
    }

    @Test
    fun showEditDialog_setsShowAddEditDialogTrue() {
        val workout = Workout(1, "Push Day", "")
        viewModel.showEditDialog(workout)
        assertTrue(viewModel.uiState.value.showAddEditDialog)
    }

    @Test
    fun showEditDialog_setsWorkoutToEdit() {
        val workout = Workout(1, "Push Day", "")
        viewModel.showEditDialog(workout)
        assertEquals(workout, viewModel.uiState.value.workoutToEdit)
    }

    @Test
    fun dismissDialog_hidesAddEditDialog() {
        viewModel.showAddDialog()
        viewModel.dismissDialog()
        assertFalse(viewModel.uiState.value.showAddEditDialog)
    }

    @Test
    fun dismissDialog_clearsWorkoutToEdit() {
        val workout = Workout(1, "Push Day", "")
        viewModel.showEditDialog(workout)
        viewModel.dismissDialog()
        assertNull(viewModel.uiState.value.workoutToEdit)
    }

    @Test
    fun showDeleteConfirmation_setsShowDeleteConfirmationTrue() {
        val workout = Workout(1, "Push Day", "")
        viewModel.showDeleteConfirmation(workout)
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
    }

    @Test
    fun showDeleteConfirmation_setsWorkoutToDelete() {
        val workout = Workout(1, "Push Day", "")
        viewModel.showDeleteConfirmation(workout)
        assertEquals(workout, viewModel.uiState.value.workoutToDelete)
    }

    @Test
    fun dismissDeleteConfirmation_hidesDialog() {
        val workout = Workout(1, "Push Day", "")
        viewModel.showDeleteConfirmation(workout)
        viewModel.dismissDeleteConfirmation()
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertNull(viewModel.uiState.value.workoutToDelete)
    }

    // ─── Error handling ───────────────────────────────────────────────────────

    @Test
    fun clearError_setsErrorToNull() = runTest(testDispatcher) {
        fakeRepository.shouldThrowOnSave = true
        advanceUntilIdle()

        viewModel.saveWorkout("Push Day", "")
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun clearError_whenNoError_remainsNull() {
        assertNull(viewModel.uiState.value.error)
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }
}

// ─── Fake repository ─────────────────────────────────────────────────────────

private class FakeWorkoutRepository : WorkoutRepository {

    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())

    var shouldThrowOnGetAll = false
    var shouldThrowOnSave = false
    var shouldThrowOnUpdate = false
    var shouldThrowOnDelete = false
    var removeExerciseCalls = 0
    var removeExerciseByIdCalls = 0

    // Mirrors real DAO behavior: ORDER BY name ASC
    fun emit(workouts: List<Workout>) {
        _workouts.value = workouts.sortedBy { it.name }
    }

    // Throws inside the Flow so the ViewModel's .catch {} can intercept it,
    // matching the real behaviour where failures surface as Flow errors.
    override fun getAll(): Flow<List<Workout>> {
        if (shouldThrowOnGetAll) return flow { throw RuntimeException("Simulated getAll error") }
        return _workouts
    }

    override fun getById(id: Long): Flow<Workout?> =
        _workouts.map { list -> list.find { it.id == id } }

    override suspend fun save(workout: Workout): Long {
        if (shouldThrowOnSave) throw RuntimeException("Simulated save error")
        val newId = (_workouts.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val saved = workout.copy(id = newId)
        _workouts.value = (_workouts.value + saved).sortedBy { it.name }
        return newId
    }

    override suspend fun update(workout: Workout) {
        if (shouldThrowOnUpdate) throw RuntimeException("Simulated update error")
        _workouts.value = (_workouts.value.filter { it.id != workout.id } + workout)
            .sortedBy { it.name }
    }

    override suspend fun delete(workout: Workout) {
        if (shouldThrowOnDelete) throw RuntimeException("Simulated delete error")
        _workouts.value = _workouts.value.filter { it.id != workout.id }
    }

    // WorkoutExercise operations — not used by WorkoutViewModel at this stage

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
    override suspend fun removeExercise(workoutExercise: WorkoutExercise) {
        removeExerciseCalls++
    }
    override suspend fun removeExerciseById(id: Long) {
        removeExerciseByIdCalls++
    }
    override suspend fun updateExercisePositions(positions: Map<Long, Int>) = Unit
}

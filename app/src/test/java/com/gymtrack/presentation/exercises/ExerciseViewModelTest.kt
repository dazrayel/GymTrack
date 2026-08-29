package com.gymtrack.presentation.exercises

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.repository.ExerciseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class ExerciseViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeExerciseRepository
    private lateinit var viewModel: ExerciseViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeExerciseRepository()
        viewModel = ExerciseViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── Initial state ────────────────────────────────────────────────────────

    @Test
    fun initialState_isLoading_isTrue() {
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun initialState_exercises_isEmpty() {
        assertTrue(viewModel.uiState.value.exercises.isEmpty())
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
        assertNull(state.exerciseToEdit)
        assertNull(state.exerciseToDelete)
    }

    // ─── Loading state after debounce ─────────────────────────────────────────

    @Test
    fun afterDebounce_emptyRepository_isLoadingFalse() = runTest(testDispatcher) {
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun afterDebounce_emptyRepository_exercisesIsEmpty() = runTest(testDispatcher) {
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.exercises.isEmpty())
    }

    @Test
    fun afterDebounce_repositoryHasExercises_exercisesPopulated() = runTest(testDispatcher) {
        fakeRepository.emit(
            listOf(
                Exercise(1, "Push-up", "Chest", "Bodyweight"),
                Exercise(2, "Squat", "Legs", "Barbell"),
            ),
        )
        advanceUntilIdle()

        val exercises = viewModel.uiState.value.exercises
        assertEquals(2, exercises.size)
        assertEquals("Push-up", exercises[0].name)
        assertEquals("Squat", exercises[1].name)
    }

    @Test
    fun afterDebounce_repositoryEmits_isLoadingFalse() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(Exercise(1, "Deadlift", "Back", "Barbell")))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    // ─── Save exercise ────────────────────────────────────────────────────────

    @Test
    fun saveExercise_newExercise_addsToList() = runTest(testDispatcher) {
        advanceUntilIdle() // initial load

        viewModel.saveExercise("Bench Press", "Chest", "Barbell")
        advanceUntilIdle()

        val exercises = viewModel.uiState.value.exercises
        assertEquals(1, exercises.size)
        assertEquals("Bench Press", exercises[0].name)
        assertEquals("Chest", exercises[0].muscleGroup)
        assertEquals("Barbell", exercises[0].equipmentType)
    }

    @Test
    fun saveExercise_newExercise_dismissesDialog() = runTest(testDispatcher) {
        viewModel.showAddDialog()
        viewModel.saveExercise("Bench Press", "Chest", "Barbell")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showAddEditDialog)
        assertNull(viewModel.uiState.value.exerciseToEdit)
    }

    @Test
    fun saveExercise_trimsWhitespace() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.saveExercise("  Pull-up  ", "  Back  ", "  Bodyweight  ")
        advanceUntilIdle()

        val exercise = viewModel.uiState.value.exercises.first()
        assertEquals("Pull-up", exercise.name)
        assertEquals("Back", exercise.muscleGroup)
        assertEquals("Bodyweight", exercise.equipmentType)
    }

    @Test
    fun saveExercise_editExisting_updatesExercise() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(Exercise(1, "Squat", "Legs", "Barbell")))
        advanceUntilIdle()

        val original = viewModel.uiState.value.exercises.first()
        viewModel.showEditDialog(original)
        viewModel.saveExercise("Front Squat", "Legs", "Barbell")
        advanceUntilIdle()

        val exercises = viewModel.uiState.value.exercises
        assertEquals(1, exercises.size)
        assertEquals("Front Squat", exercises[0].name)
        assertEquals(1L, exercises[0].id)
    }

    @Test
    fun saveExercise_editExisting_preservesId() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(Exercise(42, "Deadlift", "Back", "Barbell")))
        advanceUntilIdle()

        val original = viewModel.uiState.value.exercises.first()
        viewModel.showEditDialog(original)
        viewModel.saveExercise("Romanian Deadlift", "Hamstrings", "Barbell")
        advanceUntilIdle()

        assertEquals(42L, viewModel.uiState.value.exercises.first().id)
    }

    // ─── Delete exercise ──────────────────────────────────────────────────────

    @Test
    fun confirmDelete_removesExerciseFromList() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(Exercise(1, "Push-up", "Chest", "Bodyweight")))
        advanceUntilIdle()

        val exercise = viewModel.uiState.value.exercises.first()
        viewModel.showDeleteConfirmation(exercise)
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.exercises.isEmpty())
    }

    @Test
    fun confirmDelete_dismissesConfirmationDialog() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(Exercise(1, "Push-up", "Chest", "Bodyweight")))
        advanceUntilIdle()

        val exercise = viewModel.uiState.value.exercises.first()
        viewModel.showDeleteConfirmation(exercise)
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertNull(viewModel.uiState.value.exerciseToDelete)
    }

    @Test
    fun confirmDelete_noExerciseSelected_doesNothing() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(Exercise(1, "Push-up", "Chest", "Bodyweight")))
        advanceUntilIdle()

        viewModel.confirmDelete() // no exerciseToDelete set
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.exercises.size)
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    @Test
    fun onSearchQueryChange_updatesSearchQueryInState() {
        viewModel.onSearchQueryChange("push")

        assertEquals("push", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun onSearchQueryChange_filtersExercisesAfterDebounce() = runTest(testDispatcher) {
        fakeRepository.emit(
            listOf(
                Exercise(1, "Push-up", "Chest", "Bodyweight"),
                Exercise(2, "Squat", "Legs", "Barbell"),
            ),
        )
        advanceUntilIdle()

        viewModel.onSearchQueryChange("push")
        advanceUntilIdle()

        val exercises = viewModel.uiState.value.exercises
        assertEquals(1, exercises.size)
        assertEquals("Push-up", exercises[0].name)
    }

    @Test
    fun onSearchQueryChange_emptyQuery_showsAll() = runTest(testDispatcher) {
        fakeRepository.emit(
            listOf(
                Exercise(1, "Push-up", "Chest", "Bodyweight"),
                Exercise(2, "Squat", "Legs", "Barbell"),
            ),
        )
        advanceUntilIdle()

        viewModel.onSearchQueryChange("push")
        advanceUntilIdle()
        viewModel.onSearchQueryChange("")
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.exercises.size)
    }

    // ─── Dialog state management ──────────────────────────────────────────────

    @Test
    fun showAddDialog_setsShowAddEditDialogTrue() {
        viewModel.showAddDialog()

        assertTrue(viewModel.uiState.value.showAddEditDialog)
    }

    @Test
    fun showAddDialog_setsExerciseToEditNull() {
        viewModel.showAddDialog()

        assertNull(viewModel.uiState.value.exerciseToEdit)
    }

    @Test
    fun showEditDialog_setsShowAddEditDialogTrue() {
        val exercise = Exercise(1, "Squat", "Legs", "Barbell")
        viewModel.showEditDialog(exercise)

        assertTrue(viewModel.uiState.value.showAddEditDialog)
    }

    @Test
    fun showEditDialog_setsExerciseToEdit() {
        val exercise = Exercise(1, "Squat", "Legs", "Barbell")
        viewModel.showEditDialog(exercise)

        assertEquals(exercise, viewModel.uiState.value.exerciseToEdit)
    }

    @Test
    fun dismissDialog_hidesAddEditDialog() {
        viewModel.showAddDialog()
        viewModel.dismissDialog()

        assertFalse(viewModel.uiState.value.showAddEditDialog)
    }

    @Test
    fun dismissDialog_clearsExerciseToEdit() {
        val exercise = Exercise(1, "Squat", "Legs", "Barbell")
        viewModel.showEditDialog(exercise)
        viewModel.dismissDialog()

        assertNull(viewModel.uiState.value.exerciseToEdit)
    }

    @Test
    fun showDeleteConfirmation_setsShowDeleteConfirmationTrue() {
        val exercise = Exercise(1, "Squat", "Legs", "Barbell")
        viewModel.showDeleteConfirmation(exercise)

        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
    }

    @Test
    fun showDeleteConfirmation_setsExerciseToDelete() {
        val exercise = Exercise(1, "Squat", "Legs", "Barbell")
        viewModel.showDeleteConfirmation(exercise)

        assertEquals(exercise, viewModel.uiState.value.exerciseToDelete)
    }

    @Test
    fun dismissDeleteConfirmation_hidesDialog() {
        val exercise = Exercise(1, "Squat", "Legs", "Barbell")
        viewModel.showDeleteConfirmation(exercise)
        viewModel.dismissDeleteConfirmation()

        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertNull(viewModel.uiState.value.exerciseToDelete)
    }

    // ─── Error handling ───────────────────────────────────────────────────────

    @Test
    fun saveExercise_repositoryThrows_setsErrorState() = runTest(testDispatcher) {
        fakeRepository.shouldThrowOnSave = true
        advanceUntilIdle()

        viewModel.showAddDialog()
        viewModel.saveExercise("Push-up", "Chest", "Bodyweight")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun clearError_setsErrorToNull() = runTest(testDispatcher) {
        fakeRepository.shouldThrowOnSave = true
        advanceUntilIdle()

        viewModel.saveExercise("Push-up", "Chest", "Bodyweight")
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

    @Test
    fun confirmDelete_repositoryThrows_setsErrorState() = runTest(testDispatcher) {
        fakeRepository.emit(listOf(Exercise(1, "Push-up", "Chest", "Bodyweight")))
        advanceUntilIdle()
        fakeRepository.shouldThrowOnDelete = true

        val exercise = viewModel.uiState.value.exercises.first()
        viewModel.showDeleteConfirmation(exercise)
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }
}

// ─── Fake repository ─────────────────────────────────────────────────────────

private class FakeExerciseRepository : ExerciseRepository {

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())

    var shouldThrowOnSave = false
    var shouldThrowOnDelete = false

    fun emit(exercises: List<Exercise>) {
        _exercises.value = exercises
    }

    override fun getAll(): Flow<List<Exercise>> = _exercises

    override fun getById(id: Long): Flow<Exercise?> =
        _exercises.map { list -> list.find { it.id == id } }

    override fun search(query: String): Flow<List<Exercise>> =
        _exercises.map { list ->
            list.filter { it.name.contains(query, ignoreCase = true) }
        }

    override suspend fun save(exercise: Exercise): Long {
        if (shouldThrowOnSave) throw RuntimeException("Simulated save error")
        val newId = if (exercise.id == 0L) {
            (_exercises.value.maxOfOrNull { it.id } ?: 0L) + 1L
        } else {
            exercise.id
        }
        val saved = exercise.copy(id = newId)
        _exercises.value = (_exercises.value.filter { it.id != newId } + saved)
            .sortedBy { it.name }
        return newId
    }

    override suspend fun delete(exercise: Exercise) {
        if (shouldThrowOnDelete) throw RuntimeException("Simulated delete error")
        _exercises.value = _exercises.value.filter { it.id != exercise.id }
    }
}

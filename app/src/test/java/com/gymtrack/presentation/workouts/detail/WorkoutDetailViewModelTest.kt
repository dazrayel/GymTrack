package com.gymtrack.presentation.workouts.detail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.model.WorkoutExerciseDetail
import com.gymtrack.domain.repository.ExerciseRepository
import com.gymtrack.domain.repository.WorkoutRepository
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

private const val WORKOUT_ID = 1L

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class WorkoutDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeWorkoutRepo: FakeWorkoutDetailRepository
    private lateinit var fakeExerciseRepo: FakeExerciseDetailRepository
    private lateinit var viewModel: WorkoutDetailViewModel

    private val workout = Workout(id = WORKOUT_ID, name = "Treino A", description = "Peito")
    private val exercise1 = Exercise(id = 10L, name = "Supino", muscleGroup = "Peitoral", equipmentType = "Barra")
    private val exercise2 = Exercise(id = 20L, name = "Agachamento", muscleGroup = "Quadríceps", equipmentType = "Barra")

    private val workoutExercise1 = WorkoutExercise(
        id = 1L, workoutId = WORKOUT_ID, exerciseId = 10L,
        position = 0, sets = 4, minRepetitions = 8, maxRepetitions = 12,
        weight = 60.0, restSeconds = 90,
    )
    private val workoutExercise2 = WorkoutExercise(
        id = 2L, workoutId = WORKOUT_ID, exerciseId = 20L,
        position = 1, sets = 3, minRepetitions = 10, maxRepetitions = 15,
        weight = 80.0, restSeconds = 120,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeWorkoutRepo = FakeWorkoutDetailRepository()
        fakeExerciseRepo = FakeExerciseDetailRepository()
        buildViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() {
        viewModel = WorkoutDetailViewModel(
            workoutRepository = fakeWorkoutRepo,
            exerciseRepository = fakeExerciseRepo,
            savedStateHandle = SavedStateHandle(mapOf("workoutId" to WORKOUT_ID)),
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Loading & state
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun afterLoad_workoutIsSet() {
        fakeWorkoutRepo.emitWorkout(workout)
        assertEquals(workout, viewModel.uiState.value.workout)
    }

    @Test
    fun afterLoad_isLoadingIsFalse() {
        fakeWorkoutRepo.emitWorkout(workout)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun missingWorkout_doesNotCrash_andWorkoutIsNull() {
        // No workout emitted → null
        fakeWorkoutRepo.emitWorkout(null)
        assertNull(viewModel.uiState.value.workout)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun initialState_exercisesIsEmpty() {
        assertTrue(viewModel.uiState.value.exercises.isEmpty())
    }

    @Test
    fun initialState_allDialogsDismissed() {
        val state = viewModel.uiState.value
        assertFalse(state.showEditWorkoutDialog)
        assertFalse(state.showExercisePicker)
        assertNull(state.exerciseToConfigure)
        assertFalse(state.showDeleteConfirmation)
        assertNull(state.exerciseToDelete)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Join / combine
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun exercises_areJoinedWithCatalogue() {
        fakeExerciseRepo.emit(listOf(exercise1, exercise2))
        fakeWorkoutRepo.emitExercises(listOf(workoutExercise1, workoutExercise2))

        val details = viewModel.uiState.value.exercises
        assertEquals(2, details.size)
        assertEquals("Supino", details[0].exercise.name)
        assertEquals("Agachamento", details[1].exercise.name)
    }

    @Test
    fun exercises_areSortedByPosition() {
        fakeExerciseRepo.emit(listOf(exercise1, exercise2))
        // Emit in reverse order to confirm sorting is applied
        fakeWorkoutRepo.emitExercises(listOf(workoutExercise2, workoutExercise1))

        val details = viewModel.uiState.value.exercises
        assertEquals(0, details[0].position)
        assertEquals(1, details[1].position)
    }

    @Test
    fun exercises_missingCatalogueEntry_isSkipped() {
        // exercise1 is NOT in catalogue
        fakeExerciseRepo.emit(listOf(exercise2))
        fakeWorkoutRepo.emitExercises(listOf(workoutExercise1, workoutExercise2))

        val details = viewModel.uiState.value.exercises
        assertEquals(1, details.size)
        assertEquals("Agachamento", details[0].exercise.name)
    }

    @Test
    fun exercises_correctValuesAreMapped() {
        fakeExerciseRepo.emit(listOf(exercise1))
        fakeWorkoutRepo.emitExercises(listOf(workoutExercise1))

        val detail = viewModel.uiState.value.exercises[0]
        assertEquals(4, detail.sets)
        assertEquals(8, detail.minRepetitions)
        assertEquals(12, detail.maxRepetitions)
        assertEquals(60.0, detail.weight, 0.001)
        assertEquals(90, detail.restSeconds)
        assertEquals("Peitoral", detail.exercise.muscleGroup)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Error handling
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun workoutLoadError_setsError() = runTest(testDispatcher) {
        fakeWorkoutRepo.shouldThrowOnGetById = true
        buildViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun exercisesLoadError_setsError() = runTest(testDispatcher) {
        fakeWorkoutRepo.shouldThrowOnGetExercises = true
        buildViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun clearError_removesError() = runTest(testDispatcher) {
        fakeWorkoutRepo.shouldThrowOnGetById = true
        buildViewModel()
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

    // ──────────────────────────────────────────────────────────────────────────
    // Exercise picker
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun showExercisePicker_setsFlag() {
        viewModel.showExercisePicker()
        assertTrue(viewModel.uiState.value.showExercisePicker)
    }

    @Test
    fun dismissExercisePicker_clearsFlag() {
        viewModel.showExercisePicker()
        viewModel.dismissExercisePicker()
        assertFalse(viewModel.uiState.value.showExercisePicker)
    }

    @Test
    fun selectExercise_createsDetailWithDefaults() {
        viewModel.selectExercise(exercise1)

        val configured = viewModel.uiState.value.exerciseToConfigure
        assertNotNull(configured)
        assertEquals(0L, configured!!.workoutExercise.id)
        assertEquals(exercise1, configured.exercise)
        assertEquals(WORKOUT_ID, configured.workoutExercise.workoutId)
        assertEquals(exercise1.id, configured.workoutExercise.exerciseId)
    }

    @Test
    fun selectExercise_positionIsCurrentListSize() {
        fakeExerciseRepo.emit(listOf(exercise1, exercise2))
        fakeWorkoutRepo.emitExercises(listOf(workoutExercise1, workoutExercise2))

        viewModel.selectExercise(exercise1)

        val configured = viewModel.uiState.value.exerciseToConfigure
        assertEquals(2, configured!!.position)
    }

    @Test
    fun selectExercise_closesPicker() {
        viewModel.showExercisePicker()
        viewModel.selectExercise(exercise1)
        assertFalse(viewModel.uiState.value.showExercisePicker)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Exercise configuration — add new
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun saveExerciseConfiguration_newExercise_callsAddExercise() = runTest(testDispatcher) {
        viewModel.selectExercise(exercise1)

        viewModel.saveExerciseConfiguration(
            sets = 4, minRepetitions = 8, maxRepetitions = 12,
            weight = 60.0, restSeconds = 90, notes = "nota",
        )
        advanceUntilIdle()

        assertEquals(1, fakeWorkoutRepo.addedExercises.size)
        assertEquals(4, fakeWorkoutRepo.addedExercises[0].sets)
        assertEquals("nota", fakeWorkoutRepo.addedExercises[0].notes)
    }

    @Test
    fun saveExerciseConfiguration_newExercise_clearsConfigure() = runTest(testDispatcher) {
        viewModel.selectExercise(exercise1)

        viewModel.saveExerciseConfiguration(
            sets = 3, minRepetitions = 10, maxRepetitions = 15,
            weight = 0.0, restSeconds = 60, notes = "",
        )
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.exerciseToConfigure)
    }

    @Test
    fun saveExerciseConfiguration_newExercise_notesAreTrimmed() = runTest(testDispatcher) {
        viewModel.selectExercise(exercise1)

        viewModel.saveExerciseConfiguration(
            sets = 3, minRepetitions = 8, maxRepetitions = 12,
            weight = 0.0, restSeconds = 60, notes = "  note  ",
        )
        advanceUntilIdle()

        assertEquals("note", fakeWorkoutRepo.addedExercises[0].notes)
    }

    @Test
    fun dismissExerciseConfiguration_doesNotPersist() = runTest(testDispatcher) {
        viewModel.selectExercise(exercise1)
        viewModel.dismissExerciseConfiguration()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.exerciseToConfigure)
        assertTrue(fakeWorkoutRepo.addedExercises.isEmpty())
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Exercise configuration — edit existing
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun showEditExercise_setsExerciseToConfigure() {
        fakeExerciseRepo.emit(listOf(exercise1))
        fakeWorkoutRepo.emitExercises(listOf(workoutExercise1))

        val detail = viewModel.uiState.value.exercises[0]
        viewModel.showEditExercise(detail)

        assertEquals(detail, viewModel.uiState.value.exerciseToConfigure)
    }

    @Test
    fun saveExerciseConfiguration_existingExercise_callsUpdateExercise() = runTest(testDispatcher) {
        fakeExerciseRepo.emit(listOf(exercise1))
        fakeWorkoutRepo.emitExercises(listOf(workoutExercise1))

        val detail = viewModel.uiState.value.exercises[0]
        viewModel.showEditExercise(detail)

        viewModel.saveExerciseConfiguration(
            sets = 5, minRepetitions = 6, maxRepetitions = 10,
            weight = 70.0, restSeconds = 120, notes = "atualizado",
        )
        advanceUntilIdle()

        assertEquals(1, fakeWorkoutRepo.updatedExercises.size)
        assertEquals(workoutExercise1.id, fakeWorkoutRepo.updatedExercises[0].id)
        assertEquals(5, fakeWorkoutRepo.updatedExercises[0].sets)
    }

    @Test
    fun saveExerciseConfiguration_existingExercise_preservesId() = runTest(testDispatcher) {
        fakeExerciseRepo.emit(listOf(exercise1))
        fakeWorkoutRepo.emitExercises(listOf(workoutExercise1))

        val detail = viewModel.uiState.value.exercises[0]
        viewModel.showEditExercise(detail)
        viewModel.saveExerciseConfiguration(
            sets = 3, minRepetitions = 8, maxRepetitions = 12,
            weight = 60.0, restSeconds = 90, notes = "",
        )
        advanceUntilIdle()

        assertEquals(workoutExercise1.id, fakeWorkoutRepo.updatedExercises[0].id)
    }

    @Test
    fun saveExerciseConfiguration_onError_setsError() = runTest(testDispatcher) {
        fakeWorkoutRepo.shouldThrowOnAddExercise = true
        viewModel.selectExercise(exercise1)

        viewModel.saveExerciseConfiguration(
            sets = 3, minRepetitions = 8, maxRepetitions = 12,
            weight = 0.0, restSeconds = 60, notes = "",
        )
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Exercise deletion
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun showDeleteConfirmation_setsExerciseToDelete() {
        fakeExerciseRepo.emit(listOf(exercise1))
        fakeWorkoutRepo.emitExercises(listOf(workoutExercise1))

        val detail = viewModel.uiState.value.exercises[0]
        viewModel.showDeleteConfirmation(detail)

        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
        assertEquals(detail, viewModel.uiState.value.exerciseToDelete)
    }

    @Test
    fun dismissDeleteConfirmation_clearsState() {
        fakeExerciseRepo.emit(listOf(exercise1))
        fakeWorkoutRepo.emitExercises(listOf(workoutExercise1))

        val detail = viewModel.uiState.value.exercises[0]
        viewModel.showDeleteConfirmation(detail)
        viewModel.dismissDeleteConfirmation()

        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertNull(viewModel.uiState.value.exerciseToDelete)
    }

    @Test
    fun confirmDelete_callsRemoveExerciseById() = runTest(testDispatcher) {
        fakeExerciseRepo.emit(listOf(exercise1))
        fakeWorkoutRepo.emitExercises(listOf(workoutExercise1))

        val detail = viewModel.uiState.value.exercises[0]
        viewModel.showDeleteConfirmation(detail)
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertTrue(fakeWorkoutRepo.removedExerciseIds.contains(workoutExercise1.id))
    }

    @Test
    fun confirmDelete_clearsDialog() = runTest(testDispatcher) {
        fakeExerciseRepo.emit(listOf(exercise1))
        fakeWorkoutRepo.emitExercises(listOf(workoutExercise1))

        val detail = viewModel.uiState.value.exercises[0]
        viewModel.showDeleteConfirmation(detail)
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertNull(viewModel.uiState.value.exerciseToDelete)
    }

    @Test
    fun confirmDelete_onError_setsError() = runTest(testDispatcher) {
        fakeExerciseRepo.emit(listOf(exercise1))
        fakeWorkoutRepo.emitExercises(listOf(workoutExercise1))
        fakeWorkoutRepo.shouldThrowOnDelete = true

        val detail = viewModel.uiState.value.exercises[0]
        viewModel.showDeleteConfirmation(detail)
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun confirmDelete_whenNoExerciseSelected_doesNothing() = runTest(testDispatcher) {
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertTrue(fakeWorkoutRepo.removedExerciseIds.isEmpty())
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Workout editing
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun showEditWorkoutDialog_setsFlag() {
        viewModel.showEditWorkoutDialog()
        assertTrue(viewModel.uiState.value.showEditWorkoutDialog)
    }

    @Test
    fun dismissEditWorkoutDialog_clearsFlag() {
        viewModel.showEditWorkoutDialog()
        viewModel.dismissEditWorkoutDialog()
        assertFalse(viewModel.uiState.value.showEditWorkoutDialog)
    }

    @Test
    fun saveWorkout_callsUpdate() = runTest(testDispatcher) {
        fakeWorkoutRepo.emitWorkout(workout)

        viewModel.saveWorkout("Treino B", "Costas")
        advanceUntilIdle()

        val updated = fakeWorkoutRepo.updatedWorkouts.last()
        assertEquals("Treino B", updated.name)
        assertEquals("Costas", updated.description)
        assertEquals(WORKOUT_ID, updated.id)
    }

    @Test
    fun saveWorkout_trimsFields() = runTest(testDispatcher) {
        fakeWorkoutRepo.emitWorkout(workout)

        viewModel.saveWorkout("  Treino B  ", "  Costas  ")
        advanceUntilIdle()

        val updated = fakeWorkoutRepo.updatedWorkouts.last()
        assertEquals("Treino B", updated.name)
        assertEquals("Costas", updated.description)
    }

    @Test
    fun saveWorkout_closesDialog() = runTest(testDispatcher) {
        fakeWorkoutRepo.emitWorkout(workout)
        viewModel.showEditWorkoutDialog()

        viewModel.saveWorkout("Treino B", "Costas")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showEditWorkoutDialog)
    }

    @Test
    fun saveWorkout_whenNoWorkoutLoaded_doesNothing() = runTest(testDispatcher) {
        viewModel.saveWorkout("Treino B", "Costas")
        advanceUntilIdle()

        assertTrue(fakeWorkoutRepo.updatedWorkouts.isEmpty())
    }

    @Test
    fun saveWorkout_onError_setsError() = runTest(testDispatcher) {
        fakeWorkoutRepo.emitWorkout(workout)
        fakeWorkoutRepo.shouldThrowOnUpdate = true

        viewModel.saveWorkout("Treino B", "Costas")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Fake repositories
// ──────────────────────────────────────────────────────────────────────────────

private class FakeWorkoutDetailRepository : WorkoutRepository {

    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    private val _exercises = MutableStateFlow<List<WorkoutExercise>>(emptyList())

    var shouldThrowOnGetById = false
    var shouldThrowOnGetExercises = false
    var shouldThrowOnAddExercise = false
    var shouldThrowOnUpdate = false
    var shouldThrowOnDelete = false

    val addedExercises = mutableListOf<WorkoutExercise>()
    val updatedExercises = mutableListOf<WorkoutExercise>()
    val removedExerciseIds = mutableListOf<Long>()
    val updatedWorkouts = mutableListOf<Workout>()

    fun emitWorkout(workout: Workout?) {
        _workouts.value = if (workout != null) listOf(workout) else emptyList()
    }

    fun emitExercises(exercises: List<WorkoutExercise>) {
        _exercises.value = exercises
    }

    override fun getAll(): Flow<List<Workout>> = _workouts

    override fun getById(id: Long): Flow<Workout?> {
        if (shouldThrowOnGetById) return flow { throw RuntimeException("getById error") }
        return _workouts.map { list -> list.find { it.id == id } }
    }

    override fun getExercises(workoutId: Long): Flow<List<WorkoutExercise>> {
        if (shouldThrowOnGetExercises) return flow { throw RuntimeException("getExercises error") }
        return _exercises.map { list -> list.filter { it.workoutId == workoutId } }
    }

    override suspend fun save(workout: Workout): Long = 0L
    override suspend fun update(workout: Workout) {
        if (shouldThrowOnUpdate) throw RuntimeException("update error")
        updatedWorkouts += workout
    }
    override suspend fun delete(workout: Workout) = Unit

    override suspend fun addExercise(workoutExercise: WorkoutExercise): Long {
        if (shouldThrowOnAddExercise) throw RuntimeException("addExercise error")
        addedExercises += workoutExercise
        return addedExercises.size.toLong()
    }

    override suspend fun updateExercise(workoutExercise: WorkoutExercise) {
        updatedExercises += workoutExercise
    }

    override suspend fun removeExercise(workoutExercise: WorkoutExercise) = Unit

    override suspend fun removeExerciseById(id: Long) {
        if (shouldThrowOnDelete) throw RuntimeException("removeExerciseById error")
        removedExerciseIds += id
    }
}

private class FakeExerciseDetailRepository : ExerciseRepository {

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())

    fun emit(exercises: List<Exercise>) {
        _exercises.value = exercises
    }

    override fun getAll(): Flow<List<Exercise>> = _exercises
    override fun getById(id: Long): Flow<Exercise?> = emptyFlow()
    override fun search(query: String): Flow<List<Exercise>> = emptyFlow()
    override suspend fun save(exercise: Exercise): Long = 0L
    override suspend fun delete(exercise: Exercise) = Unit
}

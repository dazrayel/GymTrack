package com.gymtrack.presentation.planning

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.gymtrack.domain.model.WeeklyWorkoutPlan
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutBlock
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.repository.WeeklyPlanRepository
import com.gymtrack.domain.repository.WorkoutRepository
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class WeeklyPlanningViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var planRepository: FakeWeeklyPlanRepository
    private lateinit var workoutRepository: FakeWorkoutRepository
    private lateinit var viewModel: WeeklyPlanningViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        planRepository = FakeWeeklyPlanRepository()
        workoutRepository = FakeWorkoutRepository()
        viewModel = WeeklyPlanningViewModel(planRepository, workoutRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isLoading_beforeEmission() {
        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.days.isEmpty())
    }

    @Test
    fun afterLoad_emptyPlans_allDaysEmpty() = runTest(testDispatcher) {
        setWorkouts(Workout(1L, "A"))
        planRepository.emit(emptyList())
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(7, state.days.size)
        assertEquals(0, state.plannedDayCount)
        assertTrue(state.days.none { it.hasPlan })
        assertTrue(state.hasWorkouts)
    }

    @Test
    fun afterLoad_noWorkouts_exposesEmptyCatalog() = runTest(testDispatcher) {
        setWorkouts()
        planRepository.emit(emptyList())
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasWorkouts)
        assertTrue(viewModel.uiState.value.availableWorkouts.isEmpty())
    }

    @Test
    fun assignWorkout_toMonday_persistsSinglePlan() = runTest(testDispatcher) {
        setWorkouts(Workout(10L, "Peito"))
        planRepository.emit(emptyList())
        advanceUntilIdle()

        viewModel.openAddPicker(DayOfWeek.MONDAY)
        viewModel.assignWorkout(10L)
        advanceUntilIdle()

        assertEquals(listOf(DayOfWeek.MONDAY to 10L), planRepository.assignments)
        assertNull(viewModel.uiState.value.pickerDay)
        val monday = viewModel.uiState.value.days.first { it.dayOfWeek == DayOfWeek.MONDAY }
        assertEquals(10L, monday.plannedWorkoutId)
        assertEquals("Peito", monday.plannedWorkoutName)
    }

    @Test
    fun assignWorkout_toFriday_works() = runTest(testDispatcher) {
        setWorkouts(Workout(2L, "Pernas"))
        planRepository.emit(emptyList())
        advanceUntilIdle()

        viewModel.openAddPicker(DayOfWeek.FRIDAY)
        viewModel.assignWorkout(2L)
        advanceUntilIdle()

        assertEquals(
            2L,
            viewModel.uiState.value.days.first { it.dayOfWeek == DayOfWeek.FRIDAY }.plannedWorkoutId,
        )
    }

    @Test
    fun assignWorkout_allDays_keepsOnePerDay() = runTest(testDispatcher) {
        val workouts = DayOfWeek.entries.mapIndexed { index, day ->
            Workout(index + 1L, "W$day")
        }
        setWorkouts(*workouts.toTypedArray())
        planRepository.emit(emptyList())
        advanceUntilIdle()

        DayOfWeek.entries.forEachIndexed { index, day ->
            viewModel.openAddPicker(day)
            viewModel.assignWorkout(index + 1L)
            advanceUntilIdle()
        }

        assertEquals(7, planRepository.currentPlans.size)
        assertEquals(7, viewModel.uiState.value.plannedDayCount)
        assertEquals(7, viewModel.uiState.value.days.count { it.hasPlan })
    }

    @Test
    fun changeWorkout_replacesSameDay() = runTest(testDispatcher) {
        setWorkouts(Workout(1L, "A"), Workout(2L, "B"))
        planRepository.emit(listOf(WeeklyWorkoutPlan(DayOfWeek.MONDAY, 1L, "A")))
        advanceUntilIdle()

        viewModel.openAddPicker(DayOfWeek.MONDAY)
        viewModel.assignWorkout(2L)
        advanceUntilIdle()

        assertEquals(1, planRepository.currentPlans.size)
        assertEquals(2L, planRepository.currentPlans.single().workoutId)
        assertEquals(
            2L,
            viewModel.uiState.value.days.first { it.dayOfWeek == DayOfWeek.MONDAY }.plannedWorkoutId,
        )
    }

    @Test
    fun assignSameWorkoutAgain_doesNotDuplicateDay() = runTest(testDispatcher) {
        setWorkouts(Workout(1L, "A"))
        planRepository.emit(emptyList())
        advanceUntilIdle()

        viewModel.openAddPicker(DayOfWeek.TUESDAY)
        viewModel.assignWorkout(1L)
        advanceUntilIdle()
        viewModel.openAddPicker(DayOfWeek.TUESDAY)
        viewModel.assignWorkout(1L)
        advanceUntilIdle()

        assertEquals(1, planRepository.currentPlans.size)
        assertEquals(1, viewModel.uiState.value.days.count { it.hasPlan })
    }

    @Test
    fun removePlan_clearsDay() = runTest(testDispatcher) {
        setWorkouts(Workout(1L, "A"))
        planRepository.emit(listOf(WeeklyWorkoutPlan(DayOfWeek.WEDNESDAY, 1L, "A")))
        advanceUntilIdle()

        viewModel.openDayActions(DayOfWeek.WEDNESDAY)
        viewModel.removePlan()
        advanceUntilIdle()

        assertTrue(planRepository.currentPlans.isEmpty())
        assertFalse(viewModel.uiState.value.days.first { it.dayOfWeek == DayOfWeek.WEDNESDAY }.hasPlan)
        assertNull(viewModel.uiState.value.dayActionsDay)
    }

    @Test
    fun reloadAfterAssign_keepsPlan() = runTest(testDispatcher) {
        setWorkouts(Workout(5L, "Costas"))
        planRepository.emit(emptyList())
        advanceUntilIdle()

        viewModel.openAddPicker(DayOfWeek.THURSDAY)
        viewModel.assignWorkout(5L)
        advanceUntilIdle()

        val recreated = WeeklyPlanningViewModel(planRepository, workoutRepository)
        // SharedFlow has no replay — re-emit current snapshot for the new collector.
        planRepository.reemit()
        workoutRepository.reemit()
        advanceUntilIdle()

        assertEquals(
            5L,
            recreated.uiState.value.days.first { it.dayOfWeek == DayOfWeek.THURSDAY }.plannedWorkoutId,
        )
    }

    @Test
    fun planningChanges_doNotDeleteWorkouts() = runTest(testDispatcher) {
        setWorkouts(Workout(1L, "A"))
        planRepository.emit(emptyList())
        advanceUntilIdle()

        viewModel.openAddPicker(DayOfWeek.MONDAY)
        viewModel.assignWorkout(1L)
        advanceUntilIdle()
        viewModel.openDayActions(DayOfWeek.MONDAY)
        viewModel.removePlan()
        advanceUntilIdle()

        assertEquals(0, workoutRepository.deleteCount)
        assertEquals(1, workoutRepository.current.size)
    }

    @Test
    fun whenObserveThrows_errorIsSet() = runTest(testDispatcher) {
        planRepository.shouldThrow = true
        val vm = WeeklyPlanningViewModel(planRepository, workoutRepository)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("Simulated plan error", vm.uiState.value.error)
    }

    private suspend fun setWorkouts(vararg workouts: Workout) {
        workoutRepository.emit(workouts.toList())
        planRepository.workoutNames = workouts.associate { it.id to it.name }
    }
}

private class FakeWeeklyPlanRepository : WeeklyPlanRepository {
    private val plans = MutableSharedFlow<List<WeeklyWorkoutPlan>>(extraBufferCapacity = 1)
    private var latest: List<WeeklyWorkoutPlan> = emptyList()
    var workoutNames: Map<Long, String> = emptyMap()
    val currentPlans get() = latest
    val assignments = mutableListOf<Pair<DayOfWeek, Long>>()
    var shouldThrow = false

    suspend fun emit(items: List<WeeklyWorkoutPlan>) {
        latest = items
        plans.emit(items)
    }

    suspend fun reemit() {
        plans.emit(latest)
    }

    override fun observePlans(): Flow<List<WeeklyWorkoutPlan>> {
        if (shouldThrow) return flow { throw RuntimeException("Simulated plan error") }
        return plans
    }

    override suspend fun assignWorkout(dayOfWeek: DayOfWeek, workoutId: Long) {
        assignments += dayOfWeek to workoutId
        val name = workoutNames[workoutId] ?: "Treino $workoutId"
        latest = latest.filterNot { it.dayOfWeek == dayOfWeek } +
            WeeklyWorkoutPlan(dayOfWeek, workoutId, name)
        plans.emit(latest)
    }

    override suspend fun clearDay(dayOfWeek: DayOfWeek) {
        latest = latest.filterNot { plan -> plan.dayOfWeek == dayOfWeek }
        plans.emit(latest)
    }
}

private class FakeWorkoutRepository : WorkoutRepository {
    private val workouts = MutableSharedFlow<List<Workout>>(extraBufferCapacity = 1)
    private var latest: List<Workout> = emptyList()
    var deleteCount = 0
    val current get() = latest

    suspend fun emit(items: List<Workout>) {
        latest = items
        workouts.emit(items)
    }

    suspend fun reemit() {
        workouts.emit(latest)
    }

    override fun getAll(): Flow<List<Workout>> = workouts

    override fun getById(id: Long): Flow<Workout?> =
        workouts.map { list -> list.firstOrNull { it.id == id } }

    override fun observeWorkoutIdsWithExercises(): Flow<Set<Long>> = emptyFlow()

    override suspend fun save(workout: Workout): Long = workout.id

    override suspend fun update(workout: Workout) = Unit

    override suspend fun delete(workout: Workout) {
        deleteCount++
    }

    override fun getBlocks(workoutId: Long): Flow<List<WorkoutBlock>> = emptyFlow()

    override fun getExercisesForWorkout(workoutId: Long): Flow<List<WorkoutExercise>> = emptyFlow()

    override suspend fun addBlock(block: WorkoutBlock, exercises: List<WorkoutExercise>): Long = 0L

    override suspend fun duplicateBlock(blockId: Long): Long = 0L

    override suspend fun updateBlock(block: WorkoutBlock) = Unit

    override suspend fun updateBlockExercise(exercise: WorkoutExercise) = Unit

    override suspend fun replaceBlockExercise(exerciseRowId: Long, newCatalogueExerciseId: Long) = Unit

    override suspend fun removeBlock(blockId: Long) = Unit

    override suspend fun updateBlockPositions(positions: Map<Long, Int>) = Unit

    override suspend fun addExercise(workoutExercise: WorkoutExercise): Long = 0L

    override suspend fun updateExercise(workoutExercise: WorkoutExercise) = Unit

    override suspend fun removeExercise(workoutExercise: WorkoutExercise) = Unit

    override suspend fun removeExerciseById(id: Long) = Unit

    override suspend fun updateExercisePositions(positions: Map<Long, Int>) = Unit
}

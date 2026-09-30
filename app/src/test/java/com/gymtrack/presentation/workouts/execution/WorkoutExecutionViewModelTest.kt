package com.gymtrack.presentation.workouts.execution

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.gymtrack.R
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.StartSessionResult
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSessionExerciseStatus
import com.gymtrack.domain.model.WorkoutSessionStatus
import com.gymtrack.domain.model.WorkoutSet
import com.gymtrack.domain.repository.ExerciseRepository
import com.gymtrack.domain.repository.WorkoutSessionRepository
import com.gymtrack.domain.time.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

private const val SESSION_ID = 100L

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class WorkoutExecutionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeRepo: FakeExecutionSessionRepository
    private lateinit var fakeExercises: FakeExerciseRepository
    private lateinit var clock: FakeTimeProvider
    private lateinit var draftStore: WorkoutExecutionDraftStore
    private lateinit var viewModel: WorkoutExecutionViewModel

    private val session = WorkoutSession(
        id = SESSION_ID,
        workoutId = 1L,
        workoutName = "Push Day",
        workoutDescription = "Peito",
        startedAtMillis = 1_000L,
        status = WorkoutSessionStatus.IN_PROGRESS,
    )

    private val exerciseA = WorkoutSessionExercise(
        id = 10L,
        sessionId = SESSION_ID,
        exerciseId = 1L,
        position = 0,
        exerciseName = "Supino",
        muscleGroup = "Peito",
        equipmentType = "Barra",
        plannedSets = 3,
        minRepetitions = 8,
        maxRepetitions = 12,
        plannedWeight = 60.0,
        restSeconds = 0,
        blockPosition = 0,
        positionInBlock = 0,
    )

    private val exerciseB = WorkoutSessionExercise(
        id = 20L,
        sessionId = SESSION_ID,
        exerciseId = 2L,
        position = 1,
        exerciseName = "Crucifixo",
        muscleGroup = "Peito",
        equipmentType = "Halteres",
        plannedSets = 2,
        minRepetitions = 10,
        maxRepetitions = 12,
        plannedWeight = 14.5,
        restSeconds = 0,
        notes = "Controle a descida",
        blockPosition = 1,
        positionInBlock = 0,
    )

    private val exerciseC = WorkoutSessionExercise(
        id = 30L,
        sessionId = SESSION_ID,
        exerciseId = 3L,
        position = 2,
        exerciseName = "Tríceps",
        muscleGroup = "Braços",
        equipmentType = "Polia",
        plannedSets = 3,
        minRepetitions = 8,
        maxRepetitions = 12,
        plannedWeight = 20.0,
        restSeconds = 0,
        blockPosition = 2,
        positionInBlock = 0,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        clock = FakeTimeProvider(now = 1_000_000L)
        fakeRepo = FakeExecutionSessionRepository(clock)
        fakeExercises = FakeExerciseRepository()
        draftStore = WorkoutExecutionDraftStore()
        buildViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel() {
        viewModel = WorkoutExecutionViewModel(
            savedStateHandle = SavedStateHandle(mapOf("sessionId" to SESSION_ID)),
            workoutSessionRepository = fakeRepo,
            exerciseRepository = fakeExercises,
            timeProvider = clock,
            draftStore = draftStore,
        )
    }

    private fun emitSessionWithExercises() {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA, exerciseB))
    }

    @Test
    fun loadsSessionAndExercises() {
        emitSessionWithExercises()

        val state = viewModel.uiState.value
        assertEquals(session, state.session)
        assertEquals(listOf(exerciseA, exerciseB), state.exercises)
        assertFalse(state.isLoading)
    }

    @Test
    fun identifiesFirstPendingExercise() {
        emitSessionWithExercises()

        assertEquals(0, viewModel.uiState.value.currentExerciseIndex)
        assertEquals(exerciseA, viewModel.uiState.value.currentExercise)
    }

    @Test
    fun identifiesCurrentSetFromCompletedCount() {
        emitSessionWithExercises()
        fakeRepo.emitSets(exerciseA.id, listOf(set(exerciseA.id, 0)))

        assertEquals(0, viewModel.uiState.value.currentExerciseIndex)
        assertEquals(1, viewModel.uiState.value.currentSetIndex)
    }

    @Test
    fun skipsFullyCompletedExercise() {
        emitSessionWithExercises()
        fakeRepo.emitSets(
            exerciseA.id,
            listOf(set(exerciseA.id, 0), set(exerciseA.id, 1), set(exerciseA.id, 2)),
        )
        fakeRepo.emitSets(exerciseB.id, listOf(set(exerciseB.id, 0)))

        val state = viewModel.uiState.value
        assertEquals(1, state.currentExerciseIndex)
        assertEquals(1, state.currentSetIndex)
        assertEquals(exerciseB, state.currentExercise)
    }

    @Test
    fun completeCurrentSet_persistsSet() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.completeCurrentSet()

        assertEquals(1, fakeRepo.completeSetCalls.size)
        val call = fakeRepo.completeSetCalls.single()
        assertEquals(exerciseA.id, call.sessionExerciseId)
        assertEquals(8, call.reps)
        assertEquals(60.0, call.weight, 0.0)
    }

    @Test
    fun completeCurrentSet_acceptsRepsInsidePlannedRange() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.onRepsChanged("10")
        viewModel.completeCurrentSet()

        assertEquals(10, fakeRepo.completeSetCalls.single().reps)
        assertEquals(8, viewModel.uiState.value.exercises.single { it.id == exerciseA.id }.minRepetitions)
        assertEquals(12, viewModel.uiState.value.exercises.single { it.id == exerciseA.id }.maxRepetitions)
    }

    @Test
    fun completeCurrentSet_acceptsRepsBelowPlannedRange() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.onRepsChanged("6")
        viewModel.completeCurrentSet()

        assertEquals(6, fakeRepo.completeSetCalls.single().reps)
        assertEquals(8, exerciseA.minRepetitions)
        assertEquals(12, exerciseA.maxRepetitions)
    }

    @Test
    fun completeCurrentSet_acceptsRepsAbovePlannedRange() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.onRepsChanged("15")
        viewModel.completeCurrentSet()

        assertEquals(15, fakeRepo.completeSetCalls.single().reps)
        val snapshot = viewModel.uiState.value.exercises.single { it.id == exerciseA.id }
        assertEquals(8, snapshot.minRepetitions)
        assertEquals(12, snapshot.maxRepetitions)
    }

    @Test
    fun completeCurrentSet_setIndexStartsAtZero() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.completeCurrentSet()

        assertEquals(0, fakeRepo.completeSetCalls.single().setIndex)
    }

    @Test
    fun afterPersisting_advancesToNextSet() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.completeCurrentSet()

        val state = viewModel.uiState.value
        assertEquals(0, state.currentExerciseIndex)
        assertEquals(1, state.currentSetIndex)
        assertFalse(state.isWorkoutComplete)
    }

    @Test
    fun finishingExercise_advancesToNextExercise() = runTest(testDispatcher) {
        emitSessionWithExercises()
        fakeRepo.emitSets(
            exerciseA.id,
            listOf(set(exerciseA.id, 0), set(exerciseA.id, 1)),
        )

        assertEquals(0, viewModel.uiState.value.currentExerciseIndex)
        assertEquals(2, viewModel.uiState.value.currentSetIndex)

        viewModel.completeCurrentSet()

        val state = viewModel.uiState.value
        assertEquals(1, state.currentExerciseIndex)
        assertEquals(0, state.currentSetIndex)
        assertEquals(exerciseB, state.currentExercise)
        assertEquals("10", state.repsInput)
        assertEquals("14.5", state.weightInput)
    }

    @Test
    fun finishingAllSets_marksWorkoutComplete() = runTest(testDispatcher) {
        emitSessionWithExercises()
        fakeRepo.emitSets(
            exerciseA.id,
            listOf(set(exerciseA.id, 0), set(exerciseA.id, 1), set(exerciseA.id, 2)),
        )
        fakeRepo.emitSets(exerciseB.id, listOf(set(exerciseB.id, 0)))

        viewModel.completeCurrentSet()

        val state = viewModel.uiState.value
        assertTrue(state.isWorkoutComplete)
        assertEquals(0, state.currentExerciseIndex)
        assertNull(state.currentExercise)
    }

    @Test
    fun invalidReps_doesNotPersist() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.onRepsChanged("0")
        viewModel.completeCurrentSet()

        assertTrue(fakeRepo.completeSetCalls.isEmpty())
        assertEquals(R.string.validation_reps_min, viewModel.uiState.value.repsError)
        assertEquals(0, viewModel.uiState.value.currentSetIndex)
    }

    @Test
    fun invalidWeight_doesNotPersist() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.onWeightChanged("-10")
        viewModel.completeCurrentSet()

        assertTrue(fakeRepo.completeSetCalls.isEmpty())
        assertEquals(R.string.validation_weight_min, viewModel.uiState.value.weightError)
    }

    @Test
    fun nonNumericValues_doNotPersist() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.onRepsChanged("abc")
        viewModel.onWeightChanged("xyz")
        viewModel.completeCurrentSet()

        assertTrue(fakeRepo.completeSetCalls.isEmpty())
        assertEquals(R.string.validation_invalid_value, viewModel.uiState.value.repsError)
        assertEquals(R.string.validation_invalid_value, viewModel.uiState.value.weightError)
    }

    @Test
    fun repositoryError_setsError() = runTest(testDispatcher) {
        emitSessionWithExercises()
        fakeRepo.shouldThrowOnComplete = true
        viewModel.completeCurrentSet()

        assertEquals("completeSet error", viewModel.uiState.value.error)
        assertEquals(0, viewModel.uiState.value.currentSetIndex)
    }

    @Test
    fun completeCurrentSet_doesNotAlterOtherExercises() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.completeCurrentSet()

        assertEquals(1, viewModel.uiState.value.setsByExerciseId[exerciseA.id]?.size)
        assertTrue(viewModel.uiState.value.setsByExerciseId[exerciseB.id].orEmpty().isEmpty())
        assertEquals(exerciseA.id, fakeRepo.completeSetCalls.single().sessionExerciseId)
    }

    @Test
    fun openingExercise_prefillsPlannedRepsAndWeight() {
        emitSessionWithExercises()

        assertEquals("8", viewModel.uiState.value.repsInput)
        assertEquals("60", viewModel.uiState.value.weightInput)
    }

    @Test
    fun recreatingViewModel_restoresDraftForSameSessionAndExercise() {
        emitSessionWithExercises()
        viewModel.onRepsChanged("12")
        viewModel.onWeightChanged("80")

        buildViewModel()
        emitSessionWithExercises()

        assertEquals("12", viewModel.uiState.value.repsInput)
        assertEquals("80", viewModel.uiState.value.weightInput)
        assertEquals(exerciseA, viewModel.uiState.value.currentExercise)
    }

    @Test
    fun draft_overridesPlannedPrefill() {
        emitSessionWithExercises()
        viewModel.onRepsChanged("15")
        viewModel.onWeightChanged("72.5")

        buildViewModel()
        emitSessionWithExercises()

        assertEquals("15", viewModel.uiState.value.repsInput)
        assertEquals("72.5", viewModel.uiState.value.weightInput)
        assertEquals("8", exerciseA.minRepetitions.toString())
        assertEquals(60.0, exerciseA.plannedWeight, 0.0)
    }

    @Test
    fun completeCurrentSet_keepsDraftWhenMoreSetsRemain() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.onRepsChanged("12")
        viewModel.onWeightChanged("80")
        viewModel.completeCurrentSet()

        assertEquals(1, fakeRepo.completeSetCalls.size)
        assertEquals(12, fakeRepo.completeSetCalls.single().reps)
        assertEquals(80.0, fakeRepo.completeSetCalls.single().weight, 0.0)
        assertEquals(ExerciseInputDraft("12", "80"), draftStore.get(SESSION_ID, exerciseA.id))
        assertEquals("12", viewModel.uiState.value.repsInput)
        assertEquals("80", viewModel.uiState.value.weightInput)
    }

    @Test
    fun completeCurrentSet_clearsDraftWhenExerciseFinished() = runTest(testDispatcher) {
        emitSessionWithExercises()
        fakeRepo.emitSets(exerciseA.id, listOf(set(exerciseA.id, 0), set(exerciseA.id, 1)))
        viewModel.onRepsChanged("12")
        viewModel.onWeightChanged("80")
        viewModel.completeCurrentSet()

        assertNull(draftStore.get(SESSION_ID, exerciseA.id))
        assertEquals(exerciseB, viewModel.uiState.value.currentExercise)
        assertEquals(exerciseB.minRepetitions.toString(), viewModel.uiState.value.repsInput)
        assertEquals("14.5", viewModel.uiState.value.weightInput)
    }

    @Test
    fun afterRest_preservesDraftValuesForSameExercise() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA.copy(restSeconds = 90), exerciseB))
        viewModel.onRepsChanged("10")
        viewModel.onWeightChanged("20")
        viewModel.completeCurrentSet()
        assertEquals(WorkoutExecutionPhase.RESTING, viewModel.uiState.value.phase)

        // Simulate leaving and returning (new ViewModel) while resting.
        buildViewModel()
        assertEquals(WorkoutExecutionPhase.RESTING, viewModel.uiState.value.phase)

        viewModel.skipRest()
        assertEquals(WorkoutExecutionPhase.WORKING, viewModel.uiState.value.phase)
        assertEquals(exerciseA.id, viewModel.uiState.value.currentExercise?.id)
        assertEquals("10", viewModel.uiState.value.repsInput)
        assertEquals("20", viewModel.uiState.value.weightInput)
    }

    @Test
    fun afterRest_advancesToNextExerciseAndPrefillsItsDefaults() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(
            listOf(
                exerciseA.copy(plannedSets = 1, restSeconds = 90),
                exerciseB,
            ),
        )
        viewModel.onRepsChanged("10")
        viewModel.onWeightChanged("20")
        viewModel.completeCurrentSet()
        assertEquals(WorkoutExecutionPhase.RESTING, viewModel.uiState.value.phase)

        viewModel.skipRest()

        val state = viewModel.uiState.value
        assertEquals(WorkoutExecutionPhase.WORKING, state.phase)
        assertEquals(exerciseB.id, state.currentExercise?.id)
        assertEquals(exerciseB.exerciseName, state.currentExercise?.exerciseName)
        assertEquals(exerciseB.minRepetitions.toString(), state.repsInput)
        assertEquals("14.5", state.weightInput)
        assertEquals(exerciseB.id, state.inputsExerciseId)
    }

    @Test
    fun restBeepEvent_emittedOnceWhenRemainingHitsZero() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA.copy(restSeconds = 90), exerciseB))
        viewModel.completeCurrentSet()
        val endsAt = checkNotNull(viewModel.uiState.value.restEndsAtMillis)
        assertNull(viewModel.uiState.value.restBeepEvent)

        clock.now = endsAt
        // StateFlow skips equal values — nudge the session to re-apply progress at remaining=0.
        val resting = checkNotNull(viewModel.uiState.value.session)
        fakeRepo.emitSession(resting.copy(workoutDescription = resting.workoutDescription + " "))

        assertEquals(endsAt, viewModel.uiState.value.restBeepEvent)
        viewModel.consumeRestBeepEvent()
        assertNull(viewModel.uiState.value.restBeepEvent)

        // Staying at zero must not re-fire.
        val afterBeep = checkNotNull(viewModel.uiState.value.session)
        fakeRepo.emitSession(afterBeep.copy(workoutDescription = afterBeep.workoutDescription + " "))
        assertNull(viewModel.uiState.value.restBeepEvent)
    }

    @Test
    fun skipExercise_doesNotCreateSet_andAdvancesToNext() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.confirmSkip()

        assertTrue(fakeRepo.completeSetCalls.isEmpty())
        assertEquals(WorkoutSessionExerciseStatus.SKIPPED, viewModel.uiState.value.exercises[0].status)
        assertEquals(exerciseB, viewModel.uiState.value.currentExercise)
        assertEquals(0, viewModel.uiState.value.setsByExerciseId[exerciseA.id].orEmpty().size)
        assertEquals(0, viewModel.uiState.value.currentSetIndex)
        assertFalse(viewModel.uiState.value.isWorkoutComplete)
    }

    @Test
    fun skipExercise_keepsPartialSets() = runTest(testDispatcher) {
        emitSessionWithExercises()
        fakeRepo.emitSets(exerciseA.id, listOf(set(exerciseA.id, 0), set(exerciseA.id, 1)))
        viewModel.confirmSkip()

        assertEquals(2, viewModel.uiState.value.setsByExerciseId[exerciseA.id]?.size)
        assertEquals(WorkoutSessionExerciseStatus.SKIPPED, viewModel.uiState.value.exercises[0].status)
        assertEquals(exerciseB, viewModel.uiState.value.currentExercise)
        assertEquals(0, viewModel.uiState.value.currentSetIndex)
    }

    @Test
    fun resumeSkipped_startsAtNextPendingSet() = runTest(testDispatcher) {
        emitSessionWithExercises()
        fakeRepo.emitSets(exerciseA.id, listOf(set(exerciseA.id, 0), set(exerciseA.id, 1)))
        viewModel.confirmSkip()
        viewModel.resumeExercise(exerciseA.id)

        assertEquals(exerciseA.id, viewModel.uiState.value.currentExercise?.id)
        assertEquals(2, viewModel.uiState.value.currentSetIndex)
        assertEquals(WorkoutSessionExerciseStatus.IN_PROGRESS, viewModel.uiState.value.currentExercise?.status)
    }

    @Test
    fun resumeSkipped_withoutSets_startsAtFirstSet() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.confirmSkip()
        viewModel.resumeExercise(exerciseA.id)

        assertEquals(exerciseA.id, viewModel.uiState.value.currentExercise?.id)
        assertEquals(0, viewModel.uiState.value.currentSetIndex)
    }

    @Test
    fun skipThenCompleteNext_doesNotAutoReturnToSkipped() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA, exerciseB, exerciseC))
        viewModel.confirmSkip()
        fakeRepo.emitSets(exerciseB.id, listOf(set(exerciseB.id, 0), set(exerciseB.id, 1)))

        assertEquals(exerciseC.id, viewModel.uiState.value.currentExercise?.id)
        assertEquals(
            WorkoutSessionExerciseStatus.SKIPPED,
            viewModel.uiState.value.exercises.first { it.id == exerciseA.id }.status,
        )
        assertTrue(viewModel.uiState.value.pendingSkippedExercises.any { it.id == exerciseA.id })
    }

    @Test
    fun skipDoesNotMixDrafts() {
        emitSessionWithExercises()
        viewModel.onRepsChanged("12")
        viewModel.onWeightChanged("80")
        viewModel.confirmSkip()

        assertEquals("10", viewModel.uiState.value.repsInput)
        assertEquals("14.5", viewModel.uiState.value.weightInput)
        assertEquals("12", draftStore.get(SESSION_ID, exerciseA.id)?.repsInput)
        assertNull(draftStore.get(SESSION_ID, exerciseB.id))
    }

    @Test
    fun recreatingViewModel_keepsSkippedStatus() {
        emitSessionWithExercises()
        viewModel.confirmSkip()
        val persisted = listOf(
            exerciseA.copy(status = WorkoutSessionExerciseStatus.SKIPPED),
            exerciseB,
        )
        buildViewModel()
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(persisted)

        assertEquals(exerciseB.id, viewModel.uiState.value.currentExercise?.id)
        assertEquals(
            WorkoutSessionExerciseStatus.SKIPPED,
            viewModel.uiState.value.exercises.first { it.id == exerciseA.id }.status,
        )
        assertFalse(
            viewModel.uiState.value.exercises.any {
                it.status == WorkoutSessionExerciseStatus.COMPLETED && it.id == exerciseA.id
            },
        )
    }

    @Test
    fun selectPendingLaterExercise_makesItCurrent_withoutSkippingOthers() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA, exerciseB, exerciseC))

        viewModel.resumeExercise(exerciseC.id)

        val state = viewModel.uiState.value
        assertEquals(exerciseC.id, state.currentExercise?.id)
        assertEquals(WorkoutSessionExerciseStatus.IN_PROGRESS, state.currentExercise?.status)
        assertEquals(WorkoutSessionExerciseStatus.PENDING, state.exercises[0].status)
        assertEquals(WorkoutSessionExerciseStatus.PENDING, state.exercises[1].status)
        assertEquals(0, state.currentSetIndex)
        assertTrue(state.setsByExerciseId.values.all { it.isEmpty() })
        assertTrue(fakeRepo.completeSetCalls.isEmpty())
    }

    @Test
    fun selectPendingEarlierExercise_demotesPreviousInProgressToPending() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA, exerciseB, exerciseC))
        fakeRepo.emitSets(exerciseC.id, listOf(set(exerciseC.id, 0)))
        viewModel.resumeExercise(exerciseC.id)
        viewModel.resumeExercise(exerciseA.id)

        val state = viewModel.uiState.value
        assertEquals(exerciseA.id, state.currentExercise?.id)
        assertEquals(WorkoutSessionExerciseStatus.IN_PROGRESS, state.exercises[0].status)
        assertEquals(WorkoutSessionExerciseStatus.PENDING, state.exercises[2].status)
        assertEquals(1, state.setsByExerciseId[exerciseC.id]?.size)
        assertEquals(1, state.exercises.count { it.status == WorkoutSessionExerciseStatus.IN_PROGRESS })
    }

    @Test
    fun selectOtherExercise_keepsPartialSets_andResumeContinuesAtNextSet() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA, exerciseB, exerciseC))
        fakeRepo.emitSets(exerciseA.id, listOf(set(exerciseA.id, 0), set(exerciseA.id, 1)))

        viewModel.resumeExercise(exerciseC.id)
        assertEquals(2, viewModel.uiState.value.setsByExerciseId[exerciseA.id]?.size)
        assertEquals(exerciseC.id, viewModel.uiState.value.currentExercise?.id)

        viewModel.resumeExercise(exerciseA.id)
        assertEquals(exerciseA.id, viewModel.uiState.value.currentExercise?.id)
        assertEquals(2, viewModel.uiState.value.currentSetIndex)
        assertEquals(2, viewModel.uiState.value.setsByExerciseId[exerciseA.id]?.size)
    }

    @Test
    fun selectOtherExercise_preservesDraftOfPreviousExercise() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA, exerciseB, exerciseC))
        viewModel.onRepsChanged("12")
        viewModel.onWeightChanged("77")

        viewModel.resumeExercise(exerciseC.id)
        assertEquals("12", draftStore.get(SESSION_ID, exerciseA.id)?.repsInput)
        assertEquals("77", draftStore.get(SESSION_ID, exerciseA.id)?.weightInput)

        viewModel.resumeExercise(exerciseA.id)
        assertEquals("12", viewModel.uiState.value.repsInput)
        assertEquals("77", viewModel.uiState.value.weightInput)
    }

    @Test
    fun completedExercise_cannotBeSelectedAndHasNoSelectAction() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA, exerciseB, exerciseC))
        fakeRepo.emitSets(
            exerciseA.id,
            listOf(set(exerciseA.id, 0), set(exerciseA.id, 1), set(exerciseA.id, 2)),
        )

        val row = viewModel.uiState.value.sessionExerciseRows.first { it.exercise.id == exerciseA.id }
        assertTrue(row.isComplete)
        assertFalse(row.canSelectNow)
        assertEquals(SessionExerciseListStatus.COMPLETED, row.listStatus)

        viewModel.resumeExercise(exerciseA.id)
        assertEquals(exerciseB.id, viewModel.uiState.value.currentExercise?.id)
        assertEquals(3, viewModel.uiState.value.setsByExerciseId[exerciseA.id]?.size)
    }

    @Test
    fun selectSkippedExercise_marksInProgressAtFirstSet() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA, exerciseB, exerciseC))
        viewModel.confirmSkip()
        viewModel.resumeExercise(exerciseA.id)

        assertEquals(exerciseA.id, viewModel.uiState.value.currentExercise?.id)
        assertEquals(WorkoutSessionExerciseStatus.IN_PROGRESS, viewModel.uiState.value.currentExercise?.status)
        assertEquals(0, viewModel.uiState.value.currentSetIndex)
    }

    @Test
    fun recreatingViewModel_keepsManuallySelectedExercise() {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(
            listOf(
                exerciseA,
                exerciseB,
                exerciseC.copy(status = WorkoutSessionExerciseStatus.IN_PROGRESS),
            ),
        )
        assertEquals(exerciseC.id, viewModel.uiState.value.currentExercise?.id)

        buildViewModel()
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(
            listOf(
                exerciseA,
                exerciseB,
                exerciseC.copy(status = WorkoutSessionExerciseStatus.IN_PROGRESS),
            ),
        )

        assertEquals(exerciseC.id, viewModel.uiState.value.currentExercise?.id)
        assertEquals(
            WorkoutSessionExerciseStatus.IN_PROGRESS,
            viewModel.uiState.value.currentExercise?.status,
        )
    }

    @Test
    fun completingManuallySelectedExercise_returnsToFirstIncompletePending() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA, exerciseB, exerciseC))
        viewModel.resumeExercise(exerciseC.id)
        repeat(3) { viewModel.completeCurrentSet() }

        val state = viewModel.uiState.value
        assertEquals(WorkoutSessionExerciseStatus.COMPLETED, state.exercises[2].status)
        assertEquals(exerciseA.id, state.currentExercise?.id)
        assertEquals(WorkoutSessionExerciseStatus.PENDING, state.exercises[0].status)
        assertEquals(WorkoutSessionExerciseStatus.PENDING, state.exercises[1].status)
        assertFalse(state.exercises.any { it.status == WorkoutSessionExerciseStatus.SKIPPED })
    }

    @Test
    fun selectChain_aToCToBToA_neverSkipsOrCreatesSets() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA, exerciseB, exerciseC))
        assertTrue(
            viewModel.uiState.value.exercises.none {
                it.status == WorkoutSessionExerciseStatus.IN_PROGRESS
            },
        )

        viewModel.resumeExercise(exerciseC.id)
        var state = viewModel.uiState.value
        assertEquals(exerciseC.id, state.currentExercise?.id)
        assertEquals(WorkoutSessionExerciseStatus.IN_PROGRESS, state.exercises[2].status)
        assertEquals(WorkoutSessionExerciseStatus.PENDING, state.exercises[0].status)
        assertEquals(WorkoutSessionExerciseStatus.PENDING, state.exercises[1].status)
        assertEquals(1, state.exercises.count { it.status == WorkoutSessionExerciseStatus.IN_PROGRESS })
        assertFalse(state.exercises.any { it.status == WorkoutSessionExerciseStatus.SKIPPED })

        viewModel.resumeExercise(exerciseB.id)
        state = viewModel.uiState.value
        assertEquals(exerciseB.id, state.currentExercise?.id)
        assertEquals(WorkoutSessionExerciseStatus.IN_PROGRESS, state.exercises[1].status)
        assertEquals(WorkoutSessionExerciseStatus.PENDING, state.exercises[0].status)
        assertEquals(WorkoutSessionExerciseStatus.PENDING, state.exercises[2].status)
        assertEquals(1, state.exercises.count { it.status == WorkoutSessionExerciseStatus.IN_PROGRESS })

        viewModel.resumeExercise(exerciseA.id)
        state = viewModel.uiState.value
        assertEquals(exerciseA.id, state.currentExercise?.id)
        assertEquals(WorkoutSessionExerciseStatus.IN_PROGRESS, state.exercises[0].status)
        assertEquals(WorkoutSessionExerciseStatus.PENDING, state.exercises[1].status)
        assertEquals(WorkoutSessionExerciseStatus.PENDING, state.exercises[2].status)
        assertEquals(1, state.exercises.count { it.status == WorkoutSessionExerciseStatus.IN_PROGRESS })
        assertFalse(state.exercises.any { it.status == WorkoutSessionExerciseStatus.SKIPPED })
        assertTrue(fakeRepo.completeSetCalls.isEmpty())
        assertTrue(state.setsByExerciseId.values.all { it.isEmpty() })
    }

    @Test
    fun selectSwap_preservesPartialSetsOnBothExercises() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA, exerciseB, exerciseC))
        fakeRepo.emitSets(exerciseA.id, listOf(set(exerciseA.id, 0)))
        fakeRepo.emitSets(exerciseC.id, listOf(set(exerciseC.id, 0), set(exerciseC.id, 1)))
        assertEquals(exerciseA.id, viewModel.uiState.value.currentExercise?.id)

        viewModel.resumeExercise(exerciseC.id)
        var state = viewModel.uiState.value
        assertEquals(exerciseC.id, state.currentExercise?.id)
        assertEquals(1, state.setsByExerciseId[exerciseA.id]?.size)
        assertEquals(2, state.setsByExerciseId[exerciseC.id]?.size)
        assertEquals(2, state.currentSetIndex)
        assertTrue(fakeRepo.completeSetCalls.isEmpty())

        viewModel.resumeExercise(exerciseA.id)
        state = viewModel.uiState.value
        assertEquals(exerciseA.id, state.currentExercise?.id)
        assertEquals(1, state.currentSetIndex)
        assertEquals(1, state.setsByExerciseId[exerciseA.id]?.size)
        assertEquals(2, state.setsByExerciseId[exerciseC.id]?.size)

        viewModel.resumeExercise(exerciseC.id)
        state = viewModel.uiState.value
        assertEquals(exerciseC.id, state.currentExercise?.id)
        assertEquals(2, state.currentSetIndex)
        assertEquals(2, state.setsByExerciseId[exerciseC.id]?.size)
        assertEquals(1, state.setsByExerciseId[exerciseA.id]?.size)
        assertTrue(fakeRepo.completeSetCalls.isEmpty())
    }

    @Test
    fun skippedPartialExercise_resumeThenCompleteRemaining_marksCompleted() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA, exerciseB, exerciseC))
        fakeRepo.emitSets(exerciseA.id, listOf(set(exerciseA.id, 0)))
        viewModel.confirmSkip()
        assertEquals(WorkoutSessionExerciseStatus.SKIPPED, viewModel.uiState.value.exercises[0].status)
        assertEquals(1, viewModel.uiState.value.setsByExerciseId[exerciseA.id]?.size)

        viewModel.resumeExercise(exerciseA.id)
        var state = viewModel.uiState.value
        assertEquals(exerciseA.id, state.currentExercise?.id)
        assertEquals(WorkoutSessionExerciseStatus.IN_PROGRESS, state.currentExercise?.status)
        assertEquals(1, state.currentSetIndex)
        assertEquals(1, state.setsByExerciseId[exerciseA.id]?.size)

        viewModel.completeCurrentSet()
        viewModel.completeCurrentSet()

        state = viewModel.uiState.value
        assertEquals(3, state.setsByExerciseId[exerciseA.id]?.size)
        assertEquals(WorkoutSessionExerciseStatus.COMPLETED, state.exercises[0].status)
        assertFalse(state.exercises[0].status == WorkoutSessionExerciseStatus.SKIPPED)
        assertFalse(
            state.exercises.any {
                it.id == exerciseA.id && it.status == WorkoutSessionExerciseStatus.IN_PROGRESS
            },
        )
    }

    @Test
    fun requestFinish_withPending_setsPendingFlag() {
        emitSessionWithExercises()
        viewModel.requestFinish()
        assertTrue(viewModel.uiState.value.showFinishConfirmation)
        assertTrue(viewModel.uiState.value.finishHasPendingExercises)
    }

    @Test
    fun switchingExercise_doesNotShowPreviousExerciseDraft() {
        emitSessionWithExercises()
        viewModel.onRepsChanged("12")
        viewModel.onWeightChanged("80")

        fakeRepo.emitSets(
            exerciseA.id,
            listOf(set(exerciseA.id, 0), set(exerciseA.id, 1), set(exerciseA.id, 2)),
        )

        assertEquals(exerciseB, viewModel.uiState.value.currentExercise)
        assertEquals("10", viewModel.uiState.value.repsInput)
        assertEquals("14.5", viewModel.uiState.value.weightInput)
        assertEquals(ExerciseInputDraft("12", "80"), draftStore.get(SESSION_ID, exerciseA.id))
    }

    @Test
    fun confirmFinish_clearsSessionDrafts() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.onRepsChanged("12")
        viewModel.onWeightChanged("80")
        viewModel.confirmFinish()

        assertNull(draftStore.get(SESSION_ID, exerciseA.id))

        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA, exerciseB))
        buildViewModel()

        assertEquals("8", viewModel.uiState.value.repsInput)
        assertEquals("60", viewModel.uiState.value.weightInput)
    }

    @Test
    fun completeSet_withRest_entersResting() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA.copy(restSeconds = 90), exerciseB))
        viewModel.completeCurrentSet()

        val state = viewModel.uiState.value
        assertEquals(WorkoutExecutionPhase.RESTING, state.phase)
        assertEquals(exerciseA.id, state.restSessionExerciseId)
        assertEquals(0, state.restAfterSetIndex)
        assertFalse(state.isRestPaused)
        assertEquals(WorkoutSessionStatus.IN_PROGRESS, state.session?.status)
    }

    @Test
    fun completeSet_withZeroRest_staysWorking() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.completeCurrentSet()

        assertEquals(WorkoutExecutionPhase.WORKING, viewModel.uiState.value.phase)
        assertNull(viewModel.uiState.value.restEndsAtMillis)
        assertEquals(1, viewModel.uiState.value.currentSetIndex)
    }

    @Test
    fun startRest_createsFutureTimestamp() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA.copy(restSeconds = 90), exerciseB))
        viewModel.completeCurrentSet()

        assertEquals(1_090_000L, viewModel.uiState.value.restEndsAtMillis)
        assertEquals(90_000L, viewModel.uiState.value.restRemainingMillis)
    }

    @Test
    fun pauseRest_savesRemaining() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA.copy(restSeconds = 90), exerciseB))
        viewModel.completeCurrentSet()
        clock.now = 1_030_000L
        viewModel.pauseRest()

        val state = viewModel.uiState.value
        assertTrue(state.isRestPaused)
        assertEquals(60_000L, state.restPausedRemainingMillis)
        assertNull(state.restEndsAtMillis)
        assertEquals(60_000L, state.restRemainingMillis)
        assertEquals(WorkoutExecutionPhase.RESTING, state.phase)
    }

    @Test
    fun resumeRest_recreatesFutureTimestamp() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA.copy(restSeconds = 90), exerciseB))
        viewModel.completeCurrentSet()
        clock.now = 1_030_000L
        viewModel.pauseRest()
        clock.now = 1_050_000L
        viewModel.resumeRest()

        val state = viewModel.uiState.value
        assertFalse(state.isRestPaused)
        assertNull(state.restPausedRemainingMillis)
        assertEquals(1_110_000L, state.restEndsAtMillis)
        assertEquals(60_000L, state.restRemainingMillis)
    }

    @Test
    fun skipRest_clearsRest() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA.copy(restSeconds = 90), exerciseB))
        viewModel.completeCurrentSet()
        viewModel.skipRest()

        val state = viewModel.uiState.value
        assertEquals(WorkoutExecutionPhase.WORKING, state.phase)
        assertNull(state.restEndsAtMillis)
        assertNull(state.restPausedRemainingMillis)
        assertNull(state.restSessionExerciseId)
        assertEquals(WorkoutSessionStatus.IN_PROGRESS, state.session?.status)
    }

    @Test
    fun restRemainingMillis_isDerivedFromTimestamp() {
        fakeRepo.emitSession(
            session.copy(restEndsAtMillis = 1_045_000L, restSessionExerciseId = exerciseA.id, restAfterSetIndex = 0),
        )
        fakeRepo.emitExercises(listOf(exerciseA.copy(restSeconds = 90), exerciseB))

        assertEquals(45_000L, viewModel.uiState.value.restRemainingMillis)
        assertEquals(WorkoutExecutionPhase.RESTING, viewModel.uiState.value.phase)
    }

    @Test
    fun expiredTimestamp_resultsInZeroRemaining() = runTest(testDispatcher) {
        fakeRepo.emitSession(
            session.copy(restEndsAtMillis = 900_000L, restSessionExerciseId = exerciseA.id, restAfterSetIndex = 0),
        )
        fakeRepo.emitExercises(listOf(exerciseA.copy(restSeconds = 90), exerciseB))

        assertEquals(0L, viewModel.uiState.value.restRemainingMillis)
        assertNull(viewModel.uiState.value.restEndsAtMillis)
        assertEquals(WorkoutExecutionPhase.WORKING, viewModel.uiState.value.phase)
    }

    @Test
    fun newViewModel_recoversResting() {
        fakeRepo.emitSession(
            session.copy(restEndsAtMillis = 1_090_000L, restSessionExerciseId = exerciseA.id, restAfterSetIndex = 0),
        )
        fakeRepo.emitExercises(listOf(exerciseA.copy(restSeconds = 90), exerciseB))
        buildViewModel()

        val state = viewModel.uiState.value
        assertEquals(WorkoutExecutionPhase.RESTING, state.phase)
        assertEquals(90_000L, state.restRemainingMillis)
        assertFalse(state.isRestPaused)
    }

    @Test
    fun newViewModel_recoversPausedRest() {
        fakeRepo.emitSession(
            session.copy(
                restPausedRemainingMillis = 12_000L,
                restSessionExerciseId = exerciseA.id,
                restAfterSetIndex = 0,
            ),
        )
        fakeRepo.emitExercises(listOf(exerciseA.copy(restSeconds = 90), exerciseB))
        buildViewModel()

        val state = viewModel.uiState.value
        assertEquals(WorkoutExecutionPhase.RESTING, state.phase)
        assertTrue(state.isRestPaused)
        assertEquals(12_000L, state.restRemainingMillis)
    }

    @Test
    fun newViewModel_recoversWorkingWithoutRest() {
        emitSessionWithExercises()
        buildViewModel()

        assertEquals(WorkoutExecutionPhase.WORKING, viewModel.uiState.value.phase)
        assertNull(viewModel.uiState.value.restEndsAtMillis)
    }

    @Test
    fun completeSet_withRest_preservesSetIndex() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA.copy(restSeconds = 90), exerciseB))
        viewModel.completeCurrentSet()

        assertEquals(0, fakeRepo.completeSetCalls.single().setIndex)
        assertEquals(0, viewModel.uiState.value.restAfterSetIndex)
        assertEquals(1, viewModel.uiState.value.setsByExerciseId[exerciseA.id]?.size)
    }

    @Test
    fun restActions_repositoryError_setsError() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA.copy(restSeconds = 90), exerciseB))
        fakeRepo.shouldThrowOnRest = true
        viewModel.completeCurrentSet()

        assertEquals("rest error", viewModel.uiState.value.error)
        assertEquals(WorkoutSessionStatus.IN_PROGRESS, viewModel.uiState.value.session?.status)
    }

    @Test
    fun rest_doesNotMarkSessionCompleted() = runTest(testDispatcher) {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA.copy(restSeconds = 90), exerciseB))
        viewModel.completeCurrentSet()

        assertEquals(WorkoutSessionStatus.IN_PROGRESS, viewModel.uiState.value.session?.status)
        assertNull(viewModel.uiState.value.session?.endedAtMillis)
    }

    @Test
    fun elapsedMillis_isDerivedFromTimeProviderAndStartedAt() {
        clock.now = 61_000L
        fakeRepo.emitSession(session.copy(startedAtMillis = 1_000L))
        fakeRepo.emitExercises(listOf(exerciseA, exerciseB))

        assertEquals(60_000L, viewModel.uiState.value.elapsedMillis)
    }

    @Test
    fun progress_usesPersistedSetsNotCurrentIndex() {
        emitSessionWithExercises()
        fakeRepo.emitSets(
            exerciseA.id,
            listOf(set(exerciseA.id, 0), set(exerciseA.id, 1), set(exerciseA.id, 2)),
        )
        fakeRepo.emitSets(exerciseB.id, listOf(set(exerciseB.id, 0)))

        val state = viewModel.uiState.value
        assertEquals(1, state.currentSetIndex)
        assertEquals(exerciseB, state.currentExercise)
        assertEquals(4, state.completedSets)
        assertEquals(5, state.plannedSets)
        assertEquals(1, state.completedExercises)
        assertEquals(2, state.totalExercises)
        assertEquals(80, state.progressPercent)
    }

    @Test
    fun progress_zeroWhenNoExercises() {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(emptyList())

        val state = viewModel.uiState.value
        assertEquals(0, state.completedSets)
        assertEquals(0, state.plannedSets)
        assertEquals(0, state.progressPercent)
    }

    @Test
    fun requestFinish_opensConfirmation() {
        emitSessionWithExercises()
        viewModel.requestFinish()
        assertTrue(viewModel.uiState.value.showFinishConfirmation)
        assertTrue(fakeRepo.finishSessionCalls.isEmpty())
    }

    @Test
    fun dismissFinishConfirmation_doesNotFinish() {
        emitSessionWithExercises()
        viewModel.requestFinish()
        viewModel.dismissFinishConfirmation()
        assertFalse(viewModel.uiState.value.showFinishConfirmation)
        assertTrue(fakeRepo.finishSessionCalls.isEmpty())
        assertEquals(WorkoutSessionStatus.IN_PROGRESS, viewModel.uiState.value.session?.status)
    }

    @Test
    fun confirmFinish_callsRepository() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.confirmFinish()
        assertEquals(listOf(SESSION_ID), fakeRepo.finishSessionCalls)
    }

    @Test
    fun confirmFinish_success_emitsNavigationEvent() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.confirmFinish()
        assertEquals(SESSION_ID, viewModel.uiState.value.sessionFinishedEvent)
        assertEquals(WorkoutSessionStatus.COMPLETED, viewModel.uiState.value.session?.status)
    }

    @Test
    fun confirmFinish_error_doesNotNavigate() = runTest(testDispatcher) {
        emitSessionWithExercises()
        fakeRepo.shouldThrowOnFinish = true
        viewModel.confirmFinish()
        assertNull(viewModel.uiState.value.sessionFinishedEvent)
        assertEquals("finish error", viewModel.uiState.value.error)
        assertEquals(WorkoutSessionStatus.IN_PROGRESS, viewModel.uiState.value.session?.status)
    }

    @Test
    fun confirmFinish_allowsPartialSession() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.completeCurrentSet()
        viewModel.confirmFinish()
        assertEquals(1, viewModel.uiState.value.completedSets)
        assertEquals(5, viewModel.uiState.value.plannedSets)
        assertEquals(WorkoutSessionStatus.COMPLETED, viewModel.uiState.value.session?.status)
        assertEquals(SESSION_ID, viewModel.uiState.value.sessionFinishedEvent)
    }

    @Test
    fun confirmFinish_allowsFullyCompletedSession() = runTest(testDispatcher) {
        emitSessionWithExercises()
        fakeRepo.emitSets(
            exerciseA.id,
            listOf(set(exerciseA.id, 0), set(exerciseA.id, 1), set(exerciseA.id, 2)),
        )
        fakeRepo.emitSets(
            exerciseB.id,
            listOf(set(exerciseB.id, 0), set(exerciseB.id, 1)),
        )
        viewModel.confirmFinish()
        assertTrue(viewModel.uiState.value.isWorkoutComplete)
        assertEquals(WorkoutSessionStatus.COMPLETED, viewModel.uiState.value.session?.status)
        assertEquals(SESSION_ID, viewModel.uiState.value.sessionFinishedEvent)
    }

    @Test
    fun confirmFinish_clearsConfirmationOnSuccess() = runTest(testDispatcher) {
        emitSessionWithExercises()
        viewModel.requestFinish()
        assertTrue(viewModel.uiState.value.showFinishConfirmation)
        viewModel.confirmFinish()
        assertFalse(viewModel.uiState.value.showFinishConfirmation)
    }

    @Test
    fun mediaExercise_resolvedFromCatalogueByExerciseId() = runTest(testDispatcher) {
        val catalogueExercise = Exercise(
            id = 1L,
            name = "Supino",
            muscleGroup = "Peito",
            equipmentType = "Barra",
            externalSource = "free-exercise-db",
            externalId = "Barbell_Bench_Press_-_Medium_Grip",
        )
        fakeExercises.emit(listOf(catalogueExercise))
        emitSessionWithExercises()

        val media = viewModel.uiState.value.mediaExercise
        assertEquals(catalogueExercise.externalId, media?.externalId)
        assertEquals(catalogueExercise.externalSource, media?.externalSource)
    }

    @Test
    fun mediaExercise_nullWhenCatalogueMissing() = runTest(testDispatcher) {
        emitSessionWithExercises()
        assertNull(viewModel.uiState.value.mediaExercise)
    }

    private fun set(sessionExerciseId: Long, setIndex: Int) = WorkoutSet(
        id = sessionExerciseId * 10 + setIndex,
        sessionExerciseId = sessionExerciseId,
        setIndex = setIndex,
        reps = 8,
        weight = 60.0,
        completedAtMillis = 1L,
    )
}

private data class CompleteSetCall(
    val sessionExerciseId: Long,
    val setIndex: Int,
    val reps: Int,
    val weight: Double,
)

private class FakeTimeProvider(var now: Long) : TimeProvider {
    override fun nowMillis(): Long = now
}

private class FakeExerciseRepository : ExerciseRepository {
    private val exercises = MutableStateFlow<List<Exercise>>(emptyList())

    fun emit(value: List<Exercise>) {
        exercises.value = value
    }

    override fun getAll(): Flow<List<Exercise>> = exercises

    override fun getById(id: Long): Flow<Exercise?> =
        flowOf(exercises.value.firstOrNull { it.id == id })

    override fun search(query: String): Flow<List<Exercise>> =
        flowOf(exercises.value.filter { it.name.contains(query, ignoreCase = true) })

    override suspend fun save(exercise: Exercise): Long = exercise.id

    override suspend fun delete(exercise: Exercise) = Unit
}

private class FakeExecutionSessionRepository(
    private val timeProvider: TimeProvider,
) : WorkoutSessionRepository {

    var shouldThrowOnComplete = false
    var shouldThrowOnRest = false
    var shouldThrowOnFinish = false
    val completeSetCalls = mutableListOf<CompleteSetCall>()
    val finishSessionCalls = mutableListOf<Long>()

    private val sessionFlow = MutableStateFlow<WorkoutSession?>(null)
    private val exercisesFlow = MutableStateFlow<List<WorkoutSessionExercise>>(emptyList())
    private val setsFlows = mutableMapOf<Long, MutableStateFlow<List<WorkoutSet>>>()
    private var nextSetId = 1_000L

    fun emitSession(session: WorkoutSession?) {
        sessionFlow.value = session
    }

    fun emitExercises(exercises: List<WorkoutSessionExercise>) {
        exercisesFlow.value = exercises
        exercises.forEach { exercise ->
            setsFlows.getOrPut(exercise.id) { MutableStateFlow(emptyList()) }
        }
    }

    fun emitSets(sessionExerciseId: Long, sets: List<WorkoutSet>) {
        setsFlows.getOrPut(sessionExerciseId) { MutableStateFlow(emptyList()) }.value = sets
    }

    override suspend fun startSession(workoutId: Long): StartSessionResult =
        StartSessionResult.Created(0L)

    override suspend fun getSession(id: Long): WorkoutSession? = sessionFlow.value

    override fun observeSession(id: Long): Flow<WorkoutSession?> = sessionFlow

    override fun observeInProgress(): Flow<WorkoutSession?> = emptyFlow()
    override fun observeCompletedSessions(): Flow<List<com.gymtrack.domain.model.WorkoutHistoryItem>> = emptyFlow()
    override fun observeCompletedSetHistory(): Flow<List<com.gymtrack.domain.model.CompletedSetRecord>> = emptyFlow()

    override fun observeSessionExercises(sessionId: Long): Flow<List<WorkoutSessionExercise>> =
        exercisesFlow

    override fun observeSets(sessionExerciseId: Long): Flow<List<WorkoutSet>> =
        setsFlows.getOrPut(sessionExerciseId) { MutableStateFlow(emptyList()) }

    override suspend fun completeSet(
        sessionExerciseId: Long,
        setIndex: Int,
        reps: Int,
        weight: Double,
    ): Long {
        if (shouldThrowOnComplete) throw RuntimeException("completeSet error")
        completeSetCalls += CompleteSetCall(sessionExerciseId, setIndex, reps, weight)
        val flow = setsFlows.getOrPut(sessionExerciseId) { MutableStateFlow(emptyList()) }
        flow.value = flow.value + WorkoutSet(
            id = nextSetId++,
            sessionExerciseId = sessionExerciseId,
            setIndex = setIndex,
            reps = reps,
            weight = weight,
            completedAtMillis = 1L,
        )
        val planned = exercisesFlow.value.find { it.id == sessionExerciseId }?.plannedSets ?: Int.MAX_VALUE
        if (flow.value.size >= planned) {
            exercisesFlow.value = exercisesFlow.value.map { exercise ->
                if (exercise.id == sessionExerciseId) {
                    exercise.copy(status = WorkoutSessionExerciseStatus.COMPLETED)
                } else {
                    exercise
                }
            }
        }
        return nextSetId
    }

    override suspend fun startRest(
        sessionId: Long,
        sessionExerciseId: Long,
        afterSetIndex: Int,
        restSeconds: Int,
    ) {
        if (shouldThrowOnRest) throw RuntimeException("rest error")
        if (restSeconds <= 0) return
        val current = sessionFlow.value ?: return
        sessionFlow.value = current.copy(
            restEndsAtMillis = timeProvider.nowMillis() + restSeconds * 1_000L,
            restPausedRemainingMillis = null,
            restSessionExerciseId = sessionExerciseId,
            restAfterSetIndex = afterSetIndex,
        )
    }

    override suspend fun pauseRest(sessionId: Long) {
        if (shouldThrowOnRest) throw RuntimeException("rest error")
        val current = sessionFlow.value ?: return
        val endsAt = current.restEndsAtMillis ?: return
        sessionFlow.value = current.copy(
            restEndsAtMillis = null,
            restPausedRemainingMillis = maxOf(0L, endsAt - timeProvider.nowMillis()),
        )
    }

    override suspend fun resumeRest(sessionId: Long) {
        if (shouldThrowOnRest) throw RuntimeException("rest error")
        val current = sessionFlow.value ?: return
        val remaining = current.restPausedRemainingMillis ?: return
        sessionFlow.value = current.copy(
            restEndsAtMillis = timeProvider.nowMillis() + remaining,
            restPausedRemainingMillis = null,
        )
    }

    override suspend fun skipRest(sessionId: Long) {
        if (shouldThrowOnRest) throw RuntimeException("rest error")
        val current = sessionFlow.value ?: return
        sessionFlow.value = current.copy(
            restEndsAtMillis = null,
            restPausedRemainingMillis = null,
            restSessionExerciseId = null,
            restAfterSetIndex = null,
        )
    }

    override suspend fun skipSessionExercise(sessionExerciseId: Long) {
        exercisesFlow.value = exercisesFlow.value.map { exercise ->
            if (exercise.id == sessionExerciseId) {
                exercise.copy(status = WorkoutSessionExerciseStatus.SKIPPED)
            } else {
                exercise
            }
        }
    }

    override suspend fun resumeSessionExercise(sessionExerciseId: Long) {
        val completed = setsFlows[sessionExerciseId]?.value?.size ?: 0
        val planned = exercisesFlow.value.find { it.id == sessionExerciseId }?.plannedSets ?: 0
        if (completed >= planned) return
        exercisesFlow.value = exercisesFlow.value.map { exercise ->
            when {
                exercise.id == sessionExerciseId ->
                    exercise.copy(status = WorkoutSessionExerciseStatus.IN_PROGRESS)
                exercise.status == WorkoutSessionExerciseStatus.IN_PROGRESS ->
                    exercise.copy(status = WorkoutSessionExerciseStatus.PENDING)
                else -> exercise
            }
        }
    }

    override suspend fun finishSession(sessionId: Long) {
        if (shouldThrowOnFinish) throw RuntimeException("finish error")
        finishSessionCalls.add(sessionId)
        val current = sessionFlow.value ?: return
        sessionFlow.value = current.copy(
            status = WorkoutSessionStatus.COMPLETED,
            endedAtMillis = timeProvider.nowMillis(),
            restEndsAtMillis = null,
            restPausedRemainingMillis = null,
            restSessionExerciseId = null,
            restAfterSetIndex = null,
        )
    }

    override suspend fun deleteCompletedSession(sessionId: Long) = Unit
}

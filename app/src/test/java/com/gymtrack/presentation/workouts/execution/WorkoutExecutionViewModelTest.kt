package com.gymtrack.presentation.workouts.execution

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.gymtrack.R
import com.gymtrack.domain.model.CompletedSetRecord
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.ProgressionAction
import com.gymtrack.domain.model.StartSessionResult
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutBlock
import com.gymtrack.domain.model.WorkoutBlockType
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSessionExerciseStatus
import com.gymtrack.domain.model.WorkoutSessionStatus
import com.gymtrack.domain.model.WorkoutSet
import com.gymtrack.domain.repository.ExerciseRepository
import com.gymtrack.domain.repository.WorkoutRepository
import com.gymtrack.domain.repository.WorkoutSessionRepository
import com.gymtrack.domain.time.TimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    private lateinit var fakeWorkoutRepo: FakeExecutionWorkoutRepository
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
        fakeWorkoutRepo = FakeExecutionWorkoutRepository()
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
            workoutRepository = fakeWorkoutRepo,
            exerciseRepository = fakeExercises,
            timeProvider = clock,
            draftStore = draftStore,
        )
    }

    private fun emitSessionWithExercises() {
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA, exerciseB))
    }

    private fun seedTemplate(
        workoutId: Long = 1L,
        blocks: List<WorkoutBlock> = listOf(
            WorkoutBlock(
                id = 10L,
                workoutId = workoutId,
                position = 0,
                type = WorkoutBlockType.SINGLE,
                rounds = 3,
                restSeconds = 60,
            ),
            WorkoutBlock(
                id = 20L,
                workoutId = workoutId,
                position = 1,
                type = WorkoutBlockType.SINGLE,
                rounds = 2,
                restSeconds = 60,
            ),
        ),
        exercises: List<WorkoutExercise> = listOf(
            WorkoutExercise(
                id = 100L,
                blockId = 10L,
                exerciseId = 1L,
                positionInBlock = 0,
                minRepetitions = 8,
                maxRepetitions = 12,
                weight = 80.0,
                notes = "template-a",
            ),
            WorkoutExercise(
                id = 200L,
                blockId = 20L,
                exerciseId = 2L,
                positionInBlock = 0,
                minRepetitions = 10,
                maxRepetitions = 12,
                weight = 14.5,
                notes = "template-b",
            ),
        ),
    ) {
        fakeWorkoutRepo.emitTemplate(workoutId, blocks, exercises)
    }

    private fun setupIncreaseWeightScenario(
        plannedWeight: Double = 80.0,
        templateWeight: Double = 80.0,
    ) {
        val target = exerciseA.copy(
            plannedWeight = plannedWeight,
            minRepetitions = 8,
            maxRepetitions = 12,
            plannedSets = 3,
        )
        seedTemplate(
            exercises = listOf(
                WorkoutExercise(
                    id = 100L,
                    blockId = 10L,
                    exerciseId = 1L,
                    positionInBlock = 0,
                    minRepetitions = 8,
                    maxRepetitions = 12,
                    weight = templateWeight,
                    notes = "template-a",
                ),
                WorkoutExercise(
                    id = 200L,
                    blockId = 20L,
                    exerciseId = 2L,
                    positionInBlock = 0,
                    minRepetitions = 10,
                    maxRepetitions = 12,
                    weight = 14.5,
                    notes = "template-b",
                ),
            ),
        )
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(target, exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(12, 12, 12), 80.0),
        )
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

    @Test
    fun progression_noHistory_returnsNoHistory() {
        val target = exerciseA.copy(plannedWeight = 80.0)
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(target, exerciseB))
        fakeRepo.emitCompletedHistory(emptyList())

        val suggestion = viewModel.uiState.value.progressionSuggestion
        assertEquals(ProgressionAction.NO_HISTORY, suggestion!!.action)
        assertNull(suggestion.suggestedWeight)
    }

    @Test
    fun progression_lastSessionAllMax_increasesWeight() {
        val target = exerciseA.copy(plannedWeight = 80.0, minRepetitions = 8, maxRepetitions = 12, plannedSets = 3)
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(target, exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(
                sessionId = 50L,
                occurredAt = 5_000L,
                name = "Supino",
                reps = listOf(12, 12, 12),
                weight = 80.0,
            ),
        )

        val suggestion = viewModel.uiState.value.progressionSuggestion!!
        assertEquals(ProgressionAction.INCREASE_WEIGHT, suggestion.action)
        assertEquals(82.5, suggestion.suggestedWeight!!, 0.001)
        assertEquals(80.0, suggestion.plannedWeight, 0.001)
    }

    @Test
    fun progression_lastSessionMidRange_increasesReps() {
        val target = exerciseA.copy(plannedWeight = 80.0)
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(target, exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(8, 9, 9), 80.0),
        )

        val suggestion = viewModel.uiState.value.progressionSuggestion!!
        assertEquals(ProgressionAction.INCREASE_REPS, suggestion.action)
        assertNull(suggestion.suggestedWeight)
    }

    @Test
    fun progression_lastSessionBelowMin_maintains() {
        val target = exerciseA.copy(plannedWeight = 80.0)
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(target, exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(8, 8, 7), 80.0),
        )

        assertEquals(
            ProgressionAction.MAINTAIN,
            viewModel.uiState.value.progressionSuggestion!!.action,
        )
    }

    @Test
    fun progression_usesMostRecentCompletedSessionOnly() {
        val target = exerciseA.copy(plannedWeight = 80.0)
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(target, exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(40L, 4_000L, "Supino", listOf(12, 12, 12), 70.0) +
                historySets(50L, 5_000L, "Supino", listOf(8, 9, 9), 80.0),
        )

        val suggestion = viewModel.uiState.value.progressionSuggestion!!
        assertEquals(ProgressionAction.INCREASE_REPS, suggestion.action)
        assertNull(suggestion.suggestedWeight)
    }

    @Test
    fun progression_ignoresInProgressHistoryBecauseCompletedFlowOmitsIt() {
        // observeCompletedSetHistory only exposes COMPLETED sessions; an IN_PROGRESS
        // session must never appear in this list even if it is more recent.
        val target = exerciseA.copy(plannedWeight = 80.0)
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(target, exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(12, 12, 12), 80.0),
        )

        assertEquals(
            ProgressionAction.INCREASE_WEIGHT,
            viewModel.uiState.value.progressionSuggestion!!.action,
        )
    }

    @Test
    fun progression_biSet_evaluatesEachExerciseIndependently() {
        val bench = exerciseA.copy(
            plannedWeight = 80.0,
            plannedSets = 3,
            blockPosition = 0,
            positionInBlock = 0,
        )
        val fly = exerciseB.copy(
            plannedWeight = 40.0,
            plannedSets = 3,
            minRepetitions = 8,
            maxRepetitions = 12,
            blockPosition = 0,
            positionInBlock = 1,
        )
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(bench, fly))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(12, 12, 12), 80.0),
        )

        assertEquals(
            ProgressionAction.INCREASE_WEIGHT,
            viewModel.uiState.value.progressionSuggestion!!.action,
        )

        fakeRepo.emitSets(
            bench.id,
            listOf(set(bench.id, 0), set(bench.id, 1), set(bench.id, 2)),
        )

        val flySuggestion = viewModel.uiState.value.progressionSuggestion!!
        assertEquals(fly, viewModel.uiState.value.currentExercise)
        assertEquals(ProgressionAction.NO_HISTORY, flySuggestion.action)
    }

    @Test
    fun progression_replacedExercise_usesOnlyNewExerciseHistory() {
        val replaced = exerciseA.copy(
            exerciseId = 99L,
            exerciseName = "Supino Inclinado",
            plannedWeight = 80.0,
        )
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(replaced, exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(12, 12, 12), 80.0),
        )

        assertEquals(
            ProgressionAction.NO_HISTORY,
            viewModel.uiState.value.progressionSuggestion!!.action,
        )

        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(12, 12, 12), 80.0) +
                historySets(51L, 6_000L, "Supino Inclinado", listOf(8, 9, 9), 80.0),
        )

        assertEquals(
            ProgressionAction.INCREASE_REPS,
            viewModel.uiState.value.progressionSuggestion!!.action,
        )
    }

    @Test
    fun progression_usesSessionSnapshotNotTemplateMutation() {
        // Snapshot already started with 80 kg / 8–12; a later template edit is irrelevant.
        val snapshot = exerciseA.copy(
            plannedWeight = 80.0,
            minRepetitions = 8,
            maxRepetitions = 12,
            plannedSets = 3,
        )
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(snapshot, exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(12, 12, 12), 80.0),
        )

        val suggestion = viewModel.uiState.value.progressionSuggestion!!
        assertEquals(80.0, suggestion.plannedWeight, 0.001)
        assertEquals(8, suggestion.minRepetitions)
        assertEquals(12, suggestion.maxRepetitions)
        assertEquals(3, suggestion.plannedSets)
        assertEquals(ProgressionAction.INCREASE_WEIGHT, suggestion.action)
        assertEquals(82.5, suggestion.suggestedWeight!!, 0.001)
    }

    @Test
    fun progression_currentSessionSetsAreNotUsedAsHistory() {
        val target = exerciseA.copy(plannedWeight = 80.0)
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(target, exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(8, 8, 7), 80.0),
        )
        // Current IN_PROGRESS sets look like a perfect session, but must not affect suggestion.
        fakeRepo.emitSets(
            target.id,
            listOf(
                WorkoutSet(1, target.id, 0, 12, 80.0, 1L),
                WorkoutSet(2, target.id, 1, 12, 80.0, 1L),
            ),
        )

        assertEquals(
            ProgressionAction.MAINTAIN,
            viewModel.uiState.value.progressionSuggestion!!.action,
        )
    }

    @Test
    fun prefill_increaseWeight_usesSuggestedWeight() {
        val target = exerciseA.copy(plannedWeight = 80.0)
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(target, exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(12, 12, 12), 80.0),
        )

        assertEquals(ProgressionAction.INCREASE_WEIGHT, viewModel.uiState.value.progressionSuggestion!!.action)
        assertEquals(82.5, viewModel.uiState.value.progressionSuggestion!!.suggestedWeight!!, 0.001)
        assertEquals("82.5", viewModel.uiState.value.weightInput)
        assertEquals("8", viewModel.uiState.value.repsInput)
        // Prefill alone must not create a draft or mutate the snapshot/template values.
        assertNull(draftStore.get(SESSION_ID, target.id))
        assertEquals(80.0, target.plannedWeight, 0.0)
        assertEquals(8, target.minRepetitions)
        assertEquals(12, target.maxRepetitions)
    }

    @Test
    fun prefill_increaseReps_keepsPlannedWeightAndMinReps() {
        val target = exerciseA.copy(plannedWeight = 80.0, plannedSets = 3)
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(target, exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(8, 9, 9), 80.0),
        )

        val state = viewModel.uiState.value
        assertEquals(ProgressionAction.INCREASE_REPS, state.progressionSuggestion!!.action)
        assertEquals("80", state.weightInput)
        assertEquals(target.minRepetitions.toString(), state.repsInput)
        assertEquals(3, state.currentExercise!!.plannedSets)
        assertNull(draftStore.get(SESSION_ID, target.id))
    }

    @Test
    fun prefill_maintain_keepsPlannedValues() {
        val target = exerciseA.copy(plannedWeight = 80.0)
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(target, exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(8, 8, 7), 80.0),
        )

        assertEquals(ProgressionAction.MAINTAIN, viewModel.uiState.value.progressionSuggestion!!.action)
        assertEquals("80", viewModel.uiState.value.weightInput)
        assertEquals("8", viewModel.uiState.value.repsInput)
        assertNull(draftStore.get(SESSION_ID, target.id))
    }

    @Test
    fun prefill_noHistory_keepsPlannedValues() {
        val target = exerciseA.copy(plannedWeight = 80.0)
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(target, exerciseB))
        fakeRepo.emitCompletedHistory(emptyList())

        assertEquals(ProgressionAction.NO_HISTORY, viewModel.uiState.value.progressionSuggestion!!.action)
        assertEquals("80", viewModel.uiState.value.weightInput)
        assertEquals("8", viewModel.uiState.value.repsInput)
        assertNull(draftStore.get(SESSION_ID, target.id))
    }

    @Test
    fun prefill_userEdit_isNotOverwrittenByProgression() {
        val target = exerciseA.copy(plannedWeight = 80.0)
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(target, exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(12, 12, 12), 80.0),
        )
        assertEquals("82.5", viewModel.uiState.value.weightInput)
        assertNull(draftStore.get(SESSION_ID, target.id))

        viewModel.onWeightChanged("85")
        assertEquals("85", viewModel.uiState.value.weightInput)
        assertEquals("85", draftStore.get(SESSION_ID, target.id)?.weightInput)

        // Completing a set / refreshing sets must not revert the manual edit.
        fakeRepo.emitSets(target.id, listOf(set(target.id, 0)))
        assertEquals("85", viewModel.uiState.value.weightInput)

        // History refresh also must not overwrite.
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(12, 12, 12), 80.0),
        )
        assertEquals("85", viewModel.uiState.value.weightInput)

        // Session re-emission / progression recompute must not overwrite.
        fakeRepo.emitSession(session.copy(startedAtMillis = session.startedAtMillis))
        assertEquals("85", viewModel.uiState.value.weightInput)
        assertEquals(
            ProgressionAction.INCREASE_WEIGHT,
            viewModel.uiState.value.progressionSuggestion!!.action,
        )
        assertEquals(82.5, viewModel.uiState.value.progressionSuggestion!!.suggestedWeight!!, 0.001)
    }

    @Test
    fun prefill_historyArrivesLater_upgradesOnceUntilUserEdits() {
        val target = exerciseA.copy(plannedWeight = 80.0)
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(target, exerciseB))
        // No completed history yet → planned weight.
        assertEquals(ProgressionAction.NO_HISTORY, viewModel.uiState.value.progressionSuggestion!!.action)
        assertEquals("80", viewModel.uiState.value.weightInput)
        assertNull(draftStore.get(SESSION_ID, target.id))

        // History arrives with INCREASE_WEIGHT and no draft → single upgrade.
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(12, 12, 12), 80.0),
        )
        assertEquals(ProgressionAction.INCREASE_WEIGHT, viewModel.uiState.value.progressionSuggestion!!.action)
        assertEquals("82.5", viewModel.uiState.value.weightInput)
        assertNull(draftStore.get(SESSION_ID, target.id))

        // After manual edit, further history/progress updates must not upgrade again.
        viewModel.onWeightChanged("85")
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(12, 12, 12), 80.0) +
                historySets(51L, 6_000L, "Supino", listOf(12, 12, 12), 82.5),
        )
        assertEquals("85", viewModel.uiState.value.weightInput)
        assertEquals("85", draftStore.get(SESSION_ID, target.id)?.weightInput)
    }

    @Test
    fun prefill_existingDraft_winsOverIncreaseWeightSuggestion() {
        val target = exerciseA.copy(plannedWeight = 80.0)
        draftStore.put(SESSION_ID, target.id, repsInput = "8", weightInput = "85")

        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(target, exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(12, 12, 12), 80.0),
        )

        assertEquals(ProgressionAction.INCREASE_WEIGHT, viewModel.uiState.value.progressionSuggestion!!.action)
        assertEquals(82.5, viewModel.uiState.value.progressionSuggestion!!.suggestedWeight!!, 0.001)
        assertEquals("85", viewModel.uiState.value.weightInput)
        assertEquals("8", viewModel.uiState.value.repsInput)
        assertEquals(ExerciseInputDraft("8", "85"), draftStore.get(SESSION_ID, target.id))
    }

    @Test
    fun prefill_biSet_onlyIncreaseWeightExerciseGetsSuggestedLoad() {
        val bench = exerciseA.copy(
            plannedWeight = 80.0,
            plannedSets = 3,
            blockPosition = 0,
            positionInBlock = 0,
        )
        val fly = exerciseB.copy(
            plannedWeight = 40.0,
            plannedSets = 3,
            minRepetitions = 8,
            maxRepetitions = 12,
            blockPosition = 0,
            positionInBlock = 1,
        )
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(bench, fly))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(12, 12, 12), 80.0),
        )

        assertEquals("82.5", viewModel.uiState.value.weightInput)
        assertNull(draftStore.get(SESSION_ID, fly.id))

        fakeRepo.emitSets(
            bench.id,
            listOf(set(bench.id, 0), set(bench.id, 1), set(bench.id, 2)),
        )

        assertEquals(fly, viewModel.uiState.value.currentExercise)
        assertEquals(ProgressionAction.NO_HISTORY, viewModel.uiState.value.progressionSuggestion!!.action)
        assertEquals("40", viewModel.uiState.value.weightInput)
        assertEquals(fly.minRepetitions.toString(), viewModel.uiState.value.repsInput)
        assertNull(draftStore.get(SESSION_ID, fly.id))
    }

    @Test
    fun prefill_triSet_appliesSuggestionPerExercise() {
        val curl = exerciseA.copy(
            id = 10L,
            exerciseName = "Rosca",
            plannedWeight = 20.0,
            plannedSets = 3,
            minRepetitions = 10,
            maxRepetitions = 12,
            blockPosition = 0,
            positionInBlock = 0,
        )
        val kickback = exerciseB.copy(
            id = 20L,
            exerciseName = "Kickback",
            plannedWeight = 12.5,
            plannedSets = 3,
            minRepetitions = 10,
            maxRepetitions = 15,
            blockPosition = 0,
            positionInBlock = 1,
        )
        val press = exerciseC.copy(
            id = 30L,
            exerciseName = "Tríceps",
            plannedWeight = 30.0,
            plannedSets = 3,
            minRepetitions = 8,
            maxRepetitions = 10,
            blockPosition = 0,
            positionInBlock = 2,
        )
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(curl, kickback, press))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Rosca", listOf(12, 12, 12), 20.0) +
                historySets(50L, 5_000L, "Kickback", listOf(10, 11, 10), 12.5) +
                historySets(50L, 5_000L, "Tríceps", listOf(8, 7, 8), 30.0),
        )

        assertEquals(ProgressionAction.INCREASE_WEIGHT, viewModel.uiState.value.progressionSuggestion!!.action)
        assertEquals("22.5", viewModel.uiState.value.weightInput)

        fakeRepo.emitSets(curl.id, listOf(set(curl.id, 0), set(curl.id, 1), set(curl.id, 2)))
        assertEquals(ProgressionAction.INCREASE_REPS, viewModel.uiState.value.progressionSuggestion!!.action)
        assertEquals("12.5", viewModel.uiState.value.weightInput)
        assertEquals("10", viewModel.uiState.value.repsInput)
        assertNull(draftStore.get(SESSION_ID, kickback.id))

        fakeRepo.emitSets(
            kickback.id,
            listOf(set(kickback.id, 0), set(kickback.id, 1), set(kickback.id, 2)),
        )
        assertEquals(ProgressionAction.MAINTAIN, viewModel.uiState.value.progressionSuggestion!!.action)
        assertEquals("30", viewModel.uiState.value.weightInput)
        assertEquals("8", viewModel.uiState.value.repsInput)
        assertNull(draftStore.get(SESSION_ID, press.id))
    }

    @Test
    fun prefill_replacedExerciseWithoutHistory_doesNotReusePreviousSuggestion() {
        val replaced = exerciseA.copy(
            exerciseId = 99L,
            exerciseName = "Supino Inclinado",
            plannedWeight = 80.0,
            minRepetitions = 8,
            maxRepetitions = 12,
        )
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(replaced, exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(12, 12, 12), 80.0),
        )

        assertEquals(ProgressionAction.NO_HISTORY, viewModel.uiState.value.progressionSuggestion!!.action)
        assertEquals("80", viewModel.uiState.value.weightInput)
        assertEquals("8", viewModel.uiState.value.repsInput)
        assertEquals(80.0, replaced.plannedWeight, 0.0)
        assertEquals(8, replaced.minRepetitions)
        assertEquals(12, replaced.maxRepetitions)
    }

    @Test
    fun prefill_currentSessionSetsDoNotChangePrefillSource() {
        val target = exerciseA.copy(plannedWeight = 80.0)
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(target, exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(12, 12, 12), 80.0),
        )
        assertEquals("82.5", viewModel.uiState.value.weightInput)

        // Performing sets in the current session must not recalculate prefill from those sets.
        viewModel.onWeightChanged("82.5")
        fakeRepo.emitSets(
            target.id,
            listOf(
                WorkoutSet(1, target.id, 0, 12, 82.5, 1L),
                WorkoutSet(2, target.id, 1, 12, 82.5, 1L),
            ),
        )
        assertEquals("82.5", viewModel.uiState.value.weightInput)
        assertEquals(
            ProgressionAction.INCREASE_WEIGHT,
            viewModel.uiState.value.progressionSuggestion!!.action,
        )
        assertEquals(82.5, viewModel.uiState.value.progressionSuggestion!!.suggestedWeight!!, 0.001)
    }

    @Test
    fun prefill_doesNotMutateSessionExerciseSnapshotFields() {
        val target = exerciseA.copy(
            plannedWeight = 80.0,
            minRepetitions = 8,
            maxRepetitions = 12,
            plannedSets = 3,
        )
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(target, exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(12, 12, 12), 80.0),
        )
        viewModel.onWeightChanged("85")
        fakeRepo.emitSets(target.id, listOf(set(target.id, 0)))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(12, 12, 12), 80.0),
        )

        val current = viewModel.uiState.value.exercises.first { it.id == target.id }
        assertEquals(80.0, current.plannedWeight, 0.0)
        assertEquals(8, current.minRepetitions)
        assertEquals(12, current.maxRepetitions)
        assertEquals(3, current.plannedSets)
        assertEquals(80.0, target.plannedWeight, 0.0)
        assertEquals(8, target.minRepetitions)
        assertEquals(12, target.maxRepetitions)
        assertEquals("85", viewModel.uiState.value.weightInput)
    }

    @Test
    fun applyProgression_availableOnlyForIncreaseWeight() {
        setupIncreaseWeightScenario()
        assertTrue(viewModel.uiState.value.canApplyProgressionToTemplate)
        assertEquals(
            ProgressionAction.INCREASE_WEIGHT,
            viewModel.uiState.value.progressionSuggestion!!.action,
        )
    }

    @Test
    fun applyProgression_notAvailableForIncreaseReps() {
        seedTemplate()
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA.copy(plannedWeight = 80.0), exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(8, 9, 9), 80.0),
        )
        assertEquals(ProgressionAction.INCREASE_REPS, viewModel.uiState.value.progressionSuggestion!!.action)
        assertFalse(viewModel.uiState.value.canApplyProgressionToTemplate)
    }

    @Test
    fun applyProgression_notAvailableForMaintain() {
        seedTemplate()
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA.copy(plannedWeight = 80.0), exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(8, 8, 7), 80.0),
        )
        assertEquals(ProgressionAction.MAINTAIN, viewModel.uiState.value.progressionSuggestion!!.action)
        assertFalse(viewModel.uiState.value.canApplyProgressionToTemplate)
    }

    @Test
    fun applyProgression_notAvailableForNoHistory() {
        seedTemplate()
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(exerciseA.copy(plannedWeight = 80.0), exerciseB))
        fakeRepo.emitCompletedHistory(emptyList())
        assertEquals(ProgressionAction.NO_HISTORY, viewModel.uiState.value.progressionSuggestion!!.action)
        assertFalse(viewModel.uiState.value.canApplyProgressionToTemplate)
    }

    @Test
    fun applyProgression_notAvailableWhenSuggestionNull() {
        seedTemplate()
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(emptyList())
        assertNull(viewModel.uiState.value.progressionSuggestion)
        assertFalse(viewModel.uiState.value.canApplyProgressionToTemplate)
    }

    @Test
    fun applyProgression_requestShowsDialogWithoutMutatingTemplate() {
        setupIncreaseWeightScenario()
        val weightBefore = viewModel.uiState.value.weightInput

        viewModel.requestApplyProgressionSuggestion()

        val state = viewModel.uiState.value
        assertTrue(state.showApplyProgressionConfirmation)
        assertEquals(80.0, state.applyProgressionFromWeight!!, 0.0)
        assertEquals(82.5, state.applyProgressionToWeight!!, 0.0)
        assertTrue(fakeWorkoutRepo.updateBlockExerciseCalls.isEmpty())
        assertEquals(80.0, fakeWorkoutRepo.exercise(100L)!!.weight, 0.0)
        assertEquals(weightBefore, state.weightInput)
    }

    @Test
    fun applyProgression_cancelKeepsTemplateAndSessionIntact() {
        setupIncreaseWeightScenario()
        val weightBefore = viewModel.uiState.value.weightInput
        val repsBefore = viewModel.uiState.value.repsInput

        viewModel.requestApplyProgressionSuggestion()
        viewModel.dismissApplyProgressionConfirmation()

        val state = viewModel.uiState.value
        assertFalse(state.showApplyProgressionConfirmation)
        assertNull(state.applyProgressionFromWeight)
        assertNull(state.applyProgressionToWeight)
        assertTrue(fakeWorkoutRepo.updateBlockExerciseCalls.isEmpty())
        assertEquals(80.0, fakeWorkoutRepo.exercise(100L)!!.weight, 0.0)
        assertEquals(weightBefore, state.weightInput)
        assertEquals(repsBefore, state.repsInput)
        assertEquals(80.0, state.exercises.first { it.id == exerciseA.id }.plannedWeight, 0.0)
    }

    @Test
    fun applyProgression_confirmUpdatesOnlyTemplateWeight() {
        setupIncreaseWeightScenario()
        val weightBefore = viewModel.uiState.value.weightInput
        val repsBefore = viewModel.uiState.value.repsInput
        val sessionBefore = viewModel.uiState.value.exercises.first { it.id == exerciseA.id }

        viewModel.requestApplyProgressionSuggestion()
        viewModel.confirmApplyProgressionSuggestion()

        val updated = fakeWorkoutRepo.exercise(100L)!!
        assertEquals(82.5, updated.weight, 0.0)
        assertEquals(8, updated.minRepetitions)
        assertEquals(12, updated.maxRepetitions)
        assertEquals("template-a", updated.notes)
        assertEquals(1L, updated.exerciseId)
        assertEquals(10L, updated.blockId)
        assertEquals(0, updated.positionInBlock)
        assertEquals(1, fakeWorkoutRepo.updateBlockExerciseCalls.size)

        val state = viewModel.uiState.value
        assertFalse(state.showApplyProgressionConfirmation)
        assertFalse(state.canApplyProgressionToTemplate)
        assertEquals(R.string.progression_applied, state.infoMessageResId)
        assertEquals(weightBefore, state.weightInput)
        assertEquals(repsBefore, state.repsInput)
        assertEquals(sessionBefore.plannedWeight, state.exercises.first { it.id == exerciseA.id }.plannedWeight, 0.0)
        assertEquals(sessionBefore.minRepetitions, state.exercises.first { it.id == exerciseA.id }.minRepetitions)
        assertEquals(sessionBefore.maxRepetitions, state.exercises.first { it.id == exerciseA.id }.maxRepetitions)
        assertNull(state.error)
    }

    @Test
    fun applyProgression_persistsAfterReload() {
        setupIncreaseWeightScenario()
        viewModel.requestApplyProgressionSuggestion()
        viewModel.confirmApplyProgressionSuggestion()
        assertEquals(82.5, fakeWorkoutRepo.exercise(100L)!!.weight, 0.0)

        // Simulate reloading template observation from repository state.
        val reloaded = fakeWorkoutRepo.exercisesFor(1L)
        assertEquals(82.5, reloaded.first { it.id == 100L }.weight, 0.0)
    }

    @Test
    fun applyProgression_biSetUpdatesOnlyTargetSlot() {
        val blocks = listOf(
            WorkoutBlock(
                id = 10L,
                workoutId = 1L,
                position = 0,
                type = WorkoutBlockType.BI_SET,
                rounds = 3,
                restSeconds = 60,
            ),
        )
        val templateA = WorkoutExercise(
            id = 100L,
            blockId = 10L,
            exerciseId = 1L,
            positionInBlock = 0,
            minRepetitions = 8,
            maxRepetitions = 12,
            weight = 80.0,
            notes = "a",
        )
        val templateB = WorkoutExercise(
            id = 101L,
            blockId = 10L,
            exerciseId = 2L,
            positionInBlock = 1,
            minRepetitions = 10,
            maxRepetitions = 12,
            weight = 40.0,
            notes = "b",
        )
        seedTemplate(blocks = blocks, exercises = listOf(templateA, templateB))

        val sessionA = exerciseA.copy(
            plannedWeight = 80.0,
            blockPosition = 0,
            positionInBlock = 0,
            exerciseName = "Supino",
        )
        val sessionB = exerciseB.copy(
            id = 20L,
            plannedWeight = 40.0,
            blockPosition = 0,
            positionInBlock = 1,
            exerciseName = "Remada",
            plannedSets = 3,
        )
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(sessionA, sessionB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino", listOf(12, 12, 12), 80.0),
        )

        assertTrue(viewModel.uiState.value.canApplyProgressionToTemplate)
        viewModel.requestApplyProgressionSuggestion()
        viewModel.confirmApplyProgressionSuggestion()

        assertEquals(82.5, fakeWorkoutRepo.exercise(100L)!!.weight, 0.0)
        assertEquals(40.0, fakeWorkoutRepo.exercise(101L)!!.weight, 0.0)
        assertEquals("b", fakeWorkoutRepo.exercise(101L)!!.notes)
    }

    @Test
    fun applyProgression_substitutionUsesSlotNotCatalogueOrName() {
        // Template was substituted: catalogue id no longer matches session snapshot.
        seedTemplate(
            exercises = listOf(
                WorkoutExercise(
                    id = 100L,
                    blockId = 10L,
                    exerciseId = 99L, // replaced catalogue id
                    positionInBlock = 0,
                    minRepetitions = 8,
                    maxRepetitions = 12,
                    weight = 80.0,
                    notes = "slot",
                ),
                WorkoutExercise(
                    id = 200L,
                    blockId = 20L,
                    exerciseId = 2L,
                    positionInBlock = 0,
                    minRepetitions = 10,
                    maxRepetitions = 12,
                    weight = 14.5,
                ),
            ),
        )
        val target = exerciseA.copy(
            plannedWeight = 80.0,
            exerciseId = 1L, // stale snapshot catalogue id
            exerciseName = "Supino Antigo",
            blockPosition = 0,
            positionInBlock = 0,
        )
        fakeRepo.emitSession(session)
        fakeRepo.emitExercises(listOf(target, exerciseB))
        fakeRepo.emitCompletedHistory(
            historySets(50L, 5_000L, "Supino Antigo", listOf(12, 12, 12), 80.0),
        )

        viewModel.requestApplyProgressionSuggestion()
        viewModel.confirmApplyProgressionSuggestion()

        val updated = fakeWorkoutRepo.updateBlockExerciseCalls.single()
        assertEquals(100L, updated.id)
        assertEquals(99L, updated.exerciseId)
        assertEquals(82.5, updated.weight, 0.0)
        assertEquals(14.5, fakeWorkoutRepo.exercise(200L)!!.weight, 0.0)
    }

    @Test
    fun applyProgression_errorSurfacesWithoutSuccessMessage() {
        setupIncreaseWeightScenario()
        fakeWorkoutRepo.shouldThrowOnUpdate = true

        viewModel.requestApplyProgressionSuggestion()
        viewModel.confirmApplyProgressionSuggestion()

        val state = viewModel.uiState.value
        assertFalse(state.showApplyProgressionConfirmation)
        assertNull(state.infoMessageResId)
        assertEquals("update failed", state.error)
        assertEquals(80.0, fakeWorkoutRepo.exercise(100L)!!.weight, 0.0)
        assertEquals("82.5", state.weightInput)
    }

    @Test
    fun applyProgression_hiddenWhenTemplateAlreadyHasSuggestedWeight() {
        setupIncreaseWeightScenario(templateWeight = 82.5)
        assertEquals(ProgressionAction.INCREASE_WEIGHT, viewModel.uiState.value.progressionSuggestion!!.action)
        assertFalse(viewModel.uiState.value.canApplyProgressionToTemplate)
    }

    private fun historySets(
        sessionId: Long,
        occurredAt: Long,
        name: String,
        reps: List<Int>,
        weight: Double,
    ): List<CompletedSetRecord> = reps.map { rep ->
        CompletedSetRecord(
            sessionId = sessionId,
            occurredAtMillis = occurredAt,
            exerciseName = name,
            reps = rep,
            weight = weight,
        )
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

private class FakeExecutionWorkoutRepository : WorkoutRepository {
    private val blocksByWorkout = MutableStateFlow<Map<Long, List<WorkoutBlock>>>(emptyMap())
    private val exercisesByWorkout = MutableStateFlow<Map<Long, List<WorkoutExercise>>>(emptyMap())
    val updateBlockExerciseCalls = mutableListOf<WorkoutExercise>()
    var shouldThrowOnUpdate = false

    fun emitTemplate(
        workoutId: Long,
        blocks: List<WorkoutBlock>,
        exercises: List<WorkoutExercise>,
    ) {
        blocksByWorkout.value = blocksByWorkout.value + (workoutId to blocks)
        exercisesByWorkout.value = exercisesByWorkout.value + (workoutId to exercises)
    }

    fun exercise(id: Long): WorkoutExercise? =
        exercisesByWorkout.value.values.flatten().firstOrNull { it.id == id }

    fun exercisesFor(workoutId: Long): List<WorkoutExercise> =
        exercisesByWorkout.value[workoutId].orEmpty()

    override fun getAll(): Flow<List<Workout>> = flowOf(emptyList())
    override fun getById(id: Long): Flow<Workout?> = flowOf(null)
    override fun observeWorkoutIdsWithExercises(): Flow<Set<Long>> = flowOf(emptySet())
    override suspend fun save(workout: Workout): Long = 0L
    override suspend fun update(workout: Workout) = Unit
    override suspend fun delete(workout: Workout) = Unit

    override fun getBlocks(workoutId: Long): Flow<List<WorkoutBlock>> =
        blocksByWorkout.map { it[workoutId].orEmpty() }

    override fun getExercisesForWorkout(workoutId: Long): Flow<List<WorkoutExercise>> =
        exercisesByWorkout.map { it[workoutId].orEmpty() }

    override suspend fun addBlock(block: WorkoutBlock, exercises: List<WorkoutExercise>): Long = 0L
    override suspend fun duplicateBlock(blockId: Long): Long = 0L
    override suspend fun updateBlock(block: WorkoutBlock) = Unit

    override suspend fun updateBlockExercise(exercise: WorkoutExercise) {
        if (shouldThrowOnUpdate) throw RuntimeException("update failed")
        updateBlockExerciseCalls += exercise
        exercisesByWorkout.value = exercisesByWorkout.value.mapValues { (_, list) ->
            list.map { if (it.id == exercise.id) exercise else it }
        }
    }

    override suspend fun replaceBlockExercise(exerciseRowId: Long, newCatalogueExerciseId: Long) = Unit
    override suspend fun removeBlock(blockId: Long) = Unit
    override suspend fun updateBlockPositions(positions: Map<Long, Int>) = Unit
    override suspend fun addExercise(workoutExercise: WorkoutExercise): Long = 0L
    override suspend fun updateExercise(workoutExercise: WorkoutExercise) = Unit
    override suspend fun removeExercise(workoutExercise: WorkoutExercise) = Unit
    override suspend fun removeExerciseById(id: Long) = Unit
    override suspend fun updateExercisePositions(positions: Map<Long, Int>) = Unit
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
    private val completedHistoryFlow =
        MutableStateFlow<List<com.gymtrack.domain.model.CompletedSetRecord>>(emptyList())
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

    fun emitCompletedHistory(records: List<com.gymtrack.domain.model.CompletedSetRecord>) {
        completedHistoryFlow.value = records
    }

    override suspend fun startSession(workoutId: Long): StartSessionResult =
        StartSessionResult.Created(0L)

    override suspend fun getSession(id: Long): WorkoutSession? = sessionFlow.value

    override fun observeSession(id: Long): Flow<WorkoutSession?> = sessionFlow

    override fun observeInProgress(): Flow<WorkoutSession?> = emptyFlow()
    override fun observeCompletedSessions(): Flow<List<com.gymtrack.domain.model.WorkoutHistoryItem>> = emptyFlow()
    override fun observeCompletedSetHistory(): Flow<List<com.gymtrack.domain.model.CompletedSetRecord>> =
        completedHistoryFlow

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

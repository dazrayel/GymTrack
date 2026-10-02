package com.gymtrack.presentation.workouts.execution

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.R
import com.gymtrack.domain.model.CompletedSetRecord
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.ProgressionAction
import com.gymtrack.domain.model.ProgressionSuggestion
import com.gymtrack.domain.model.WorkoutBlock
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSessionStatus
import com.gymtrack.domain.model.WorkoutSet
import com.gymtrack.domain.model.areAllSessionExercisesComplete
import com.gymtrack.domain.model.elapsedMillis
import com.gymtrack.domain.model.evaluateLoadProgression
import com.gymtrack.domain.model.lastCompletedSessionSetsForExercise
import com.gymtrack.domain.model.resolveCurrentExerciseIndex
import com.gymtrack.domain.model.shouldRestAfterCompletingSet
import com.gymtrack.domain.model.workoutProgress
import com.gymtrack.domain.repository.ExerciseRepository
import com.gymtrack.domain.repository.WorkoutRepository
import com.gymtrack.domain.repository.WorkoutSessionRepository
import com.gymtrack.domain.time.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutExecutionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val timeProvider: TimeProvider,
    private val draftStore: WorkoutExecutionDraftStore,
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(WorkoutExecutionUiState(sessionId = sessionId))
    val uiState: StateFlow<WorkoutExecutionUiState> = _uiState.asStateFlow()

    private val exerciseCatalogue = MutableStateFlow<Map<Long, Exercise>>(emptyMap())
    private val templateBlocks = MutableStateFlow<List<WorkoutBlock>>(emptyList())
    private val templateExercises = MutableStateFlow<List<WorkoutExercise>>(emptyList())

    /** Avoids re-evaluating progression when the current exercise/history snapshot is unchanged. */
    private var lastProgressionExerciseKey: String? = null

    private var restExpiryHandledFor: Long? = null
    private var restBeepHandledFor: Long? = null
    private var observingWorkoutId: Long? = null
    private var templateObservationJob: Job? = null

    init {
        observeExerciseCatalogue()
        observeSessionProgress()
        startUiTicker()
    }

    private fun observeExerciseCatalogue() {
        viewModelScope.launch {
            exerciseRepository.getAll()
                .catch { /* Media is optional; keep empty catalogue. */ }
                .collect { exercises ->
                    exerciseCatalogue.value = exercises.associateBy { it.id }
                    _uiState.update { current ->
                        current.copy(
                            mediaExercise = resolveMediaExercise(
                                current.currentExercise,
                                exerciseCatalogue.value,
                            ),
                        )
                    }
                }
        }
    }

    private fun observeSessionProgress() {
        viewModelScope.launch {
            combine(
                workoutSessionRepository.observeSession(sessionId),
                observeExercisesWithSets(),
                workoutSessionRepository.observeCompletedSetHistory(),
            ) { session, exercisesWithSets, completedHistory ->
                Triple(session, exercisesWithSets, completedHistory)
            }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { (session, exercisesWithSets, completedHistory) ->
                    val (exercises, setsByExerciseId) = exercisesWithSets
                    applyPersistedProgress(
                        session = session,
                        exercises = exercises,
                        setsByExerciseId = setsByExerciseId,
                        completedHistory = completedHistory,
                    )
                    maybeExpireRest(session)
                }
        }
    }

    private fun observeExercisesWithSets() =
        workoutSessionRepository.observeSessionExercises(sessionId)
            .flatMapLatest { exercises ->
                val sorted = exercises.sortedBy { it.position }
                if (sorted.isEmpty()) {
                    flowOf(sorted to emptyMap<Long, List<WorkoutSet>>())
                } else {
                    combine(sorted.map { exercise ->
                        workoutSessionRepository.observeSets(exercise.id)
                    }) { setLists ->
                        val setsByExerciseId = sorted.mapIndexed { index, exercise ->
                            exercise.id to setLists[index]
                        }.toMap()
                        sorted to setsByExerciseId
                    }
                }
            }

    private fun startUiTicker() {
        viewModelScope.launch(Dispatchers.Default) {
            while (coroutineContext.isActive) {
                delay(1_000)
                val session = _uiState.value.session
                val remaining = remainingFrom(session)
                _uiState.update { current ->
                    current.copy(
                        elapsedMillis = elapsedMillis(current.session, timeProvider.nowMillis()),
                        restRemainingMillis = remaining,
                        restBeepEvent = restBeepEventOrNull(session, remaining, current.restBeepEvent),
                    )
                }
                maybeExpireRest(session)
            }
        }
    }

    private fun applyPersistedProgress(
        session: WorkoutSession?,
        exercises: List<WorkoutSessionExercise>,
        setsByExerciseId: Map<Long, List<WorkoutSet>>,
        completedHistory: List<CompletedSetRecord>,
    ) {
        val pendingIndex = resolveCurrentExerciseIndex(exercises, setsByExerciseId)
        val isComplete = areAllSessionExercisesComplete(exercises, setsByExerciseId)
        val exerciseIndex = if (isComplete) 0 else pendingIndex
        val currentExercise = if (isComplete || pendingIndex < 0) {
            null
        } else {
            exercises.getOrNull(pendingIndex)
        }
        val setIndex = currentExercise?.let { setsByExerciseId[it.id]?.size ?: 0 } ?: 0
        val remaining = remainingFrom(session)
        val isCompleted = session?.status == WorkoutSessionStatus.COMPLETED
        val isPaused = session?.restPausedRemainingMillis != null
        val isTimedRest = session?.restEndsAtMillis != null && remaining > 0L
        val isResting = !isCompleted && (isPaused || isTimedRest)
        val phase = when {
            isCompleted -> WorkoutExecutionPhase.FINISHED
            isResting -> WorkoutExecutionPhase.RESTING
            isComplete -> WorkoutExecutionPhase.FINISHED
            else -> WorkoutExecutionPhase.WORKING
        }
        val progress = workoutProgress(exercises, setsByExerciseId)
        ensureTemplateObservation(session?.workoutId)

        _uiState.update { current ->
            val workingExercise =
                if (phase == WorkoutExecutionPhase.WORKING) currentExercise else null
            val inputsBelongToOther =
                workingExercise != null && current.inputsExerciseId != workingExercise.id
            val inputsBlank = current.repsInput.isBlank() || current.weightInput.isBlank()
            val shouldPrefill = workingExercise != null && (inputsBelongToOther || inputsBlank)
            val draft = workingExercise?.let { draftStore.get(sessionId, it.id) }
            val progressionSuggestion = progressionSuggestionFor(
                exercise = currentExercise,
                completedHistory = completedHistory,
                previous = current.progressionSuggestion,
            )
            val canApply = canApplyProgressionToTemplate(
                sessionExercise = workingExercise,
                suggestion = progressionSuggestion,
                blocks = templateBlocks.value,
                exercises = templateExercises.value,
            )

            val nextReps = if (shouldPrefill) {
                draft?.repsInput ?: workingExercise.minRepetitions.toString()
            } else {
                current.repsInput
            }
            val nextWeight = resolveWeightInput(
                workingExercise = workingExercise,
                current = current,
                draft = draft,
                shouldPrefill = shouldPrefill,
                suggestion = progressionSuggestion,
            )

            current.copy(
                session = session,
                exercises = exercises,
                setsByExerciseId = setsByExerciseId,
                mediaExercise = resolveMediaExercise(currentExercise, exerciseCatalogue.value),
                progressionSuggestion = progressionSuggestion,
                canApplyProgressionToTemplate = canApply,
                currentExerciseIndex = exerciseIndex,
                currentSetIndex = setIndex,
                isWorkoutComplete = isComplete && !isResting,
                phase = phase,
                restEndsAtMillis = session?.restEndsAtMillis,
                restPausedRemainingMillis = session?.restPausedRemainingMillis,
                restSessionExerciseId = session?.restSessionExerciseId,
                restAfterSetIndex = session?.restAfterSetIndex,
                restRemainingMillis = remaining,
                restBeepEvent = restBeepEventOrNull(session, remaining, current.restBeepEvent),
                elapsedMillis = elapsedMillis(session, timeProvider.nowMillis()),
                completedSets = progress.completedSets,
                plannedSets = progress.plannedSets,
                completedExercises = progress.completedExercises,
                totalExercises = progress.totalExercises,
                progressPercent = progress.progressPercent,
                isLoading = false,
                repsInput = nextReps,
                weightInput = nextWeight,
                inputsExerciseId = workingExercise?.id ?: current.inputsExerciseId,
                repsError = if (shouldPrefill) null else current.repsError,
                weightError = if (shouldPrefill) null else current.weightError,
            )
        }
    }

    /**
     * Prefills weight only when there is no draft for this exercise.
     * [ProgressionAction.INCREASE_WEIGHT] uses [ProgressionSuggestion.suggestedWeight];
     * other actions keep [WorkoutSessionExercise.plannedWeight].
     *
     * If history arrives after an initial planned-weight prefill and the user has not edited
     * (no draft, weight still equals planned), upgrades once to the suggested weight.
     */
    private fun resolveWeightInput(
        workingExercise: WorkoutSessionExercise?,
        current: WorkoutExecutionUiState,
        draft: ExerciseInputDraft?,
        shouldPrefill: Boolean,
        suggestion: ProgressionSuggestion?,
    ): String {
        if (workingExercise == null) return current.weightInput
        if (draft != null) {
            return if (shouldPrefill) draft.weightInput else current.weightInput
        }
        val progressionDefault = defaultWeightPrefill(workingExercise, suggestion)
        if (shouldPrefill) return progressionDefault

        val plannedText = formatWeight(workingExercise.plannedWeight)
        val canUpgradeFromPlannedPrefill =
            current.inputsExerciseId == workingExercise.id &&
                current.weightInput == plannedText &&
                progressionDefault != plannedText
        return if (canUpgradeFromPlannedPrefill) progressionDefault else current.weightInput
    }

    private fun defaultWeightPrefill(
        exercise: WorkoutSessionExercise,
        suggestion: ProgressionSuggestion?,
    ): String {
        val suggested = suggestion
            ?.takeIf { it.action == ProgressionAction.INCREASE_WEIGHT }
            ?.suggestedWeight
        return formatWeight(suggested ?: exercise.plannedWeight)
    }

    private fun progressionSuggestionFor(
        exercise: WorkoutSessionExercise?,
        completedHistory: List<CompletedSetRecord>,
        previous: ProgressionSuggestion?,
    ): ProgressionSuggestion? {
        if (exercise == null) {
            lastProgressionExerciseKey = null
            return null
        }
        val key = progressionCacheKey(exercise, completedHistory)
        if (key == lastProgressionExerciseKey && previous != null) {
            return previous
        }
        lastProgressionExerciseKey = key
        return evaluateLoadProgression(
            plannedWeight = exercise.plannedWeight,
            minRepetitions = exercise.minRepetitions,
            maxRepetitions = exercise.maxRepetitions,
            plannedSets = exercise.plannedSets,
            lastSessionSets = lastCompletedSessionSetsForExercise(
                history = completedHistory,
                exerciseName = exercise.exerciseName,
                excludeSessionId = sessionId,
            ),
        )
    }

    private fun progressionCacheKey(
        exercise: WorkoutSessionExercise,
        completedHistory: List<CompletedSetRecord>,
    ): String {
        val historyFingerprint = completedHistory
            .asSequence()
            .filter { it.exerciseName == exercise.exerciseName && it.sessionId != sessionId }
            .map { "${it.sessionId}:${it.occurredAtMillis}:${it.reps}:${it.weight}" }
            .joinToString("|")
        return listOf(
            exercise.id.toString(),
            exercise.exerciseName,
            exercise.plannedWeight.toString(),
            exercise.minRepetitions.toString(),
            exercise.maxRepetitions.toString(),
            exercise.plannedSets.toString(),
            historyFingerprint,
        ).joinToString("#")
    }

    private fun ensureTemplateObservation(workoutId: Long?) {
        if (workoutId == observingWorkoutId) return
        templateObservationJob?.cancel()
        observingWorkoutId = workoutId
        templateBlocks.value = emptyList()
        templateExercises.value = emptyList()
        if (workoutId == null) {
            _uiState.update { it.copy(canApplyProgressionToTemplate = false) }
            return
        }
        templateObservationJob = viewModelScope.launch {
            combine(
                workoutRepository.getBlocks(workoutId),
                workoutRepository.getExercisesForWorkout(workoutId),
            ) { blocks, exercises -> blocks to exercises }
                .collect { (blocks, exercises) ->
                    templateBlocks.value = blocks
                    templateExercises.value = exercises
                    _uiState.update { current ->
                        current.copy(
                            canApplyProgressionToTemplate = canApplyProgressionToTemplate(
                                sessionExercise = current.currentExercise
                                    ?.takeIf { current.phase == WorkoutExecutionPhase.WORKING },
                                suggestion = current.progressionSuggestion,
                                blocks = blocks,
                                exercises = exercises,
                            ),
                        )
                    }
                }
        }
    }

    private fun canApplyProgressionToTemplate(
        sessionExercise: WorkoutSessionExercise?,
        suggestion: ProgressionSuggestion?,
        blocks: List<WorkoutBlock>,
        exercises: List<WorkoutExercise>,
    ): Boolean {
        if (suggestion?.action != ProgressionAction.INCREASE_WEIGHT) return false
        val suggested = suggestion.suggestedWeight ?: return false
        val template = resolveTemplateExercise(sessionExercise, blocks, exercises) ?: return false
        return abs(template.weight - suggested) >= 0.001
    }

    /**
     * Resolves the template slot by block position + positionInBlock (stable WorkoutExercise row),
     * not by catalogue id or exercise name.
     */
    private fun resolveTemplateExercise(
        sessionExercise: WorkoutSessionExercise?,
        blocks: List<WorkoutBlock>,
        exercises: List<WorkoutExercise>,
    ): WorkoutExercise? {
        if (sessionExercise == null) return null
        val block = blocks.firstOrNull { it.position == sessionExercise.blockPosition } ?: return null
        return exercises.firstOrNull {
            it.blockId == block.id && it.positionInBlock == sessionExercise.positionInBlock
        }
    }

    fun requestApplyProgressionSuggestion() {
        val state = _uiState.value
        if (state.phase != WorkoutExecutionPhase.WORKING) return
        if (!state.canApplyProgressionToTemplate) return
        val suggestion = state.progressionSuggestion ?: return
        val suggested = suggestion.suggestedWeight ?: return
        if (suggestion.action != ProgressionAction.INCREASE_WEIGHT) return
        val template = resolveTemplateExercise(
            sessionExercise = state.currentExercise,
            blocks = templateBlocks.value,
            exercises = templateExercises.value,
        ) ?: return
        _uiState.update {
            it.copy(
                showApplyProgressionConfirmation = true,
                applyProgressionFromWeight = template.weight,
                applyProgressionToWeight = suggested,
            )
        }
    }

    fun dismissApplyProgressionConfirmation() {
        _uiState.update {
            it.copy(
                showApplyProgressionConfirmation = false,
                applyProgressionFromWeight = null,
                applyProgressionToWeight = null,
            )
        }
    }

    fun confirmApplyProgressionSuggestion() {
        val state = _uiState.value
        if (!state.showApplyProgressionConfirmation) return
        val suggestion = state.progressionSuggestion ?: return
        val suggested = suggestion.suggestedWeight ?: return
        if (suggestion.action != ProgressionAction.INCREASE_WEIGHT) return
        val template = resolveTemplateExercise(
            sessionExercise = state.currentExercise,
            blocks = templateBlocks.value,
            exercises = templateExercises.value,
        ) ?: run {
            _uiState.update {
                it.copy(
                    showApplyProgressionConfirmation = false,
                    applyProgressionFromWeight = null,
                    applyProgressionToWeight = null,
                    error = "Exercício do treino não encontrado",
                )
            }
            return
        }
        viewModelScope.launch {
            try {
                workoutRepository.updateBlockExercise(template.copy(weight = suggested))
                _uiState.update {
                    it.copy(
                        showApplyProgressionConfirmation = false,
                        applyProgressionFromWeight = null,
                        applyProgressionToWeight = null,
                        infoMessageResId = R.string.progression_applied,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        showApplyProgressionConfirmation = false,
                        applyProgressionFromWeight = null,
                        applyProgressionToWeight = null,
                        error = e.message,
                    )
                }
            }
        }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessageResId = null) }
    }

    /**
     * Emits a one-shot beep event when rest remaining first hits zero for a given endsAt.
     * Does not repeat while the UI stays at 00:00.
     */
    private fun restBeepEventOrNull(
        session: WorkoutSession?,
        remainingMillis: Long,
        currentEvent: Long?,
    ): Long? {
        val endsAt = session?.restEndsAtMillis ?: return currentEvent
        if (session.restPausedRemainingMillis != null) return currentEvent
        if (remainingMillis > 0L) return currentEvent
        if (restBeepHandledFor == endsAt) return currentEvent
        restBeepHandledFor = endsAt
        return endsAt
    }

    private fun remainingFrom(session: WorkoutSession?): Long {
        val paused = session?.restPausedRemainingMillis
        if (paused != null) return maxOf(0L, paused)
        val endsAt = session?.restEndsAtMillis ?: return 0L
        return maxOf(0L, endsAt - timeProvider.nowMillis())
    }

    private fun resolveMediaExercise(
        sessionExercise: WorkoutSessionExercise?,
        catalogue: Map<Long, Exercise>,
    ): Exercise? {
        val exerciseId = sessionExercise?.exerciseId ?: return null
        return catalogue[exerciseId]
    }

    private fun maybeExpireRest(session: WorkoutSession?) {
        if (session?.status == WorkoutSessionStatus.COMPLETED) return
        val endsAt = session?.restEndsAtMillis ?: return
        if (session.restPausedRemainingMillis != null) return
        if (remainingFrom(session) > 0L) return
        if (restExpiryHandledFor == endsAt) return
        restExpiryHandledFor = endsAt
        viewModelScope.launch {
            try {
                workoutSessionRepository.skipRest(sessionId)
            } catch (e: Exception) {
                restExpiryHandledFor = null
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onRepsChanged(value: String) {
        _uiState.update {
            it.copy(
                repsInput = value,
                repsError = null,
                inputsExerciseId = it.currentExercise?.id ?: it.inputsExerciseId,
            )
        }
        persistCurrentDraft()
    }

    fun onWeightChanged(value: String) {
        _uiState.update {
            it.copy(
                weightInput = value,
                weightError = null,
                inputsExerciseId = it.currentExercise?.id ?: it.inputsExerciseId,
            )
        }
        persistCurrentDraft()
    }

    private fun persistCurrentDraft() {
        val state = _uiState.value
        val exercise = state.currentExercise ?: return
        if (state.phase != WorkoutExecutionPhase.WORKING) return
        draftStore.put(
            sessionId = sessionId,
            exerciseId = exercise.id,
            repsInput = state.repsInput,
            weightInput = state.weightInput,
        )
    }

    fun completeCurrentSet() {
        val state = _uiState.value
        if (state.phase != WorkoutExecutionPhase.WORKING) return
        if (state.isWorkoutComplete) return
        val exercise = state.currentExercise ?: return

        val repsError = integerFieldError(
            raw = state.repsInput,
            minValue = 1,
            belowMinRes = R.string.validation_reps_min,
        )
        val weightError = doubleFieldError(state.weightInput)
        if (repsError != null || weightError != null) {
            _uiState.update { it.copy(repsError = repsError, weightError = weightError) }
            return
        }

        val reps = parseRequiredIntAtLeast(state.repsInput, minValue = 1) ?: return
        val weight = parseRequiredNonNegativeDouble(state.weightInput) ?: return
        val setIndex = state.setsByExerciseId[exercise.id]?.size ?: 0

        viewModelScope.launch {
            try {
                workoutSessionRepository.completeSet(
                    sessionExerciseId = exercise.id,
                    setIndex = setIndex,
                    reps = reps,
                    weight = weight,
                )
                val completedSetsAfter = setIndex + 1
                if (completedSetsAfter < exercise.plannedSets) {
                    // Keep last entered values as draft for the next set of the same exercise.
                    draftStore.put(
                        sessionId = sessionId,
                        exerciseId = exercise.id,
                        repsInput = state.repsInput,
                        weightInput = state.weightInput,
                    )
                } else {
                    draftStore.clear(sessionId, exercise.id)
                }
                val latestSets = state.setsByExerciseId.toMutableMap()
                val existing = latestSets[exercise.id].orEmpty().toMutableList()
                // Optimistic local count for rest decision; DB already persisted the set.
                while (existing.size < completedSetsAfter) {
                    existing.add(
                        com.gymtrack.domain.model.WorkoutSet(
                            sessionExerciseId = exercise.id,
                            setIndex = existing.size,
                            reps = reps,
                            weight = weight,
                            completedAtMillis = 0L,
                        ),
                    )
                }
                latestSets[exercise.id] = existing
                if (
                    shouldRestAfterCompletingSet(
                        completedExercise = exercise,
                        completedSetCountAfter = completedSetsAfter,
                        exercises = state.exercises,
                        setsByExerciseId = latestSets,
                    )
                ) {
                    workoutSessionRepository.startRest(
                        sessionId = sessionId,
                        sessionExerciseId = exercise.id,
                        afterSetIndex = setIndex,
                        restSeconds = exercise.restSeconds,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun pauseRest() {
        viewModelScope.launch {
            try {
                workoutSessionRepository.pauseRest(sessionId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun resumeRest() {
        viewModelScope.launch {
            try {
                workoutSessionRepository.resumeRest(sessionId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun skipRest() {
        viewModelScope.launch {
            try {
                workoutSessionRepository.skipRest(sessionId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun requestSkip() {
        val state = _uiState.value
        if (state.phase != WorkoutExecutionPhase.WORKING) return
        if (state.currentExercise == null) return
        _uiState.update { it.copy(showSkipConfirmation = true) }
    }

    fun dismissSkipConfirmation() {
        _uiState.update { it.copy(showSkipConfirmation = false) }
    }

    fun confirmSkip() {
        val exercise = _uiState.value.currentExercise ?: return
        if (_uiState.value.phase != WorkoutExecutionPhase.WORKING) return
        viewModelScope.launch {
            try {
                workoutSessionRepository.skipSessionExercise(exercise.id)
                _uiState.update { it.copy(showSkipConfirmation = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(showSkipConfirmation = false, error = e.message) }
            }
        }
    }

    fun resumeExercise(sessionExerciseId: Long) {
        val state = _uiState.value
        if (state.phase != WorkoutExecutionPhase.WORKING) return
        val target = state.exercises.firstOrNull { it.id == sessionExerciseId } ?: return
        val completedSets = state.setsByExerciseId[target.id]?.size ?: 0
        if (completedSets >= target.plannedSets) return
        if (state.currentExercise?.id == sessionExerciseId) return
        viewModelScope.launch {
            try {
                workoutSessionRepository.resumeSessionExercise(sessionExerciseId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun requestFinish() {
        val pending = _uiState.value.hasIncompleteExercises
        _uiState.update {
            it.copy(showFinishConfirmation = true, finishHasPendingExercises = pending)
        }
    }

    fun dismissFinishConfirmation() {
        _uiState.update { it.copy(showFinishConfirmation = false) }
    }

    fun confirmFinish() {
        viewModelScope.launch {
            try {
                workoutSessionRepository.finishSession(sessionId)
                draftStore.clearSession(sessionId)
                _uiState.update {
                    it.copy(
                        showFinishConfirmation = false,
                        sessionFinishedEvent = sessionId,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        showFinishConfirmation = false,
                        error = e.message,
                    )
                }
            }
        }
    }

    fun consumeSessionFinishedEvent() {
        _uiState.update { it.copy(sessionFinishedEvent = null) }
    }

    fun consumeRestBeepEvent() {
        _uiState.update { it.copy(restBeepEvent = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

private fun formatWeight(weight: Double): String =
    if (weight == weight.toLong().toDouble()) weight.toLong().toString() else weight.toString()

private fun parseRequiredIntAtLeast(raw: String, minValue: Int): Int? {
    val trimmed = raw.trim()
    if (!trimmed.matches(Regex("-?\\d+"))) return null
    val parsed = trimmed.toIntOrNull() ?: return null
    return parsed.takeIf { it >= minValue }
}

private fun parseRequiredNonNegativeDouble(raw: String): Double? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val parsed = trimmed.toDoubleOrNull() ?: return null
    return parsed.takeIf { it >= 0.0 }
}

private fun integerFieldError(raw: String, minValue: Int, belowMinRes: Int): Int? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return R.string.field_required
    if (!trimmed.matches(Regex("-?\\d+"))) return R.string.validation_invalid_value
    val parsed = trimmed.toIntOrNull() ?: return R.string.validation_invalid_value
    if (parsed < minValue) return belowMinRes
    return null
}

private fun doubleFieldError(raw: String): Int? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return R.string.field_required
    val parsed = trimmed.toDoubleOrNull() ?: return R.string.validation_invalid_value
    if (parsed < 0.0) return R.string.validation_weight_min
    return null
}

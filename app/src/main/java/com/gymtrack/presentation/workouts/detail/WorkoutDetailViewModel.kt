package com.gymtrack.presentation.workouts.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.StartSessionResult
import com.gymtrack.domain.model.WorkoutBlock
import com.gymtrack.domain.model.WorkoutBlockDetail
import com.gymtrack.domain.model.WorkoutBlockType
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.model.WorkoutExerciseDetail
import com.gymtrack.domain.repository.ExerciseRepository
import com.gymtrack.domain.repository.WorkoutRepository
import com.gymtrack.domain.repository.WorkoutSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val workoutId: Long = checkNotNull(savedStateHandle["workoutId"])

    private val _uiState = MutableStateFlow(WorkoutDetailUiState())
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()

    val availableExercises: StateFlow<List<Exercise>> = exerciseRepository.getAll()
        .catch { e -> _uiState.update { it.copy(error = e.message) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    init {
        observeWorkout()
        observeBlocks()
    }

    private fun observeWorkout() {
        viewModelScope.launch {
            workoutRepository.getById(workoutId)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { workout ->
                    _uiState.update { it.copy(workout = workout, isLoading = false) }
                }
        }
    }

    private fun observeBlocks() {
        viewModelScope.launch {
            combine(
                workoutRepository.getBlocks(workoutId),
                workoutRepository.getExercisesForWorkout(workoutId),
                exerciseRepository.getAll(),
            ) { blocks, exercises, catalogue ->
                val catalogueById = catalogue.associateBy { it.id }
                val byBlock = exercises.groupBy { it.blockId }
                blocks.sortedBy { it.position }.map { block ->
                    val items = byBlock[block.id]
                        .orEmpty()
                        .sortedBy { it.positionInBlock }
                        .mapNotNull { we ->
                            val exercise = catalogueById[we.exerciseId] ?: return@mapNotNull null
                            WorkoutExerciseDetail(we, exercise)
                        }
                    WorkoutBlockDetail(block = block, items = items)
                }
            }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { details ->
                    _uiState.update { it.copy(blocks = details, isLoading = false) }
                }
        }
    }

    fun showAddTypeDialog() {
        _uiState.update { it.copy(showAddTypeDialog = true) }
    }

    fun dismissAddTypeDialog() {
        _uiState.update { it.copy(showAddTypeDialog = false) }
    }

    fun chooseAddType(type: WorkoutBlockType) {
        when (type) {
            WorkoutBlockType.SINGLE -> {
                _uiState.update {
                    it.copy(
                        showAddTypeDialog = false,
                        pendingBlockType = WorkoutBlockType.SINGLE,
                        showExercisePicker = true,
                        blockDraftSlotIndex = null,
                        showBlockBuilder = false,
                    )
                }
            }
            WorkoutBlockType.BI_SET, WorkoutBlockType.TRI_SET -> {
                val slots = List(type.requiredExerciseCount) { null as Exercise? }
                _uiState.update {
                    it.copy(
                        showAddTypeDialog = false,
                        pendingBlockType = type,
                        blockDraftSlots = slots,
                        showBlockBuilder = true,
                        blockDraftSlotIndex = null,
                    )
                }
            }
        }
    }

    fun dismissBlockBuilder() {
        _uiState.update {
            it.copy(
                showBlockBuilder = false,
                pendingBlockType = null,
                blockDraftSlots = emptyList(),
                blockDraftSlotIndex = null,
            )
        }
    }

    fun pickSlot(index: Int) {
        _uiState.update {
            it.copy(blockDraftSlotIndex = index, showExercisePicker = true)
        }
    }

    fun clearSlot(index: Int) {
        _uiState.update { state ->
            val slots = state.blockDraftSlots.toMutableList()
            if (index in slots.indices) slots[index] = null
            state.copy(blockDraftSlots = slots)
        }
    }

    fun confirmBlockDraft(rounds: Int, restSeconds: Int) {
        val type = _uiState.value.pendingBlockType ?: return
        val slots = _uiState.value.blockDraftSlots
        if (slots.any { it == null } || slots.size != type.requiredExerciseCount) return
        val position = _uiState.value.blocks.size
        val exercises = slots.mapIndexed { index, exercise ->
            WorkoutExercise(
                blockId = 0,
                exerciseId = exercise!!.id,
                positionInBlock = index,
                minRepetitions = 8,
                maxRepetitions = 12,
                weight = 0.0,
            )
        }
        val block = WorkoutBlock(
            workoutId = workoutId,
            position = position,
            type = type,
            rounds = rounds.coerceAtLeast(1),
            restSeconds = restSeconds.coerceAtLeast(0),
        )
        viewModelScope.launch {
            try {
                workoutRepository.addBlock(block, exercises)
                _uiState.update {
                    it.copy(
                        showBlockBuilder = false,
                        pendingBlockType = null,
                        blockDraftSlots = emptyList(),
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun showExercisePicker() {
        _uiState.update { it.copy(showExercisePicker = true, pendingBlockType = WorkoutBlockType.SINGLE) }
    }

    fun dismissExercisePicker() {
        _uiState.update {
            it.copy(showExercisePicker = false, blockDraftSlotIndex = null)
        }
    }

    fun selectExercise(exercise: Exercise) {
        val slotIndex = _uiState.value.blockDraftSlotIndex
        if (slotIndex != null) {
            _uiState.update { state ->
                val slots = state.blockDraftSlots.toMutableList()
                if (slotIndex in slots.indices) slots[slotIndex] = exercise
                state.copy(
                    blockDraftSlots = slots,
                    showExercisePicker = false,
                    blockDraftSlotIndex = null,
                )
            }
            return
        }

        val draft = WorkoutExercise(
            id = 0L,
            blockId = 0L,
            exerciseId = exercise.id,
            positionInBlock = 0,
            minRepetitions = 8,
            maxRepetitions = 12,
            weight = 0.0,
            notes = "",
        )
        _uiState.update {
            it.copy(
                showExercisePicker = false,
                exerciseToConfigure = WorkoutExerciseDetail(draft, exercise),
                configureBlock = null,
                configureRounds = 3,
                configureRestSeconds = 60,
            )
        }
    }

    fun showEditExercise(block: WorkoutBlockDetail, item: WorkoutExerciseDetail) {
        _uiState.update {
            it.copy(
                exerciseToConfigure = item,
                configureBlock = block.block,
                configureRounds = block.rounds,
                configureRestSeconds = block.restSeconds,
            )
        }
    }

    fun dismissExerciseConfiguration() {
        _uiState.update {
            it.copy(exerciseToConfigure = null, configureBlock = null)
        }
    }

    fun saveExerciseConfiguration(
        sets: Int,
        minRepetitions: Int,
        maxRepetitions: Int,
        weight: Double,
        restSeconds: Int,
        notes: String,
    ) {
        val toConfigure = _uiState.value.exerciseToConfigure ?: return
        val updated = toConfigure.workoutExercise.copy(
            minRepetitions = minRepetitions,
            maxRepetitions = maxRepetitions,
            weight = weight,
            notes = notes.trim(),
        )
        viewModelScope.launch {
            try {
                val existingBlock = _uiState.value.configureBlock
                if (existingBlock == null || updated.blockId == 0L) {
                    val block = WorkoutBlock(
                        workoutId = workoutId,
                        position = _uiState.value.blocks.size,
                        type = WorkoutBlockType.SINGLE,
                        rounds = sets.coerceAtLeast(1),
                        restSeconds = restSeconds.coerceAtLeast(0),
                    )
                    workoutRepository.addBlock(block, listOf(updated))
                } else {
                    // Preserve blockId / positionInBlock — only update exercise fields.
                    workoutRepository.updateBlockExercise(updated)
                    // Séries/descanso belong to the block; only SINGLE edits them here.
                    // BI/TRI keep shared block rounds/rest unchanged when configuring a slot.
                    if (existingBlock.type == WorkoutBlockType.SINGLE) {
                        workoutRepository.updateBlock(
                            existingBlock.copy(
                                rounds = sets.coerceAtLeast(1),
                                restSeconds = restSeconds.coerceAtLeast(0),
                            ),
                        )
                    }
                }
                _uiState.update {
                    it.copy(exerciseToConfigure = null, configureBlock = null)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun showDeleteConfirmation(block: WorkoutBlockDetail) {
        _uiState.update { it.copy(showDeleteConfirmation = true, blockToDelete = block) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false, blockToDelete = null) }
    }

    fun confirmDelete() {
        val block = _uiState.value.blockToDelete ?: return
        viewModelScope.launch {
            try {
                workoutRepository.removeBlock(block.id)
                _uiState.update { it.copy(showDeleteConfirmation = false, blockToDelete = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(showDeleteConfirmation = false, blockToDelete = null, error = e.message)
                }
            }
        }
    }

    fun duplicateBlock(blockId: Long) {
        viewModelScope.launch {
            try {
                workoutRepository.duplicateBlock(blockId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun reorderBlocks(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.blocks
        if (fromIndex == toIndex) return
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val reordered = current.toMutableList()
        val moved = reordered.removeAt(fromIndex)
        reordered.add(toIndex, moved)
        val positions = reordered.mapIndexed { index, detail -> detail.id to index }.toMap()
        viewModelScope.launch {
            try {
                workoutRepository.updateBlockPositions(positions)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun showEditWorkoutDialog() {
        _uiState.update { it.copy(showEditWorkoutDialog = true) }
    }

    fun dismissEditWorkoutDialog() {
        _uiState.update { it.copy(showEditWorkoutDialog = false) }
    }

    fun startWorkout() {
        viewModelScope.launch {
            try {
                when (val result = workoutSessionRepository.startSession(workoutId)) {
                    is StartSessionResult.Created -> {
                        _uiState.update {
                            it.copy(sessionStartedEvent = result.sessionId, inProgressConflict = null)
                        }
                    }
                    is StartSessionResult.Resumed -> {
                        _uiState.update {
                            it.copy(sessionStartedEvent = result.sessionId, inProgressConflict = null)
                        }
                    }
                    is StartSessionResult.BlockedOtherWorkout -> {
                        _uiState.update {
                            it.copy(
                                inProgressConflict = InProgressConflictUiState(
                                    sessionId = result.sessionId,
                                    workoutName = result.workoutName,
                                ),
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun continueInProgressSession() {
        val conflict = _uiState.value.inProgressConflict ?: return
        _uiState.update {
            it.copy(sessionStartedEvent = conflict.sessionId, inProgressConflict = null)
        }
    }

    fun dismissInProgressConflict() {
        _uiState.update { it.copy(inProgressConflict = null) }
    }

    fun consumeSessionStartedEvent() {
        _uiState.update { it.copy(sessionStartedEvent = null) }
    }

    fun saveWorkout(name: String, description: String) {
        val workout = _uiState.value.workout ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                workoutRepository.update(workout.copy(name = name.trim(), description = description.trim()))
                _uiState.update { it.copy(showEditWorkoutDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

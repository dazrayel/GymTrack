package com.gymtrack.presentation.workouts.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.model.WorkoutExerciseDetail
import com.gymtrack.domain.repository.ExerciseRepository
import com.gymtrack.domain.repository.WorkoutRepository
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val workoutId: Long = checkNotNull(savedStateHandle["workoutId"])

    private val _uiState = MutableStateFlow(WorkoutDetailUiState())
    val uiState: StateFlow<WorkoutDetailUiState> = _uiState.asStateFlow()

    /**
     * The full exercise catalogue, exposed as a separate StateFlow so the UI
     * can drive the exercise picker without polluting WorkoutDetailUiState.
     */
    val availableExercises: StateFlow<List<Exercise>> = exerciseRepository.getAll()
        .catch { e -> _uiState.update { it.copy(error = e.message) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    init {
        observeWorkout()
        observeExercises()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Observation
    // ──────────────────────────────────────────────────────────────────────────

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

    private fun observeExercises() {
        viewModelScope.launch {
            combine(
                workoutRepository.getExercises(workoutId),
                exerciseRepository.getAll(),
            ) { workoutExercises, catalogue ->
                val catalogueById = catalogue.associateBy { it.id }
                workoutExercises
                    .mapNotNull { we ->
                        // If the referenced Exercise no longer exists in the catalogue
                        // (e.g. deleted concurrently), skip rather than crash.
                        val exercise = catalogueById[we.exerciseId] ?: return@mapNotNull null
                        WorkoutExerciseDetail(workoutExercise = we, exercise = exercise)
                    }
                    .sortedBy { it.position }
            }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { details ->
                    _uiState.update { it.copy(exercises = details, isLoading = false) }
                }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Exercise picker
    // ──────────────────────────────────────────────────────────────────────────

    fun showExercisePicker() {
        _uiState.update { it.copy(showExercisePicker = true) }
    }

    fun dismissExercisePicker() {
        _uiState.update { it.copy(showExercisePicker = false) }
    }

    /**
     * Called when the user picks an exercise from the catalogue.
     * Creates a new [WorkoutExerciseDetail] with sensible defaults and stores
     * it in [WorkoutDetailUiState.exerciseToConfigure]. The picker is closed
     * and the configuration screen/dialog should open.
     *
     * Nothing is persisted at this point.
     */
    fun selectExercise(exercise: Exercise) {
        val position = _uiState.value.exercises.size
        val draft = WorkoutExercise(
            id = 0L,
            workoutId = workoutId,
            exerciseId = exercise.id,
            position = position,
            sets = 3,
            minRepetitions = 8,
            maxRepetitions = 12,
            weight = 0.0,
            restSeconds = 60,
            notes = "",
        )
        _uiState.update {
            it.copy(
                showExercisePicker = false,
                exerciseToConfigure = WorkoutExerciseDetail(draft, exercise),
            )
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Exercise configuration (add / edit)
    // ──────────────────────────────────────────────────────────────────────────

    fun showEditExercise(exercise: WorkoutExerciseDetail) {
        _uiState.update { it.copy(exerciseToConfigure = exercise) }
    }

    fun dismissExerciseConfiguration() {
        _uiState.update { it.copy(exerciseToConfigure = null) }
    }

    /**
     * Persists the configured exercise.
     * - id == 0L → new exercise, calls [WorkoutRepository.addExercise]
     * - id != 0L → existing exercise, calls [WorkoutRepository.updateExercise]
     */
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
            sets = sets,
            minRepetitions = minRepetitions,
            maxRepetitions = maxRepetitions,
            weight = weight,
            restSeconds = restSeconds,
            notes = notes.trim(),
        )
        viewModelScope.launch {
            try {
                if (updated.id == 0L) {
                    workoutRepository.addExercise(updated)
                } else {
                    workoutRepository.updateExercise(updated)
                }
                _uiState.update { it.copy(exerciseToConfigure = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Exercise deletion
    // ──────────────────────────────────────────────────────────────────────────

    fun showDeleteConfirmation(exercise: WorkoutExerciseDetail) {
        _uiState.update { it.copy(showDeleteConfirmation = true, exerciseToDelete = exercise) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false, exerciseToDelete = null) }
    }

    fun confirmDelete() {
        val exercise = _uiState.value.exerciseToDelete ?: return
        viewModelScope.launch {
            try {
                workoutRepository.removeExerciseById(exercise.id)
                _uiState.update { it.copy(showDeleteConfirmation = false, exerciseToDelete = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(showDeleteConfirmation = false, exerciseToDelete = null, error = e.message) }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Reorder
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Moves the exercise at [fromIndex] to [toIndex] and persists the new
     * 0-based positions for the entire list. Invalid or no-op indices are ignored.
     * The UI list is refreshed by the existing Room Flow — no optimistic update.
     */
    fun reorderExercises(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.exercises
        if (fromIndex == toIndex) return
        if (fromIndex !in current.indices || toIndex !in current.indices) return

        val reordered = current.toMutableList()
        val moved = reordered.removeAt(fromIndex)
        reordered.add(toIndex, moved)

        val positions = reordered.mapIndexed { index, detail -> detail.id to index }.toMap()
        viewModelScope.launch {
            try {
                workoutRepository.updateExercisePositions(positions)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Workout editing
    // ──────────────────────────────────────────────────────────────────────────

    fun showEditWorkoutDialog() {
        _uiState.update { it.copy(showEditWorkoutDialog = true) }
    }

    fun dismissEditWorkoutDialog() {
        _uiState.update { it.copy(showEditWorkoutDialog = false) }
    }

    fun saveWorkout(name: String, description: String) {
        val workout = _uiState.value.workout ?: return
        viewModelScope.launch {
            try {
                workoutRepository.update(
                    workout.copy(name = name.trim(), description = description.trim()),
                )
                _uiState.update { it.copy(showEditWorkoutDialog = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Error
    // ──────────────────────────────────────────────────────────────────────────

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

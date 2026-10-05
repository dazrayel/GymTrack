package com.gymtrack.presentation.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAll()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { workouts ->
                    _uiState.update { it.copy(workouts = workouts, isLoading = false, error = null) }
                }
        }
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddEditDialog = true, workoutToEdit = null) }
    }

    fun showEditDialog(workout: Workout) {
        _uiState.update { it.copy(showAddEditDialog = true, workoutToEdit = workout) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showAddEditDialog = false, workoutToEdit = null) }
    }

    fun saveWorkout(name: String, description: String) {
        val toEdit = _uiState.value.workoutToEdit
        viewModelScope.launch {
            try {
                if (toEdit != null) {
                    repository.update(toEdit.copy(name = name.trim(), description = description.trim()))
                } else {
                    repository.save(Workout(name = name.trim(), description = description.trim()))
                }
                _uiState.update { it.copy(showAddEditDialog = false, workoutToEdit = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun showDeleteConfirmation(workout: Workout) {
        _uiState.update { it.copy(showDeleteConfirmation = true, workoutToDelete = workout) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false, workoutToDelete = null) }
    }

    fun confirmDelete() {
        val workout = _uiState.value.workoutToDelete ?: return
        viewModelScope.launch {
            try {
                repository.delete(workout)
                _uiState.update { it.copy(showDeleteConfirmation = false, workoutToDelete = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(showDeleteConfirmation = false, workoutToDelete = null, error = e.message) }
            }
        }
    }

    /**
     * Moves a workout from [fromIndex] to [toIndex] in the visual list.
     * Applies an optimistic UI update, then persists consecutive positions `0..n-1`.
     * On persistence failure, restores the previous list.
     */
    fun reorderWorkouts(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.workouts
        if (fromIndex == toIndex) return
        if (fromIndex !in current.indices || toIndex !in current.indices) return

        val previous = current
        val reordered = current.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }.mapIndexed { index, workout -> workout.copy(position = index) }

        _uiState.update { it.copy(workouts = reordered) }

        val positions = reordered.associate { it.id to it.position }
        viewModelScope.launch {
            try {
                repository.updateWorkoutPositions(positions)
            } catch (e: Exception) {
                _uiState.update { it.copy(workouts = previous, error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

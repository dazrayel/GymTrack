package com.gymtrack.presentation.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val repository: ExerciseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseUiState())
    val uiState: StateFlow<ExerciseUiState> = _uiState.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(300L)
                .flatMapLatest { query ->
                    if (query.isBlank()) repository.getAll() else repository.search(query)
                }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { exercises ->
                    _uiState.update { it.copy(exercises = exercises, isLoading = false, error = null) }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQuery.value = query
    }

    fun showAddDialog() {
        _uiState.update { it.copy(showAddEditDialog = true, exerciseToEdit = null) }
    }

    fun showEditDialog(exercise: Exercise) {
        _uiState.update { it.copy(showAddEditDialog = true, exerciseToEdit = exercise) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(showAddEditDialog = false, exerciseToEdit = null) }
    }

    fun saveExercise(name: String, muscleGroup: String, equipmentType: String) {
        val toEdit = _uiState.value.exerciseToEdit
        val exercise = if (toEdit != null) {
            toEdit.copy(
                name = name.trim(),
                muscleGroup = muscleGroup.trim(),
                equipmentType = equipmentType.trim(),
            )
        } else {
            Exercise(
                name = name.trim(),
                muscleGroup = muscleGroup.trim(),
                equipmentType = equipmentType.trim(),
            )
        }
        viewModelScope.launch {
            repository.save(exercise)
            _uiState.update { it.copy(showAddEditDialog = false, exerciseToEdit = null) }
        }
    }

    fun showDeleteConfirmation(exercise: Exercise) {
        _uiState.update { it.copy(showDeleteConfirmation = true, exerciseToDelete = exercise) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false, exerciseToDelete = null) }
    }

    fun confirmDelete() {
        val exercise = _uiState.value.exerciseToDelete ?: return
        viewModelScope.launch {
            repository.delete(exercise)
            _uiState.update { it.copy(showDeleteConfirmation = false, exerciseToDelete = null) }
        }
    }
}

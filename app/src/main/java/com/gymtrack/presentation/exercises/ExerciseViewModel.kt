package com.gymtrack.presentation.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.domain.exercise.CatalogDemoOptionsSource
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.sanitizedSecondaryMuscles
import com.gymtrack.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val repository: ExerciseRepository,
    private val catalogDemoOptionsSource: CatalogDemoOptionsSource,
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
        _uiState.update {
            it.copy(
                showAddEditDialog = false,
                exerciseToEdit = null,
                showDemoPicker = false,
                catalogDemoOptions = emptyList(),
            )
        }
    }

    fun openDemoPicker() {
        viewModelScope.launch {
            val options = withContext(Dispatchers.IO) {
                catalogDemoOptionsSource.optionsWithAvailableMedia()
            }
            _uiState.update { it.copy(showDemoPicker = true, catalogDemoOptions = options) }
        }
    }

    fun dismissDemoPicker() {
        _uiState.update { it.copy(showDemoPicker = false) }
    }

    fun saveExercise(
        name: String,
        muscleGroup: String,
        equipmentType: String,
        secondaryMuscles: List<String> = emptyList(),
        mediaExternalSource: String? = null,
        mediaExternalId: String? = null,
    ) {
        if (name.isBlank() || muscleGroup.isBlank() || equipmentType.isBlank()) return
        val toEdit = _uiState.value.exerciseToEdit
        val sanitized = sanitizedSecondaryMuscles(muscleGroup.trim(), secondaryMuscles)
        val mediaSource = mediaExternalSource?.trim()?.takeIf { it.isNotEmpty() }
        val mediaId = mediaExternalId?.trim()?.takeIf { it.isNotEmpty() }
        val linkedMediaSource = if (mediaSource != null && mediaId != null) mediaSource else null
        val linkedMediaId = if (mediaSource != null && mediaId != null) mediaId else null
        val exercise = if (toEdit != null) {
            toEdit.copy(
                name = name.trim(),
                muscleGroup = muscleGroup.trim(),
                equipmentType = equipmentType.trim(),
                secondaryMuscles = sanitized,
                mediaExternalSource = linkedMediaSource,
                mediaExternalId = linkedMediaId,
            )
        } else {
            Exercise(
                name = name.trim(),
                muscleGroup = muscleGroup.trim(),
                equipmentType = equipmentType.trim(),
                secondaryMuscles = sanitized,
                mediaExternalSource = linkedMediaSource,
                mediaExternalId = linkedMediaId,
            )
        }
        viewModelScope.launch {
            try {
                repository.save(exercise)
                _uiState.update {
                    it.copy(
                        showAddEditDialog = false,
                        exerciseToEdit = null,
                        showDemoPicker = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
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
            try {
                repository.delete(exercise)
                _uiState.update { it.copy(showDeleteConfirmation = false, exerciseToDelete = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        showDeleteConfirmation = false,
                        exerciseToDelete = null,
                        error = e.message,
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

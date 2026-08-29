package com.gymtrack.domain.repository

import com.gymtrack.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAll(): Flow<List<Exercise>>
    fun getById(id: Long): Flow<Exercise?>
    fun search(query: String): Flow<List<Exercise>>
    suspend fun save(exercise: Exercise): Long
    suspend fun delete(exercise: Exercise)
}

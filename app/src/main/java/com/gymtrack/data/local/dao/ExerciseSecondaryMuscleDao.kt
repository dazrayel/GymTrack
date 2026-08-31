package com.gymtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymtrack.data.local.entity.ExerciseSecondaryMuscleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseSecondaryMuscleDao {

    @Query("SELECT * FROM exercise_secondary_muscles")
    fun getAll(): Flow<List<ExerciseSecondaryMuscleEntity>>

    @Query(
        "SELECT muscle FROM exercise_secondary_muscles " +
            "WHERE exerciseId = :exerciseId ORDER BY muscle ASC",
    )
    fun getMuscles(exerciseId: Long): Flow<List<String>>

    @Query("DELETE FROM exercise_secondary_muscles WHERE exerciseId = :exerciseId")
    fun deleteByExerciseId(exerciseId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(muscles: List<ExerciseSecondaryMuscleEntity>)
}

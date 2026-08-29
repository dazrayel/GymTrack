package com.gymtrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("workoutId"),
        Index("exerciseId"),
    ],
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val position: Int,
    val sets: Int,
    val minRepetitions: Int,
    val maxRepetitions: Int,
    val weight: Double,
    val restSeconds: Int,
    val notes: String = "",
)

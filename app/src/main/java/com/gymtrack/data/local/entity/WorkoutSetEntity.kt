package com.gymtrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("sessionExerciseId"),
        Index(value = ["sessionExerciseId", "setIndex"], unique = true),
    ],
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionExerciseId: Long,
    val setIndex: Int,
    val reps: Int,
    val weight: Double,
    val completedAtMillis: Long,
)

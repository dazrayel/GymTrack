package com.gymtrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_session_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("sessionId"),
        Index("exerciseId"),
    ],
)
data class WorkoutSessionExerciseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long? = null,
    val position: Int,
    val exerciseName: String,
    val muscleGroup: String,
    val equipmentType: String,
    val plannedSets: Int,
    val minRepetitions: Int,
    val maxRepetitions: Int,
    val plannedWeight: Double,
    val restSeconds: Int,
    val notes: String = "",
    val secondaryMuscles: String = "",
    val status: String = STATUS_PENDING,
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_SKIPPED = "SKIPPED"
    }
}

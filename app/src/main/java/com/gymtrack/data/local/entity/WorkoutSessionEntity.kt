package com.gymtrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("status"),
        Index("workoutId"),
    ],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workoutId: Long? = null,
    val workoutName: String,
    val workoutDescription: String = "",
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val status: String,
    val restEndsAtMillis: Long? = null,
    val restPausedRemainingMillis: Long? = null,
    val restSessionExerciseId: Long? = null,
    val restAfterSetIndex: Int? = null,
) {
    companion object {
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_COMPLETED = "COMPLETED"
    }
}

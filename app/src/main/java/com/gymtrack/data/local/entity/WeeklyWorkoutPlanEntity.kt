package com.gymtrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Recurring weekly plan slot: one workout template per ISO [dayOfWeek] (1=Mon … 7=Sun).
 * Cascade-deletes when the referenced workout is removed.
 */
@Entity(
    tableName = "weekly_workout_plans",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutId")],
)
data class WeeklyWorkoutPlanEntity(
    @PrimaryKey
    val dayOfWeek: Int,
    val workoutId: Long,
)

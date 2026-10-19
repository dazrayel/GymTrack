package com.gymtrack.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted mission instance for a definition + period.
 * Progress is derived; only lifecycle/status is stored.
 */
@Entity(
    tableName = "mission_instances",
    indices = [
        Index(value = ["definitionId", "periodKey"], unique = true),
        Index(value = ["status"]),
    ],
)
data class MissionInstanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val definitionId: String,
    val periodKey: String,
    val status: String,
    val startedAtMillis: Long,
    val completedAtMillis: Long? = null,
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_COMPLETED = "COMPLETED"
    }
}

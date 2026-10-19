package com.gymtrack.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Immutable XP ledger row. [sourceKey] is unique for idempotent awards.
 * Deleting a workout session must not cascade-delete these rows.
 */
@Entity(
    tableName = "xp_events",
    indices = [
        Index(value = ["sourceKey"], unique = true),
        Index(value = ["createdAtMillis"]),
    ],
)
data class XpEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val amount: Int,
    val sourceKey: String,
    val createdAtMillis: Long,
)

package com.gymtrack.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    indices = [
        Index(
            value = ["externalSource", "externalId"],
            unique = true,
        ),
    ],
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String,
    val equipmentType: String,
    val externalSource: String? = null,
    val externalId: String? = null,
)

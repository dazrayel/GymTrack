package com.gymtrack.domain.model

data class Workout(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    /** 0-based visual list order; independent of recommendation (`id ASC`). */
    val position: Int = 0,
)

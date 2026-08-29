package com.gymtrack.domain.model

data class Workout(
    val id: Long = 0,
    val name: String,
    val description: String = "",
)

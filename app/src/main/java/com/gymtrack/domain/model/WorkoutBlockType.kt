package com.gymtrack.domain.model

enum class WorkoutBlockType {
    SINGLE,
    BI_SET,
    TRI_SET,
    ;

    val requiredExerciseCount: Int
        get() = when (this) {
            SINGLE -> 1
            BI_SET -> 2
            TRI_SET -> 3
        }

    fun validateExerciseCount(count: Int) {
        require(count == requiredExerciseCount) {
            "$name requires exactly $requiredExerciseCount exercise(s), but was $count"
        }
    }

    companion object {
        fun parse(raw: String): WorkoutBlockType =
            entries.firstOrNull { it.name == raw } ?: SINGLE
    }
}

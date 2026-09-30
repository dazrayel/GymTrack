package com.gymtrack.domain.exercise

import com.gymtrack.domain.model.Exercise

object ExerciseMediaConstants {
    const val DEFAULT_FRAME_DURATION_MS = 600L
    const val FREE_EXERCISE_DB_SOURCE = "free-exercise-db"
    const val ASSETS_EXERCISES_ROOT = "exercises"
    const val FRAME_0_FILE = "0.jpg"
    const val FRAME_1_FILE = "1.jpg"
}

data class ExerciseMediaFrames(
    val frame0AssetPath: String,
    val frame1AssetPath: String,
)

sealed interface ExerciseMediaResolution {
    data object Unavailable : ExerciseMediaResolution

    data class Available(val frames: ExerciseMediaFrames) : ExerciseMediaResolution
}

/**
 * Resolves local asset paths for exercise demonstration frames.
 * Does not verify that files exist on disk (see asset loader in presentation layer).
 */
class ExerciseMediaResolver {

    fun resolve(exercise: Exercise): ExerciseMediaResolution {
        val source = exercise.externalSource?.trim().orEmpty()
        val externalId = exercise.externalId?.trim().orEmpty()
        if (source.isEmpty() || externalId.isEmpty()) {
            return ExerciseMediaResolution.Unavailable
        }
        if (source != ExerciseMediaConstants.FREE_EXERCISE_DB_SOURCE) {
            return ExerciseMediaResolution.Unavailable
        }
        val base = "${ExerciseMediaConstants.ASSETS_EXERCISES_ROOT}/$externalId"
        return ExerciseMediaResolution.Available(
            ExerciseMediaFrames(
                frame0AssetPath = "$base/${ExerciseMediaConstants.FRAME_0_FILE}",
                frame1AssetPath = "$base/${ExerciseMediaConstants.FRAME_1_FILE}",
            ),
        )
    }

    fun mediaIdentityKey(exercise: Exercise?): String {
        if (exercise == null) return ""
        val source = exercise.externalSource?.trim().orEmpty()
        val externalId = exercise.externalId?.trim().orEmpty()
        if (source.isEmpty() || externalId.isEmpty()) return ""
        return "$source|$externalId"
    }
}

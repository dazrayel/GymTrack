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
 *
 * Priority:
 * 1. Explicitly selected catalog media ([Exercise.mediaExternalSource]/[Exercise.mediaExternalId])
 * 2. Imported identity ([Exercise.externalSource]/[Exercise.externalId])
 */
class ExerciseMediaResolver {

    fun resolve(exercise: Exercise): ExerciseMediaResolution {
        resolvePair(exercise.mediaExternalSource, exercise.mediaExternalId)?.let { return it }
        resolvePair(exercise.externalSource, exercise.externalId)?.let { return it }
        return ExerciseMediaResolution.Unavailable
    }

    fun mediaIdentityKey(exercise: Exercise?): String {
        if (exercise == null) return ""
        val selected = pairKey(exercise.mediaExternalSource, exercise.mediaExternalId)
        if (selected.isNotEmpty()) return selected
        return pairKey(exercise.externalSource, exercise.externalId)
    }

    private fun resolvePair(sourceRaw: String?, idRaw: String?): ExerciseMediaResolution.Available? {
        val source = sourceRaw?.trim().orEmpty()
        val externalId = idRaw?.trim().orEmpty()
        if (source.isEmpty() || externalId.isEmpty()) return null
        if (source != ExerciseMediaConstants.FREE_EXERCISE_DB_SOURCE) return null
        val base = "${ExerciseMediaConstants.ASSETS_EXERCISES_ROOT}/$externalId"
        return ExerciseMediaResolution.Available(
            ExerciseMediaFrames(
                frame0AssetPath = "$base/${ExerciseMediaConstants.FRAME_0_FILE}",
                frame1AssetPath = "$base/${ExerciseMediaConstants.FRAME_1_FILE}",
            ),
        )
    }

    private fun pairKey(sourceRaw: String?, idRaw: String?): String {
        val source = sourceRaw?.trim().orEmpty()
        val externalId = idRaw?.trim().orEmpty()
        if (source.isEmpty() || externalId.isEmpty()) return ""
        return "$source|$externalId"
    }
}

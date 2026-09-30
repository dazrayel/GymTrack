package com.gymtrack.data.catalog

import android.content.Context
import com.gymtrack.domain.exercise.CatalogDemoOption
import com.gymtrack.domain.exercise.CatalogDemoOptionsSource
import com.gymtrack.domain.exercise.ExerciseMediaConstants
import com.gymtrack.domain.model.MUSCLE_GROUPS
import com.gymtrack.presentation.exercises.ExerciseAssetImageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads published catalog entries that have local demonstration frames available.
 * Read-only over assets; does not write Room.
 */
@Singleton
class CatalogDemoOptionsProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : CatalogDemoOptionsSource {
    @Volatile
    private var cached: List<CatalogDemoOption>? = null

    override fun optionsWithAvailableMedia(): List<CatalogDemoOption> {
        cached?.let { return it }
        val loaded = load().sortedWith(
            compareBy<CatalogDemoOption> { muscleSortKey(it.muscleGroup) }
                .thenBy { it.displayName.lowercase() },
        )
        cached = loaded
        return loaded
    }

    private fun load(): List<CatalogDemoOption> {
        val text = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }.trim()
        val arr = if (text.startsWith("[")) {
            JSONArray(text)
        } else {
            JSONObject(text).getJSONArray("exercises")
        }
        val out = ArrayList<CatalogDemoOption>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val source = obj.optString("source").ifBlank {
                obj.optString("externalSource")
            }.ifBlank { ExerciseMediaConstants.FREE_EXERCISE_DB_SOURCE }
            val externalId = obj.optString("externalId").trim()
            val name = obj.optString("name").trim()
            val muscle = obj.optString("muscleGroup").trim()
            if (externalId.isEmpty() || name.isEmpty()) continue
            if (source != ExerciseMediaConstants.FREE_EXERCISE_DB_SOURCE) continue
            val frame0 = "${ExerciseMediaConstants.ASSETS_EXERCISES_ROOT}/$externalId/" +
                ExerciseMediaConstants.FRAME_0_FILE
            val frame1 = "${ExerciseMediaConstants.ASSETS_EXERCISES_ROOT}/$externalId/" +
                ExerciseMediaConstants.FRAME_1_FILE
            if (!ExerciseAssetImageLoader.assetExists(context, frame0)) continue
            if (!ExerciseAssetImageLoader.assetExists(context, frame1)) continue
            out += CatalogDemoOption(
                mediaExternalSource = source,
                mediaExternalId = externalId,
                displayName = name,
                muscleGroup = muscle.ifBlank { "Outro" },
            )
        }
        return out
    }

    private fun muscleSortKey(muscle: String): Int {
        val idx = MUSCLE_GROUPS.indexOf(muscle)
        return if (idx >= 0) idx else MUSCLE_GROUPS.size
    }

    companion object {
        const val ASSET_PATH = "exercises/gymtrack-exercises.json"
    }
}

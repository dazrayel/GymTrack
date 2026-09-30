package com.gymtrack.presentation.exercises

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object ExerciseAssetImageLoader {

    suspend fun loadBitmap(context: Context, assetPath: String): ImageBitmap? =
        withContext(Dispatchers.IO) {
            try {
                context.assets.open(assetPath).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (_: IOException) {
                null
            }
        }

    fun assetExists(context: Context, assetPath: String): Boolean =
        try {
            context.assets.open(assetPath).close()
            true
        } catch (_: IOException) {
            false
        }
}

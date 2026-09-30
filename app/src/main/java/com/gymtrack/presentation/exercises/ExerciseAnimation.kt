package com.gymtrack.presentation.exercises

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymtrack.R
import com.gymtrack.domain.exercise.ExerciseMediaConstants
import com.gymtrack.domain.exercise.ExerciseMediaResolution
import com.gymtrack.domain.exercise.ExerciseMediaResolver
import com.gymtrack.domain.model.Exercise
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ExerciseAnimation(
    exercise: Exercise?,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
    mediaResolver: ExerciseMediaResolver = remember { ExerciseMediaResolver() },
    frameDurationMs: Long = ExerciseMediaConstants.DEFAULT_FRAME_DURATION_MS,
) {
    val context = LocalContext.current
    val mediaKey = remember(exercise) { mediaResolver.mediaIdentityKey(exercise) }
    val resolution = remember(exercise) {
        exercise?.let { mediaResolver.resolve(it) } ?: ExerciseMediaResolution.Unavailable
    }

    var frame0 by remember(mediaKey) { mutableStateOf<ImageBitmap?>(null) }
    var frame1 by remember(mediaKey) { mutableStateOf<ImageBitmap?>(null) }
    var activeFrameIndex by remember(mediaKey) { mutableIntStateOf(0) }

    LaunchedEffect(mediaKey, resolution) {
        frame0 = null
        frame1 = null
        activeFrameIndex = 0
        val available = resolution as? ExerciseMediaResolution.Available ?: return@LaunchedEffect
        if (!ExerciseAssetImageLoader.assetExists(context, available.frames.frame0AssetPath) ||
            !ExerciseAssetImageLoader.assetExists(context, available.frames.frame1AssetPath)
        ) {
            return@LaunchedEffect
        }
        frame0 = ExerciseAssetImageLoader.loadBitmap(context, available.frames.frame0AssetPath)
        frame1 = ExerciseAssetImageLoader.loadBitmap(context, available.frames.frame1AssetPath)
    }

    LaunchedEffect(mediaKey, frame0, frame1, frameDurationMs) {
        if (frame0 == null || frame1 == null) return@LaunchedEffect
        activeFrameIndex = 0
        while (isActive) {
            delay(frameDurationMs)
            activeFrameIndex = if (activeFrameIndex == 0) 1 else 0
        }
    }

    val displayedBitmap = when (activeFrameIndex) {
        0 -> frame0
        else -> frame1
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .testTag("exercise_animation"),
        contentAlignment = Alignment.Center,
    ) {
        if (displayedBitmap != null) {
            Image(
                bitmap = displayedBitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .testTag("exercise_animation_frame"),
            )
        } else {
            ExerciseAnimationPlaceholder(
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun ExerciseAnimationPlaceholder(modifier: Modifier = Modifier) {
    ColumnPlaceholderContent(modifier)
}

@Composable
private fun ColumnPlaceholderContent(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Outlined.FitnessCenter,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
        Text(
            text = stringResource(R.string.exercise_demo_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

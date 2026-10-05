package com.gymtrack.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.gymtrack.R
import com.gymtrack.domain.model.WorkoutBlockType
import com.gymtrack.presentation.theme.GymShapeTokens
import com.gymtrack.presentation.theme.GymSpacing
import com.gymtrack.presentation.theme.GymTheme

/**
 * Presentational badge for workout block types. No training logic.
 */
@Composable
fun GymTypeBadge(
    type: WorkoutBlockType,
    modifier: Modifier = Modifier,
) {
    val label = when (type) {
        WorkoutBlockType.SINGLE -> stringResource(R.string.block_type_single)
        WorkoutBlockType.BI_SET -> stringResource(R.string.block_type_bi_set)
        WorkoutBlockType.TRI_SET -> stringResource(R.string.block_type_tri_set)
    }
    val background: Color
    val content: Color
    when (type) {
        WorkoutBlockType.SINGLE -> {
            background = MaterialTheme.colorScheme.secondary
            content = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.85f)
        }
        WorkoutBlockType.BI_SET -> {
            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            content = MaterialTheme.colorScheme.primary
        }
        WorkoutBlockType.TRI_SET -> {
            background = GymTheme.extendedColors.warning.copy(alpha = 0.18f)
            content = GymTheme.extendedColors.warning
        }
    }

    Text(
        text = label,
        modifier = modifier
            .clip(RoundedCornerShape(GymShapeTokens.Badge))
            .background(background)
            .padding(horizontal = GymSpacing.Sm, vertical = GymSpacing.Xs),
        style = MaterialTheme.typography.labelSmall,
        color = content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

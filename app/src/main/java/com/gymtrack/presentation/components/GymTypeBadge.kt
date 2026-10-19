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
import com.gymtrack.domain.model.AchievementRarity
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
    GymMetaBadge(
        label = label,
        background = background,
        contentColor = content,
        modifier = modifier,
    )
}

/**
 * Compact labeled badge sharing [GymTypeBadge] visual language.
 * Label text is required so meaning is not color-only.
 */
@Composable
fun GymMetaBadge(
    label: String,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        modifier = modifier
            .clip(RoundedCornerShape(GymShapeTokens.Badge))
            .background(background)
            .padding(horizontal = GymSpacing.Sm, vertical = GymSpacing.Xs),
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Achievement rarity badge. Always includes the rarity name; color is secondary emphasis.
 */
@Composable
fun GymRarityBadge(
    rarity: AchievementRarity,
    modifier: Modifier = Modifier,
) {
    val label = when (rarity) {
        AchievementRarity.COMMON -> stringResource(R.string.achievement_rarity_common)
        AchievementRarity.UNCOMMON -> stringResource(R.string.achievement_rarity_uncommon)
        AchievementRarity.RARE -> stringResource(R.string.achievement_rarity_rare)
        AchievementRarity.EPIC -> stringResource(R.string.achievement_rarity_epic)
        AchievementRarity.LEGENDARY -> stringResource(R.string.achievement_rarity_legendary)
    }
    val background: Color
    val content: Color
    when (rarity) {
        AchievementRarity.COMMON -> {
            background = MaterialTheme.colorScheme.surfaceVariant
            content = MaterialTheme.colorScheme.onSurfaceVariant
        }
        AchievementRarity.UNCOMMON -> {
            background = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            content = MaterialTheme.colorScheme.primary
        }
        AchievementRarity.RARE -> {
            background = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
            content = MaterialTheme.colorScheme.tertiary
        }
        AchievementRarity.EPIC -> {
            background = GymTheme.extendedColors.warning.copy(alpha = 0.18f)
            content = GymTheme.extendedColors.warning
        }
        AchievementRarity.LEGENDARY -> {
            background = GymTheme.extendedColors.success.copy(alpha = 0.18f)
            content = GymTheme.extendedColors.success
        }
    }
    GymMetaBadge(
        label = label,
        background = background,
        contentColor = content,
        modifier = modifier,
    )
}

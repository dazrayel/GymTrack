package com.gymtrack.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymtrack.presentation.theme.GymShapeTokens
import com.gymtrack.presentation.theme.GymSpacing
import com.gymtrack.presentation.theme.GymTheme

enum class GymCardTone {
    /** Default elevated surface with subtle border. */
    Surface,

    /** Stronger elevated surface for emphasis. */
    Elevated,

    /** Soft primary-tinted highlight (e.g. recommended workout). */
    Highlight,

    /** Warning / PR emphasis. */
    Warning,
}

@Composable
fun GymCard(
    modifier: Modifier = Modifier,
    tone: GymCardTone = GymCardTone.Surface,
    shape: Shape = RoundedCornerShape(GymShapeTokens.Card),
    contentPadding: PaddingValues = PaddingValues(GymSpacing.CardPadding),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = cardColors(tone)
    val border = cardBorder(tone)
    val elevation = CardDefaults.cardElevation(defaultElevation = cardElevation(tone))

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            border = border,
            elevation = elevation,
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(contentPadding),
                content = content,
            )
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            border = border,
            elevation = elevation,
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(contentPadding),
                content = content,
            )
        }
    }
}

@Composable
private fun cardColors(tone: GymCardTone) = when (tone) {
    GymCardTone.Surface -> CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
    GymCardTone.Elevated -> CardDefaults.cardColors(
        containerColor = GymTheme.extendedColors.surfaceElevated,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
    GymCardTone.Highlight -> CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
    GymCardTone.Warning -> CardDefaults.cardColors(
        containerColor = GymTheme.extendedColors.warningContainer.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun cardBorder(tone: GymCardTone): BorderStroke = when (tone) {
    GymCardTone.Surface, GymCardTone.Elevated -> BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
    )
    GymCardTone.Highlight -> BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
    )
    GymCardTone.Warning -> BorderStroke(
        width = 1.dp,
        color = GymTheme.extendedColors.warning.copy(alpha = 0.35f),
    )
}

private fun cardElevation(tone: GymCardTone): Dp = when (tone) {
    GymCardTone.Elevated -> 2.dp
    else -> 0.dp
}

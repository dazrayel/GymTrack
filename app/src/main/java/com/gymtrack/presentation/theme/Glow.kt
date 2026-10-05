package com.gymtrack.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

/**
 * Subtle primary glow for occasional CTAs. Keep usage sparse.
 */
@Composable
fun Modifier.gymPrimaryGlow(): Modifier {
    val glow = GymTheme.extendedColors.primaryGlow
    return this.shadow(
        elevation = 10.dp,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(GymShapeTokens.Button),
        ambientColor = glow,
        spotColor = glow,
        clip = false,
    )
}

package com.gymtrack.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gymtrack.R
import com.gymtrack.presentation.theme.GymShapeTokens
import com.gymtrack.presentation.theme.GymSpacing

/**
 * Sign in with Google button styled per Google identity branding (outlined).
 */
@Composable
fun GymGoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDark) Color(0xFF131314) else Color.White
    val contentColor = if (isDark) Color(0xFFE3E3E3) else Color(0xFF1F1F1F)
    val borderColor = if (isDark) Color(0xFF8E918F) else Color(0xFF747775)

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = GymSpacing.TouchTarget)
            .defaultMinSize(minHeight = 40.dp)
            .testTag("settings_google_sign_in"),
        shape = RoundedCornerShape(GymShapeTokens.Button),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.6f),
            disabledContentColor = contentColor.copy(alpha = 0.6f),
        ),
        border = BorderStroke(1.dp, borderColor),
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = GymSpacing.Xs),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_google_logo),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.Unspecified,
            )
            Text(
                text = stringResource(R.string.settings_account_sign_in_google),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = contentColor,
            )
        }
    }
}

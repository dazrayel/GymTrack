package com.gymtrack.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.gymtrack.presentation.theme.GymSpacing
import com.gymtrack.presentation.theme.GymTheme

/**
 * Small eyebrow/label used to contextualize sections.
 * Pass already-cased text; do not rely on forced uppercase (tests assert exact strings).
 */
@Composable
fun GymLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = text,
        modifier = modifier,
        style = GymTheme.gymTypography.eyebrow,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun GymSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(GymSpacing.Xs),
        ) {
            if (!eyebrow.isNullOrBlank()) {
                GymLabel(text = eyebrow)
            }
            Text(
                text = title,
                style = GymTheme.gymTypography.sectionTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (action != null) {
            action()
        }
    }
}

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
import androidx.compose.ui.text.style.TextOverflow
import com.gymtrack.presentation.theme.GymSpacing
import com.gymtrack.presentation.theme.GymTheme

/**
 * Compact metric display for volume, sets, PRs, etc.
 * Presentation-only; no data logic.
 */
@Composable
fun GymMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GymSpacing.Xs),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = GymTheme.gymTypography.metric,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!unit.isNullOrBlank()) {
                Text(
                    text = " $unit",
                    style = GymTheme.gymTypography.metricUnit,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun GymMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
) {
    GymCard(modifier = modifier.fillMaxWidth()) {
        GymMetric(label = label, value = value, unit = unit)
    }
}

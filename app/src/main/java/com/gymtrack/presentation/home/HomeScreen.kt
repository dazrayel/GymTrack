package com.gymtrack.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gymtrack.R
import com.gymtrack.presentation.theme.GymTrackTheme

@Composable
fun HomeScreen(
    onNavigateToExercises: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.home_welcome_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Text(
            text = stringResource(R.string.home_quick_access),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Card(
            onClick = onNavigateToExercises,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.exercises_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.home_exercises_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    GymTrackTheme {
        HomeScreen()
    }
}

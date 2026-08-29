package com.gymtrack.presentation.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
fun WorkoutsScreen(
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.workouts_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WorkoutsScreenPreview() {
    GymTrackTheme {
        WorkoutsScreen()
    }
}

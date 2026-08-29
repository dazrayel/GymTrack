package com.gymtrack.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = GreenSecondary,
    onSecondary = GreenOnSecondary,
    secondaryContainer = GreenSecondaryContainer,
    onSecondaryContainer = GreenOnSecondaryContainer,
    background = GreenBackground,
    onBackground = GreenOnBackground,
    surface = GreenSurface,
    onSurface = GreenOnSurface,
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimaryContainer,
    onPrimary = GreenOnPrimaryContainer,
    primaryContainer = GreenPrimary,
    onPrimaryContainer = GreenOnPrimary,
    secondary = GreenSecondaryContainer,
    onSecondary = GreenOnSecondaryContainer,
    secondaryContainer = GreenSecondary,
    onSecondaryContainer = GreenOnSecondary,
)

@Composable
fun GymTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

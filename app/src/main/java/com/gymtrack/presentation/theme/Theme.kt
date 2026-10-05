package com.gymtrack.presentation.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Dark-first GymTrack identity. Dynamic Color is disabled by default so brand
 * colors stay consistent across devices.
 *
 * [darkTheme] defaults to `true` (dark). The persisted user preference is applied
 * at the app root — screens should not call [androidx.compose.foundation.isSystemInDarkTheme].
 */
private val DarkColorScheme = darkColorScheme(
    primary = GymPrimary,
    onPrimary = GymOnPrimary,
    primaryContainer = GymPrimaryContainer,
    onPrimaryContainer = GymOnPrimaryContainer,
    secondary = GymSecondary,
    onSecondary = GymOnSecondary,
    secondaryContainer = GymSecondaryContainer,
    onSecondaryContainer = GymOnSecondaryContainer,
    tertiary = GymWarning,
    onTertiary = GymOnWarning,
    tertiaryContainer = GymWarningContainer,
    onTertiaryContainer = GymOnWarningContainer,
    background = GymDarkBackground,
    onBackground = GymDarkOnBackground,
    surface = GymDarkSurface,
    onSurface = GymDarkOnSurface,
    surfaceVariant = GymDarkSurfaceVariant,
    onSurfaceVariant = GymDarkOnSurfaceVariant,
    surfaceContainerLowest = GymDarkBackground,
    surfaceContainerLow = GymDarkSurface,
    surfaceContainer = GymDarkSurfaceElevated,
    surfaceContainerHigh = GymDarkSurfaceVariant,
    surfaceContainerHighest = GymDarkSurfaceVariant,
    outline = GymDarkOutline,
    outlineVariant = GymDarkOutlineVariant,
    error = GymError,
    onError = GymOnError,
    errorContainer = GymErrorContainer,
    onErrorContainer = GymOnErrorContainer,
)

private val LightColorScheme = lightColorScheme(
    primary = GymPrimary,
    onPrimary = GymOnPrimary,
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF141B5C),
    secondary = Color(0xFF555866),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = GymLightSurfaceVariant,
    onSecondaryContainer = GymLightOnSurfaceVariant,
    tertiary = Color(0xFFB8860B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFF0C2),
    onTertiaryContainer = Color(0xFF3D3200),
    background = GymLightBackground,
    onBackground = GymLightOnBackground,
    surface = GymLightSurface,
    onSurface = GymLightOnSurface,
    surfaceVariant = GymLightSurfaceVariant,
    onSurfaceVariant = GymLightOnSurfaceVariant,
    surfaceContainerLowest = GymLightBackground,
    surfaceContainerLow = GymLightSurface,
    surfaceContainer = GymLightSurfaceElevated,
    surfaceContainerHigh = GymLightSurfaceVariant,
    surfaceContainerHighest = GymLightSurfaceVariant,
    outline = GymLightOutline,
    outlineVariant = GymLightOutlineVariant,
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

@Composable
fun GymTrackTheme(
    darkTheme: Boolean = true,
    /** Kept for API compatibility; brand identity does not use wallpaper Dynamic Color. */
    @Suppress("UNUSED_PARAMETER")
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extended = if (darkTheme) DarkGymExtendedColors else LightGymExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalGymExtendedColors provides extended,
        LocalGymTypography provides DefaultGymTypography,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = GymShapes,
            content = content,
        )
    }
}

/** Access semantic GymTrack colors outside Material [androidx.compose.material3.ColorScheme]. */
object GymTheme {
    val extendedColors: GymExtendedColors
        @Composable
        get() = LocalGymExtendedColors.current

    val gymTypography: GymTypography
        @Composable
        get() = LocalGymTypography.current
}

package com.gymtrack.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors that extend Material 3 [androidx.compose.material3.ColorScheme].
 */
@Immutable
data class GymExtendedColors(
    val surfaceElevated: Color,
    val textMuted: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val primaryGlow: Color,
)

val DarkGymExtendedColors = GymExtendedColors(
    surfaceElevated = GymDarkSurfaceElevated,
    textMuted = GymDarkOnSurfaceVariant.copy(alpha = 0.72f),
    warning = GymWarning,
    onWarning = GymOnWarning,
    warningContainer = GymWarningContainer,
    onWarningContainer = GymOnWarningContainer,
    success = GymSuccess,
    onSuccess = GymOnSuccess,
    successContainer = GymSuccessContainer,
    onSuccessContainer = GymOnSuccessContainer,
    info = GymInfo,
    onInfo = GymOnInfo,
    infoContainer = GymInfoContainer,
    onInfoContainer = GymOnInfoContainer,
    primaryGlow = GymPrimaryGlow,
)

val LightGymExtendedColors = GymExtendedColors(
    surfaceElevated = GymLightSurfaceElevated,
    textMuted = GymLightOnSurfaceVariant.copy(alpha = 0.72f),
    warning = Color(0xFFB8860B),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFF0C2),
    onWarningContainer = Color(0xFF3D3200),
    success = Color(0xFF1B7A4E),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFC8F5DF),
    onSuccessContainer = Color(0xFF003822),
    info = Color(0xFF1565C0),
    onInfo = Color(0xFFFFFFFF),
    infoContainer = Color(0xFFD6EBFF),
    onInfoContainer = Color(0xFF003258),
    primaryGlow = GymPrimaryGlow.copy(alpha = 0.28f),
)

val LocalGymExtendedColors = staticCompositionLocalOf { DarkGymExtendedColors }
val LocalGymTypography = staticCompositionLocalOf { DefaultGymTypography }

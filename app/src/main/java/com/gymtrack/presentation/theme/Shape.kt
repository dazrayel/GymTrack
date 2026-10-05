package com.gymtrack.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val GymShapes = Shapes(
    extraSmall = RoundedCornerShape(GymShapeTokens.ExtraSmall),
    small = RoundedCornerShape(GymShapeTokens.Small),
    medium = RoundedCornerShape(GymShapeTokens.Medium),
    large = RoundedCornerShape(GymShapeTokens.Large),
    extraLarge = RoundedCornerShape(GymShapeTokens.ExtraLarge),
)

object GymShapeTokens {
    val ExtraSmall = 6.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 20.dp

    /** Default card corner radius (reference: rounded-2xl). */
    val Card = 16.dp

    val Dialog = 20.dp
    val BottomSheet = 24.dp
    val Button = 14.dp
    val Badge = 8.dp
    val Chip = 8.dp
    val IconButton = 12.dp
}

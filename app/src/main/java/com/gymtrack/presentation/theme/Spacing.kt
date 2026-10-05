package com.gymtrack.presentation.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing scale for the GymTrack design system.
 * Prefer these tokens over ad-hoc dp values in new UI.
 */
object GymSpacing {
    val None = 0.dp
    val Xxs = 2.dp
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 12.dp
    val Lg = 16.dp
    val Xl = 20.dp
    val Xxl = 24.dp
    val Xxxl = 32.dp

    /** Horizontal / vertical inset for screen content. */
    val ScreenPadding = 24.dp

    /** Space between major page sections. */
    val SectionSpacing = 24.dp

    /** Space between stacked cards. */
    val CardSpacing = 12.dp

    /** Default padding inside cards. */
    val CardPadding = 16.dp

    /** Compact gaps inside dense rows. */
    val CompactSpacing = 8.dp

    /** Large gaps for hero/CTA regions. */
    val LargeSpacing = 32.dp

    /** Minimum recommended touch target. */
    val TouchTarget = 48.dp
}

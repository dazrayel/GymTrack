package com.gymtrack.domain.model

/**
 * In-memory Google identity for UI personalization only.
 * No email, Google ID, tokens, or profile photo are stored in app state.
 */
data class GoogleUser(
    val displayName: String,
)

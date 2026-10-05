package com.gymtrack.domain.identity

/**
 * Extracts the first given name for general UI greetings/headers.
 * Full [displayName] remains the identity source (e.g. Settings → Conta).
 *
 * @return trimmed first token, or null when [displayName] is null/blank
 * (same “no name” behavior used by unauthenticated / empty greeting paths).
 */
fun firstNameFromDisplayName(displayName: String?): String? {
    val trimmed = displayName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return trimmed.substringBefore(' ')
}

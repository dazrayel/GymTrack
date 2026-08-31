package com.gymtrack.domain.model

/**
 * Planned repetition target for display. Returns null when the stored values
 * cannot form a safe label, so the UI can omit the line instead of inventing a range.
 */
fun formatRepetitionTarget(minRepetitions: Int, maxRepetitions: Int): String? {
    if (minRepetitions < 1 || maxRepetitions < 1 || maxRepetitions < minRepetitions) {
        return null
    }
    return if (minRepetitions == maxRepetitions) {
        minRepetitions.toString()
    } else {
        "$minRepetitions–$maxRepetitions"
    }
}

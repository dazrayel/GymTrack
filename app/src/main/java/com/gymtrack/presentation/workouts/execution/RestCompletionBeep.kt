package com.gymtrack.presentation.workouts.execution

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Plays a short notification tone when a rest timer reaches zero.
 * Uses platform [ToneGenerator] — no external audio dependency.
 */
object RestCompletionBeep {
    private const val VOLUME = 80
    private const val DURATION_MS = 250

    fun play() {
        var tone: ToneGenerator? = null
        try {
            tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, VOLUME)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, DURATION_MS)
            // Allow the tone to finish before releasing.
            Thread.sleep(DURATION_MS.toLong() + 50L)
        } catch (_: Exception) {
            // Best-effort feedback; never crash the workout UI.
        } finally {
            runCatching { tone?.release() }
        }
    }
}

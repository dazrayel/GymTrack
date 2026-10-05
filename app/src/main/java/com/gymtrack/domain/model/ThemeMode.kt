package com.gymtrack.domain.model

/**
 * User-selected app appearance. Default is [DARK] to preserve GymTrack identity.
 * System-follow is intentionally not supported in this phase.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    ;

    val isDark: Boolean get() = this == DARK

    companion object {
        val Default: ThemeMode = DARK

        fun fromStorageKey(key: String?): ThemeMode = when (key) {
            LIGHT.name -> LIGHT
            DARK.name -> DARK
            else -> Default
        }
    }
}

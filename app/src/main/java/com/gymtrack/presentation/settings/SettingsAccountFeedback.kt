package com.gymtrack.presentation.settings

sealed class SettingsAccountFeedback {
    data object Cancelled : SettingsAccountFeedback()
    data object NotConfigured : SettingsAccountFeedback()
    data object GenericError : SettingsAccountFeedback()
}

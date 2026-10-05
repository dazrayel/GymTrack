package com.gymtrack.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.domain.identity.GoogleIdentityFailure
import com.gymtrack.domain.identity.GoogleSignInResult
import com.gymtrack.domain.model.GoogleUser
import com.gymtrack.domain.model.ThemeMode
import com.gymtrack.domain.repository.GoogleIdentityRepository
import com.gymtrack.domain.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.Default,
    val googleUser: GoogleUser? = null,
    val accountActionInProgress: Boolean = false,
    val accountFeedback: SettingsAccountFeedback? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
    private val googleIdentityRepository: GoogleIdentityRepository,
) : ViewModel() {

    private val accountActionInProgress = MutableStateFlow(false)
    private val accountFeedback = MutableStateFlow<SettingsAccountFeedback?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        themeRepository.themeMode,
        googleIdentityRepository.currentUser,
        accountActionInProgress,
        accountFeedback,
    ) { themeMode, googleUser, inProgress, feedback ->
        SettingsUiState(
            themeMode = themeMode,
            googleUser = googleUser,
            accountActionInProgress = inProgress,
            accountFeedback = feedback,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SettingsUiState(),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeRepository.setThemeMode(mode)
        }
    }

    fun signInWithGoogle(hostContext: Context) {
        if (accountActionInProgress.value) return
        viewModelScope.launch {
            accountActionInProgress.value = true
            accountFeedback.value = null
            when (val result = googleIdentityRepository.signIn(hostContext)) {
                GoogleSignInResult.Success -> Unit
                GoogleSignInResult.Cancelled -> {
                    accountFeedback.value = SettingsAccountFeedback.Cancelled
                }
                is GoogleSignInResult.Failed -> {
                    accountFeedback.value = result.failure.toFeedback()
                }
            }
            accountActionInProgress.value = false
        }
    }

    fun signOut(hostContext: Context) {
        if (accountActionInProgress.value) return
        viewModelScope.launch {
            accountActionInProgress.value = true
            accountFeedback.value = null
            googleIdentityRepository.signOut(hostContext)
            accountActionInProgress.value = false
        }
    }

    fun clearAccountFeedback() {
        accountFeedback.value = null
    }

    private fun GoogleIdentityFailure.toFeedback(): SettingsAccountFeedback = when (this) {
        GoogleIdentityFailure.NotConfigured -> SettingsAccountFeedback.NotConfigured
        GoogleIdentityFailure.CredentialUnavailable,
        GoogleIdentityFailure.InvalidCredential,
        is GoogleIdentityFailure.Unknown,
        -> SettingsAccountFeedback.GenericError
    }
}

package com.gymtrack.data.repository

import android.content.Context
import com.gymtrack.domain.identity.CredentialSignInOutcome
import com.gymtrack.domain.identity.GoogleCredentialProvider
import com.gymtrack.domain.identity.GoogleIdentityFailure
import com.gymtrack.domain.identity.GoogleSignInResult
import com.gymtrack.domain.model.GoogleUser
import com.gymtrack.domain.repository.GoogleIdentityRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class GoogleIdentityRepositoryImpl @Inject constructor(
    private val credentialProvider: GoogleCredentialProvider,
) : GoogleIdentityRepository {

    private val _currentUser = MutableStateFlow<GoogleUser?>(null)
    override val currentUser: StateFlow<GoogleUser?> = _currentUser.asStateFlow()

    override suspend fun tryRestoreSilentSignIn(hostContext: Context) {
        when (val outcome = credentialProvider.trySilentSignIn(hostContext)) {
            is CredentialSignInOutcome.Success -> {
                _currentUser.value = GoogleUser(displayName = outcome.displayName)
            }
            is CredentialSignInOutcome.Cancelled,
            is CredentialSignInOutcome.Error,
            CredentialSignInOutcome.NoCredential,
            -> Unit
        }
    }

    override suspend fun signIn(hostContext: Context): GoogleSignInResult {
        val previousUser = _currentUser.value
        return when (val outcome = credentialProvider.signIn(hostContext)) {
            is CredentialSignInOutcome.Success -> {
                _currentUser.value = GoogleUser(displayName = outcome.displayName)
                GoogleSignInResult.Success
            }
            CredentialSignInOutcome.Cancelled -> GoogleSignInResult.Cancelled
            is CredentialSignInOutcome.Error -> {
                _currentUser.value = previousUser
                GoogleSignInResult.Failed(outcome.failure)
            }
            CredentialSignInOutcome.NoCredential -> {
                _currentUser.value = previousUser
                GoogleSignInResult.Failed(GoogleIdentityFailure.CredentialUnavailable)
            }
        }
    }

    override suspend fun signOut(hostContext: Context) {
        credentialProvider.clearCredentialState(hostContext)
        _currentUser.value = null
    }
}

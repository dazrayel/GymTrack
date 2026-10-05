package com.gymtrack.domain.identity

import android.content.Context

sealed class CredentialSignInOutcome {
    data class Success(val displayName: String) : CredentialSignInOutcome()
    data object Cancelled : CredentialSignInOutcome()
    data class Error(val failure: GoogleIdentityFailure) : CredentialSignInOutcome()
    data object NoCredential : CredentialSignInOutcome()
}

/**
 * Abstraction over Credential Manager + Sign in with Google for unit tests.
 */
interface GoogleCredentialProvider {
    suspend fun signIn(hostContext: Context): CredentialSignInOutcome
    suspend fun trySilentSignIn(hostContext: Context): CredentialSignInOutcome
    suspend fun clearCredentialState(hostContext: Context)
}

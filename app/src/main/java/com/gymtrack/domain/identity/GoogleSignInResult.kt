package com.gymtrack.domain.identity

sealed class GoogleSignInResult {
    data object Success : GoogleSignInResult()
    data object Cancelled : GoogleSignInResult()
    data class Failed(val failure: GoogleIdentityFailure) : GoogleSignInResult()
}

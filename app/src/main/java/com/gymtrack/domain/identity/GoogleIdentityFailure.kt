package com.gymtrack.domain.identity

sealed class GoogleIdentityFailure {
    data object NotConfigured : GoogleIdentityFailure()
    data object CredentialUnavailable : GoogleIdentityFailure()
    data object InvalidCredential : GoogleIdentityFailure()
    data class Unknown(val cause: Throwable? = null) : GoogleIdentityFailure()
}

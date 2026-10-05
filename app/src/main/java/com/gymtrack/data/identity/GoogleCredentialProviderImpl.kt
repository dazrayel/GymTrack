package com.gymtrack.data.identity

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.gymtrack.domain.identity.CredentialSignInOutcome
import com.gymtrack.domain.identity.GoogleCredentialProvider
import com.gymtrack.domain.identity.GoogleIdentityFailure
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleCredentialProviderImpl @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val oauthConfig: GoogleOAuthConfig,
) : GoogleCredentialProvider {

    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(applicationContext)
    }

    override suspend fun signIn(hostContext: Context): CredentialSignInOutcome {
        if (!oauthConfig.isConfigured) {
            return CredentialSignInOutcome.Error(GoogleIdentityFailure.NotConfigured)
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(oauthConfig.webClientId)
            .setAutoSelectEnabled(false)
            .build()
        return requestCredential(hostContext, googleIdOption)
    }

    override suspend fun trySilentSignIn(hostContext: Context): CredentialSignInOutcome {
        if (!oauthConfig.isConfigured) {
            return CredentialSignInOutcome.NoCredential
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(oauthConfig.webClientId)
            .setAutoSelectEnabled(true)
            .build()
        return requestCredential(hostContext, googleIdOption, treatNoCredentialAsSilent = true)
    }

    override suspend fun clearCredentialState(hostContext: Context) {
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private suspend fun requestCredential(
        hostContext: Context,
        googleIdOption: GetGoogleIdOption,
        treatNoCredentialAsSilent: Boolean = false,
    ): CredentialSignInOutcome {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        return try {
            val result = credentialManager.getCredential(
                request = request,
                context = hostContext,
            )
            parseCredential(result.credential)
        } catch (_: GetCredentialCancellationException) {
            CredentialSignInOutcome.Cancelled
        } catch (_: NoCredentialException) {
            if (treatNoCredentialAsSilent) {
                CredentialSignInOutcome.NoCredential
            } else {
                CredentialSignInOutcome.Error(GoogleIdentityFailure.CredentialUnavailable)
            }
        } catch (e: GetCredentialException) {
            CredentialSignInOutcome.Error(GoogleIdentityFailure.Unknown(e))
        } catch (e: Exception) {
            CredentialSignInOutcome.Error(GoogleIdentityFailure.Unknown(e))
        }
    }

    private fun parseCredential(credential: androidx.credentials.Credential): CredentialSignInOutcome {
        if (credential !is CustomCredential) {
            return CredentialSignInOutcome.Error(GoogleIdentityFailure.InvalidCredential)
        }
        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return CredentialSignInOutcome.Error(GoogleIdentityFailure.InvalidCredential)
        }
        return try {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val displayName = googleCredential.displayName?.trim()?.takeIf { it.isNotEmpty() }
                ?: googleCredential.givenName?.trim()?.takeIf { it.isNotEmpty() }
            if (displayName == null) {
                CredentialSignInOutcome.Error(GoogleIdentityFailure.InvalidCredential)
            } else {
                CredentialSignInOutcome.Success(displayName)
            }
        } catch (_: GoogleIdTokenParsingException) {
            CredentialSignInOutcome.Error(GoogleIdentityFailure.InvalidCredential)
        }
    }
}

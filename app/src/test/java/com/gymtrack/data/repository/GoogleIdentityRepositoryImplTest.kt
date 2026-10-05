package com.gymtrack.data.repository

import android.app.Application
import android.content.Context
import com.gymtrack.domain.identity.CredentialSignInOutcome
import com.gymtrack.domain.identity.GoogleCredentialProvider
import com.gymtrack.domain.identity.GoogleIdentityFailure
import com.gymtrack.domain.identity.GoogleSignInResult
import com.gymtrack.domain.model.GoogleUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GoogleIdentityRepositoryImplTest {

    private lateinit var provider: RecordingGoogleCredentialProvider
    private lateinit var repository: GoogleIdentityRepositoryImpl
    private val hostContext: Context = Application()

    @Before
    fun setUp() {
        provider = RecordingGoogleCredentialProvider()
        repository = GoogleIdentityRepositoryImpl(provider)
    }

    @Test
    fun initialState_hasNoUser() = runTest {
        assertNull(repository.currentUser.first())
    }

    @Test
    fun signIn_success_setsDisplayNameOnly() = runTest {
        provider.signInOutcome = CredentialSignInOutcome.Success("Danilo")

        val result = repository.signIn(hostContext)

        assertEquals(GoogleSignInResult.Success, result)
        assertEquals(GoogleUser("Danilo"), repository.currentUser.first())
    }

    @Test
    fun signIn_cancelled_keepsAnonymous() = runTest {
        val result = repository.signIn(hostContext)

        assertEquals(GoogleSignInResult.Cancelled, result)
        assertNull(repository.currentUser.first())
    }

    @Test
    fun signIn_error_afterPreviousLogin_restoresPreviousUser() = runTest {
        provider.signInOutcome = CredentialSignInOutcome.Success("Danilo")
        repository.signIn(hostContext)
        provider.signInOutcome = CredentialSignInOutcome.Error(GoogleIdentityFailure.Unknown())

        val result = repository.signIn(hostContext)

        assertEquals(
            GoogleSignInResult.Failed(GoogleIdentityFailure.Unknown()),
            result,
        )
        assertEquals(GoogleUser("Danilo"), repository.currentUser.first())
    }

    @Test
    fun logout_clearsInMemoryUser() = runTest {
        provider.signInOutcome = CredentialSignInOutcome.Success("Danilo")
        repository.signIn(hostContext)

        repository.signOut(hostContext)

        assertNull(repository.currentUser.first())
        assertEquals(1, provider.clearCalls)
    }

    @Test
    fun tryRestoreSilentSignIn_success_setsUser() = runTest {
        provider.silentOutcome = CredentialSignInOutcome.Success("Ana")

        repository.tryRestoreSilentSignIn(hostContext)

        assertEquals(GoogleUser("Ana"), repository.currentUser.first())
    }

    @Test
    fun tryRestoreSilentSignIn_noCredential_staysAnonymous() = runTest {
        provider.silentOutcome = CredentialSignInOutcome.NoCredential

        repository.tryRestoreSilentSignIn(hostContext)

        assertNull(repository.currentUser.first())
    }

    @Test
    fun identityLayer_doesNotUseLocalPersistence() {
        val fields = GoogleIdentityRepositoryImpl::class.java.declaredFields.map { it.type.name }
        assert(fields.none { it.contains("DataStore") })
        assert(fields.none { it.contains("SharedPreferences") })
        assert(fields.none { it.contains("Room") })
    }
}

private class RecordingGoogleCredentialProvider : GoogleCredentialProvider {
    var signInOutcome: CredentialSignInOutcome = CredentialSignInOutcome.Cancelled
    var silentOutcome: CredentialSignInOutcome = CredentialSignInOutcome.NoCredential
    var clearCalls = 0

    override suspend fun signIn(hostContext: Context): CredentialSignInOutcome = signInOutcome

    override suspend fun trySilentSignIn(hostContext: Context): CredentialSignInOutcome = silentOutcome

    override suspend fun clearCredentialState(hostContext: Context) {
        clearCalls++
    }
}

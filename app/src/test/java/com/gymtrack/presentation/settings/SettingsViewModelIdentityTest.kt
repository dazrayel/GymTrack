package com.gymtrack.presentation.settings

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.gymtrack.domain.identity.GoogleIdentityFailure
import com.gymtrack.domain.identity.GoogleSignInResult
import com.gymtrack.domain.model.GoogleUser
import com.gymtrack.domain.model.ThemeMode
import com.gymtrack.domain.repository.GoogleIdentityRepository
import com.gymtrack.domain.repository.ThemeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class SettingsViewModelIdentityTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var themeRepository: FakeSettingsThemeRepository
    private lateinit var identityRepository: FakeGoogleIdentityRepository
    private lateinit var viewModel: SettingsViewModel
    private val hostContext: Context = Application()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        themeRepository = FakeSettingsThemeRepository()
        identityRepository = FakeGoogleIdentityRepository()
        viewModel = SettingsViewModel(themeRepository, identityRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_noGoogleUser() {
        assertNull(viewModel.uiState.value.googleUser)
        assertFalse(viewModel.uiState.value.accountActionInProgress)
    }

    @Test
    fun signIn_success_exposesUser() = runTest(testDispatcher) {
        identityRepository.nextSignInResult = GoogleSignInResult.Success

        viewModel.signInWithGoogle(hostContext)
        advanceUntilIdle()

        assertEquals(GoogleUser("Danilo Barros"), viewModel.uiState.value.googleUser)
        assertFalse(viewModel.uiState.value.accountActionInProgress)
        assertNull(viewModel.uiState.value.accountFeedback)
    }

    @Test
    fun signIn_cancelled_showsCancelledFeedback() = runTest(testDispatcher) {
        identityRepository.nextSignInResult = GoogleSignInResult.Cancelled

        viewModel.signInWithGoogle(hostContext)
        advanceUntilIdle()

        assertEquals(SettingsAccountFeedback.Cancelled, viewModel.uiState.value.accountFeedback)
    }

    @Test
    fun signIn_notConfigured_showsNotConfiguredFeedback() = runTest(testDispatcher) {
        identityRepository.nextSignInResult =
            GoogleSignInResult.Failed(GoogleIdentityFailure.NotConfigured)

        viewModel.signInWithGoogle(hostContext)
        advanceUntilIdle()

        assertEquals(SettingsAccountFeedback.NotConfigured, viewModel.uiState.value.accountFeedback)
    }

    @Test
    fun signOut_clearsUser() = runTest(testDispatcher) {
        identityRepository.user.value = GoogleUser("Danilo")

        viewModel.signOut(hostContext)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.googleUser)
        assertTrue(identityRepository.signOutCalls == 1)
    }

}

private class FakeSettingsThemeRepository : ThemeRepository {
    private val state = MutableStateFlow(ThemeMode.Default)
    override val themeMode: Flow<ThemeMode> = state.asStateFlow()
    override suspend fun setThemeMode(mode: ThemeMode) {
        state.value = mode
    }
}

private class FakeGoogleIdentityRepository : GoogleIdentityRepository {
    val user = MutableStateFlow<GoogleUser?>(null)
    var nextSignInResult: GoogleSignInResult = GoogleSignInResult.Cancelled
    var signInCalls = 0
    var signOutCalls = 0
    override val currentUser = user.asStateFlow()

    override suspend fun tryRestoreSilentSignIn(hostContext: Context) = Unit

    override suspend fun signIn(hostContext: Context): GoogleSignInResult {
        signInCalls++
        return when (val result = nextSignInResult) {
            GoogleSignInResult.Success -> {
                user.value = GoogleUser("Danilo Barros")
                result
            }
            else -> result
        }
    }

    override suspend fun signOut(hostContext: Context) {
        signOutCalls++
        user.value = null
    }
}

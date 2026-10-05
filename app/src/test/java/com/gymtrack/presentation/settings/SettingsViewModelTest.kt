package com.gymtrack.presentation.settings

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.gymtrack.domain.model.ThemeMode
import android.content.Context
import com.gymtrack.domain.identity.GoogleSignInResult
import com.gymtrack.domain.model.GoogleUser
import com.gymtrack.domain.repository.GoogleIdentityRepository
import com.gymtrack.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.StateFlow
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var themeRepository: FakeThemeRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        themeRepository = FakeThemeRepository()
        viewModel = SettingsViewModel(themeRepository, FakeGoogleIdentityRepositoryForTheme())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_defaultsToDark() {
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun setThemeMode_light_updatesState() = runTest(testDispatcher) {
        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        assertEquals(ThemeMode.LIGHT, themeRepository.current)
        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
    }

    @Test
    fun setThemeMode_dark_updatesState() = runTest(testDispatcher) {
        themeRepository.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, themeRepository.current)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun selectingTheme_persistsThroughRepository() = runTest(testDispatcher) {
        viewModel.setThemeMode(ThemeMode.LIGHT)
        advanceUntilIdle()
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(
            listOf(ThemeMode.LIGHT, ThemeMode.DARK),
            themeRepository.writes,
        )
    }
}

private class FakeGoogleIdentityRepositoryForTheme : GoogleIdentityRepository {
    private val state = MutableStateFlow<GoogleUser?>(null)
    override val currentUser: StateFlow<GoogleUser?> = state.asStateFlow()
    override suspend fun tryRestoreSilentSignIn(hostContext: Context) = Unit
    override suspend fun signIn(hostContext: Context) = GoogleSignInResult.Cancelled
    override suspend fun signOut(hostContext: Context) {
        state.value = null
    }
}

private class FakeThemeRepository : ThemeRepository {
    private val state = MutableStateFlow(ThemeMode.Default)
    val current get() = state.value
    val writes = mutableListOf<ThemeMode>()

    override val themeMode: Flow<ThemeMode> = state.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        writes += mode
        state.value = mode
    }
}

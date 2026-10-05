package com.gymtrack.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.R
import com.gymtrack.domain.model.ThemeMode
import com.gymtrack.presentation.components.GymCard
import com.gymtrack.presentation.components.GymGoogleSignInButton
import com.gymtrack.presentation.components.GymSecondaryButton
import com.gymtrack.presentation.components.GymSectionHeader
import com.gymtrack.presentation.components.GymTopAppBar
import com.gymtrack.presentation.theme.GymSpacing
import com.gymtrack.presentation.theme.GymTrackTheme

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsScreen(
        uiState = uiState,
        onThemeModeSelected = viewModel::setThemeMode,
        onSignInWithGoogle = { viewModel.signInWithGoogle(context) },
        onSignOut = { viewModel.signOut(context) },
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onSignInWithGoogle: () -> Unit = {},
    onSignOut: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier.padding(contentPadding),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GymTopAppBar(
                title = stringResource(R.string.settings_title),
                eyebrow = stringResource(R.string.settings_eyebrow),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(GymSpacing.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(GymSpacing.SectionSpacing),
        ) {
            AccountSettingsSection(
                uiState = uiState,
                onSignInWithGoogle = onSignInWithGoogle,
                onSignOut = onSignOut,
            )
            AppearanceSettingsSection(
                selectedMode = uiState.themeMode,
                onThemeModeSelected = onThemeModeSelected,
            )
            // Only existing settings live here. Release has none yet; debug adds catalog import.
            DebugSettingsSection(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AccountSettingsSection(
    uiState: SettingsUiState,
    onSignInWithGoogle: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_account"),
        verticalArrangement = Arrangement.spacedBy(GymSpacing.Md),
    ) {
        GymSectionHeader(
            title = stringResource(R.string.settings_account_title),
            eyebrow = stringResource(R.string.settings_account_eyebrow),
        )
        GymCard(
            contentPadding = PaddingValues(GymSpacing.CardPadding),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(GymSpacing.Md),
            ) {
                val user = uiState.googleUser
                if (user == null) {
                    Text(
                        text = stringResource(R.string.settings_account_signed_out_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    GymGoogleSignInButton(
                        onClick = onSignInWithGoogle,
                        enabled = !uiState.accountActionInProgress,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.settings_account_signed_in_greeting, user.displayName),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("settings_account_greeting"),
                    )
                    Text(
                        text = stringResource(R.string.settings_account_connected),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    GymSecondaryButton(
                        text = stringResource(R.string.settings_account_sign_out),
                        onClick = onSignOut,
                        enabled = !uiState.accountActionInProgress,
                        modifier = Modifier.testTag("settings_account_sign_out"),
                    )
                }
                if (uiState.accountActionInProgress) {
                    Text(
                        text = stringResource(R.string.settings_account_sign_in_loading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("settings_account_loading"),
                    )
                }
                uiState.accountFeedback?.let { feedback ->
                    val messageRes = when (feedback) {
                        SettingsAccountFeedback.Cancelled -> R.string.settings_account_error_cancelled
                        SettingsAccountFeedback.NotConfigured -> R.string.settings_account_error_not_configured
                        SettingsAccountFeedback.GenericError -> R.string.settings_account_error_generic
                    }
                    val feedbackColor = when (feedback) {
                        SettingsAccountFeedback.Cancelled -> MaterialTheme.colorScheme.onSurfaceVariant
                        SettingsAccountFeedback.NotConfigured,
                        SettingsAccountFeedback.GenericError,
                        -> MaterialTheme.colorScheme.error
                    }
                    Text(
                        text = stringResource(messageRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = feedbackColor,
                        modifier = Modifier.testTag("settings_account_feedback"),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceSettingsSection(
    selectedMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_theme"),
        verticalArrangement = Arrangement.spacedBy(GymSpacing.Md),
    ) {
        GymSectionHeader(
            title = stringResource(R.string.settings_appearance_title),
            eyebrow = stringResource(R.string.settings_theme_label),
        )
        GymCard(
            contentPadding = PaddingValues(GymSpacing.CardPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(GymSpacing.Xs),
            ) {
                ThemeModeOption(
                    label = stringResource(R.string.settings_theme_light),
                    selected = selectedMode == ThemeMode.LIGHT,
                    onClick = { onThemeModeSelected(ThemeMode.LIGHT) },
                    testTag = "settings_theme_light",
                )
                ThemeModeOption(
                    label = stringResource(R.string.settings_theme_dark),
                    selected = selectedMode == ThemeMode.DARK,
                    onClick = { onThemeModeSelected(ThemeMode.DARK) },
                    testTag = "settings_theme_dark",
                )
            }
        }
    }
}

@Composable
private fun ThemeModeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(GymSpacing.TouchTarget)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .semantics { this.selected = selected }
            .testTag(testTag)
            .padding(horizontal = GymSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(Modifier.weight(1f))
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenDarkPreview() {
    GymTrackTheme(darkTheme = true) {
        SettingsScreen(
            uiState = SettingsUiState(themeMode = ThemeMode.DARK),
            onThemeModeSelected = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenLightPreview() {
    GymTrackTheme(darkTheme = false) {
        SettingsScreen(
            uiState = SettingsUiState(themeMode = ThemeMode.LIGHT),
            onThemeModeSelected = {},
        )
    }
}

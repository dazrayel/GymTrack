package com.gymtrack.presentation.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.R
import com.gymtrack.domain.model.AchievementCatalog
import com.gymtrack.domain.model.AchievementDefinition
import com.gymtrack.domain.model.AchievementMetric
import com.gymtrack.domain.model.AchievementStatus
import com.gymtrack.domain.model.formatVolumeKg
import com.gymtrack.presentation.components.GymCard
import com.gymtrack.presentation.components.GymCardTone
import com.gymtrack.presentation.components.GymIconButton
import com.gymtrack.presentation.components.GymLabel
import com.gymtrack.presentation.components.GymTopAppBar
import com.gymtrack.presentation.theme.GymSpacing
import com.gymtrack.presentation.theme.GymTheme
import com.gymtrack.presentation.theme.GymTrackTheme

@Composable
fun AchievementScreen(
    onNavigateBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    viewModel: AchievementViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AchievementScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onErrorShown = viewModel::clearError,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementScreen(
    uiState: AchievementUiState,
    onNavigateBack: () -> Unit,
    onErrorShown: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        val message = uiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = message)
        onErrorShown()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier.padding(contentPadding),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GymTopAppBar(
                title = stringResource(R.string.achievements_title),
                eyebrow = stringResource(R.string.achievements_eyebrow),
                navigationIcon = {
                    GymIconButton(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        onClick = onNavigateBack,
                    )
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.testTag("achievements_loading"),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            else -> {
                AchievementsContent(
                    achievements = uiState.achievements,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun AchievementsContent(
    achievements: List<AchievementStatus>,
    modifier: Modifier = Modifier,
) {
    val definitionsById = remember { AchievementCatalog.associateBy { it.id } }
    val unlockedCount = achievements.count { it.unlocked }
    val totalCount = achievements.size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("achievements_list"),
        contentPadding = PaddingValues(
            start = GymSpacing.ScreenPadding,
            end = GymSpacing.ScreenPadding,
            top = GymSpacing.Sm,
            bottom = GymSpacing.Lg,
        ),
        verticalArrangement = Arrangement.spacedBy(GymSpacing.CardSpacing),
    ) {
        item {
            GymCard(
                tone = GymCardTone.Highlight,
                contentPadding = PaddingValues(GymSpacing.CardPadding),
            ) {
                GymLabel(text = stringResource(R.string.achievements_progress_label))
                Text(
                    text = stringResource(
                        R.string.achievements_summary,
                        unlockedCount,
                        totalCount,
                    ),
                    style = GymTheme.gymTypography.metric,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(top = GymSpacing.Xs)
                        .testTag("achievements_summary"),
                )
            }
        }
        items(achievements, key = { it.achievementId }) { status ->
            val definition = definitionsById[status.achievementId] ?: return@items
            AchievementCard(status = status, definition = definition)
        }
    }
}

@Composable
private fun AchievementCard(
    status: AchievementStatus,
    definition: AchievementDefinition,
    modifier: Modifier = Modifier,
) {
    val name = achievementString(definition.nameKey)
    val description = achievementString(definition.descriptionKey)
    val contentDescription = stringResource(
        if (status.unlocked) R.string.achievement_unlocked_cd else R.string.achievement_locked_cd,
        name,
    )
    val tone = if (status.unlocked) GymCardTone.Warning else GymCardTone.Surface
    val titleColor = MaterialTheme.colorScheme.onSurface
    val bodyColor = if (status.unlocked) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
    }
    val iconTint = if (status.unlocked) {
        GymTheme.extendedColors.warning
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val progressColor = if (status.unlocked) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    GymCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("achievement_card_${status.achievementId}")
            .semantics { this.contentDescription = contentDescription },
        tone = tone,
        contentPadding = PaddingValues(GymSpacing.CardPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (status.unlocked) {
                    Icons.Outlined.EmojiEvents
                } else {
                    Icons.Outlined.Lock
                },
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(28.dp)
                    .testTag(
                        if (status.unlocked) {
                            "achievement_unlocked_${status.achievementId}"
                        } else {
                            "achievement_locked_${status.achievementId}"
                        },
                    ),
            )
            Spacer(Modifier.width(GymSpacing.Md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = bodyColor,
                    modifier = Modifier.padding(top = GymSpacing.Xxs),
                )
                Text(
                    text = formatAchievementProgress(status, definition.metric),
                    style = MaterialTheme.typography.labelLarge,
                    color = progressColor,
                    modifier = Modifier
                        .padding(top = GymSpacing.Sm)
                        .testTag("achievement_progress_${status.achievementId}"),
                )
            }
        }
    }
}

@Composable
private fun achievementString(key: String): String {
    val context = LocalContext.current
    val resId = context.resources.getIdentifier(
        key.replace('.', '_'),
        "string",
        context.packageName,
    )
    return if (resId != 0) stringResource(resId) else key
}

@Composable
private fun formatAchievementProgress(
    status: AchievementStatus,
    metric: AchievementMetric,
): String {
    return if (metric == AchievementMetric.TOTAL_VOLUME_KG) {
        val currentLabel = formatVolumeKg(status.current.toDouble()).removeSuffix(" kg")
        val targetLabel = formatVolumeKg(status.target.toDouble())
        stringResource(R.string.achievement_progress_volume, currentLabel, targetLabel)
    } else {
        stringResource(
            R.string.achievement_progress,
            status.current.toInt(),
            status.target.toInt(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AchievementScreenPreview() {
    GymTrackTheme(darkTheme = true) {
        AchievementScreen(
            uiState = AchievementUiState(
                isLoading = false,
                achievements = AchievementCatalog.map { definition ->
                    AchievementStatus(
                        achievementId = definition.id,
                        unlocked = definition.id == "FIRST_WORKOUT",
                        current = if (definition.id == "FIRST_WORKOUT") 1L else 0L,
                        target = definition.target,
                    )
                },
            ),
            onNavigateBack = {},
            onErrorShown = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AchievementScreenLoadingPreview() {
    GymTrackTheme(darkTheme = true) {
        AchievementScreen(
            uiState = AchievementUiState(isLoading = true),
            onNavigateBack = {},
            onErrorShown = {},
        )
    }
}

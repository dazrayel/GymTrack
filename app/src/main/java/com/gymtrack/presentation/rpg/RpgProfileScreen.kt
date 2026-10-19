package com.gymtrack.presentation.rpg

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.R
import com.gymtrack.domain.model.AchievementCatalog
import com.gymtrack.domain.model.LevelProgress
import com.gymtrack.domain.model.TitleCatalog
import com.gymtrack.domain.model.TitleStatus
import com.gymtrack.domain.model.TitleUnlockCondition
import com.gymtrack.domain.model.levelProgressFromTotalXp
import com.gymtrack.presentation.components.GymCard
import com.gymtrack.presentation.components.GymCardTone
import com.gymtrack.presentation.components.GymIconButton
import com.gymtrack.presentation.components.GymLabel
import com.gymtrack.presentation.components.GymMetric
import com.gymtrack.presentation.components.GymRarityBadge
import com.gymtrack.presentation.components.GymSectionHeader
import com.gymtrack.presentation.components.GymSecondaryButton
import com.gymtrack.presentation.components.GymTextButton
import com.gymtrack.presentation.components.GymTopAppBar
import com.gymtrack.presentation.theme.GymShapeTokens
import com.gymtrack.presentation.theme.GymSpacing
import com.gymtrack.presentation.theme.GymTheme
import com.gymtrack.presentation.theme.GymTrackTheme

@Composable
fun RpgProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToMissions: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    viewModel: RpgProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    RpgProfileScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onNavigateToAchievements = onNavigateToAchievements,
        onNavigateToMissions = onNavigateToMissions,
        onSelectTitle = viewModel::selectTitle,
        onClearActiveTitle = viewModel::clearActiveTitle,
        onErrorShown = viewModel::clearError,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RpgProfileScreen(
    uiState: RpgProfileUiState,
    onNavigateBack: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToMissions: () -> Unit,
    onSelectTitle: (String) -> Unit,
    onClearActiveTitle: () -> Unit,
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
        modifier = modifier
            .padding(contentPadding)
            .testTag("rpg_profile"),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GymTopAppBar(
                title = stringResource(R.string.rpg_profile_title),
                eyebrow = stringResource(R.string.rpg_profile_eyebrow),
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
                        modifier = Modifier.testTag("rpg_profile_loading"),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            else -> {
                RpgProfileContent(
                    uiState = uiState,
                    onNavigateToAchievements = onNavigateToAchievements,
                    onNavigateToMissions = onNavigateToMissions,
                    onSelectTitle = onSelectTitle,
                    onClearActiveTitle = onClearActiveTitle,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun RpgProfileContent(
    uiState: RpgProfileUiState,
    onNavigateToAchievements: () -> Unit,
    onNavigateToMissions: () -> Unit,
    onSelectTitle: (String) -> Unit,
    onClearActiveTitle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeTitle = uiState.titles.find {
        it.definition.id == uiState.activeTitleId && it.unlocked
    }
    val levelProgress = uiState.levelProgress

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("rpg_profile_list"),
        contentPadding = PaddingValues(
            start = GymSpacing.ScreenPadding,
            end = GymSpacing.ScreenPadding,
            top = GymSpacing.Sm,
            bottom = GymSpacing.Lg,
        ),
        verticalArrangement = Arrangement.spacedBy(GymSpacing.CardSpacing),
    ) {
        item(key = "active_title") {
            ActiveTitleCard(
                activeTitle = activeTitle,
                onClear = onClearActiveTitle,
            )
        }
        if (levelProgress != null) {
            item(key = "level") {
                ProfileLevelCard(progress = levelProgress)
            }
        }
        item(key = "summary_metrics") {
            ProfileSummaryMetrics(
                unlockedAchievements = uiState.unlockedAchievementCount,
                totalAchievements = uiState.totalAchievementCount,
                activeMissions = uiState.activeMissionCount,
                completedMissions = uiState.completedMissionCount,
                personalRecords = uiState.personalRecordCount,
                workouts = uiState.completedWorkoutCount,
            )
        }
        item(key = "links") {
            ProfileQuickLinks(
                onNavigateToAchievements = onNavigateToAchievements,
                onNavigateToMissions = onNavigateToMissions,
            )
        }
        item(key = "titles_header") {
            GymSectionHeader(
                title = stringResource(R.string.rpg_profile_titles_section),
                modifier = Modifier
                    .padding(top = GymSpacing.Sm)
                    .testTag("rpg_profile_titles_header"),
            )
        }
        items(
            items = uiState.titles,
            key = { it.definition.id },
        ) { status ->
            TitleCard(
                status = status,
                isActive = status.definition.id == uiState.activeTitleId && status.unlocked,
                onSelect = { onSelectTitle(status.definition.id) },
            )
        }
    }
}

@Composable
private fun ActiveTitleCard(
    activeTitle: TitleStatus?,
    onClear: () -> Unit,
) {
    GymCard(
        tone = GymCardTone.Highlight,
        contentPadding = PaddingValues(GymSpacing.CardPadding),
        modifier = Modifier.testTag("rpg_profile_active_title"),
    ) {
        GymLabel(text = stringResource(R.string.rpg_profile_active_title_label))
        if (activeTitle == null) {
            Text(
                text = stringResource(R.string.rpg_profile_no_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = GymSpacing.Xs)
                    .testTag("rpg_profile_no_title"),
            )
            Text(
                text = stringResource(R.string.rpg_profile_no_title_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = GymSpacing.Xs),
            )
        } else {
            Text(
                text = stringResource(activeTitle.definition.nameRes),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(top = GymSpacing.Xs)
                    .testTag("rpg_profile_active_title_name"),
            )
            GymRarityBadge(
                rarity = activeTitle.definition.rarity,
                modifier = Modifier.padding(top = GymSpacing.Sm),
            )
            GymTextButton(
                text = stringResource(R.string.rpg_profile_clear_title),
                onClick = onClear,
                modifier = Modifier
                    .padding(top = GymSpacing.Sm)
                    .testTag("rpg_profile_clear_title"),
            )
        }
    }
}

@Composable
private fun ProfileLevelCard(progress: LevelProgress) {
    GymCard(
        contentPadding = PaddingValues(GymSpacing.CardPadding),
        modifier = Modifier.testTag("rpg_profile_level"),
    ) {
        GymLabel(text = stringResource(R.string.home_level_label, progress.level))
        if (progress.isMaxLevel) {
            Text(
                text = stringResource(R.string.home_level_max),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = GymSpacing.Xs),
            )
            Text(
                text = stringResource(
                    R.string.rpg_profile_total_xp,
                    progress.totalXp.toInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = GymSpacing.Xs),
            )
        } else {
            Text(
                text = stringResource(
                    R.string.home_level_xp_progress,
                    progress.xpIntoLevel.toInt(),
                    progress.xpForNextLevel?.toInt() ?: 0,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = GymSpacing.Xs),
            )
            LinearProgressIndicator(
                progress = { progress.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = GymSpacing.Sm)
                    .height(8.dp)
                    .clip(RoundedCornerShape(GymShapeTokens.ExtraSmall))
                    .testTag("rpg_profile_level_bar"),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.rpg_profile_total_xp,
                    progress.totalXp.toInt(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = GymSpacing.Xs),
            )
        }
    }
}

@Composable
private fun ProfileSummaryMetrics(
    unlockedAchievements: Int,
    totalAchievements: Int,
    activeMissions: Int,
    completedMissions: Int,
    personalRecords: Int,
    workouts: Int,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GymSpacing.CardSpacing),
    ) {
        GymSectionHeader(title = stringResource(R.string.rpg_profile_summary_section))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val useSingleColumn = maxWidth < 360.dp
            if (useSingleColumn) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(GymSpacing.CardSpacing),
                ) {
                    MetricAchievementsCard(unlockedAchievements, totalAchievements)
                    MetricWorkoutsCard(workouts)
                    MetricPrsCard(personalRecords)
                    MetricMissionsCard(activeMissions, completedMissions)
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(GymSpacing.CardSpacing),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GymSpacing.CardSpacing),
                    ) {
                        MetricAchievementsCard(
                            unlockedAchievements,
                            totalAchievements,
                            modifier = Modifier.weight(1f),
                        )
                        MetricWorkoutsCard(workouts, modifier = Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(GymSpacing.CardSpacing),
                    ) {
                        MetricPrsCard(personalRecords, modifier = Modifier.weight(1f))
                        MetricMissionsCard(
                            activeMissions,
                            completedMissions,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricAchievementsCard(
    unlocked: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    GymCard(
        modifier = modifier.testTag("rpg_profile_metric_achievements"),
        contentPadding = PaddingValues(GymSpacing.CardPadding),
    ) {
        GymMetric(
            label = stringResource(R.string.rpg_profile_metric_achievements),
            value = "$unlocked / $total",
        )
    }
}

@Composable
private fun MetricWorkoutsCard(
    workouts: Int,
    modifier: Modifier = Modifier,
) {
    GymCard(
        modifier = modifier.testTag("rpg_profile_metric_workouts"),
        contentPadding = PaddingValues(GymSpacing.CardPadding),
    ) {
        GymMetric(
            label = stringResource(R.string.rpg_profile_metric_workouts),
            value = workouts.toString(),
        )
    }
}

@Composable
private fun MetricPrsCard(
    personalRecords: Int,
    modifier: Modifier = Modifier,
) {
    GymCard(
        modifier = modifier.testTag("rpg_profile_metric_prs"),
        contentPadding = PaddingValues(GymSpacing.CardPadding),
    ) {
        GymMetric(
            label = stringResource(R.string.rpg_profile_metric_prs),
            value = personalRecords.toString(),
        )
    }
}

@Composable
private fun MetricMissionsCard(
    activeMissions: Int,
    completedMissions: Int,
    modifier: Modifier = Modifier,
) {
    GymCard(
        modifier = modifier.testTag("rpg_profile_metric_missions"),
        contentPadding = PaddingValues(GymSpacing.CardPadding),
    ) {
        GymMetric(
            label = stringResource(R.string.rpg_profile_metric_missions),
            value = stringResource(
                R.string.rpg_profile_missions_summary,
                activeMissions,
                completedMissions,
            ),
        )
    }
}

@Composable
private fun ProfileQuickLinks(
    onNavigateToAchievements: () -> Unit,
    onNavigateToMissions: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GymSpacing.Sm),
    ) {
        GymSecondaryButton(
            text = stringResource(R.string.rpg_profile_open_achievements),
            onClick = onNavigateToAchievements,
            modifier = Modifier.testTag("rpg_profile_open_achievements"),
        )
        GymSecondaryButton(
            text = stringResource(R.string.rpg_profile_open_missions),
            onClick = onNavigateToMissions,
            modifier = Modifier.testTag("rpg_profile_open_missions"),
        )
    }
}

@Composable
private fun TitleCard(
    status: TitleStatus,
    isActive: Boolean,
    onSelect: () -> Unit,
) {
    val definition = status.definition
    val tone = when {
        isActive -> GymCardTone.Highlight
        status.unlocked -> GymCardTone.Elevated
        else -> GymCardTone.Surface
    }
    val iconTint = when {
        isActive -> MaterialTheme.colorScheme.primary
        status.unlocked -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    GymCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rpg_title_card_${definition.id}"),
        tone = tone,
        contentPadding = PaddingValues(GymSpacing.CardPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = if (status.unlocked) {
                    Icons.Outlined.MilitaryTech
                } else {
                    Icons.Outlined.Lock
                },
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(28.dp)
                    .testTag(
                        if (status.unlocked) {
                            "rpg_title_unlocked_${definition.id}"
                        } else {
                            "rpg_title_locked_${definition.id}"
                        },
                    ),
            )
            Spacer(Modifier.width(GymSpacing.Md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(definition.nameRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(definition.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = GymSpacing.Xxs),
                )
                GymRarityBadge(
                    rarity = definition.rarity,
                    modifier = Modifier
                        .padding(top = GymSpacing.Sm)
                        .testTag("rpg_title_rarity_${definition.id}"),
                )
                if (!status.unlocked) {
                    Text(
                        text = unlockHint(definition.unlockCondition),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = GymSpacing.Sm)
                            .testTag("rpg_title_requirement_${definition.id}"),
                    )
                } else if (isActive) {
                    Text(
                        text = stringResource(R.string.rpg_profile_title_in_use),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = GymSpacing.Sm)
                            .testTag("rpg_title_in_use_${definition.id}"),
                    )
                } else {
                    GymTextButton(
                        text = stringResource(R.string.rpg_profile_use_title),
                        onClick = onSelect,
                        modifier = Modifier
                            .padding(top = GymSpacing.Xs)
                            .testTag("rpg_title_use_${definition.id}"),
                    )
                }
            }
        }
    }
}

@Composable
private fun unlockHint(condition: TitleUnlockCondition): String = when (condition) {
    is TitleUnlockCondition.MinLevel ->
        stringResource(R.string.rpg_profile_requires_level, condition.level)
    is TitleUnlockCondition.AchievementUnlocked -> {
        val achievement = AchievementCatalog.find { it.id == condition.achievementId }
        val name = achievement?.let { achievementString(it.nameKey) } ?: condition.achievementId
        stringResource(R.string.rpg_profile_requires_achievement, name)
    }
}

@Composable
private fun achievementString(key: String): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    val resId = context.resources.getIdentifier(
        key.replace('.', '_'),
        "string",
        context.packageName,
    )
    return if (resId != 0) stringResource(resId) else key
}

@Preview(showBackground = true)
@Composable
private fun RpgProfilePreviewDark() {
    GymTrackTheme(darkTheme = true) {
        RpgProfileScreen(
            uiState = previewState(),
            onNavigateBack = {},
            onNavigateToAchievements = {},
            onNavigateToMissions = {},
            onSelectTitle = {},
            onClearActiveTitle = {},
            onErrorShown = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RpgProfilePreviewLight() {
    GymTrackTheme(darkTheme = false) {
        RpgProfileScreen(
            uiState = previewState(),
            onNavigateBack = {},
            onNavigateToAchievements = {},
            onNavigateToMissions = {},
            onSelectTitle = {},
            onClearActiveTitle = {},
            onErrorShown = {},
        )
    }
}

private fun previewState(): RpgProfileUiState {
    val level = levelProgressFromTotalXp(5_609)
    val titles = TitleCatalog.all.mapIndexed { index, definition ->
        TitleStatus(definition = definition, unlocked = index < 3)
    }
    return RpgProfileUiState(
        isLoading = false,
        levelProgress = level,
        titles = titles,
        activeTitleId = TitleCatalog.all.first().id,
        unlockedAchievementCount = 4,
        totalAchievementCount = 18,
        activeMissionCount = 2,
        completedMissionCount = 1,
        personalRecordCount = 3,
        completedWorkoutCount = 12,
    )
}

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
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
import com.gymtrack.domain.model.AchievementCategory
import com.gymtrack.domain.model.AchievementCategoryOrder
import com.gymtrack.domain.model.AchievementDefinition
import com.gymtrack.domain.model.AchievementFamilies
import com.gymtrack.domain.model.AchievementMetric
import com.gymtrack.domain.model.AchievementStatus
import com.gymtrack.domain.model.formatVolumeKg
import com.gymtrack.presentation.components.GymCard
import com.gymtrack.presentation.components.GymCardTone
import com.gymtrack.presentation.components.GymIconButton
import com.gymtrack.presentation.components.GymLabel
import com.gymtrack.presentation.components.GymRarityBadge
import com.gymtrack.presentation.components.GymSectionHeader
import com.gymtrack.presentation.components.GymTopAppBar
import com.gymtrack.presentation.theme.GymShapeTokens
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
    val unlockedCount = achievements.count { it.unlocked }
    val totalCount = achievements.size
    val overallFraction = if (totalCount == 0) {
        0f
    } else {
        unlockedCount.toFloat() / totalCount.toFloat()
    }

    val listRows = remember(achievements) {
        buildAchievementListRows(achievements)
    }

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
        item(key = "overall_progress") {
            OverallProgressCard(
                unlockedCount = unlockedCount,
                totalCount = totalCount,
                fraction = overallFraction,
            )
        }
        items(
            items = listRows,
            key = { it.key },
        ) { row ->
            when (row) {
                is AchievementListRow.SectionHeader -> {
                    GymSectionHeader(
                        title = categoryTitle(row.category),
                        modifier = Modifier
                            .padding(top = GymSpacing.Sm)
                            .testTag("achievement_section_${row.category.name}"),
                    )
                }
                is AchievementListRow.FamilyLabel -> {
                    GymLabel(
                        text = familyTitle(row.familyId),
                        modifier = Modifier
                            .padding(top = GymSpacing.Xs)
                            .testTag("achievement_family_${row.familyId}"),
                    )
                }
                is AchievementListRow.Card -> {
                    AchievementCard(
                        status = row.status,
                        definition = row.definition,
                    )
                }
            }
        }
    }
}

private sealed class AchievementListRow {
    abstract val key: String

    data class SectionHeader(val category: AchievementCategory) : AchievementListRow() {
        override val key: String = "section_${category.name}"
    }

    data class FamilyLabel(val familyId: String) : AchievementListRow() {
        override val key: String = "family_$familyId"
    }

    data class Card(
        val status: AchievementStatus,
        val definition: AchievementDefinition,
    ) : AchievementListRow() {
        override val key: String = status.achievementId
    }
}

private fun buildAchievementListRows(
    achievements: List<AchievementStatus>,
): List<AchievementListRow> {
    val statusById = achievements.associateBy { it.achievementId }
    val rows = mutableListOf<AchievementListRow>()
    for (category in AchievementCategoryOrder) {
        val definitions = AchievementCatalog
            .filter { it.category == category }
            .sortedWith(
                compareBy<AchievementDefinition> { it.familyId ?: "" }
                    .thenBy { it.tierIndex ?: Int.MAX_VALUE }
                    .thenBy { it.id },
            )
        val cards = definitions.mapNotNull { definition ->
            val status = statusById[definition.id] ?: return@mapNotNull null
            definition to status
        }
        if (cards.isEmpty()) continue
        rows.add(AchievementListRow.SectionHeader(category))
        var lastFamily: String? = null
        for ((definition, status) in cards) {
            val family = definition.familyId
            if (family != null && family != lastFamily) {
                rows.add(AchievementListRow.FamilyLabel(family))
                lastFamily = family
            } else if (family == null) {
                lastFamily = null
            }
            rows.add(AchievementListRow.Card(status, definition))
        }
    }
    return rows
}

@Composable
private fun OverallProgressCard(
    unlockedCount: Int,
    totalCount: Int,
    fraction: Float,
) {
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
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = GymSpacing.Md)
                .height(8.dp)
                .clip(RoundedCornerShape(GymShapeTokens.ExtraSmall))
                .testTag("achievements_overall_progress"),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
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
    val fraction = if (status.target <= 0L) {
        0f
    } else {
        (status.current.toFloat() / status.target.toFloat()).coerceIn(0f, 1f)
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
            verticalAlignment = Alignment.Top,
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
                GymRarityBadge(
                    rarity = definition.rarity,
                    modifier = Modifier
                        .padding(top = GymSpacing.Sm)
                        .testTag("achievement_rarity_${status.achievementId}"),
                )
                Text(
                    text = formatAchievementProgress(status, definition.metric),
                    style = MaterialTheme.typography.labelLarge,
                    color = progressColor,
                    modifier = Modifier
                        .padding(top = GymSpacing.Sm)
                        .testTag("achievement_progress_${status.achievementId}"),
                )
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = GymSpacing.Sm)
                        .height(6.dp)
                        .clip(RoundedCornerShape(GymShapeTokens.ExtraSmall))
                        .testTag("achievement_bar_${status.achievementId}"),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun categoryTitle(category: AchievementCategory): String = when (category) {
    AchievementCategory.MILESTONES -> stringResource(R.string.achievement_category_milestones)
    AchievementCategory.VOLUME -> stringResource(R.string.achievement_category_volume)
    AchievementCategory.EFFORT -> stringResource(R.string.achievement_category_effort)
    AchievementCategory.EXPLORATION -> stringResource(R.string.achievement_category_exploration)
    AchievementCategory.RECORDS -> stringResource(R.string.achievement_category_records)
    AchievementCategory.CONSISTENCY -> stringResource(R.string.achievement_category_consistency)
}

@Composable
private fun familyTitle(familyId: String): String = when (familyId) {
    AchievementFamilies.WORKOUTS -> stringResource(R.string.achievement_family_workouts)
    AchievementFamilies.VOLUME -> stringResource(R.string.achievement_family_volume)
    AchievementFamilies.SETS -> stringResource(R.string.achievement_family_sets)
    AchievementFamilies.EXERCISES -> stringResource(R.string.achievement_family_exercises)
    AchievementFamilies.PRS -> stringResource(R.string.achievement_family_prs)
    AchievementFamilies.TRAINED_DAYS -> stringResource(R.string.achievement_family_trained_days)
    else -> familyId
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

@Preview(showBackground = true)
@Composable
private fun AchievementScreenLightPreview() {
    GymTrackTheme(darkTheme = false) {
        AchievementScreen(
            uiState = AchievementUiState(
                isLoading = false,
                achievements = AchievementCatalog.map { definition ->
                    AchievementStatus(
                        achievementId = definition.id,
                        unlocked = false,
                        current = 0L,
                        target = definition.target,
                    )
                },
            ),
            onNavigateBack = {},
            onErrorShown = {},
        )
    }
}

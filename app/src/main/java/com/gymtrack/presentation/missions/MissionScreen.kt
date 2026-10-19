package com.gymtrack.presentation.missions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Flag
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.R
import com.gymtrack.domain.model.MissionProgressView
import com.gymtrack.domain.model.MissionStatus
import com.gymtrack.domain.model.MissionType
import com.gymtrack.presentation.components.GymCard
import com.gymtrack.presentation.components.GymCardTone
import com.gymtrack.presentation.components.GymIconButton
import com.gymtrack.presentation.components.GymLabel
import com.gymtrack.presentation.components.GymMetaBadge
import com.gymtrack.presentation.components.GymTopAppBar
import com.gymtrack.presentation.theme.GymSpacing

@Composable
fun MissionScreen(
    onNavigateBack: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    viewModel: MissionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MissionScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onErrorShown = viewModel::clearError,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionScreen(
    uiState: MissionUiState,
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
                title = stringResource(R.string.missions_title),
                eyebrow = stringResource(R.string.missions_eyebrow),
                navigationIcon = {
                    GymIconButton(
                        onClick = onNavigateBack,
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
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
                        .padding(innerPadding)
                        .testTag("missions_loading"),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            uiState.missions.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(GymSpacing.ScreenPadding)
                        .testTag("missions_empty"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    GymLabel(text = stringResource(R.string.missions_eyebrow))
                    Text(
                        text = stringResource(R.string.missions_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = GymSpacing.Sm),
                    )
                }
            }
            else -> {
                MissionList(
                    missions = uiState.missions,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun MissionList(
    missions: List<MissionProgressView>,
    modifier: Modifier = Modifier,
) {
    val daily = missions.filter { it.definition.type == MissionType.DAILY }
    val weekly = missions.filter { it.definition.type == MissionType.WEEKLY }
    val longTerm = missions.filter {
        it.definition.type == MissionType.ONE_TIME || it.definition.type == MissionType.LONG_TERM
    }

    LazyColumn(
        modifier = modifier.testTag("missions_list"),
        contentPadding = PaddingValues(
            horizontal = GymSpacing.ScreenPadding,
            vertical = GymSpacing.Md,
        ),
        verticalArrangement = Arrangement.spacedBy(GymSpacing.CardSpacing),
    ) {
        if (daily.isNotEmpty()) {
            item {
                GymLabel(
                    text = stringResource(R.string.missions_section_today),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(daily, key = { "${it.definition.id}:${it.instance.periodKey}" }) { mission ->
                MissionCard(mission = mission)
            }
        }
        if (weekly.isNotEmpty()) {
            item {
                GymLabel(
                    text = stringResource(R.string.missions_section_week),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = GymSpacing.Sm),
                )
            }
            items(weekly, key = { "${it.definition.id}:${it.instance.periodKey}" }) { mission ->
                MissionCard(mission = mission)
            }
        }
        if (longTerm.isNotEmpty()) {
            item {
                GymLabel(
                    text = stringResource(R.string.missions_section_long_term),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = GymSpacing.Sm),
                )
            }
            items(longTerm, key = { "${it.definition.id}:${it.instance.periodKey}" }) { mission ->
                MissionCard(mission = mission)
            }
        }
    }
}

@Composable
private fun MissionCard(
    mission: MissionProgressView,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(mission.definition.titleRes)
    val description = stringResource(mission.definition.descriptionRes)
    val cd = stringResource(
        R.string.missions_cd,
        title,
        mission.progress,
        mission.target,
    )
    val completed = mission.instance.status == MissionStatus.COMPLETED || mission.isCompleted
    val typeLabel = when (mission.definition.type) {
        MissionType.DAILY -> stringResource(R.string.missions_section_today)
        MissionType.WEEKLY -> stringResource(R.string.missions_section_week)
        MissionType.ONE_TIME, MissionType.LONG_TERM ->
            stringResource(R.string.missions_section_long_term)
    }
    val tone = if (completed) GymCardTone.Highlight else GymCardTone.Surface

    GymCard(
        modifier = modifier
            .testTag("mission_${mission.definition.id}")
            .semantics { contentDescription = cd },
        tone = tone,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                GymMetaBadge(
                    label = typeLabel,
                    background = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("mission_type_${mission.definition.id}"),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = GymSpacing.Sm),
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = GymSpacing.Xxs),
                )
            }
            Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = null,
                tint = if (completed) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Text(
            text = stringResource(R.string.missions_progress, mission.progress, mission.target),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = GymSpacing.Md),
        )
        LinearProgressIndicator(
            progress = { mission.progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = GymSpacing.Sm),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = GymSpacing.Sm),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.missions_xp_reward, mission.definition.xpReward),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (completed) {
                Text(
                    text = stringResource(R.string.missions_completed_badge),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

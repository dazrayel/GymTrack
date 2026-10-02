package com.gymtrack.achievements

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.TestActivity
import com.gymtrack.domain.model.AchievementCatalog
import com.gymtrack.domain.model.AchievementStatus
import com.gymtrack.domain.model.formatVolumeKg
import com.gymtrack.presentation.achievements.AchievementScreen
import com.gymtrack.presentation.achievements.AchievementUiState
import com.gymtrack.presentation.theme.GymTrackTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AchievementScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun waitUntilTextIsDisplayed(text: String, substring: Boolean = false) {
        composeTestRule.waitUntil(5_000) {
            val nodes = composeTestRule.onAllNodesWithText(
                text,
                substring = substring,
                useUnmergedTree = true,
            )
            nodes.fetchSemanticsNodes().isNotEmpty() && nodes[0].isDisplayed()
        }
        composeTestRule.onAllNodesWithText(
            text,
            substring = substring,
            useUnmergedTree = true,
        )[0].assertIsDisplayed()
    }

    private fun setContent(uiState: AchievementUiState) {
        composeTestRule.setContent {
            GymTrackTheme {
                AchievementScreen(
                    uiState = uiState,
                    onNavigateBack = {},
                    onErrorShown = {},
                    contentPadding = PaddingValues(),
                )
            }
        }
    }

    private fun allLocked(): List<AchievementStatus> =
        AchievementCatalog.map { definition ->
            AchievementStatus(
                achievementId = definition.id,
                unlocked = false,
                current = 0L,
                target = definition.target,
            )
        }

    private fun withUnlocks(vararg unlockedIds: String): List<AchievementStatus> {
        val unlocked = unlockedIds.toSet()
        return AchievementCatalog.map { definition ->
            val isUnlocked = definition.id in unlocked
            AchievementStatus(
                achievementId = definition.id,
                unlocked = isUnlocked,
                current = if (isUnlocked) definition.target else 0L,
                target = definition.target,
            )
        }
    }

    @Test
    fun loadingState_showsProgressIndicator() {
        setContent(AchievementUiState(isLoading = true))
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithTag("achievements_loading")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag("achievements_loading").assertIsDisplayed()
        composeTestRule.onNodeWithTag("achievements_list").assertDoesNotExist()
    }

    @Test
    fun title_isDisplayed() {
        setContent(
            AchievementUiState(isLoading = false, achievements = allLocked()),
        )
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Conquistas")
    }

    private fun scrollToAchievement(id: String) {
        composeTestRule.onNodeWithTag("achievements_list")
            .performScrollToNode(hasTestTag("achievement_card_$id"))
    }

    @Test
    fun zeroUnlocked_showsSummaryAndAllCardsLocked() {
        setContent(
            AchievementUiState(isLoading = false, achievements = allLocked()),
        )
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("0 de 18 conquistas desbloqueadas")
        composeTestRule.onNodeWithTag("achievements_summary").assertIsDisplayed()
        composeTestRule.onNodeWithTag("achievements_list").assertIsDisplayed()
        AchievementCatalog.forEach { definition ->
            scrollToAchievement(definition.id)
            composeTestRule.onNodeWithTag("achievement_card_${definition.id}")
                .assertIsDisplayed()
            composeTestRule.onNodeWithTag("achievement_locked_${definition.id}")
                .assertIsDisplayed()
            composeTestRule.onNodeWithTag("achievement_unlocked_${definition.id}")
                .assertDoesNotExist()
        }
    }

    @Test
    fun someUnlocked_showsCorrectSummaryAndVisualStates() {
        val statuses = withUnlocks("FIRST_WORKOUT", "WORKOUTS_10")
        setContent(AchievementUiState(isLoading = false, achievements = statuses))
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("2 de 18 conquistas desbloqueadas")
        scrollToAchievement("FIRST_WORKOUT")
        composeTestRule.onNodeWithTag("achievement_unlocked_FIRST_WORKOUT").assertIsDisplayed()
        scrollToAchievement("WORKOUTS_10")
        composeTestRule.onNodeWithTag("achievement_unlocked_WORKOUTS_10").assertIsDisplayed()
        scrollToAchievement("WORKOUTS_25")
        composeTestRule.onNodeWithTag("achievement_locked_WORKOUTS_25").assertIsDisplayed()
        waitUntilTextIsDisplayed("Primeiro treino")
        waitUntilTextIsDisplayed("10 treinos")
    }

    @Test
    fun allEighteenAchievements_areDisplayed() {
        setContent(
            AchievementUiState(isLoading = false, achievements = allLocked()),
        )
        composeTestRule.waitForIdle()
        AchievementCatalog.forEach { definition ->
            scrollToAchievement(definition.id)
            composeTestRule.onNodeWithTag("achievement_card_${definition.id}")
                .assertIsDisplayed()
        }
    }

    @Test
    fun progress_isDisplayedForWorkoutsAndVolume() {
        val statuses = AchievementCatalog.map { definition ->
            when (definition.id) {
                "WORKOUTS_10" -> AchievementStatus(
                    achievementId = definition.id,
                    unlocked = false,
                    current = 7L,
                    target = 10L,
                )
                "VOLUME_10000" -> AchievementStatus(
                    achievementId = definition.id,
                    unlocked = false,
                    current = 7_450L,
                    target = 10_000L,
                )
                else -> AchievementStatus(
                    achievementId = definition.id,
                    unlocked = false,
                    current = 0L,
                    target = definition.target,
                )
            }
        }
        setContent(AchievementUiState(isLoading = false, achievements = statuses))
        composeTestRule.waitForIdle()

        scrollToAchievement("WORKOUTS_10")
        composeTestRule.onNodeWithTag("achievement_progress_WORKOUTS_10").assertIsDisplayed()
        waitUntilTextIsDisplayed("7 / 10")

        scrollToAchievement("VOLUME_10000")
        val currentVolume = formatVolumeKg(7_450.0).removeSuffix(" kg")
        val targetVolume = formatVolumeKg(10_000.0)
        val volumeProgress = "$currentVolume / $targetVolume"
        composeTestRule.onNodeWithTag("achievement_progress_VOLUME_10000").assertIsDisplayed()
        waitUntilTextIsDisplayed(volumeProgress)
        assert(volumeProgress.contains("kg")) {
            "Volume progress should include kg unit: $volumeProgress"
        }
        composeTestRule.onNodeWithText(volumeProgress, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun error_isConsumedViaSnackbarPattern() {
        var cleared = false
        composeTestRule.setContent {
            GymTrackTheme {
                AchievementScreen(
                    uiState = AchievementUiState(
                        isLoading = false,
                        achievements = allLocked(),
                        error = "Falha ao carregar conquistas",
                    ),
                    onNavigateBack = {},
                    onErrorShown = { cleared = true },
                    contentPadding = PaddingValues(),
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(5_000) { cleared }
        waitUntilTextIsDisplayed("0 de 18 conquistas desbloqueadas")
    }
}

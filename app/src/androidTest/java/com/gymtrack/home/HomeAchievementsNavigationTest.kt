package com.gymtrack.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HomeAchievementsNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun waitUntilTextIsDisplayed(text: String) {
        composeTestRule.waitUntil(8_000) {
            val nodes = composeTestRule.onAllNodesWithText(text, useUnmergedTree = true)
            nodes.fetchSemanticsNodes().isNotEmpty() && nodes[0].isDisplayed()
        }
        composeTestRule.onAllNodesWithText(text, useUnmergedTree = true)[0].assertIsDisplayed()
    }

    @Test
    fun homeAchievementsEntry_opensAchievementsScreen_andBackReturnsHome() {
        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("home_achievements")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag("home_achievements").assertIsDisplayed()
        waitUntilTextIsDisplayed("0 de 18 desbloqueadas")

        composeTestRule.onNodeWithTag("home_achievements").performClick()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Conquistas")
        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("achievements_list")
                .fetchSemanticsNodes()
                .isNotEmpty() ||
                composeTestRule.onAllNodesWithTag("achievements_loading")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
        }
        waitUntilTextIsDisplayed("0 de 18 conquistas desbloqueadas")

        composeTestRule.onNodeWithContentDescription("Voltar").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("home_achievements")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag("home_achievements").assertIsDisplayed()
        waitUntilTextIsDisplayed("0 de 18 desbloqueadas")
        composeTestRule.onNodeWithTag("achievements_list").assertDoesNotExist()
    }
}

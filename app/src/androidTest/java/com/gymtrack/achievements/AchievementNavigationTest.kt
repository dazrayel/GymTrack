package com.gymtrack.achievements

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.TestActivity
import com.gymtrack.presentation.navigation.GymTrackNavGraph
import com.gymtrack.presentation.navigation.ROUTE_ACHIEVEMENTS
import com.gymtrack.presentation.theme.GymTrackTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AchievementNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<TestActivity>()

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

    private fun waitUntilHomeIsDisplayed() {
        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("home_achievements")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag("home_achievements").assertIsDisplayed()
        composeTestRule.onNodeWithText("GymTrack").assertIsDisplayed()
    }

    private fun setNavGraphContent(): NavHostController {
        val holder = arrayOfNulls<NavHostController>(1)
        composeTestRule.setContent {
            val navController = rememberNavController()
            holder[0] = navController
            GymTrackTheme {
                GymTrackNavGraph(navController = navController)
            }
        }
        composeTestRule.waitForIdle()
        return checkNotNull(holder[0])
    }

    @Test
    fun achievementsRoute_existsWithoutRequiredArguments() {
        val navController = setNavGraphContent()

        val destination = navController.graph.findNode(ROUTE_ACHIEVEMENTS)
        assertNotNull(destination)
        assertTrue(destination!!.arguments.isEmpty())
        assertEquals(ROUTE_ACHIEVEMENTS, destination.route)
    }

    @Test
    fun navigateToAchievements_showsTitle() {
        val navController = setNavGraphContent()

        composeTestRule.runOnIdle {
            navController.navigate(ROUTE_ACHIEVEMENTS) {
                launchSingleTop = true
            }
        }
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
    }

    @Test
    fun achievementsBack_returnsToPreviousScreen() {
        val navController = setNavGraphContent()

        waitUntilHomeIsDisplayed()

        composeTestRule.runOnIdle {
            navController.navigate(ROUTE_ACHIEVEMENTS) {
                launchSingleTop = true
            }
        }
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Conquistas")

        composeTestRule.onNodeWithContentDescription("Voltar").performClick()
        composeTestRule.waitForIdle()

        waitUntilHomeIsDisplayed()
        composeTestRule.onNodeWithTag("achievements_list").assertDoesNotExist()
        composeTestRule.onNodeWithTag("achievements_loading").assertDoesNotExist()
    }

    @Test
    fun achievementsNavigation_doesNotBreakExistingHomeRoute() {
        val navController = setNavGraphContent()

        waitUntilHomeIsDisplayed()

        composeTestRule.runOnIdle {
            navController.navigate(ROUTE_ACHIEVEMENTS) {
                launchSingleTop = true
            }
        }
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Conquistas")

        composeTestRule.onNodeWithContentDescription("Voltar").performClick()
        composeTestRule.waitForIdle()

        waitUntilHomeIsDisplayed()
        composeTestRule.onNodeWithTag("achievements_list").assertDoesNotExist()
        composeTestRule.onNodeWithTag("achievements_loading").assertDoesNotExist()
    }
}

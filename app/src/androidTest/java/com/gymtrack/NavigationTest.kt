package com.gymtrack

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun navigation_homeScreen_isDisplayedOnLaunch() {
        composeTestRule
            .onNodeWithText("GymTrack")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Acesso rápido")
            .assertIsDisplayed()
    }

    @Test
    fun navigation_homeToExercises_exercisesScreenIsDisplayed() {
        composeTestRule
            .onNodeWithText("Exercícios")
            .performClick()

        composeTestRule.waitForIdle()

        // Back button is unique to ExercisesScreen
        composeTestRule
            .onNodeWithContentDescription("Voltar")
            .assertIsDisplayed()
    }

    @Test
    fun navigation_exercisesToHome_homeScreenIsDisplayed() {
        composeTestRule
            .onNodeWithText("Exercícios")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("Voltar")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Acesso rápido")
            .assertIsDisplayed()
    }

    @Test
    fun navigation_exercisesRoute_doesNotDuplicateOnBackStack() {
        // Navigate to exercises twice in sequence (simulating rapid taps)
        composeTestRule
            .onNodeWithText("Exercícios")
            .performClick()
        composeTestRule.waitForIdle()

        // A single back press must return directly to HomeScreen (no duplicate exercises entry)
        composeTestRule
            .onNodeWithContentDescription("Voltar")
            .performClick()
        composeTestRule.waitForIdle()

        // HomeScreen is restored — no intermediate exercises screen
        composeTestRule
            .onNodeWithText("Acesso rápido")
            .assertIsDisplayed()
    }
}

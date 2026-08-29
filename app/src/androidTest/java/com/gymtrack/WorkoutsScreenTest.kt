package com.gymtrack

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WorkoutsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun workoutsScreen_isAccessibleViaBottomNav() {
        // Navigate to Workouts via BottomNav
        composeTestRule
            .onNodeWithText("Treinos")
            .performClick()

        composeTestRule.waitForIdle()

        // Verify WorkoutsScreen title is displayed
        composeTestRule
            .onNodeWithText("Meus treinos")
            .assertIsDisplayed()
    }

    @Test
    fun workoutsScreen_emptyState_isDisplayed() {
        composeTestRule
            .onNodeWithText("Treinos")
            .performClick()

        composeTestRule.waitForIdle()

        // Empty state message should be visible when no workouts exist
        composeTestRule
            .onNodeWithText("Nenhum treino cadastrado")
            .assertIsDisplayed()
    }

    @Test
    fun workoutsScreen_tapWorkoutCard_navigatesToDetail() {
        // Navigate to Workouts
        composeTestRule.onNodeWithText("Treinos").performClick()
        composeTestRule.waitForIdle()

        // Create a workout via the FAB + dialog
        composeTestRule.onNodeWithContentDescription("Adicionar treino").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Nome do treino").performTextInput("Treino Nav Test")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Salvar").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Treino Nav Test").assertIsDisplayed()

        // Tap the card — now wired to navigate to WorkoutDetailScreen
        composeTestRule.onNodeWithText("Treino Nav Test").performClick()
        composeTestRule.waitForIdle()

        // WorkoutDetailScreen is open: workout name appears in TopAppBar and back button is visible
        composeTestRule.onNodeWithText("Treino Nav Test").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Voltar").assertIsDisplayed()

        // Go back to WorkoutsScreen
        composeTestRule.onNodeWithContentDescription("Voltar").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Meus treinos").assertIsDisplayed()

        // Clean up: delete the workout so subsequent tests start with an empty database
        composeTestRule.onNodeWithContentDescription("Excluir treino").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Excluir").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun workoutsScreen_addWorkout_dialogOpensAndCloses() {
        composeTestRule
            .onNodeWithText("Treinos")
            .performClick()

        composeTestRule.waitForIdle()

        // FAB uses Icon with contentDescription — use contentDescription matcher to click it
        composeTestRule
            .onNodeWithContentDescription("Adicionar treino")
            .performClick()

        composeTestRule.waitForIdle()

        // Dialog title is real text — verify dialog is open
        composeTestRule
            .onNodeWithText("Adicionar treino")
            .assertIsDisplayed()

        // Dismiss dialog
        composeTestRule
            .onNodeWithText("Cancelar")
            .performClick()

        composeTestRule.waitForIdle()

        // Dialog is dismissed — screen title is visible again
        composeTestRule
            .onNodeWithText("Meus treinos")
            .assertIsDisplayed()
    }
}

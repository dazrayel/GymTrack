package com.gymtrack

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.repository.WorkoutRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Instrumented E2E navigation tests for the Workouts → WorkoutDetail flow.
 *
 * Uses the full [MainActivity] + [GymTrackNavGraph] to validate that
 * [WorkoutDetailScreen] is correctly wired and receives the right workoutId.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WorkoutDetailNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var workoutRepository: WorkoutRepository

    private var workoutId: Long = -1L
    private val workoutName = "Treino Nav Detail"

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            workoutId = workoutRepository.save(
                Workout(name = workoutName, description = "Teste de navegação"),
            )
        }
    }

    @After
    fun tearDown() {
        if (workoutId == -1L) return
        runBlocking {
            workoutRepository.delete(Workout(id = workoutId, name = workoutName, description = ""))
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Teste 1 — Workouts → WorkoutDetail
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Validates that tapping a workout card navigates to WorkoutDetailScreen and
     * that the correct workout is displayed (confirming workoutId was transmitted).
     */
    @Test
    fun navigation_workoutsToDetail_detailScreenIsDisplayed() {
        // Navigate to Workouts via Bottom Nav
        composeTestRule.onNodeWithText("Treinos").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Meus treinos").assertIsDisplayed()

        // Tap the workout card
        composeTestRule.onNodeWithText(workoutName).performClick()
        composeTestRule.waitForIdle()

        // WorkoutDetailScreen shows the correct workout name in the TopAppBar
        composeTestRule.onNodeWithText(workoutName).assertIsDisplayed()

        // Back button is present (unique to detail screen, not a top-level destination)
        composeTestRule.onNodeWithContentDescription("Voltar").assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Teste 2 — WorkoutDetail → Workouts (back navigation)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun navigation_detailToWorkouts_workoutsScreenIsRestored() {
        composeTestRule.onNodeWithText("Treinos").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(workoutName).performClick()
        composeTestRule.waitForIdle()

        // We are in WorkoutDetailScreen — press back
        composeTestRule.onNodeWithContentDescription("Voltar").performClick()
        composeTestRule.waitForIdle()

        // WorkoutsScreen must be visible again
        composeTestRule.onNodeWithText("Meus treinos").assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Teste 3 — Back stack: Workouts → Detail → Back, sem duplicação
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun navigation_workoutsDetailBack_noBackStackDuplication() {
        composeTestRule.onNodeWithText("Treinos").performClick()
        composeTestRule.waitForIdle()

        // Navigate to detail
        composeTestRule.onNodeWithText(workoutName).performClick()
        composeTestRule.waitForIdle()

        // A single back press must return to WorkoutsScreen directly
        composeTestRule.onNodeWithContentDescription("Voltar").performClick()
        composeTestRule.waitForIdle()

        // WorkoutsScreen is visible — no intermediate detail screen
        composeTestRule.onNodeWithText("Meus treinos").assertIsDisplayed()

        // The detail screen is gone — back button is no longer visible
        composeTestRule.onNodeWithContentDescription("Voltar").assertDoesNotExist()
    }
}

package com.gymtrack.workouts.execution

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.TestActivity
import com.gymtrack.domain.model.ProgressionAction
import com.gymtrack.domain.model.ProgressionPerformedSet
import com.gymtrack.domain.model.ProgressionSuggestion
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSessionStatus
import com.gymtrack.presentation.theme.GymTrackTheme
import com.gymtrack.presentation.workouts.execution.WorkoutExecutionPhase
import com.gymtrack.presentation.workouts.execution.WorkoutExecutionScreen
import com.gymtrack.presentation.workouts.execution.WorkoutExecutionUiState
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WorkoutExecutionProgressionUiTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun increaseWeight_showsSuggestionAndTargetLoad() {
        setContent(
            suggestion = ProgressionSuggestion(
                action = ProgressionAction.INCREASE_WEIGHT,
                plannedWeight = 80.0,
                suggestedWeight = 82.5,
                minRepetitions = 8,
                maxRepetitions = 12,
                plannedSets = 3,
                lastSessionSets = sets(12, 12, 12, weight = 80.0),
            ),
            canApply = true,
        )

        composeTestRule.onNodeWithTag("progression_suggestion").assertIsDisplayed()
        composeTestRule.onNodeWithText("Progressão sugerida").assertIsDisplayed()
        composeTestRule.onNodeWithText("Última vez: 12/12/12 × 80 kg").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tentar 82,5 kg").assertIsDisplayed()
        composeTestRule.onNodeWithTag("apply_progression_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("exercise_actions_menu").assertIsDisplayed()
    }

    @Test
    fun increaseReps_hidesApplyAction() {
        setContent(
            suggestion = ProgressionSuggestion(
                action = ProgressionAction.INCREASE_REPS,
                plannedWeight = 80.0,
                suggestedWeight = null,
                minRepetitions = 8,
                maxRepetitions = 12,
                plannedSets = 3,
                lastSessionSets = sets(8, 9, 9, weight = 80.0),
            ),
        )

        composeTestRule.onNodeWithTag("progression_suggestion").assertIsDisplayed()
        composeTestRule.onNodeWithText("Buscar mais repetições na mesma carga").assertIsDisplayed()
        assertTrue(
            composeTestRule.onAllNodesWithTag("apply_progression_button")
                .fetchSemanticsNodes()
                .isEmpty(),
        )
        assertTrue(
            composeTestRule.onAllNodesWithTag("exercise_actions_menu")
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }

    @Test
    fun maintain_hidesApplyAction() {
        setContent(
            suggestion = ProgressionSuggestion(
                action = ProgressionAction.MAINTAIN,
                plannedWeight = 80.0,
                suggestedWeight = null,
                minRepetitions = 8,
                maxRepetitions = 12,
                plannedSets = 3,
                lastSessionSets = sets(8, 8, 7, weight = 80.0),
            ),
        )

        composeTestRule.onNodeWithTag("progression_suggestion").assertIsDisplayed()
        assertTrue(
            composeTestRule.onAllNodesWithTag("apply_progression_button")
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }

    @Test
    fun noHistory_hidesApplyAction() {
        setContent(
            suggestion = ProgressionSuggestion(
                action = ProgressionAction.NO_HISTORY,
                plannedWeight = 80.0,
                suggestedWeight = null,
                minRepetitions = 8,
                maxRepetitions = 12,
                plannedSets = 3,
                lastSessionSets = emptyList(),
            ),
        )

        composeTestRule.onNodeWithTag("progression_suggestion").assertIsDisplayed()
        assertTrue(
            composeTestRule.onAllNodesWithTag("apply_progression_button")
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }

    @Test
    fun nullSuggestion_hidesProgressionArea() {
        setContent(suggestion = null)

        assertEquals(
            0,
            composeTestRule.onAllNodesWithTag("progression_suggestion")
                .fetchSemanticsNodes()
                .size,
        )
        assertTrue(
            composeTestRule.onAllNodesWithTag("progression_guidance")
                .fetchSemanticsNodes()
                .isEmpty(),
        )
        assertTrue(
            composeTestRule.onAllNodesWithTag("apply_progression_button")
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }

    @Test
    fun applyButton_opensConfirmationDialog() {
        var requestCount = 0
        setContent(
            suggestion = ProgressionSuggestion(
                action = ProgressionAction.INCREASE_WEIGHT,
                plannedWeight = 80.0,
                suggestedWeight = 82.5,
                minRepetitions = 8,
                maxRepetitions = 12,
                plannedSets = 3,
                lastSessionSets = sets(12, 12, 12, weight = 80.0),
            ),
            canApply = true,
            showApplyConfirmation = true,
            applyFrom = 80.0,
            applyTo = 82.5,
            onRequestApply = { requestCount++ },
        )

        composeTestRule.onNodeWithTag("apply_progression_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Aplicar progressão?").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Alterar a carga planejada de 80 kg para 82,5 kg neste exercício?",
        ).assertIsDisplayed()
        composeTestRule.onNodeWithTag("confirm_apply_progression_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cancel_apply_progression_button").assertIsDisplayed()
        assertEquals(0, requestCount)
    }

    @Test
    fun applyButton_clickRequestsConfirmation() {
        var requestCount = 0
        setContent(
            suggestion = ProgressionSuggestion(
                action = ProgressionAction.INCREASE_WEIGHT,
                plannedWeight = 80.0,
                suggestedWeight = 82.5,
                minRepetitions = 8,
                maxRepetitions = 12,
                plannedSets = 3,
                lastSessionSets = sets(12, 12, 12, weight = 80.0),
            ),
            canApply = true,
            onRequestApply = { requestCount++ },
        )

        composeTestRule.onNodeWithTag("apply_progression_button").performClick()
        assertEquals(1, requestCount)
    }

    @Test
    fun menuItem_clickRequestsSameConfirmationFlow() {
        var requestCount = 0
        setContent(
            suggestion = ProgressionSuggestion(
                action = ProgressionAction.INCREASE_WEIGHT,
                plannedWeight = 80.0,
                suggestedWeight = 82.5,
                minRepetitions = 8,
                maxRepetitions = 12,
                plannedSets = 3,
                lastSessionSets = sets(12, 12, 12, weight = 80.0),
            ),
            canApply = true,
            onRequestApply = { requestCount++ },
        )

        composeTestRule.onNodeWithTag("exercise_actions_menu").performClick()
        composeTestRule.onNodeWithTag("apply_progression_menu_item").performClick()
        assertEquals(1, requestCount)
    }

    @Test
    fun cancelApply_dismissesDialog() {
        var dismissCount = 0
        setContent(
            suggestion = ProgressionSuggestion(
                action = ProgressionAction.INCREASE_WEIGHT,
                plannedWeight = 80.0,
                suggestedWeight = 82.5,
                minRepetitions = 8,
                maxRepetitions = 12,
                plannedSets = 3,
                lastSessionSets = sets(12, 12, 12, weight = 80.0),
            ),
            canApply = true,
            showApplyConfirmation = true,
            applyFrom = 80.0,
            applyTo = 82.5,
            onDismissApply = { dismissCount++ },
        )

        composeTestRule.onNodeWithTag("cancel_apply_progression_button").performClick()
        assertEquals(1, dismissCount)
    }

    @Test
    fun confirmApply_invokesConfirmCallback() {
        var confirmCount = 0
        setContent(
            suggestion = ProgressionSuggestion(
                action = ProgressionAction.INCREASE_WEIGHT,
                plannedWeight = 80.0,
                suggestedWeight = 82.5,
                minRepetitions = 8,
                maxRepetitions = 12,
                plannedSets = 3,
                lastSessionSets = sets(12, 12, 12, weight = 80.0),
            ),
            canApply = true,
            showApplyConfirmation = true,
            applyFrom = 80.0,
            applyTo = 82.5,
            onConfirmApply = { confirmCount++ },
        )

        composeTestRule.onNodeWithTag("confirm_apply_progression_button").performClick()
        assertEquals(1, confirmCount)
    }

    @Test
    fun infoMessage_showsSnackbarFeedback() {
        setContent(
            suggestion = ProgressionSuggestion(
                action = ProgressionAction.INCREASE_WEIGHT,
                plannedWeight = 80.0,
                suggestedWeight = 82.5,
                minRepetitions = 8,
                maxRepetitions = 12,
                plannedSets = 3,
                lastSessionSets = sets(12, 12, 12, weight = 80.0),
            ),
            canApply = false,
            infoMessageResId = com.gymtrack.R.string.progression_applied,
        )

        composeTestRule.onNodeWithText("Progressão aplicada ao treino.").assertIsDisplayed()
    }

    private fun setContent(
        suggestion: ProgressionSuggestion?,
        canApply: Boolean = false,
        showApplyConfirmation: Boolean = false,
        applyFrom: Double? = null,
        applyTo: Double? = null,
        infoMessageResId: Int? = null,
        onRequestApply: () -> Unit = {},
        onDismissApply: () -> Unit = {},
        onConfirmApply: () -> Unit = {},
    ) {
        val exercise = WorkoutSessionExercise(
            id = 10L,
            sessionId = 100L,
            exerciseId = 1L,
            position = 0,
            exerciseName = "Supino",
            muscleGroup = "Peito",
            equipmentType = "Barra",
            plannedSets = 3,
            minRepetitions = 8,
            maxRepetitions = 12,
            plannedWeight = 80.0,
            restSeconds = 60,
        )
        val uiState = WorkoutExecutionUiState(
            sessionId = 100L,
            session = WorkoutSession(
                id = 100L,
                workoutId = 1L,
                workoutName = "Push",
                workoutDescription = "",
                startedAtMillis = 1_000L,
                status = WorkoutSessionStatus.IN_PROGRESS,
            ),
            exercises = listOf(exercise),
            progressionSuggestion = suggestion,
            canApplyProgressionToTemplate = canApply,
            showApplyProgressionConfirmation = showApplyConfirmation,
            applyProgressionFromWeight = applyFrom,
            applyProgressionToWeight = applyTo,
            infoMessageResId = infoMessageResId,
            currentExerciseIndex = 0,
            currentSetIndex = 0,
            phase = WorkoutExecutionPhase.WORKING,
            repsInput = "8",
            weightInput = "80",
            inputsExerciseId = exercise.id,
            isLoading = false,
        )
        composeTestRule.setContent {
            GymTrackTheme {
                WorkoutExecutionScreen(
                    uiState = uiState,
                    onNavigateBack = {},
                    onRequestApplyProgressionSuggestion = onRequestApply,
                    onDismissApplyProgressionConfirmation = onDismissApply,
                    onConfirmApplyProgressionSuggestion = onConfirmApply,
                    contentPadding = PaddingValues(),
                )
            }
        }
    }

    private fun sets(vararg reps: Int, weight: Double): List<ProgressionPerformedSet> =
        reps.mapIndexed { index, rep ->
            ProgressionPerformedSet(setIndex = index, reps = rep, weight = weight)
        }
}

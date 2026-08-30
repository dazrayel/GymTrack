package com.gymtrack.exercises

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.TestActivity
import com.gymtrack.domain.model.Exercise
import com.gymtrack.presentation.exercises.ExerciseUiState
import com.gymtrack.presentation.exercises.ExercisesScreen
import com.gymtrack.presentation.theme.GymTrackTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExercisesScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun setContent(
        uiState: ExerciseUiState,
        onExerciseClick: (Exercise) -> Unit = {},
        onExerciseStatsClick: (String) -> Unit = {},
        onDeleteClick: (Exercise) -> Unit = {},
    ) {
        composeTestRule.setContent {
            GymTrackTheme {
                ExercisesScreen(
                    uiState = uiState,
                    outerPadding = PaddingValues(),
                    onNavigateBack = {},
                    onSearchQueryChange = {},
                    onAddClick = {},
                    onExerciseClick = onExerciseClick,
                    onExerciseStatsClick = onExerciseStatsClick,
                    onDeleteClick = onDeleteClick,
                    onSaveExercise = { _, _, _ -> },
                    onDismissDialog = {},
                    onConfirmDelete = {},
                    onDismissDelete = {},
                    onErrorShown = {},
                )
            }
        }
    }

    private fun listed(vararg names: String) = ExerciseUiState(
        isLoading = false,
        exercises = names.mapIndexed { index, name ->
            Exercise(
                id = index + 1L,
                name = name,
                muscleGroup = "Peitoral",
                equipmentType = "Barra",
            )
        },
    )

    @Test
    fun list_showsExerciseAndViewPerformanceAction() {
        setContent(listed("Supino reto"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("exercise_card_Supino reto").assertIsDisplayed()
        composeTestRule.onNodeWithText("Supino reto").assertIsDisplayed()
        composeTestRule.onNodeWithTag("exercise_view_performance_Supino reto").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ver desempenho").assertIsDisplayed()
    }

    @Test
    fun viewPerformance_exposesExactExerciseName() {
        var clickedName: String? = null
        var edited: Exercise? = null
        setContent(
            listed("Supino reto"),
            onExerciseClick = { edited = it },
            onExerciseStatsClick = { clickedName = it },
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("exercise_view_performance_Supino reto").performClick()
        composeTestRule.waitForIdle()
        assertEquals("Supino reto", clickedName)
        assertNull(edited)
    }

    @Test
    fun viewPerformance_exposesNameWithSpace() {
        var clickedName: String? = null
        setContent(
            listed("Supino inclinado"),
            onExerciseStatsClick = { clickedName = it },
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("exercise_view_performance_Supino inclinado").performClick()
        composeTestRule.waitForIdle()
        assertEquals("Supino inclinado", clickedName)
    }

    @Test
    fun viewPerformance_exposesNameWithAccent() {
        var clickedName: String? = null
        setContent(
            listed("Elevação lateral"),
            onExerciseStatsClick = { clickedName = it },
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("exercise_view_performance_Elevação lateral").performClick()
        composeTestRule.waitForIdle()
        assertEquals("Elevação lateral", clickedName)
    }

    @Test
    fun viewPerformance_availableWithoutHistory() {
        var clickedName: String? = null
        setContent(
            listed("Crucifixo"),
            onExerciseStatsClick = { clickedName = it },
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("exercise_view_performance_Crucifixo").performClick()
        composeTestRule.waitForIdle()
        assertEquals("Crucifixo", clickedName)
    }

    @Test
    fun clickingCard_opensEditDialog() {
        val exercise = Exercise(1, "Supino reto", "Peitoral", "Barra")
        var edited: Exercise? = null
        var statsName: String? = null
        composeTestRule.setContent {
            var uiState by mutableStateOf(
                ExerciseUiState(isLoading = false, exercises = listOf(exercise)),
            )
            GymTrackTheme {
                ExercisesScreen(
                    uiState = uiState,
                    outerPadding = PaddingValues(),
                    onNavigateBack = {},
                    onSearchQueryChange = {},
                    onAddClick = {},
                    onExerciseClick = {
                        edited = it
                        uiState = uiState.copy(showAddEditDialog = true, exerciseToEdit = it)
                    },
                    onExerciseStatsClick = { statsName = it },
                    onDeleteClick = {},
                    onSaveExercise = { _, _, _ -> },
                    onDismissDialog = {},
                    onConfirmDelete = {},
                    onDismissDelete = {},
                    onErrorShown = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("exercise_card_Supino reto").performClick()
        composeTestRule.waitForIdle()
        assertEquals(exercise, edited)
        assertNull(statsName)
        composeTestRule.onNodeWithText("Editar exercício").assertIsDisplayed()
    }

    @Test
    fun clickingDelete_stillRequestsDeletion() {
        val exercise = Exercise(1, "Supino reto", "Peitoral", "Barra")
        var deleted: Exercise? = null
        var statsName: String? = null
        composeTestRule.setContent {
            var uiState by mutableStateOf(
                ExerciseUiState(isLoading = false, exercises = listOf(exercise)),
            )
            GymTrackTheme {
                ExercisesScreen(
                    uiState = uiState,
                    outerPadding = PaddingValues(),
                    onNavigateBack = {},
                    onSearchQueryChange = {},
                    onAddClick = {},
                    onExerciseClick = {},
                    onExerciseStatsClick = { statsName = it },
                    onDeleteClick = {
                        deleted = it
                        uiState = uiState.copy(showDeleteConfirmation = true, exerciseToDelete = it)
                    },
                    onSaveExercise = { _, _, _ -> },
                    onDismissDialog = {},
                    onConfirmDelete = {},
                    onDismissDelete = {},
                    onErrorShown = {},
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("exercise_delete_Supino reto").performClick()
        composeTestRule.waitForIdle()
        assertEquals(exercise, deleted)
        assertNull(statsName)
        composeTestRule.onNodeWithText("Excluir exercício").assertIsDisplayed()
    }
}

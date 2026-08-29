package com.gymtrack.workouts.detail

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.TestActivity
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.repository.ExerciseRepository
import com.gymtrack.domain.repository.WorkoutRepository
import com.gymtrack.presentation.workouts.detail.WorkoutDetailScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

private const val TEST_ROUTE = "workout_detail/{workoutId}"

/**
 * Instrumented tests for [WorkoutDetailScreen].
 *
 * Uses [TestActivity] (declared in androidTest/AndroidManifest.xml) so the screen can be
 * rendered in isolation before it is wired into GymTrackNavGraph.
 *
 * A minimal NavHost provides the workoutId argument through SavedStateHandle, which is how
 * WorkoutDetailViewModel reads it.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WorkoutDetailScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var exerciseRepository: ExerciseRepository

    private var workoutId: Long = -1L
    private var exerciseId: Long = -1L

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            workoutId = workoutRepository.save(
                Workout(name = "Treino Teste", description = "Descrição de teste"),
            )
            exerciseId = exerciseRepository.save(
                Exercise(name = "Supino Teste", muscleGroup = "Peitoral", equipmentType = "Barra"),
            )
        }
    }

    @After
    fun tearDown() {
        if (workoutId == -1L) return
        runBlocking {
            // Deleting the workout cascades to workout_exercises (FK CASCADE).
            workoutRepository.delete(
                Workout(id = workoutId, name = "Treino Teste", description = ""),
            )
            exerciseRepository.delete(
                Exercise(id = exerciseId, name = "Supino Teste", muscleGroup = "", equipmentType = ""),
            )
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper — renders the screen inside a minimal NavHost so that
    // SavedStateHandle receives the workoutId correctly via the route arg.
    // ──────────────────────────────────────────────────────────────────────────

    private fun setScreen() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = "workout_detail/$workoutId",
            ) {
                composable(
                    route = TEST_ROUTE,
                    arguments = listOf(navArgument("workoutId") { type = NavType.LongType }),
                ) {
                    WorkoutDetailScreen(
                        onNavigateBack = {},
                        contentPadding = PaddingValues(),
                    )
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 1 — empty state
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun emptyState_isDisplayed_whenNoExercises() {
        setScreen()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Nenhum exercício neste treino").assertIsDisplayed()
        composeTestRule.onNodeWithText("Toque no botão + para adicionar o primeiro exercício")
            .assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 2 — FAB opens picker
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun fab_isVisible_andOpensPicker() {
        setScreen()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Adicionar exercício").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Adicionar exercício").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Selecionar exercício").assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 3 — picker shows catalogue exercises
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun picker_showsAvailableExercises() {
        setScreen()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Adicionar exercício").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Supino Teste").assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 4 — selecting an exercise opens the configuration dialog
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun selectingExercise_opensConfigurationDialog() {
        setScreen()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Adicionar exercício").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Supino Teste").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Configurar exercício").assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 5 — cancelling configuration dismisses dialog, nothing is persisted
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun cancellingConfiguration_dismissesDialog_andNothingIsSaved() {
        setScreen()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Adicionar exercício").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Supino Teste").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Cancelar").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Configurar exercício").assertIsNotDisplayed()
        composeTestRule.onNodeWithText("Nenhum exercício neste treino").assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 6 — edit exercise opens the edit dialog
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun editExercise_opensEditDialog() {
        runBlocking {
            workoutRepository.addExercise(
                WorkoutExercise(
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                    position = 0,
                    sets = 3,
                    minRepetitions = 8,
                    maxRepetitions = 12,
                    weight = 40.0,
                    restSeconds = 60,
                ),
            )
        }

        setScreen()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Supino Teste").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Editar exercício").performClick()
        composeTestRule.waitForIdle()

        // When editing an existing entry, the dialog title is "Editar exercício"
        composeTestRule.onNodeWithText("Editar exercício").assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Test 7 — cancel delete confirmation leaves exercise in the list
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun cancelDeleteConfirmation_exerciseRemainsInList() {
        runBlocking {
            workoutRepository.addExercise(
                WorkoutExercise(
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                    position = 0,
                    sets = 3,
                    minRepetitions = 8,
                    maxRepetitions = 12,
                    weight = 40.0,
                    restSeconds = 60,
                ),
            )
        }

        setScreen()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Remover exercício").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Remover exercício").assertIsDisplayed()

        composeTestRule.onNodeWithText("Cancelar").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Supino Teste").assertIsDisplayed()
    }
}

package com.gymtrack.workouts.detail

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.TestActivity
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.StartSessionResult
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.repository.ExerciseRepository
import com.gymtrack.domain.repository.WorkoutRepository
import com.gymtrack.domain.repository.WorkoutSessionRepository
import com.gymtrack.presentation.workouts.detail.WorkoutDetailScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    @Inject lateinit var workoutSessionRepository: WorkoutSessionRepository

    private var workoutId: Long = -1L
    private var extraWorkoutId: Long = -1L
    private var exerciseId: Long = -1L
    private val extraExerciseIds = mutableListOf<Long>()

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
            workoutSessionRepository.observeInProgress().first()?.let { active ->
                workoutSessionRepository.finishSession(active.id)
            }
            if (extraWorkoutId != -1L) {
                workoutRepository.delete(
                    Workout(id = extraWorkoutId, name = "", description = ""),
                )
                extraWorkoutId = -1L
            }
            // Deleting the workout cascades to workout_exercises (FK CASCADE).
            workoutRepository.delete(
                Workout(id = workoutId, name = "Treino Teste", description = ""),
            )
            extraExerciseIds.forEach { id ->
                exerciseRepository.delete(
                    Exercise(id = id, name = "", muscleGroup = "", equipmentType = ""),
                )
            }
            extraExerciseIds.clear()
            exerciseRepository.delete(
                Exercise(id = exerciseId, name = "Supino Teste", muscleGroup = "", equipmentType = ""),
            )
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helper — renders the screen inside a minimal NavHost so that
    // SavedStateHandle receives the workoutId correctly via the route arg.
    // ──────────────────────────────────────────────────────────────────────────

    private fun setScreen(onNavigateToExecution: (Long) -> Unit = {}) {
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
                        onNavigateToExecution = onNavigateToExecution,
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

        waitUntilTextIsDisplayed("Nenhum exercício neste treino")
        composeTestRule.onNodeWithText("Toque no botão + para adicionar o primeiro exercício")
            .assertIsDisplayed()
    }

    @Test
    fun startWorkoutButton_isDisabled_whenWorkoutHasNoExercises() {
        setScreen()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Iniciar treino").assertIsDisplayed()
        composeTestRule.onNodeWithText("Iniciar treino").assertIsNotEnabled()
    }

    @Test
    fun startWorkoutButton_isEnabled_whenWorkoutHasExercises() {
        addDefaultListedExercise(position = 0)
        setScreen()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Iniciar treino").assertIsEnabled()
    }

    @Test
    fun startWorkout_otherInProgress_showsConflictDialog_cancelDoesNotNavigate() {
        val activeName = "Treino Ativo"
        val activeSessionId = runBlocking {
            extraWorkoutId = workoutRepository.save(Workout(name = activeName, description = ""))
            workoutRepository.addExercise(
                WorkoutExercise(
                    workoutId = extraWorkoutId,
                    exerciseId = exerciseId,
                    position = 0,
                    sets = 1,
                    minRepetitions = 1,
                    maxRepetitions = 1,
                    weight = 1.0,
                    restSeconds = 0,
                ),
            )
            (workoutSessionRepository.startSession(extraWorkoutId) as StartSessionResult.Created).sessionId
        }
        addDefaultListedExercise(position = 0)

        var navigatedSessionId: Long? = null
        setScreen { navigatedSessionId = it }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Iniciar treino").performClick()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Já existe um treino em andamento")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Já existe um treino em andamento").assertIsDisplayed()
        composeTestRule.onNodeWithText("Você já está realizando o treino $activeName.")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Cancelar").performClick()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Já existe um treino em andamento")
                .fetchSemanticsNodes()
                .isEmpty()
        }
        composeTestRule.onNodeWithText("Treino Teste").assertIsDisplayed()
        assertNull(navigatedSessionId)
        assertEquals(
            activeSessionId,
            runBlocking { workoutSessionRepository.observeInProgress().first() }?.id,
        )
    }

    @Test
    fun startWorkout_otherInProgress_continueNavigatesToActiveSession() {
        val activeName = "Treino Ativo"
        val activeSessionId = runBlocking {
            extraWorkoutId = workoutRepository.save(Workout(name = activeName, description = ""))
            workoutRepository.addExercise(
                WorkoutExercise(
                    workoutId = extraWorkoutId,
                    exerciseId = exerciseId,
                    position = 0,
                    sets = 1,
                    minRepetitions = 1,
                    maxRepetitions = 1,
                    weight = 1.0,
                    restSeconds = 0,
                ),
            )
            (workoutSessionRepository.startSession(extraWorkoutId) as StartSessionResult.Created).sessionId
        }
        addDefaultListedExercise(position = 0)

        var navigatedSessionId: Long? = null
        setScreen { navigatedSessionId = it }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Iniciar treino").performClick()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Continuar treino em andamento")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Continuar treino em andamento").performClick()
        composeTestRule.waitUntil(5_000) { navigatedSessionId == activeSessionId }
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
        waitUntilTextIsDisplayed("Nenhum exercício neste treino")
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

    private fun openConfigurationDialog() {
        setScreen()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Adicionar exercício").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Supino Teste").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Configurar exercício").assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Validation — séries
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun configuration_setsZero_showsError_andDoesNotSave() {
        openConfigurationDialog()

        composeTestRule.onNodeWithTag("sets_field").performTextReplacement("0")
        composeTestRule.onNodeWithText("Salvar").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("As séries devem ser pelo menos 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Configurar exercício").assertIsDisplayed()
    }

    @Test
    fun configuration_setsNegative_showsError_andDoesNotSave() {
        openConfigurationDialog()

        composeTestRule.onNodeWithTag("sets_field").performTextReplacement("-1")
        composeTestRule.onNodeWithText("Salvar").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("As séries devem ser pelo menos 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Configurar exercício").assertIsDisplayed()
    }

    @Test
    fun configuration_setsNonNumeric_showsError_andDoesNotSave() {
        openConfigurationDialog()

        composeTestRule.onNodeWithTag("sets_field").performTextReplacement("abc")
        composeTestRule.onNodeWithText("Salvar").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Valor inválido").assertIsDisplayed()
        composeTestRule.onNodeWithText("Configurar exercício").assertIsDisplayed()
    }

    @Test
    fun configuration_minRepsGreaterThanMax_showsError_andDoesNotSave() {
        openConfigurationDialog()

        composeTestRule.onNodeWithTag("min_reps_field").performTextReplacement("12")
        composeTestRule.onNodeWithTag("max_reps_field").performTextReplacement("8")
        composeTestRule.onNodeWithText("Salvar").performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("O máximo de repetições deve ser maior ou igual ao mínimo")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Configurar exercício").assertIsDisplayed()
    }

    @Test
    fun configuration_negativeWeight_showsError_andDoesNotSave() {
        openConfigurationDialog()

        composeTestRule.onNodeWithTag("weight_field").performTextReplacement("-10")
        composeTestRule.onNodeWithText("Salvar").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("A carga deve ser zero ou maior").assertIsDisplayed()
        composeTestRule.onNodeWithText("Configurar exercício").assertIsDisplayed()
    }

    @Test
    fun configuration_negativeRest_showsError_andDoesNotSave() {
        openConfigurationDialog()

        composeTestRule.onNodeWithTag("rest_seconds_field").performTextReplacement("-30")
        composeTestRule.onNodeWithText("Salvar").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("O descanso deve ser zero ou maior").assertIsDisplayed()
        composeTestRule.onNodeWithText("Configurar exercício").assertIsDisplayed()
    }

    @Test
    fun configuration_validValues_savesAndClosesDialog() {
        openConfigurationDialog()

        composeTestRule.onNodeWithTag("sets_field").performTextReplacement("4")
        composeTestRule.onNodeWithTag("min_reps_field").performTextReplacement("8")
        composeTestRule.onNodeWithTag("max_reps_field").performTextReplacement("12")
        composeTestRule.onNodeWithTag("weight_field").performTextReplacement("60.5")
        composeTestRule.onNodeWithTag("rest_seconds_field").performTextReplacement("90")
        composeTestRule.onNodeWithText("Salvar").performClick()
        composeTestRule.waitUntil(5_000) {
            composeTestRule
                .onAllNodesWithText("4× • 8–12 reps • 60.5 kg")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithText("Supino Teste").assertIsDisplayed()
        composeTestRule.onNodeWithText("4× • 8–12 reps • 60.5 kg").assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Reorder
    // ──────────────────────────────────────────────────────────────────────────

    private fun addListedExercise(name: String, position: Int) {
        runBlocking {
            val id = exerciseRepository.save(
                Exercise(name = name, muscleGroup = "Teste", equipmentType = "Barra"),
            )
            extraExerciseIds += id
            workoutRepository.addExercise(
                WorkoutExercise(
                    workoutId = workoutId,
                    exerciseId = id,
                    position = position,
                    sets = 3,
                    minRepetitions = 8,
                    maxRepetitions = 12,
                    weight = 40.0,
                    restSeconds = 60,
                ),
            )
        }
    }

    private fun addDefaultListedExercise(position: Int) {
        runBlocking {
            workoutRepository.addExercise(
                WorkoutExercise(
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                    position = position,
                    sets = 3,
                    minRepetitions = 8,
                    maxRepetitions = 12,
                    weight = 40.0,
                    restSeconds = 60,
                ),
            )
        }
    }

    private fun waitUntilTextIsDisplayed(text: String) {
        composeTestRule.waitUntil(5_000) {
            val nodes = composeTestRule.onAllNodesWithText(text)
            nodes.fetchSemanticsNodes().isNotEmpty() && nodes[0].isDisplayed()
        }
        composeTestRule.onNodeWithText(text).assertIsDisplayed()
    }

    private fun waitForMoveControl(description: String) {
        composeTestRule.waitUntil(5_000) {
            composeTestRule
                .onAllNodesWithContentDescription(description)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun clickMoveControl(description: String) {
        waitForMoveControl(description)
        composeTestRule
            .onNodeWithContentDescription(description)
            .performScrollTo()
            .performClick()
    }

    private fun isNameDisplayedAbove(upper: String, lower: String): Boolean {
        val upperNodes = composeTestRule.onAllNodesWithText(upper).fetchSemanticsNodes()
        val lowerNodes = composeTestRule.onAllNodesWithText(lower).fetchSemanticsNodes()
        if (upperNodes.isEmpty() || lowerNodes.isEmpty()) return false
        return upperNodes.first().positionInRoot.y < lowerNodes.first().positionInRoot.y
    }

    private fun assertNameDisplayedAbove(upper: String, lower: String) {
        assertTrue("$upper should appear above $lower", isNameDisplayedAbove(upper, lower))
    }

    @Test
    fun reorder_firstExercise_upDisabled_downEnabled() {
        addDefaultListedExercise(position = 0)
        addListedExercise("Agachamento Teste", position = 1)
        setScreen()
        composeTestRule.waitForIdle()
        waitForMoveControl("Mover Supino Teste para baixo")

        composeTestRule
            .onNodeWithContentDescription("Mover Supino Teste para cima")
            .assertIsNotEnabled()
        composeTestRule
            .onNodeWithContentDescription("Mover Supino Teste para baixo")
            .assertIsEnabled()
    }

    @Test
    fun reorder_middleExercise_bothEnabled() {
        addDefaultListedExercise(position = 0)
        addListedExercise("Agachamento Teste", position = 1)
        addListedExercise("Remada Teste", position = 2)
        setScreen()
        composeTestRule.waitForIdle()
        waitForMoveControl("Mover Agachamento Teste para cima")

        composeTestRule
            .onNodeWithContentDescription("Mover Agachamento Teste para cima")
            .performScrollTo()
            .assertIsEnabled()
        composeTestRule
            .onNodeWithContentDescription("Mover Agachamento Teste para baixo")
            .assertIsEnabled()
        composeTestRule.waitForIdle()
    }

    @Test
    fun reorder_lastExercise_upEnabled_downDisabled() {
        addDefaultListedExercise(position = 0)
        addListedExercise("Agachamento Teste", position = 1)
        setScreen()
        composeTestRule.waitForIdle()
        waitForMoveControl("Mover Agachamento Teste para cima")

        composeTestRule
            .onNodeWithContentDescription("Mover Agachamento Teste para cima")
            .assertIsEnabled()
        composeTestRule
            .onNodeWithContentDescription("Mover Agachamento Teste para baixo")
            .assertIsNotEnabled()
    }

    @Test
    fun reorder_moveFirstExerciseDown_changesDisplayedOrder() {
        addDefaultListedExercise(position = 0)
        addListedExercise("Agachamento Teste", position = 1)
        setScreen()
        composeTestRule.waitForIdle()

        clickMoveControl("Mover Supino Teste para baixo")
        composeTestRule.waitUntil(5_000) {
            isNameDisplayedAbove("Agachamento Teste", "Supino Teste")
        }

        assertNameDisplayedAbove("Agachamento Teste", "Supino Teste")
    }

    @Test
    fun reorder_moveSecondExerciseUp_changesDisplayedOrder() {
        addDefaultListedExercise(position = 0)
        addListedExercise("Agachamento Teste", position = 1)
        setScreen()
        composeTestRule.waitForIdle()

        clickMoveControl("Mover Agachamento Teste para cima")
        composeTestRule.waitUntil(5_000) {
            isNameDisplayedAbove("Agachamento Teste", "Supino Teste")
        }

        assertNameDisplayedAbove("Agachamento Teste", "Supino Teste")
    }

    @Test
    fun reorder_persistsAfterLeavingAndReenteringScreen() {
        addDefaultListedExercise(position = 0)
        addListedExercise("Agachamento Teste", position = 1)

        composeTestRule.setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = "list",
            ) {
                composable("list") {
                    androidx.compose.material3.TextButton(
                        onClick = { navController.navigate("workout_detail/$workoutId") },
                    ) {
                        androidx.compose.material3.Text("Abrir detalhe")
                    }
                }
                composable(
                    route = TEST_ROUTE,
                    arguments = listOf(navArgument("workoutId") { type = NavType.LongType }),
                ) {
                    WorkoutDetailScreen(
                        onNavigateBack = { navController.popBackStack() },
                        contentPadding = PaddingValues(),
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Abrir detalhe").performClick()
        composeTestRule.waitForIdle()

        clickMoveControl("Mover Supino Teste para baixo")
        composeTestRule.waitUntil(5_000) {
            isNameDisplayedAbove("Agachamento Teste", "Supino Teste")
        }

        composeTestRule.onNodeWithContentDescription("Voltar").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Abrir detalhe").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(5_000) {
            isNameDisplayedAbove("Agachamento Teste", "Supino Teste")
        }

        assertNameDisplayedAbove("Agachamento Teste", "Supino Teste")
    }
}

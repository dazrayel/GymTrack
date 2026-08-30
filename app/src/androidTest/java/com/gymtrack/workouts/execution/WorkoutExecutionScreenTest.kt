package com.gymtrack.workouts.execution

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import com.gymtrack.presentation.workouts.execution.WorkoutExecutionScreen
import com.gymtrack.presentation.workouts.summary.WorkoutSessionSummaryScreen
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

private const val TEST_ROUTE = "workout_execution/{sessionId}"
private const val SUMMARY_ROUTE = "workout_session_summary/{sessionId}"

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WorkoutExecutionScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var exerciseRepository: ExerciseRepository
    @Inject lateinit var workoutSessionRepository: WorkoutSessionRepository

    private var workoutId: Long = -1L
    private var exerciseAId: Long = -1L
    private var exerciseBId: Long = -1L
    private var sessionId: Long = -1L

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            workoutId = workoutRepository.save(
                Workout(name = "Treino Execução", description = "Teste 4.2"),
            )
            exerciseAId = exerciseRepository.save(
                Exercise(name = "Supino Execução", muscleGroup = "Peitoral", equipmentType = "Barra"),
            )
            exerciseBId = exerciseRepository.save(
                Exercise(name = "Crucifixo Execução", muscleGroup = "Peitoral", equipmentType = "Halteres"),
            )
            workoutRepository.addExercise(
                WorkoutExercise(
                    workoutId = workoutId,
                    exerciseId = exerciseAId,
                    position = 0,
                    sets = 2,
                    minRepetitions = 8,
                    maxRepetitions = 12,
                    weight = 40.0,
                    restSeconds = 0,
                ),
            )
            workoutRepository.addExercise(
                WorkoutExercise(
                    workoutId = workoutId,
                    exerciseId = exerciseBId,
                    position = 1,
                    sets = 1,
                    minRepetitions = 10,
                    maxRepetitions = 12,
                    weight = 12.0,
                    restSeconds = 0,
                ),
            )
            sessionId = (workoutSessionRepository.startSession(workoutId) as StartSessionResult.Created).sessionId
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            workoutSessionRepository.observeInProgress().first()?.let { active ->
                workoutSessionRepository.finishSession(active.id)
            }
            if (workoutId != -1L) {
                workoutRepository.delete(
                    Workout(id = workoutId, name = "Treino Execução", description = ""),
                )
            }
            if (exerciseAId != -1L) {
                exerciseRepository.delete(
                    Exercise(id = exerciseAId, name = "", muscleGroup = "", equipmentType = ""),
                )
            }
            if (exerciseBId != -1L) {
                exerciseRepository.delete(
                    Exercise(id = exerciseBId, name = "", muscleGroup = "", equipmentType = ""),
                )
            }
        }
    }

    private fun setScreen() {
        composeTestRule.setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = "workout_execution/$sessionId",
            ) {
                composable(
                    route = TEST_ROUTE,
                    arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
                ) {
                    WorkoutExecutionScreen(
                        onNavigateBack = {},
                        onNavigateToSummary = { id ->
                            navController.navigate("workout_session_summary/$id")
                        },
                        contentPadding = PaddingValues(),
                    )
                }
                composable(
                    route = SUMMARY_ROUTE,
                    arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
                ) {
                    WorkoutSessionSummaryScreen(
                        onNavigateBack = {},
                        contentPadding = PaddingValues(),
                    )
                }
            }
        }
    }

    private fun waitUntilTextIsDisplayed(text: String) {
        composeTestRule.waitUntil(5_000) {
            val nodes = composeTestRule.onAllNodesWithText(text)
            nodes.fetchSemanticsNodes().isNotEmpty() && nodes[0].isDisplayed()
        }
        composeTestRule.onNodeWithText(text).assertIsDisplayed()
    }

    @Test
    fun screen_showsCurrentExercise() {
        setScreen()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Supino Execução")
        composeTestRule.onNodeWithText("Peitoral").assertIsDisplayed()
        composeTestRule.onNodeWithText("Treino Execução").assertIsDisplayed()
    }

    @Test
    fun screen_showsCurrentSet() {
        setScreen()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Série 1 de 2")
    }

    @Test
    fun completeSetButton_isDisplayed() {
        setScreen()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Concluir série")
    }

    @Test
    fun completingSet_advancesToNextSet() {
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Série 1 de 2")

        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Série 2 de 2")
        composeTestRule.onNodeWithText("Supino Execução").assertIsDisplayed()
    }

    @Test
    fun completingAllSetsOfExercise_advancesToNextExercise() {
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Supino Execução")

        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Série 2 de 2")

        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Crucifixo Execução")
        waitUntilTextIsDisplayed("Série 1 de 1")
    }

    @Test
    fun invalidReps_showsError_andDoesNotAdvance() {
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Série 1 de 2")

        composeTestRule.onNodeWithTag("reps_field").performTextReplacement("0")
        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("As repetições devem ser pelo menos 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Série 1 de 2").assertIsDisplayed()
    }

    @Test
    fun invalidWeight_showsError_andDoesNotAdvance() {
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Série 1 de 2")

        composeTestRule.onNodeWithTag("weight_field").performTextReplacement("-10")
        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("A carga deve ser zero ou maior").assertIsDisplayed()
        composeTestRule.onNodeWithText("Série 1 de 2").assertIsDisplayed()
    }

    @Test
    fun completingAllSets_showsBasicCompleteState() {
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Supino Execução")

        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Série 2 de 2")
        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Crucifixo Execução")
        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Treino concluído")
    }

    @Test
    fun screen_receivesPersistedSessionId() {
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Supino Execução")

        val session = runBlocking { workoutSessionRepository.getSession(sessionId) }
        org.junit.Assert.assertNotNull(session)
        org.junit.Assert.assertEquals(sessionId, session!!.id)
        org.junit.Assert.assertEquals("Treino Execução", session.workoutName)
    }

    private fun startFreshSessionWithRest(restSeconds: Int) {
        runBlocking {
            workoutSessionRepository.observeInProgress().first()?.let { active ->
                workoutSessionRepository.finishSession(active.id)
            }
            workoutRepository.getExercises(workoutId).first().forEach { exercise ->
                workoutRepository.updateExercise(exercise.copy(restSeconds = restSeconds))
            }
            sessionId = (workoutSessionRepository.startSession(workoutId) as StartSessionResult.Created).sessionId
        }
    }

    @Test
    fun completingSet_withRest_showsRestScreen() {
        startFreshSessionWithRest(restSeconds = 90)
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Série 1 de 2")

        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Descanso")
        composeTestRule.onNodeWithText("Supino Execução").assertIsDisplayed()
    }

    @Test
    fun restScreen_showsTimerAndPause() {
        startFreshSessionWithRest(restSeconds = 90)
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Série 1 de 2")
        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Descanso")

        composeTestRule.onNodeWithTag("rest_timer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pausar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pular").assertIsDisplayed()
    }

    @Test
    fun pauseRest_showsPausedState() {
        startFreshSessionWithRest(restSeconds = 90)
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Série 1 de 2")
        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Pausar")

        composeTestRule.onNodeWithText("Pausar").performClick()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Descanso pausado")
        composeTestRule.onNodeWithText("Retomar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pular").assertIsDisplayed()
    }

    @Test
    fun resumeRest_returnsToActiveRest() {
        startFreshSessionWithRest(restSeconds = 90)
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Série 1 de 2")
        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Pausar")
        composeTestRule.onNodeWithText("Pausar").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Retomar")

        composeTestRule.onNodeWithText("Retomar").performClick()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Descanso")
        composeTestRule.onNodeWithText("Pausar").assertIsDisplayed()
    }

    @Test
    fun skipRest_returnsToWorking() {
        startFreshSessionWithRest(restSeconds = 90)
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Série 1 de 2")
        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Pular")

        composeTestRule.onNodeWithText("Pular").performClick()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Série 2 de 2")
        composeTestRule.onNodeWithText("Concluir série").assertIsDisplayed()
    }

    @Test
    fun restEnds_returnsToWorking() {
        startFreshSessionWithRest(restSeconds = 1)
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Série 1 de 2")
        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Descanso")

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Série 2 de 2").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Série 2 de 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Concluir série").assertIsDisplayed()
    }

    @Test
    fun completingSet_withoutRest_staysOnWorking() {
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Série 1 de 2")
        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Série 2 de 2")
        composeTestRule.onNodeWithText("Descanso").assertDoesNotExist()
    }

    @Test
    fun restState_isRecoveredFromRoom() {
        startFreshSessionWithRest(restSeconds = 90)
        runBlocking {
            val exerciseId = workoutSessionRepository.observeSessionExercises(sessionId).first()[0].id
            workoutSessionRepository.startRest(sessionId, exerciseId, afterSetIndex = 0, restSeconds = 90)
        }
        setScreen()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Descanso")
        composeTestRule.onNodeWithTag("rest_timer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pausar").assertIsDisplayed()
    }

    @Test
    fun screen_showsElapsedTime() {
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Supino Execução")

        composeTestRule.onNodeWithTag("elapsed_timer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tempo").assertIsDisplayed()
    }

    @Test
    fun screen_showsInitialProgressAsZeroPercent() {
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Supino Execução")

        waitUntilTextIsDisplayed("0% concluído")
        composeTestRule.onNodeWithText("Exercícios: 0/2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Séries: 0/3").assertIsDisplayed()
        composeTestRule.onNodeWithTag("workout_progress_bar").assertIsDisplayed()
    }

    @Test
    fun completingSet_updatesSetCounterAndPercent() {
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("0% concluído")

        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Séries: 1/3")
        composeTestRule.onNodeWithText("33% concluído").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exercícios: 0/2").assertIsDisplayed()
    }

    @Test
    fun completingExercise_updatesCompletedExercises() {
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Supino Execução")

        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Série 2 de 2")
        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Crucifixo Execução")
        composeTestRule.onNodeWithText("Exercícios: 1/2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Séries: 2/3").assertIsDisplayed()
        composeTestRule.onNodeWithText("66% concluído").assertIsDisplayed()
    }

    @Test
    fun progressValues_remainAfterIdleRecomposition() {
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("0% concluído")
        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("33% concluído")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("33% concluído").assertIsDisplayed()
        composeTestRule.onNodeWithText("Séries: 1/3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Série 2 de 2").assertIsDisplayed()
    }

    @Test
    fun restTimer_stillWorksWithProgressVisible() {
        startFreshSessionWithRest(restSeconds = 90)
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Série 1 de 2")
        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Descanso")
        composeTestRule.onNodeWithTag("rest_timer").assertIsDisplayed()
        composeTestRule.onNodeWithTag("elapsed_timer").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pausar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pular").assertIsDisplayed()
        composeTestRule.onNodeWithText("33% concluído").assertIsDisplayed()
    }

    @Test
    fun finishButton_isDisplayed() {
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Supino Execução")
        composeTestRule.onNodeWithTag("finish_workout_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("Finalizar").assertIsDisplayed()
    }

    @Test
    fun finishButton_opensConfirmation() {
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Supino Execução")
        composeTestRule.onNodeWithTag("finish_workout_button").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Finalizar treino?")
        composeTestRule.onNodeWithText("O treino será encerrado. Você poderá ver o resumo do que foi realizado.")
            .assertIsDisplayed()
    }

    @Test
    fun cancelFinish_keepsExecution() {
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Série 1 de 2")
        composeTestRule.onNodeWithTag("finish_workout_button").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Finalizar treino?")
        composeTestRule.onNodeWithTag("cancel_finish_button").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Série 1 de 2")
        composeTestRule.onNodeWithText("Concluir série").assertIsDisplayed()
        val session = runBlocking { workoutSessionRepository.getSession(sessionId) }
        org.junit.Assert.assertEquals(
            com.gymtrack.domain.model.WorkoutSessionStatus.IN_PROGRESS,
            session!!.status,
        )
    }

    @Test
    fun confirmFinish_opensSummary() {
        setScreen()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Supino Execução")
        composeTestRule.onNodeWithText("Concluir série").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Série 2 de 2")
        composeTestRule.onNodeWithTag("finish_workout_button").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Finalizar treino?")
        composeTestRule.onNodeWithTag("confirm_finish_button").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Resumo do treino")
        composeTestRule.onNodeWithText("Treino Execução").assertIsDisplayed()
        composeTestRule.onNodeWithTag("summary_duration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exercícios: 0/2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Séries: 1/3").assertIsDisplayed()
        composeTestRule.onNodeWithTag("summary_volume").assertIsDisplayed()
        val session = runBlocking { workoutSessionRepository.getSession(sessionId) }
        org.junit.Assert.assertEquals(
            com.gymtrack.domain.model.WorkoutSessionStatus.COMPLETED,
            session!!.status,
        )
        org.junit.Assert.assertNotNull(session.endedAtMillis)
    }
}

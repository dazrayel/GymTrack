package com.gymtrack.workouts.summary

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
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
import com.gymtrack.domain.time.formatHistoryDate
import com.gymtrack.domain.time.formatHistoryTime
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

private const val SUMMARY_ROUTE = "workout_session_summary/{sessionId}"

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WorkoutSessionSummaryScreenTest {

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
                Workout(name = "Treino Resumo", description = "Teste 4.5"),
            )
            exerciseAId = exerciseRepository.save(
                Exercise(name = "Supino Resumo", muscleGroup = "Peitoral", equipmentType = "Barra"),
            )
            exerciseBId = exerciseRepository.save(
                Exercise(name = "Crucifixo Resumo", muscleGroup = "Peitoral", equipmentType = "Halteres"),
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
            val exercises = workoutSessionRepository.observeSessionExercises(sessionId).first()
            workoutSessionRepository.completeSet(exercises[0].id, setIndex = 0, reps = 8, weight = 40.0)
            workoutSessionRepository.finishSession(sessionId)
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
                    Workout(id = workoutId, name = "Treino Resumo", description = ""),
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
                startDestination = "workout_session_summary/$sessionId",
            ) {
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
    fun summary_showsWorkoutNameDurationExercisesSetsAndVolume() {
        setScreen()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Resumo do treino")
        composeTestRule.onNodeWithText("Treino Resumo").assertIsDisplayed()
        composeTestRule.onNodeWithTag("summary_duration").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exercícios: 0/2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Séries: 1/3").assertIsDisplayed()
        composeTestRule.onNodeWithTag("summary_volume").assertIsDisplayed()
        composeTestRule.onNodeWithText("Supino Resumo").assertIsDisplayed()
        composeTestRule.onNodeWithText("8 × 40 kg").assertIsDisplayed()
        composeTestRule.onNodeWithText("Crucifixo Resumo").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Concluir série").assertDoesNotExist()
        composeTestRule.onNodeWithText("Descanso").assertDoesNotExist()
        composeTestRule.onNodeWithText("Finalizar").assertDoesNotExist()
    }

    @Test
    fun summary_showsUtcDateAndTimesFromPersistedSession() {
        val persisted = runBlocking { workoutSessionRepository.getSession(sessionId) }!!
        setScreen()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Treino Resumo")
        composeTestRule.onNodeWithText(formatHistoryDate(persisted.startedAtMillis)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Início: ${formatHistoryTime(persisted.startedAtMillis)}")
            .assertIsDisplayed()
        val endedAt = persisted.endedAtMillis
        if (endedAt != null) {
            composeTestRule.onNodeWithText("Término: ${formatHistoryTime(endedAt)}").assertIsDisplayed()
            composeTestRule.onNodeWithTag("summary_duration").assertIsDisplayed()
        }
        composeTestRule.onNodeWithTag("summary_volume").assertIsDisplayed()
    }

    @Test
    fun summary_missingSession_showsNotFound() {
        sessionId = 999_999_999L
        setScreen()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Sessão não encontrada")
        composeTestRule.onNodeWithTag("summary_not_found").assertIsDisplayed()
        composeTestRule.onNodeWithTag("session_summary").assertDoesNotExist()
    }

    @Test
    fun summary_survivesDeletedWorkoutTemplate() {
        runBlocking {
            workoutRepository.delete(
                Workout(id = workoutId, name = "Treino Resumo", description = ""),
            )
            workoutId = -1L
        }
        setScreen()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Treino Resumo")
        composeTestRule.onNodeWithText("Supino Resumo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Séries: 1/3").assertIsDisplayed()
        composeTestRule.onNodeWithTag("summary_volume").assertIsDisplayed()
    }

    @Test
    fun summary_firstSession_showsBestMarksWithoutEvolution() {
        setScreen()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Supino Resumo")
        composeTestRule.onNodeWithTag("progress_best_Supino Resumo").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun summary_secondSession_showsEvolutionAgainstPreviousBest() {
        val uniqueName = "Supino Evolucao ${System.nanoTime()}"
        var localWorkoutId = -1L
        var localExerciseId = -1L
        val secondId = runBlocking {
            localWorkoutId = workoutRepository.save(
                Workout(name = "Treino Evolucao ${System.nanoTime()}", description = ""),
            )
            localExerciseId = exerciseRepository.save(
                Exercise(name = uniqueName, muscleGroup = "Peitoral", equipmentType = "Barra"),
            )
            workoutRepository.addExercise(
                WorkoutExercise(
                    workoutId = localWorkoutId,
                    exerciseId = localExerciseId,
                    position = 0,
                    sets = 1,
                    minRepetitions = 8,
                    maxRepetitions = 12,
                    weight = 80.0,
                    restSeconds = 0,
                ),
            )
            val firstId = (workoutSessionRepository.startSession(localWorkoutId) as StartSessionResult.Created).sessionId
            val firstExercises = workoutSessionRepository.observeSessionExercises(firstId).first()
            workoutSessionRepository.completeSet(firstExercises[0].id, setIndex = 0, reps = 8, weight = 80.0)
            workoutSessionRepository.finishSession(firstId)
            val id = (workoutSessionRepository.startSession(localWorkoutId) as StartSessionResult.Created).sessionId
            val secondExercises = workoutSessionRepository.observeSessionExercises(id).first()
            workoutSessionRepository.completeSet(secondExercises[0].id, setIndex = 0, reps = 8, weight = 82.5)
            workoutSessionRepository.finishSession(id)
            id
        }
        sessionId = secondId
        setScreen()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed(uniqueName)
        composeTestRule.onNodeWithTag("progress_best_$uniqueName").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Carga 82,5 kg · Reps 8 · Volume 660 kg").assertIsDisplayed()
        composeTestRule.onNodeWithTag("progress_evolution_$uniqueName").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Carga +2,5 kg · Reps 0 · Volume +20 kg").assertIsDisplayed()

        runBlocking {
            if (localWorkoutId != -1L) {
                workoutRepository.delete(Workout(id = localWorkoutId, name = "", description = ""))
            }
            if (localExerciseId != -1L) {
                exerciseRepository.delete(
                    Exercise(id = localExerciseId, name = "", muscleGroup = "", equipmentType = ""),
                )
            }
        }
    }
}

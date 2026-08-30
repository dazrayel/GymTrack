package com.gymtrack.workouts.summary

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.TestActivity
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.repository.ExerciseRepository
import com.gymtrack.domain.repository.WorkoutRepository
import com.gymtrack.domain.repository.WorkoutSessionRepository
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
            sessionId = workoutSessionRepository.startSession(workoutId)
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
}

package com.gymtrack.exercises

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.MainActivity
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.StartSessionResult
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.repository.ExerciseRepository
import com.gymtrack.domain.repository.WorkoutRepository
import com.gymtrack.domain.repository.WorkoutSessionRepository
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

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExercisesStatsNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var exerciseRepository: ExerciseRepository
    @Inject lateinit var workoutSessionRepository: WorkoutSessionRepository

    private var workoutId: Long = -1L
    private val exerciseIds = mutableListOf<Long>()
    private var sessionId: Long = -1L
    private val workoutName = "Treino Exercises Stats ${System.nanoTime()}"

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        runBlocking {
            workoutSessionRepository.observeInProgress().first()?.let { active ->
                workoutSessionRepository.finishSession(active.id)
            }
            if (workoutId != -1L) {
                workoutRepository.delete(Workout(id = workoutId, name = workoutName, description = ""))
            }
            exerciseIds.forEach { id ->
                exerciseRepository.delete(
                    Exercise(id = id, name = "", muscleGroup = "", equipmentType = ""),
                )
            }
        }
    }

    private fun seedCatalogOnly(exerciseName: String) {
        runBlocking {
            exerciseIds += exerciseRepository.save(
                Exercise(name = exerciseName, muscleGroup = "Peitoral", equipmentType = "Barra"),
            )
        }
    }

    private fun seedCompletedSession(exerciseName: String) {
        runBlocking {
            workoutId = workoutRepository.save(
                Workout(name = workoutName, description = "7.3"),
            )
            val exerciseId = exerciseRepository.save(
                Exercise(name = exerciseName, muscleGroup = "Peitoral", equipmentType = "Barra"),
            )
            exerciseIds += exerciseId
            workoutRepository.addExercise(
                WorkoutExercise(
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                    position = 0,
                    sets = 1,
                    minRepetitions = 8,
                    maxRepetitions = 12,
                    weight = 40.0,
                    restSeconds = 0,
                ),
            )
            sessionId = (workoutSessionRepository.startSession(workoutId) as StartSessionResult.Created).sessionId
            val exercises = workoutSessionRepository.observeSessionExercises(sessionId).first()
            workoutSessionRepository.completeSet(exercises[0].id, setIndex = 0, reps = 8, weight = 40.0)
            workoutSessionRepository.finishSession(sessionId)
        }
    }

    private fun waitUntilTextIsDisplayed(text: String) {
        composeTestRule.waitUntil(8_000) {
            val nodes = composeTestRule.onAllNodesWithText(text, useUnmergedTree = true)
            nodes.fetchSemanticsNodes().isNotEmpty() && nodes[0].isDisplayed()
        }
        composeTestRule.onAllNodesWithText(text, useUnmergedTree = true)[0].assertIsDisplayed()
    }

    private fun openExercisesAndViewPerformance(exerciseName: String) {
        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("home_exercises").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("home_exercises").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("exercise_search").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("exercise_search").performTextInput(exerciseName)
        composeTestRule.waitForIdle()

        val actionTag = "exercise_view_performance_$exerciseName"
        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag(actionTag).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag(actionTag).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun viewPerformance_withoutHistory_opensEmptyStats() {
        val withHistory = "Agachamento ${System.nanoTime()}"
        val exerciseName = "Supino reto ${System.nanoTime()}"
        seedCompletedSession(withHistory)
        seedCatalogOnly(exerciseName)
        openExercisesAndViewPerformance(exerciseName)

        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("exercise_stats_empty").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("exercise_stats_empty").assertIsDisplayed()
        waitUntilTextIsDisplayed(exerciseName)
        waitUntilTextIsDisplayed("Sem histórico para este exercício")
        composeTestRule.onNodeWithTag("exercise_stats_records").assertDoesNotExist()
        composeTestRule.onNodeWithTag("exercise_stats_history").assertDoesNotExist()
    }

    @Test
    fun viewPerformance_withSpaceInName_opensStats() {
        val exerciseName = "Supino inclinado ${System.nanoTime()}"
        seedCompletedSession(exerciseName)
        openExercisesAndViewPerformance(exerciseName)

        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("exercise_stats").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("exercise_stats").assertIsDisplayed()
        waitUntilTextIsDisplayed(exerciseName)
        composeTestRule.onNodeWithTag("exercise_stats_records").assertIsDisplayed()
        composeTestRule.onNodeWithTag("exercise_stats_history").assertIsDisplayed()
        composeTestRule.onNodeWithTag("exercise_stats_point_$sessionId").assertIsDisplayed()
    }

    @Test
    fun viewPerformance_withAccentInName_opensStats() {
        val exerciseName = "Elevação lateral ${System.nanoTime()}"
        seedCompletedSession(exerciseName)
        openExercisesAndViewPerformance(exerciseName)

        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("exercise_stats").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("exercise_stats").assertIsDisplayed()
        waitUntilTextIsDisplayed(exerciseName)
        composeTestRule.onNodeWithTag("exercise_stats_record_$exerciseName").assertIsDisplayed()
    }
}

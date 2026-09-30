package com.gymtrack.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.MainActivity
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.StartSessionResult
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutBlock
import com.gymtrack.domain.model.WorkoutBlockType
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
class ExerciseStatsNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var exerciseRepository: ExerciseRepository
    @Inject lateinit var workoutSessionRepository: WorkoutSessionRepository

    private var workoutId: Long = -1L
    private var exerciseId: Long = -1L
    private var sessionId: Long = -1L
    private val workoutName = "Treino Stats Nav ${System.nanoTime()}"

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
            if (exerciseId != -1L) {
                exerciseRepository.delete(
                    Exercise(id = exerciseId, name = "", muscleGroup = "", equipmentType = ""),
                )
            }
        }
    }

    private fun seedCompletedSession(exerciseName: String) {
        runBlocking {
            workoutId = workoutRepository.save(
                Workout(name = workoutName, description = "7.2"),
            )
            exerciseId = exerciseRepository.save(
                Exercise(name = exerciseName, muscleGroup = "Peitoral", equipmentType = "Barra"),
            )
            workoutRepository.addBlock(
                WorkoutBlock(workoutId = workoutId, position = 0, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 60),
                listOf(WorkoutExercise(blockId = 0, exerciseId = exerciseId, positionInBlock = 0, minRepetitions = 8, maxRepetitions = 12, weight = 40.0, notes = "")),
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

    private fun openRecord(exerciseName: String) {
        val tag = "home_record_$exerciseName"
        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("home_records").performScrollTo()
        composeTestRule.onNodeWithTag(tag).performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun recordClick_opensExerciseStats_withSpaceInName() {
        val exerciseName = "Supino Inclinado ${System.nanoTime()}"
        seedCompletedSession(exerciseName)
        openRecord(exerciseName)

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
    fun recordClick_opensExerciseStats_withAccentInName() {
        val exerciseName = "Elevação frontal ${System.nanoTime()}"
        seedCompletedSession(exerciseName)
        openRecord(exerciseName)

        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("exercise_stats").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("exercise_stats").assertIsDisplayed()
        waitUntilTextIsDisplayed(exerciseName)
        composeTestRule.onNodeWithTag("exercise_stats_record_$exerciseName").assertIsDisplayed()
    }
}

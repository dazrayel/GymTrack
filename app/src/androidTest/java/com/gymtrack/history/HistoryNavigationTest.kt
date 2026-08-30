package com.gymtrack.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.MainActivity
import com.gymtrack.domain.model.Exercise
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HistoryNavigationTest {

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
    private val workoutName = "Treino Histórico Nav ${System.nanoTime()}"

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            workoutId = workoutRepository.save(
                Workout(name = workoutName, description = "5.3"),
            )
            exerciseId = exerciseRepository.save(
                Exercise(name = "Supino Histórico Nav", muscleGroup = "Peitoral", equipmentType = "Barra"),
            )
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
                workoutRepository.delete(Workout(id = workoutId, name = workoutName, description = ""))
            }
            if (exerciseId != -1L) {
                exerciseRepository.delete(
                    Exercise(id = exerciseId, name = "", muscleGroup = "", equipmentType = ""),
                )
            }
        }
    }

    private fun waitUntilTextIsDisplayed(text: String) {
        composeTestRule.waitUntil(5_000) {
            val nodes = composeTestRule.onAllNodesWithText(text, useUnmergedTree = true)
            nodes.fetchSemanticsNodes().isNotEmpty() && nodes[0].isDisplayed()
        }
        composeTestRule.onAllNodesWithText(text, useUnmergedTree = true)[0].assertIsDisplayed()
    }

    @Test
    fun historyItem_click_opensSummaryWithPersistedSessionId() {
        val persisted = runBlocking { workoutSessionRepository.getSession(sessionId) }!!
        assertEquals(sessionId, persisted.id)
        assertEquals(workoutName, persisted.workoutName)

        composeTestRule.onNodeWithText("Histórico").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithTag("history_item_$sessionId")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag("history_item_$sessionId").performClick()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Resumo do treino")
        composeTestRule.onNodeWithTag("summary_workout_name").assertIsDisplayed()
        waitUntilTextIsDisplayed(workoutName)
        composeTestRule.onNodeWithTag("session_summary").assertIsDisplayed()
        composeTestRule.onNodeWithText("Supino Histórico Nav").assertIsDisplayed()
        composeTestRule.onNodeWithText("8 × 40 kg").assertIsDisplayed()

        val opened = runBlocking { workoutSessionRepository.getSession(sessionId) }!!
        assertEquals(persisted.id, opened.id)
        assertEquals(persisted.workoutName, opened.workoutName)
        assertEquals(persisted.startedAtMillis, opened.startedAtMillis)
    }

    @Test
    fun historyDetail_back_returnsToHistory() {
        composeTestRule.onNodeWithText("Histórico").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithTag("history_item_$sessionId")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag("history_item_$sessionId").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Resumo do treino")

        composeTestRule.onNodeWithContentDescription("Voltar").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithTag("history_item_$sessionId")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag("history_item_$sessionId").assertIsDisplayed()
        composeTestRule.onNodeWithTag("session_summary").assertDoesNotExist()
    }
}

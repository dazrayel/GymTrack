package com.gymtrack.home

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.MainActivity
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.StartSessionResult
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutBlock
import com.gymtrack.domain.model.WorkoutBlockType
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.model.WorkoutSessionStatus
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
class HomeNavigationTest {

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
    private val workoutName = "Treino Home Nav ${System.nanoTime()}"

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            workoutId = workoutRepository.save(
                Workout(name = workoutName, description = "6.1"),
            )
            exerciseId = exerciseRepository.save(
                Exercise(name = "Supino Home Nav", muscleGroup = "Peitoral", equipmentType = "Barra"),
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
    fun recentWorkout_click_opensSummaryWithPersistedSessionId() {
        val persisted = runBlocking { workoutSessionRepository.getSession(sessionId) }!!
        assertEquals(sessionId, persisted.id)
        assertEquals(workoutName, persisted.workoutName)

        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("home_recent_$sessionId")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag("home_recent_$sessionId").performClick()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Resumo do treino")
        composeTestRule.onNodeWithTag("session_summary").assertIsDisplayed()
        waitUntilTextIsDisplayed(workoutName)
        composeTestRule.onNodeWithText("Supino Home Nav").assertIsDisplayed()
        composeTestRule.onNodeWithText("8 × 40 kg").assertIsDisplayed()

        val opened = runBlocking { workoutSessionRepository.getSession(sessionId) }!!
        assertEquals(persisted.id, opened.id)
        assertEquals(persisted.startedAtMillis, opened.startedAtMillis)
    }

    @Test
    fun inProgressCta_opensExistingExecutionWithoutCreatingSession() {
        val activeId = runBlocking {
            (workoutSessionRepository.startSession(workoutId) as StartSessionResult.Created).sessionId
        }

        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("home_in_progress").fetchSemanticsNodes().isNotEmpty()
        }
        waitUntilTextIsDisplayed(workoutName)
        composeTestRule.onNodeWithTag("home_continue_in_progress").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("session_exercises_button")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag("session_exercises_button").assertIsDisplayed()
        val active = runBlocking { workoutSessionRepository.observeInProgress().first() }
        assertEquals(activeId, active!!.id)
        assertEquals(WorkoutSessionStatus.IN_PROGRESS, active.status)
        assertEquals(workoutName, active.workoutName)
        val session = runBlocking { workoutSessionRepository.getSession(activeId) }!!
        assertEquals(WorkoutSessionStatus.IN_PROGRESS, session.status)
    }

    @Test
    fun nextWorkoutCard_opensWorkoutDetail() {
        composeTestRule.onNode(hasText("Início") and hasClickAction()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("next_workout_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("next_workout_title", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("next_workout_card").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithText("Iniciar treino").fetchSemanticsNodes().isNotEmpty()
        }
        waitUntilTextIsDisplayed(workoutName)
        composeTestRule.onNodeWithText("Iniciar treino").assertIsDisplayed()
    }

    @Test
    fun homeTab_fromExecution_returnsHomeWithoutStackingExecution() {
        composeTestRule.onNode(hasText("Treinos") and hasClickAction()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Meus treinos").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(workoutName).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Iniciar treino").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("session_exercises_button")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag("session_exercises_button").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Voltar").assertIsDisplayed()

        val started = runBlocking { workoutSessionRepository.observeInProgress().first() }!!
        assertEquals(WorkoutSessionStatus.IN_PROGRESS, started.status)
        assertEquals(workoutName, started.workoutName)

        composeTestRule.onNode(hasText("Início") and hasClickAction()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("home_in_progress").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("home_in_progress").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_continue_in_progress").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Voltar").assertDoesNotExist()

        val onHome = runBlocking { workoutSessionRepository.observeInProgress().first() }!!
        assertEquals(started.id, onHome.id)
        assertEquals(WorkoutSessionStatus.IN_PROGRESS, onHome.status)

        composeTestRule.onNodeWithTag("home_continue_in_progress").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("session_exercises_button")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithTag("session_exercises_button").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Voltar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_continue_in_progress").assertDoesNotExist()

        val reopened = runBlocking { workoutSessionRepository.observeInProgress().first() }!!
        assertEquals(started.id, reopened.id)
        assertEquals(WorkoutSessionStatus.IN_PROGRESS, reopened.status)

        composeTestRule.onNodeWithContentDescription("Voltar").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("home_in_progress").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("home_in_progress").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_continue_in_progress").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Voltar").assertDoesNotExist()
    }

    @Test
    fun homeTab_fromExecution_restoresTypedDraftViaContinueCta() {
        composeTestRule.onNode(hasText("Treinos") and hasClickAction()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Meus treinos").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(workoutName).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Iniciar treino").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("reps_field").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("reps_field").performTextReplacement("12")
        composeTestRule.onNodeWithTag("weight_field").performTextReplacement("77")
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasText("Início") and hasClickAction()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("home_continue_in_progress").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("home_continue_in_progress").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(8_000) {
            composeTestRule.onAllNodesWithTag("reps_field").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("reps_field").assert(hasText("12"))
        composeTestRule.onNodeWithTag("weight_field").assert(hasText("77"))
        val active = runBlocking { workoutSessionRepository.observeInProgress().first() }!!
        assertEquals(workoutName, active.workoutName)
        assertEquals(WorkoutSessionStatus.IN_PROGRESS, active.status)
    }
}

package com.gymtrack

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Instrumented E2E navigation tests for the Workouts → WorkoutDetail flow.
 *
 * Uses the full [MainActivity] + [GymTrackNavGraph] to validate that
 * [WorkoutDetailScreen] is correctly wired and receives the right workoutId.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WorkoutDetailNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var exerciseRepository: ExerciseRepository
    @Inject lateinit var workoutSessionRepository: WorkoutSessionRepository

    private var workoutId: Long = -1L
    private var extraWorkoutId: Long = -1L
    private var exerciseId: Long = -1L
    private val workoutName = "Treino Nav Detail"

    @Before
    fun setUp() {
        hiltRule.inject()
        runBlocking {
            workoutId = workoutRepository.save(
                Workout(name = workoutName, description = "Teste de navegação"),
            )
            exerciseId = exerciseRepository.save(
                Exercise(name = "Supino Nav", muscleGroup = "Peito", equipmentType = "Barra"),
            )
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
            if (extraWorkoutId != -1L) {
                workoutRepository.delete(Workout(id = extraWorkoutId, name = "", description = ""))
            }
            if (exerciseId != -1L) {
                exerciseRepository.delete(
                    Exercise(id = exerciseId, name = "", muscleGroup = "", equipmentType = ""),
                )
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Teste 1 — Workouts → WorkoutDetail
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Validates that tapping a workout card navigates to WorkoutDetailScreen and
     * that the correct workout is displayed (confirming workoutId was transmitted).
     */
    @Test
    fun navigation_workoutsToDetail_detailScreenIsDisplayed() {
        // Navigate to Workouts via Bottom Nav
        composeTestRule.onNodeWithText("Treinos").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Meus treinos").assertIsDisplayed()

        // Tap the workout card
        composeTestRule.onNodeWithText(workoutName).performClick()
        composeTestRule.waitForIdle()

        // WorkoutDetailScreen shows the correct workout name in the TopAppBar
        composeTestRule.onNodeWithText(workoutName).assertIsDisplayed()

        // Back button is present (unique to detail screen, not a top-level destination)
        composeTestRule.onNodeWithContentDescription("Voltar").assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Teste 2 — WorkoutDetail → Workouts (back navigation)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun navigation_detailToWorkouts_workoutsScreenIsRestored() {
        composeTestRule.onNodeWithText("Treinos").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(workoutName).performClick()
        composeTestRule.waitForIdle()

        // We are in WorkoutDetailScreen — press back
        composeTestRule.onNodeWithContentDescription("Voltar").performClick()
        composeTestRule.waitForIdle()

        // WorkoutsScreen must be visible again
        composeTestRule.onNodeWithText("Meus treinos").assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Teste 3 — Back stack: Workouts → Detail → Back, sem duplicação
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun navigation_workoutsDetailBack_noBackStackDuplication() {
        composeTestRule.onNodeWithText("Treinos").performClick()
        composeTestRule.waitForIdle()

        // Navigate to detail
        composeTestRule.onNodeWithText(workoutName).performClick()
        composeTestRule.waitForIdle()

        // A single back press must return to WorkoutsScreen directly
        composeTestRule.onNodeWithContentDescription("Voltar").performClick()
        composeTestRule.waitForIdle()

        // WorkoutsScreen is visible — no intermediate detail screen
        composeTestRule.onNodeWithText("Meus treinos").assertIsDisplayed()

        // The detail screen is gone — back button is no longer visible
        composeTestRule.onNodeWithContentDescription("Voltar").assertDoesNotExist()
    }

    @Test
    fun navigation_detailStartWorkout_opensExecutionWithPersistedSessionId() {
        composeTestRule.onNodeWithText("Treinos").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(workoutName).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Iniciar treino").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(5_000) {
            runBlocking { workoutSessionRepository.observeInProgress().first() } != null
        }
        val session = runBlocking { workoutSessionRepository.observeInProgress().first() }
        assertNotNull(session)
        // TopAppBar can expose the same title twice in the merged semantics tree.
        composeTestRule.onAllNodesWithText("Treino em andamento").onFirst().assertIsDisplayed()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText(workoutName).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(workoutName).assertIsDisplayed()
        assertEquals(workoutId, session!!.workoutId)
        assertEquals(workoutName, session.workoutName)
        assertTrue(session.id > 0L)
    }

    @Test
    fun navigation_otherInProgress_continueOpensExistingSession() {
        val activeName = "Treino Nav Ativo"
        val sessionA = runBlocking {
            extraWorkoutId = workoutRepository.save(
                Workout(name = activeName, description = ""),
            )
            workoutRepository.addExercise(
                WorkoutExercise(
                    workoutId = extraWorkoutId,
                    exerciseId = exerciseId,
                    position = 0,
                    sets = 1,
                    minRepetitions = 8,
                    maxRepetitions = 12,
                    weight = 10.0,
                    restSeconds = 0,
                ),
            )
            (workoutSessionRepository.startSession(extraWorkoutId) as StartSessionResult.Created).sessionId
        }

        composeTestRule.onAllNodesWithText("Treinos").onFirst().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(workoutName).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Iniciar treino").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Já existe um treino em andamento").assertIsDisplayed()
        composeTestRule.onNodeWithText("Você já está realizando o treino $activeName.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Continuar treino em andamento").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Treino em andamento").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithText("Treino em andamento").onFirst().assertIsDisplayed()
        composeTestRule.onNodeWithText(activeName).assertIsDisplayed()
        val session = runBlocking { workoutSessionRepository.observeInProgress().first() }
        assertEquals(sessionA, session!!.id)
        assertEquals(extraWorkoutId, session.workoutId)
    }
}

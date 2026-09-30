package com.gymtrack.history

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.TestActivity
import com.gymtrack.domain.model.Exercise
import com.gymtrack.domain.model.StartSessionResult
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutBlock
import com.gymtrack.domain.model.WorkoutBlockType
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.model.formatVolumeKg
import com.gymtrack.domain.repository.ExerciseRepository
import com.gymtrack.domain.repository.WorkoutRepository
import com.gymtrack.domain.repository.WorkoutSessionRepository
import com.gymtrack.domain.time.formatElapsedMillis
import com.gymtrack.domain.time.formatHistoryDate
import com.gymtrack.presentation.history.HistoryScreen
import com.gymtrack.presentation.history.HistoryUiState
import com.gymtrack.presentation.theme.GymTrackTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HistoryScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var exerciseRepository: ExerciseRepository
    @Inject lateinit var workoutSessionRepository: WorkoutSessionRepository

    private var workoutId: Long = -1L
    private var exerciseId: Long = -1L
    private var sessionId: Long = -1L

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
                workoutRepository.delete(
                    Workout(id = workoutId, name = "Treino Histórico 5.2", description = ""),
                )
            }
            if (exerciseId != -1L) {
                exerciseRepository.delete(
                    Exercise(id = exerciseId, name = "", muscleGroup = "", equipmentType = ""),
                )
            }
        }
    }

    private val sampleItem = WorkoutHistoryItem(
        sessionId = 42L,
        workoutName = "Treino A",
        startedAtMillis = Instant.parse("2026-08-28T15:00:00Z").toEpochMilli(),
        endedAtMillis = Instant.parse("2026-08-28T15:42:00Z").toEpochMilli(),
        durationMillis = 2_520_000L,
        volume = 4_320.0,
        exerciseCount = 3,
        completedSetCount = 12,
        plannedSetCount = 12,
    )

    private fun waitUntilTextIsDisplayed(text: String, substring: Boolean = false) {
        composeTestRule.waitUntil(5_000) {
            val nodes = composeTestRule.onAllNodesWithText(
                text,
                substring = substring,
                useUnmergedTree = true,
            )
            val semantics = nodes.fetchSemanticsNodes()
            semantics.isNotEmpty() && nodes[0].isDisplayed()
        }
        composeTestRule.onAllNodesWithText(
            text,
            substring = substring,
            useUnmergedTree = true,
        )[0].assertIsDisplayed()
    }

    private fun setContent(uiState: HistoryUiState, onSessionClick: (Long) -> Unit = {}) {
        composeTestRule.setContent {
            GymTrackTheme {
                HistoryScreen(
                    uiState = uiState,
                    onSessionClick = onSessionClick,
                    onErrorShown = {},
                    contentPadding = PaddingValues(),
                )
            }
        }
    }

    @Test
    fun loadingState_showsProgressIndicator() {
        setContent(HistoryUiState(isLoading = true))
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithTag("history_loading").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("history_loading").assertIsDisplayed()
    }

    @Test
    fun emptyState_isDisplayed_whenNoSessions() {
        setContent(HistoryUiState(isLoading = false, items = emptyList()))
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Nenhum treino realizado")
        waitUntilTextIsDisplayed("Os treinos concluídos aparecerão aqui.")
        composeTestRule.onNodeWithTag("history_empty").assertIsDisplayed()
    }

    @Test
    fun list_showsNameDateDurationVolumeAndCounts() {
        setContent(HistoryUiState(isLoading = false, items = listOf(sampleItem)))
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Treino A")
        waitUntilTextIsDisplayed("28/08/2026")
        waitUntilTextIsDisplayed("42:00", substring = true)
        waitUntilTextIsDisplayed("3 exercícios", substring = true)
        waitUntilTextIsDisplayed("12 séries", substring = true)
        waitUntilTextIsDisplayed("Volume: ${formatVolumeKg(4_320.0)}")
        composeTestRule.onNodeWithTag("history_list").assertIsDisplayed()
    }

    @Test
    fun clickingItem_exposesSessionId() {
        var clickedId: Long? = null
        setContent(
            HistoryUiState(isLoading = false, items = listOf(sampleItem)),
            onSessionClick = { clickedId = it },
        )
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Treino A")
        composeTestRule.onNodeWithTag("history_item_42").performClick()
        composeTestRule.waitForIdle()

        assertEquals(42L, clickedId)
    }

    @Test
    fun list_showsDeleteAction() {
        setContent(HistoryUiState(isLoading = false, items = listOf(sampleItem)))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("history_delete_42").assertIsDisplayed()
    }

    @Test
    fun deleteDialog_cancelKeepsItem_confirmRemovesSession() {
        val item = seedCompletedSession()
        composeTestRule.setContent {
            GymTrackTheme {
                HistoryScreen(contentPadding = PaddingValues())
            }
        }
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed(item.workoutName)
        composeTestRule.onNodeWithTag("history_delete_${item.sessionId}").performClick()
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Excluir treino?")
        waitUntilTextIsDisplayed("Esta sessão será removida do histórico. Essa ação não pode ser desfeita.")
        composeTestRule.onNodeWithTag("cancel_delete_history_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("history_item_${item.sessionId}").assertIsDisplayed()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Excluir treino?").fetchSemanticsNodes().isEmpty()
        }

        composeTestRule.onNodeWithTag("history_delete_${item.sessionId}").performClick()
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Excluir treino?")
        composeTestRule.onNodeWithTag("confirm_delete_history_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithTag("history_item_${item.sessionId}")
                .fetchSemanticsNodes()
                .isEmpty()
        }
        assertTrue(
            composeTestRule.onAllNodesWithTag("history_item_${item.sessionId}")
                .fetchSemanticsNodes()
                .isEmpty(),
        )
    }

    @Test
    fun errorState_showsSnackbarMessage() {
        setContent(
            HistoryUiState(
                isLoading = false,
                items = emptyList(),
                error = "Falha ao carregar histórico",
            ),
        )
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Falha ao carregar histórico")
    }

    @Test
    fun hiltScreen_showsCompletedSessionFromRepository() {
        val item = seedCompletedSession()
        composeTestRule.setContent {
            GymTrackTheme {
                HistoryScreen(contentPadding = PaddingValues())
            }
        }
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed(item.workoutName)
        waitUntilTextIsDisplayed(formatHistoryDate(item.endedAtMillis ?: item.startedAtMillis))
        waitUntilTextIsDisplayed(formatElapsedMillis(item.durationMillis), substring = true)
        waitUntilTextIsDisplayed("${item.exerciseCount} exercícios", substring = true)
        waitUntilTextIsDisplayed("${item.completedSetCount} séries", substring = true)
        waitUntilTextIsDisplayed("Volume: ${formatVolumeKg(item.volume)}")
    }

    @Test
    fun hiltScreen_click_returnsPersistedSessionId() {
        val item = seedCompletedSession()
        var clickedId: Long? = null
        composeTestRule.setContent {
            GymTrackTheme {
                HistoryScreen(
                    contentPadding = PaddingValues(),
                    onSessionClick = { clickedId = it },
                )
            }
        }
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed(item.workoutName)
        composeTestRule.onNodeWithTag("history_item_${item.sessionId}").performClick()
        composeTestRule.waitForIdle()

        assertEquals(item.sessionId, clickedId)
    }

    @Test
    fun hiltScreen_doesNotShowInProgressSession() {
        val inProgressName = "Treino Histórico Andamento ${System.nanoTime()}"
        runBlocking {
            workoutId = workoutRepository.save(
                Workout(name = inProgressName, description = ""),
            )
            exerciseId = exerciseRepository.save(
                Exercise(name = "Supino Histórico ${System.nanoTime()}", muscleGroup = "Peitoral", equipmentType = "Barra"),
            )
            workoutRepository.addBlock(
                WorkoutBlock(workoutId = workoutId, position = 0, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 60),
                listOf(WorkoutExercise(blockId = 0, exerciseId = exerciseId, positionInBlock = 0, minRepetitions = 8, maxRepetitions = 12, weight = 40.0, notes = "")),
            )
            sessionId = (workoutSessionRepository.startSession(workoutId) as StartSessionResult.Created).sessionId
        }

        composeTestRule.setContent {
            GymTrackTheme {
                HistoryScreen(contentPadding = PaddingValues())
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithTag("history_empty").fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithTag("history_list").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            composeTestRule.onAllNodesWithText(
                inProgressName,
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isEmpty(),
        )
    }

    private fun seedCompletedSession(): WorkoutHistoryItem = runBlocking {
        val workoutName = "Treino Histórico ${System.nanoTime()}"
        workoutId = workoutRepository.save(
            Workout(name = workoutName, description = "Lista"),
        )
        exerciseId = exerciseRepository.save(
            Exercise(name = "Supino Histórico ${System.nanoTime()}", muscleGroup = "Peitoral", equipmentType = "Barra"),
        )
        workoutRepository.addBlock(
                WorkoutBlock(workoutId = workoutId, position = 0, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 60),
                listOf(WorkoutExercise(blockId = 0, exerciseId = exerciseId, positionInBlock = 0, minRepetitions = 8, maxRepetitions = 12, weight = 40.0, notes = "")),
            )
        sessionId = (workoutSessionRepository.startSession(workoutId) as StartSessionResult.Created).sessionId
        val exercises = workoutSessionRepository.observeSessionExercises(sessionId).first()
        workoutSessionRepository.completeSet(exercises[0].id, setIndex = 0, reps = 8, weight = 40.0)
        workoutSessionRepository.finishSession(sessionId)
        workoutSessionRepository.observeCompletedSessions().first()
            .first { it.sessionId == sessionId }
    }
}

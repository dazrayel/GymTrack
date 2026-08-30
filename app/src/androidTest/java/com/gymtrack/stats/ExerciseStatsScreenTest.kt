package com.gymtrack.stats

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.TestActivity
import com.gymtrack.domain.model.ExercisePerformancePoint
import com.gymtrack.domain.model.ExercisePersonalRecords
import com.gymtrack.domain.model.formatVolumeKg
import com.gymtrack.domain.time.formatLocalDate
import com.gymtrack.domain.time.formatLocalTime
import com.gymtrack.presentation.stats.ExerciseStatsScreen
import com.gymtrack.presentation.stats.ExerciseStatsUiState
import com.gymtrack.presentation.theme.GymTrackTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExerciseStatsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private val zone: ZoneId = ZoneId.systemDefault()
    private val occurredAt = 1_756_543_200_000L

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    private fun waitUntilTextIsDisplayed(text: String, substring: Boolean = false) {
        composeTestRule.waitUntil(5_000) {
            val nodes = composeTestRule.onAllNodesWithText(
                text,
                substring = substring,
                useUnmergedTree = true,
            )
            nodes.fetchSemanticsNodes().isNotEmpty() && nodes[0].isDisplayed()
        }
        composeTestRule.onAllNodesWithText(
            text,
            substring = substring,
            useUnmergedTree = true,
        )[0].assertIsDisplayed()
    }

    private fun setContent(uiState: ExerciseStatsUiState) {
        composeTestRule.setContent {
            GymTrackTheme {
                ExerciseStatsScreen(
                    uiState = uiState,
                    onNavigateBack = {},
                    onErrorShown = {},
                    contentPadding = PaddingValues(),
                )
            }
        }
    }

    @Test
    fun loadingState_showsProgressIndicator() {
        setContent(ExerciseStatsUiState(exerciseName = "Supino", isLoading = true))
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithTag("exercise_stats_loading").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("exercise_stats_loading").assertIsDisplayed()
    }

    @Test
    fun emptyState_showsMessageWithoutRecords() {
        setContent(
            ExerciseStatsUiState(
                exerciseName = "Supino",
                isLoading = false,
                performanceHistory = emptyList(),
                personalRecord = null,
            ),
        )
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Sem histórico para este exercício")
        composeTestRule.onNodeWithTag("exercise_stats_empty").assertIsDisplayed()
        composeTestRule.onNodeWithTag("exercise_stats_records").assertDoesNotExist()
        composeTestRule.onNodeWithTag("exercise_stats_history").assertDoesNotExist()
    }

    @Test
    fun content_showsRecordsHistoryVolumeAndLocalDate() {
        val point = ExercisePerformancePoint(
            sessionId = 42L,
            exerciseName = "Supino",
            occurredAtMillis = occurredAt,
            bestWeight = 80.0,
            bestReps = 10,
            volume = 800.0,
        )
        setContent(
            ExerciseStatsUiState(
                exerciseName = "Supino",
                isLoading = false,
                performanceHistory = listOf(point),
                personalRecord = record("Supino", weight = 80.0, reps = 10, volume = 800.0),
            ),
        )
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Supino")
        composeTestRule.onNodeWithTag("exercise_stats").assertIsDisplayed()
        composeTestRule.onNodeWithTag("exercise_stats_records").assertIsDisplayed()
        composeTestRule.onNodeWithTag("exercise_stats_record_Supino").assertIsDisplayed()
        composeTestRule.onNodeWithTag("exercise_stats_history").assertIsDisplayed()
        composeTestRule.onNodeWithTag("exercise_stats_point_42").assertIsDisplayed()
        waitUntilTextIsDisplayed("Carga: ${formatVolumeKg(80.0)}")
        waitUntilTextIsDisplayed("Reps: 10")
        waitUntilTextIsDisplayed("Volume: ${formatVolumeKg(800.0)}")
        waitUntilTextIsDisplayed(formatLocalDate(occurredAt, zone), substring = true)
        waitUntilTextIsDisplayed(formatLocalTime(occurredAt, zone), substring = true)
    }

    @Test
    fun content_showsMultipleHistoryPoints() {
        setContent(
            ExerciseStatsUiState(
                exerciseName = "Supino",
                isLoading = false,
                performanceHistory = listOf(
                    ExercisePerformancePoint(1L, "Supino", occurredAt, 70.0, 8, 560.0),
                    ExercisePerformancePoint(2L, "Supino", occurredAt + 86_400_000L, 80.0, 8, 640.0),
                ),
                personalRecord = record("Supino", weight = 80.0, reps = 8, volume = 640.0),
            ),
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("exercise_stats_point_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("exercise_stats_point_2").assertIsDisplayed()
    }

    @Test
    fun zeroWeight_isDisplayed() {
        setContent(
            ExerciseStatsUiState(
                exerciseName = "Abdominal",
                isLoading = false,
                performanceHistory = listOf(
                    ExercisePerformancePoint(1L, "Abdominal", occurredAt, 0.0, 20, 0.0),
                ),
                personalRecord = record("Abdominal", weight = 0.0, reps = 20, volume = 0.0),
            ),
        )
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Carga: ${formatVolumeKg(0.0)}")
        waitUntilTextIsDisplayed("Volume: ${formatVolumeKg(0.0)}")
        waitUntilTextIsDisplayed("Reps: 20")
    }

    private fun record(
        name: String,
        weight: Double,
        reps: Int,
        volume: Double,
    ) = ExercisePersonalRecords(
        exerciseName = name,
        bestWeight = weight,
        bestWeightReps = reps,
        bestWeightSessionId = 1L,
        bestReps = reps,
        bestRepsWeight = weight,
        bestRepsSessionId = 1L,
        bestVolume = volume,
        bestVolumeSessionId = 1L,
    )
}

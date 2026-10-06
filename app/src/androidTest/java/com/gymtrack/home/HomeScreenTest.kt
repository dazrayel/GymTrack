package com.gymtrack.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.TestActivity
import com.gymtrack.domain.model.HomeActivityDay
import com.gymtrack.domain.model.DashboardPeriod
import com.gymtrack.domain.model.ExercisePersonalRecords
import com.gymtrack.domain.model.PeriodDashboardStats
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutHistoryItem
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionStatus
import com.gymtrack.domain.model.formatVolumeKg
import com.gymtrack.domain.time.formatDashboardDuration
import com.gymtrack.domain.time.formatLocalDate
import com.gymtrack.domain.time.formatLocalTime
import com.gymtrack.presentation.home.HomeScreen
import com.gymtrack.presentation.home.HomeUiState
import com.gymtrack.presentation.theme.GymTrackTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.ZoneId

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private val zone: ZoneId = ZoneId.systemDefault()
    private val occurredAt = 1_756_543_200_000L
    private val sampleItem = WorkoutHistoryItem(
        sessionId = 42L,
        workoutName = "Peito e Tríceps",
        startedAtMillis = occurredAt,
        endedAtMillis = occurredAt,
        durationMillis = 48 * 60_000L,
        volume = 6_250.0,
        exerciseCount = 4,
        completedSetCount = 12,
        plannedSetCount = 12,
    )
    private val nextWorkout = Workout(id = 9L, name = "Push")
    private val activeSession = WorkoutSession(
        id = 77L,
        workoutId = 9L,
        workoutName = "Push Day",
        startedAtMillis = occurredAt,
        status = WorkoutSessionStatus.IN_PROGRESS,
    )

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

    private fun setContent(
        uiState: HomeUiState,
        onSessionClick: (Long) -> Unit = {},
        onNavigateToWorkouts: () -> Unit = {},
        onNavigateToWorkoutDetail: (Long) -> Unit = {},
        onNavigateToAchievements: () -> Unit = {},
        onPeriodSelected: (DashboardPeriod) -> Unit = {},
        onRecordClick: (String) -> Unit = {},
        onContinueInProgress: (Long) -> Unit = {},
    ) {
        composeTestRule.setContent {
            GymTrackTheme {
                HomeScreen(
                    uiState = uiState,
                    onNavigateToExercises = {},
                    onNavigateToWorkouts = onNavigateToWorkouts,
                    onNavigateToWorkoutDetail = onNavigateToWorkoutDetail,
                    onNavigateToAchievements = onNavigateToAchievements,
                    onSessionClick = onSessionClick,
                    onRecordClick = onRecordClick,
                    onContinueInProgress = onContinueInProgress,
                    onPeriodSelected = onPeriodSelected,
                    onErrorShown = {},
                    contentPadding = PaddingValues(),
                )
            }
        }
    }

    @Test
    fun title_isDisplayed() {
        setContent(HomeUiState(isLoading = false, recentWorkout = sampleItem))
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("GymTrack")
    }

    @Test
    fun loadingState_showsProgressIndicator() {
        setContent(HomeUiState(isLoading = true))
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithTag("home_loading").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("home_loading").assertIsDisplayed()
    }

    @Test
    fun emptyState_showsMessageAndWorkoutsCta() {
        var workoutsClicked = false
        setContent(
            HomeUiState(isLoading = false),
            onNavigateToWorkouts = { workoutsClicked = true },
        )
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Nenhum treino concluído")
        composeTestRule.onNodeWithTag("home_empty").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_workouts_cta").performClick()
        composeTestRule.waitForIdle()
        assertEquals(true, workoutsClicked)
    }

    @Test
    fun content_showsRecentWorkoutWeeklyMetricsAndVolume() {
        val stats = PeriodDashboardStats(
            sessionCount = 3,
            volume = 18_450.0,
            durationMillis = (2 * 60 + 17) * 60_000L,
            distinctExerciseCount = 8,
        )
        setContent(
            HomeUiState(
                isLoading = false,
                recentWorkout = sampleItem,
                periodStats = stats,
                trainedDayCount = 3,
                activityWeekDays = sevenActivityDays(),
                records = listOf(
                    record("Crucifixo", weight = 25.0, reps = 12, volume = 300.0),
                    record("Supino", weight = 80.0, reps = 10, volume = 800.0),
                ),
            ),
        )
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Treino recente")
        waitUntilTextIsDisplayed("Peito e Tríceps")
        waitUntilTextIsDisplayed(formatLocalDate(occurredAt, zone), substring = true)
        waitUntilTextIsDisplayed(formatLocalTime(occurredAt, zone), substring = true)
        waitUntilTextIsDisplayed("Duração: ${formatDashboardDuration(sampleItem.durationMillis)}")
        waitUntilTextIsDisplayed("Volume: ${formatVolumeKg(6_250.0)}")
        waitUntilTextIsDisplayed("Esta semana")
        composeTestRule.onNodeWithTag("home_period_selector").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_period_week").assertIsSelected()
        composeTestRule.onNodeWithTag("home_period_month").assertIsNotSelected()
        composeTestRule.onNodeWithTag("home_period_all").assertIsNotSelected()
        composeTestRule.onNodeWithTag("home_period_sessions").assertTextEquals("3")
        composeTestRule.onNodeWithTag("home_period_volume").assertTextEquals(formatVolumeKg(18_450.0))
        composeTestRule.onNodeWithTag("home_period_duration")
            .assertTextEquals(formatDashboardDuration(stats.durationMillis))
        composeTestRule.onNodeWithTag("home_period_exercises").assertTextEquals("8")
        waitUntilTextIsDisplayed("Frequência")
        waitUntilTextIsDisplayed("3 dias treinados")
        composeTestRule.onNodeWithTag("home_frequency").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_activity_week").assertIsDisplayed()
        repeat(7) { index ->
            composeTestRule.onNodeWithTag("home_activity_day_$index").assertIsDisplayed()
        }
        // sevenActivityDays marks today as trained → static with border, no pulse
        composeTestRule.onNodeWithTag("home_activity_today_pulse").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_records").performScrollTo().assertIsDisplayed()
        waitUntilTextIsDisplayed("Recordes")
        composeTestRule.onNodeWithTag("home_record_Crucifixo").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_record_Supino").assertIsDisplayed()
        waitUntilTextIsDisplayed("Carga: ${formatVolumeKg(25.0)}")
        waitUntilTextIsDisplayed("Reps: 12")
        waitUntilTextIsDisplayed("Volume: ${formatVolumeKg(300.0)}")
        waitUntilTextIsDisplayed("Carga: ${formatVolumeKg(80.0)}")
        waitUntilTextIsDisplayed("Reps: 10")
        waitUntilTextIsDisplayed("Volume: ${formatVolumeKg(800.0)}")
        composeTestRule.onNodeWithTag("home_dashboard").assertIsDisplayed()
    }

    @Test
    fun records_zeroWeightIsDisplayed() {
        setContent(
            HomeUiState(
                isLoading = false,
                recentWorkout = sampleItem,
                records = listOf(record("Abdominal", weight = 0.0, reps = 20, volume = 0.0)),
            ),
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_records").performScrollTo().assertIsDisplayed()
        waitUntilTextIsDisplayed("Abdominal")
        waitUntilTextIsDisplayed("Carga: ${formatVolumeKg(0.0)}")
        waitUntilTextIsDisplayed("Reps: 20")
        waitUntilTextIsDisplayed("Volume: ${formatVolumeKg(0.0)}")
    }

    @Test
    fun sessionsWithoutSets_doNotShowRecordsSection() {
        setContent(
            HomeUiState(
                isLoading = false,
                recentWorkout = sampleItem,
                records = emptyList(),
            ),
        )
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Treino recente")
        composeTestRule.onNodeWithTag("home_records").assertDoesNotExist()
    }

    @Test
    fun emptyState_stillShowsActivityWeek() {
        setContent(
            HomeUiState(
                isLoading = false,
                activityWeekDays = sevenActivityDays(),
            ),
        )
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Nenhum treino concluído")
        composeTestRule.onNodeWithTag("home_activity_week").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_frequency").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_records").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_period_selector").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_period_week").assertDoesNotExist()
    }

    @Test
    fun activityWeek_todayUntrained_showsPulseTag() {
        setContent(
            HomeUiState(
                isLoading = false,
                recentWorkout = sampleItem,
                activityWeekDays = sevenActivityDays(todayTrained = false),
            ),
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_activity_week").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_activity_today_pulse").assertIsDisplayed()
    }

    @Test
    fun activityWeek_todayTrained_doesNotShowPulseTag() {
        setContent(
            HomeUiState(
                isLoading = false,
                recentWorkout = sampleItem,
                activityWeekDays = sevenActivityDays(todayTrained = true),
            ),
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_activity_week").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_activity_today_pulse").assertDoesNotExist()
    }

    @Test
    fun clickingRecentWorkout_exposesSessionId() {
        var clickedId: Long? = null
        setContent(
            HomeUiState(isLoading = false, recentWorkout = sampleItem),
            onSessionClick = { clickedId = it },
        )
        composeTestRule.waitForIdle()

        waitUntilTextIsDisplayed("Peito e Tríceps")
        composeTestRule.onNodeWithTag("home_recent_42").performClick()
        composeTestRule.waitForIdle()
        assertEquals(42L, clickedId)
    }

    @Test
    fun clickingRecord_exposesExerciseName() {
        var clickedName: String? = null
        setContent(
            HomeUiState(
                isLoading = false,
                recentWorkout = sampleItem,
                records = listOf(record("Supino", weight = 80.0, reps = 10, volume = 800.0)),
            ),
            onRecordClick = { clickedName = it },
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_records").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_record_Supino").performClick()
        composeTestRule.waitForIdle()
        assertEquals("Supino", clickedName)
    }

    @Test
    fun selectingMonthChip_exposesMonthPeriod() {
        var selected: DashboardPeriod? = null
        setContent(
            HomeUiState(
                isLoading = false,
                recentWorkout = sampleItem,
                periodStats = PeriodDashboardStats(3, 100.0, 60_000L, 2),
            ),
            onPeriodSelected = { selected = it },
        )
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Esta semana")
        composeTestRule.onNodeWithTag("home_period_month").performClick()
        composeTestRule.waitForIdle()
        assertEquals(DashboardPeriod.MONTH, selected)
    }

    @Test
    fun selectingAllChip_exposesAllPeriod() {
        var selected: DashboardPeriod? = null
        setContent(
            HomeUiState(
                isLoading = false,
                recentWorkout = sampleItem,
                periodStats = PeriodDashboardStats(3, 100.0, 60_000L, 2),
            ),
            onPeriodSelected = { selected = it },
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_period_all").performClick()
        composeTestRule.waitForIdle()
        assertEquals(DashboardPeriod.ALL, selected)
    }

    @Test
    fun monthPeriod_showsMonthTitleAndTotals() {
        val stats = PeriodDashboardStats(
            sessionCount = 5,
            volume = 1_200.0,
            durationMillis = 90_000L,
            distinctExerciseCount = 4,
        )
        setContent(
            HomeUiState(
                isLoading = false,
                recentWorkout = sampleItem,
                selectedPeriod = DashboardPeriod.MONTH,
                periodStats = stats,
                trainedDayCount = 4,
                activityWeekDays = sevenActivityDays(),
                records = listOf(record("Supino", weight = 80.0, reps = 10, volume = 800.0)),
            ),
        )
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Este mês")
        composeTestRule.onNodeWithTag("home_period_month").assertIsSelected()
        composeTestRule.onNodeWithTag("home_period_week").assertIsNotSelected()
        composeTestRule.onNodeWithTag("home_period_sessions").assertTextEquals("5")
        composeTestRule.onNodeWithTag("home_period_volume").assertTextEquals(formatVolumeKg(1_200.0))
        composeTestRule.onNodeWithTag("home_period_duration")
            .assertTextEquals(formatDashboardDuration(90_000L))
        composeTestRule.onNodeWithTag("home_period_exercises").assertTextEquals("4")
        waitUntilTextIsDisplayed("4 dias treinados")
        composeTestRule.onNodeWithTag("home_activity_week").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_records").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun allPeriod_showsAllTimeTitleAndTotals() {
        val stats = PeriodDashboardStats(
            sessionCount = 20,
            volume = 9_000.0,
            durationMillis = 120_000L,
            distinctExerciseCount = 12,
        )
        setContent(
            HomeUiState(
                isLoading = false,
                recentWorkout = sampleItem,
                selectedPeriod = DashboardPeriod.ALL,
                periodStats = stats,
                trainedDayCount = 15,
                activityWeekDays = sevenActivityDays(),
            ),
        )
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Todo o histórico")
        composeTestRule.onNodeWithTag("home_period_all").assertIsSelected()
        composeTestRule.onNodeWithTag("home_period_sessions").assertTextEquals("20")
        composeTestRule.onNodeWithTag("home_period_volume").assertTextEquals(formatVolumeKg(9_000.0))
        waitUntilTextIsDisplayed("15 dias treinados")
        composeTestRule.onNodeWithTag("home_activity_week").assertIsDisplayed()
    }

    @Test
    fun emptyPeriod_withHistory_doesNotShowGlobalEmpty() {
        setContent(
            HomeUiState(
                isLoading = false,
                recentWorkout = sampleItem,
                selectedPeriod = DashboardPeriod.MONTH,
                periodStats = PeriodDashboardStats.Empty,
                trainedDayCount = 0,
                activityWeekDays = sevenActivityDays(),
                records = listOf(record("Supino", weight = 80.0, reps = 10, volume = 800.0)),
            ),
        )
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Este mês")
        composeTestRule.onNodeWithTag("home_empty").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_period_selector").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_period_sessions").assertTextEquals("0")
        waitUntilTextIsDisplayed("0 dias treinados")
        composeTestRule.onNodeWithTag("home_recent_42").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_activity_week").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_records").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun dashboardWithoutInProgress_doesNotShowResumeCta() {
        setContent(HomeUiState(isLoading = false, recentWorkout = sampleItem))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_in_progress").assertDoesNotExist()
        composeTestRule.onNodeWithTag("home_continue_in_progress").assertDoesNotExist()
    }

    @Test
    fun emptyWithoutInProgress_doesNotShowResumeCta() {
        setContent(HomeUiState(isLoading = false))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_empty").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_in_progress").assertDoesNotExist()
    }

    @Test
    fun dashboardWithInProgress_showsResumeCtaAndReportsSessionId() {
        var continuedId: Long? = null
        setContent(
            HomeUiState(
                isLoading = false,
                recentWorkout = sampleItem,
                inProgressSession = activeSession,
            ),
            onContinueInProgress = { continuedId = it },
        )
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Treino em andamento")
        waitUntilTextIsDisplayed("Push Day")
        composeTestRule.onNodeWithTag("home_continue_in_progress").performClick()
        composeTestRule.waitForIdle()
        assertEquals(77L, continuedId)
    }

    @Test
    fun emptyWithInProgress_stillShowsResumeCta() {
        var continuedId: Long? = null
        setContent(
            HomeUiState(isLoading = false, inProgressSession = activeSession),
            onContinueInProgress = { continuedId = it },
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_empty").assertIsDisplayed()
        waitUntilTextIsDisplayed("Treino em andamento")
        waitUntilTextIsDisplayed("Push Day")
        composeTestRule.onNodeWithTag("home_continue_in_progress").performClick()
        composeTestRule.waitForIdle()
        assertEquals(77L, continuedId)
    }

    @Test
    fun nextWorkout_isDisplayedWithName() {
        setContent(
            HomeUiState(
                isLoading = false,
                recentWorkout = sampleItem,
                nextWorkout = nextWorkout,
            ),
        )
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Próximo treino")
        composeTestRule.onNodeWithTag("next_workout_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("next_workout_title", useUnmergedTree = true)
            .assertTextEquals("Push")
        composeTestRule.onNodeWithTag("next_workout_open", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun nextWorkout_null_hidesCard() {
        setContent(
            HomeUiState(
                isLoading = false,
                recentWorkout = sampleItem,
                nextWorkout = null,
            ),
        )
        composeTestRule.waitForIdle()
        waitUntilTextIsDisplayed("Treino recente")
        assertTrue(composeTestRule.onAllNodesWithTag("next_workout_card").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun nextWorkout_click_reportsWorkoutId() {
        var openedId: Long? = null
        setContent(
            HomeUiState(
                isLoading = false,
                recentWorkout = sampleItem,
                nextWorkout = nextWorkout,
            ),
            onNavigateToWorkoutDetail = { openedId = it },
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("next_workout_card").performClick()
        composeTestRule.waitForIdle()
        assertEquals(9L, openedId)
    }

    @Test
    fun nextWorkout_withInProgress_keepsContinueWorking() {
        var continuedId: Long? = null
        var openedId: Long? = null
        setContent(
            HomeUiState(
                isLoading = false,
                recentWorkout = sampleItem,
                nextWorkout = nextWorkout,
                inProgressSession = activeSession,
            ),
            onContinueInProgress = { continuedId = it },
            onNavigateToWorkoutDetail = { openedId = it },
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_in_progress").assertIsDisplayed()
        composeTestRule.onNodeWithTag("next_workout_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_continue_in_progress").performClick()
        composeTestRule.waitForIdle()
        assertEquals(77L, continuedId)
        assertEquals(null, openedId)
    }

    @Test
    fun achievementsEntry_isDisplayedWithZeroUnlocked() {
        setContent(
            HomeUiState(
                isLoading = false,
                unlockedAchievementCount = 0,
                totalAchievementCount = 18,
            ),
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_achievements").assertIsDisplayed()
        waitUntilTextIsDisplayed("0 de 18 desbloqueadas")
        composeTestRule.onNodeWithTag("home_empty").assertIsDisplayed()
    }

    @Test
    fun achievementsEntry_showsUnlockedCount() {
        setContent(
            HomeUiState(
                isLoading = false,
                recentWorkout = sampleItem,
                unlockedAchievementCount = 3,
                totalAchievementCount = 18,
            ),
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_achievements").assertIsDisplayed()
        waitUntilTextIsDisplayed("3 de 18 desbloqueadas")
    }

    @Test
    fun achievementsEntry_click_reportsNavigation() {
        var opened = false
        setContent(
            HomeUiState(
                isLoading = false,
                unlockedAchievementCount = 0,
                totalAchievementCount = 18,
            ),
            onNavigateToAchievements = { opened = true },
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_achievements").performClick()
        composeTestRule.waitForIdle()
        assertTrue(opened)
    }

    @Test
    fun loadingState_doesNotShowAchievementsEntry() {
        setContent(HomeUiState(isLoading = true))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_loading").assertIsDisplayed()
        composeTestRule.onNodeWithTag("home_achievements").assertDoesNotExist()
    }

    private fun sevenActivityDays(todayTrained: Boolean = true): List<HomeActivityDay> {
        val days = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY,
        )
        val start = occurredAt
        return days.mapIndexed { index, dayOfWeek ->
            val isToday = index == 2
            HomeActivityDay(
                dayStartMillis = start + index * 86_400_000L,
                dayOfWeek = dayOfWeek,
                trained = if (isToday) todayTrained else index % 2 == 0,
                isToday = isToday,
            )
        }
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

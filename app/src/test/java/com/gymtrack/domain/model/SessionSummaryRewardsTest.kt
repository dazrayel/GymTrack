package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SessionSummaryRewardsTest {

    @Test
    fun xpEventsForSession_filtersBySourceKeyAndExcludesMissions() {
        val events = listOf(
            xp(XpEventType.WORKOUT_COMPLETED, 100, "WORKOUT_COMPLETED:session:10", 1_000L),
            xp(XpEventType.PERSONAL_RECORD, 50, "PERSONAL_RECORD:session:10:exercise:Supino", 1_010L),
            xp(XpEventType.WORKOUT_COMPLETED, 100, "WORKOUT_COMPLETED:session:99", 1_020L),
            xp(
                XpEventType.MISSION_COMPLETED,
                25,
                "MISSION_COMPLETED:training_day:2026-W01",
                1_030L,
            ),
        )

        val breakdown = xpEventsForSession(events, sessionId = 10L)

        assertEquals(150, breakdown.totalAmount)
        assertEquals(2, breakdown.events.size)
        assertTrue(breakdown.events.none { it.type == XpEventType.MISSION_COMPLETED })
    }

    @Test
    fun xpEventsForSession_emptyWhenNoMatchingEvents() {
        val breakdown = xpEventsForSession(
            events = listOf(
                xp(XpEventType.WORKOUT_COMPLETED, 100, "WORKOUT_COMPLETED:session:2", 1L),
            ),
            sessionId = 1L,
        )
        assertTrue(breakdown.isEmpty)
        assertEquals(0, breakdown.totalAmount)
    }

    @Test
    fun missionRewardsNearSessionEnd_returnsCompletedWithinWindow() {
        val endedAt = 10_000L
        val events = listOf(
            xp(
                XpEventType.MISSION_COMPLETED,
                25,
                "MISSION_COMPLETED:training_day:2026-W40",
                endedAt + 30_000L,
            ),
            xp(
                XpEventType.MISSION_COMPLETED,
                75,
                "MISSION_COMPLETED:consistency:2026-W40",
                endedAt + 200_000L,
            ),
            xp(XpEventType.WORKOUT_COMPLETED, 100, "WORKOUT_COMPLETED:session:1", endedAt),
        )

        val rewards = missionRewardsNearSessionEnd(events, endedAt)

        assertEquals(1, rewards.size)
        assertEquals("training_day", rewards[0].definitionId)
        assertEquals(25, rewards[0].xpAmount)
    }

    @Test
    fun missionRewardsNearSessionEnd_emptyWithoutEndedAt() {
        assertTrue(
            missionRewardsNearSessionEnd(
                events = listOf(
                    xp(
                        XpEventType.MISSION_COMPLETED,
                        25,
                        "MISSION_COMPLETED:training_day:2026-W40",
                        1L,
                    ),
                ),
                sessionEndedAtMillis = null,
            ).isEmpty(),
        )
    }

    @Test
    fun newlyUnlockedAchievementIds_onlyThoseUnlockedBySession() {
        val withSession = listOf(
            status("first_workout", unlocked = true),
            status("ten_workouts", unlocked = false),
        )
        val withoutSession = listOf(
            status("first_workout", unlocked = false),
            status("ten_workouts", unlocked = false),
        )

        assertEquals(
            listOf("first_workout"),
            newlyUnlockedAchievementIds(withSession, withoutSession),
        )
    }

    @Test
    fun newlyUnlockedAchievementIds_emptyWhenAlreadyUnlocked() {
        val statuses = listOf(status("first_workout", unlocked = true))
        assertTrue(newlyUnlockedAchievementIds(statuses, statuses).isEmpty())
    }

    @Test
    fun progressionHighlightsFromXp_mapsWeightAndReps() {
        val events = listOf(
            xp(
                XpEventType.PROGRESSION_WEIGHT,
                30,
                "PROGRESSION_WEIGHT:session:5:exercise:Supino reto",
                1L,
            ),
            xp(
                XpEventType.PROGRESSION_REPS,
                20,
                "PROGRESSION_REPS:session:5:exercise:Remada",
                2L,
            ),
            xp(
                XpEventType.PROGRESSION_WEIGHT,
                30,
                "PROGRESSION_WEIGHT:session:9:exercise:Leg press",
                3L,
            ),
        )

        val highlights = progressionHighlightsFromXp(events, sessionId = 5L)

        assertEquals(2, highlights.size)
        assertEquals("Supino reto", highlights[0].exerciseName)
        assertEquals(ProgressionAction.INCREASE_WEIGHT, highlights[0].action)
        assertEquals("Remada", highlights[1].exerciseName)
        assertEquals(ProgressionAction.INCREASE_REPS, highlights[1].action)
    }

    private fun xp(
        type: XpEventType,
        amount: Int,
        sourceKey: String,
        createdAt: Long,
    ) = XpEvent(
        id = createdAt,
        type = type,
        amount = amount,
        sourceKey = sourceKey,
        createdAtMillis = createdAt,
    )

    private fun status(id: String, unlocked: Boolean) = AchievementStatus(
        achievementId = id,
        unlocked = unlocked,
        current = if (unlocked) 1L else 0L,
        target = 1L,
    )
}

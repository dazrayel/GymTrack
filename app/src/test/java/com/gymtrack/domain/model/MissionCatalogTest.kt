package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId

class MissionCatalogTest {

    @Test
    fun idsAreUnique() {
        val ids = MissionCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun targetsPositiveOrNullForFullWeek() {
        MissionCatalog.all.forEach { definition ->
            if (definition.id == MissionIds.FULL_WEEK) {
                assertNull(definition.target)
            } else {
                assertTrue(definition.id, (definition.target ?: 0) > 0)
            }
            assertTrue(definition.xpReward > 0)
        }
    }

    @Test
    fun expectedCatalogSizeAndRewards() {
        assertEquals(8, MissionCatalog.all.size)
        assertEquals(20, MissionCatalog.byId(MissionIds.FIRST_STEP)!!.xpReward)
        assertEquals(25, MissionCatalog.byId(MissionIds.TRAINING_DAY)!!.xpReward)
        assertEquals(75, MissionCatalog.byId(MissionIds.CONSISTENCY)!!.xpReward)
        assertEquals(100, MissionCatalog.byId(MissionIds.FULL_WEEK)!!.xpReward)
        assertEquals(75, MissionCatalog.byId(MissionIds.EVOLUTION)!!.xpReward)
        assertEquals(100, MissionCatalog.byId(MissionIds.START_JOURNEY)!!.xpReward)
        assertEquals(150, MissionCatalog.byId(MissionIds.GROWING_STRENGTH)!!.xpReward)
        assertEquals(250, MissionCatalog.byId(MissionIds.VETERAN)!!.xpReward)
    }
}

class MissionEligibilityAndProgressTest {

    private val zone = ZoneId.of("America/Sao_Paulo")
    private val now = local("2026-10-07T12:00:00") // Wednesday

    @Test
    fun trainingDay_requiresPlanForToday() {
        val def = MissionCatalog.byId(MissionIds.TRAINING_DAY)!!
        assertFalse(isMissionEligible(def, context(plans = emptyMap())))
        assertTrue(
            isMissionEligible(
                def,
                context(plans = mapOf(DayOfWeek.WEDNESDAY to 1L)),
            ),
        )
    }

    @Test
    fun fullWeek_requiresAnyPlan() {
        val def = MissionCatalog.byId(MissionIds.FULL_WEEK)!!
        assertFalse(isMissionEligible(def, context(plans = emptyMap())))
        assertTrue(
            isMissionEligible(
                def,
                context(plans = mapOf(DayOfWeek.MONDAY to 1L)),
            ),
        )
    }

    @Test
    fun oneTime_notEligibleWhenCompleted() {
        val def = MissionCatalog.byId(MissionIds.START_JOURNEY)!!
        val completed = MissionInstance(
            definitionId = MissionIds.START_JOURNEY,
            periodKey = MissionIds.ONE_TIME_PERIOD_KEY,
            status = MissionStatus.COMPLETED,
            startedAtMillis = now,
            completedAtMillis = now,
        )
        assertFalse(isMissionEligible(def, context(instances = listOf(completed))))
        assertTrue(isMissionEligible(def, context()))
    }

    @Test
    fun fullWeek_targetDerivedFromPlanSize() {
        val def = MissionCatalog.byId(MissionIds.FULL_WEEK)!!
        assertEquals(
            4,
            resolveMissionTarget(
                def,
                context(
                    plans = mapOf(
                        DayOfWeek.MONDAY to 1L,
                        DayOfWeek.TUESDAY to 2L,
                        DayOfWeek.THURSDAY to 3L,
                        DayOfWeek.FRIDAY to 4L,
                    ),
                ),
            ),
        )
    }

    @Test
    fun firstStep_progressCapsAtOne() {
        val def = MissionCatalog.byId(MissionIds.FIRST_STEP)!!
        val instance = active(MissionIds.FIRST_STEP, "2026-10-07")
        val sessions = listOf(
            MissionSessionSnapshot(1L, 1L, local("2026-10-07T08:00:00")),
            MissionSessionSnapshot(2L, 1L, local("2026-10-07T18:00:00")),
        )
        assertEquals(1, computeMissionProgress(def, instance, context(sessions = sessions)))
    }

    @Test
    fun consistency_countsSessionsInWeek() {
        val def = MissionCatalog.byId(MissionIds.CONSISTENCY)!!
        val weekKey = isoWeekKey(now, zone)
        val instance = active(
            MissionIds.CONSISTENCY,
            weekKey,
            startedAt = local("2026-10-05T00:00:00"),
        )
        val sessions = listOf(
            MissionSessionSnapshot(1L, 1L, local("2026-10-05T10:00:00")),
            MissionSessionSnapshot(2L, 1L, local("2026-10-06T10:00:00")),
            MissionSessionSnapshot(3L, 1L, local("2026-10-07T10:00:00")),
        )
        assertEquals(3, computeMissionProgress(def, instance, context(sessions = sessions)))
    }

    @Test
    fun startJourney_ignoresSessionsBeforeStartedAt() {
        val def = MissionCatalog.byId(MissionIds.START_JOURNEY)!!
        val instance = active(MissionIds.START_JOURNEY, MissionIds.ONE_TIME_PERIOD_KEY, startedAt = now)
        val sessions = listOf(
            MissionSessionSnapshot(1L, 1L, local("2026-10-01T10:00:00")), // before
            MissionSessionSnapshot(2L, 1L, local("2026-10-07T13:00:00")),
            MissionSessionSnapshot(3L, 1L, local("2026-10-07T14:00:00")),
        )
        assertEquals(2, computeMissionProgress(def, instance, context(sessions = sessions)))
    }

    @Test
    fun evolution_capsAtOneDespiteMultipleProgressions() {
        val def = MissionCatalog.byId(MissionIds.EVOLUTION)!!
        val weekKey = isoWeekKey(now, zone)
        val instance = active(
            MissionIds.EVOLUTION,
            weekKey,
            startedAt = local("2026-10-05T00:00:00"),
        )
        val sessions = listOf(
            MissionSessionSnapshot(1L, 1L, local("2026-10-07T10:00:00")),
            MissionSessionSnapshot(2L, 1L, local("2026-10-07T11:00:00")),
        )
        val progressions = mapOf(1L to 3, 2L to 2)
        assertEquals(
            1,
            computeMissionProgress(
                def,
                instance,
                context(sessions = sessions, progressions = progressions),
            ),
        )
    }

    @Test
    fun biSet_countsAsSingleSessionForConsistency() {
        val def = MissionCatalog.byId(MissionIds.CONSISTENCY)!!
        val weekKey = isoWeekKey(now, zone)
        val instance = active(
            MissionIds.CONSISTENCY,
            weekKey,
            startedAt = local("2026-10-05T00:00:00"),
        )
        // One session id even if it had multiple exercises internally
        val sessions = listOf(
            MissionSessionSnapshot(10L, 1L, local("2026-10-07T10:00:00")),
        )
        assertEquals(1, computeMissionProgress(def, instance, context(sessions = sessions)))
    }

    @Test
    fun missionCompletedSourceKey_isDeterministic() {
        assertEquals(
            "MISSION_COMPLETED:CONSISTENCY:2026-W41",
            XpSourceKeys.missionCompleted("CONSISTENCY", "2026-W41"),
        )
        assertEquals(
            "MISSION_COMPLETED:START_JOURNEY:ONCE",
            XpSourceKeys.missionCompleted(MissionIds.START_JOURNEY, MissionIds.ONE_TIME_PERIOD_KEY),
        )
    }

    private fun active(definitionId: String, periodKey: String, startedAt: Long = now) =
        MissionInstance(
            definitionId = definitionId,
            periodKey = periodKey,
            status = MissionStatus.ACTIVE,
            startedAtMillis = startedAt,
        )

    private fun context(
        sessions: List<MissionSessionSnapshot> = emptyList(),
        plans: Map<DayOfWeek, Long> = emptyMap(),
        instances: List<MissionInstance> = emptyList(),
        progressions: Map<Long, Int> = emptyMap(),
    ) = MissionEvaluationContext(
        nowMillis = now,
        zoneId = zone,
        completedSessions = sessions,
        weeklyPlans = plans,
        instances = instances,
        progressionCountsBySessionId = progressions,
    )

    private fun local(dateTime: String): Long =
        java.time.LocalDateTime.parse(dateTime).atZone(zone).toInstant().toEpochMilli()
}

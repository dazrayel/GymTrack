package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TitleCatalogTest {

    @Test
    fun catalog_hasUniqueIdsAndValidConditions() {
        val ids = TitleCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(TitleCatalog.all.size in 8..10)
        TitleCatalog.all.forEach { definition ->
            when (val condition = definition.unlockCondition) {
                is TitleUnlockCondition.AchievementUnlocked -> {
                    assertTrue(
                        definition.id,
                        AchievementCatalog.any { it.id == condition.achievementId },
                    )
                }
                is TitleUnlockCondition.MinLevel -> {
                    assertTrue(definition.id, condition.level in 1..XpLevelCurve.MAX_LEVEL)
                }
            }
            assertTrue(definition.rarity.name.isNotEmpty())
        }
    }

    @Test
    fun approvedTitles_matchExpectedConditions() {
        assertCondition(TitleIds.BEGINNER, TitleUnlockCondition.AchievementUnlocked("FIRST_WORKOUT"))
        assertCondition(TitleIds.TEN_WORKOUTS, TitleUnlockCondition.AchievementUnlocked("WORKOUTS_10"))
        assertCondition(TitleIds.VETERAN, TitleUnlockCondition.AchievementUnlocked("WORKOUTS_50"))
        assertCondition(TitleIds.TEN_TONS, TitleUnlockCondition.AchievementUnlocked("VOLUME_10000"))
        assertCondition(TitleIds.EXPLORER, TitleUnlockCondition.AchievementUnlocked("EXERCISES_25"))
        assertCondition(TitleIds.RECORD_HOLDER, TitleUnlockCondition.AchievementUnlocked("PR_EXERCISES_10"))
        assertCondition(TitleIds.CONSISTENT, TitleUnlockCondition.AchievementUnlocked("TRAINED_DAYS_20"))
        assertCondition(TitleIds.ASCENDANT, TitleUnlockCondition.MinLevel(10))
        assertCondition(TitleIds.ELITE, TitleUnlockCondition.MinLevel(25))
        assertCondition(TitleIds.LEGENDARY, TitleUnlockCondition.MinLevel(50))
    }

    @Test
    fun achievementLocked_titleLocked() {
        val achievements = lockedAchievements()
        val titles = evaluateTitles(achievements, levelProgressFromTotalXp(0))
        assertFalse(titles.single { it.definition.id == TitleIds.BEGINNER }.unlocked)
    }

    @Test
    fun achievementUnlocked_titleUnlocked() {
        val achievements = lockedAchievements().map {
            if (it.achievementId == "FIRST_WORKOUT") it.copy(unlocked = true, current = 1) else it
        }
        val titles = evaluateTitles(achievements, levelProgressFromTotalXp(0))
        assertTrue(titles.single { it.definition.id == TitleIds.BEGINNER }.unlocked)
        assertFalse(titles.single { it.definition.id == TitleIds.TEN_WORKOUTS }.unlocked)
    }

    @Test
    fun minLevel_thresholds() {
        val achievements = lockedAchievements()
        assertFalse(
            evaluateTitles(achievements, levelProgressFromTotalXp(5_608))
                .single { it.definition.id == TitleIds.ASCENDANT }.unlocked,
        )
        assertTrue(
            evaluateTitles(achievements, levelProgressFromTotalXp(5_609))
                .single { it.definition.id == TitleIds.ASCENDANT }.unlocked,
        )
        assertTrue(
            evaluateTitles(achievements, levelProgressFromTotalXp(28_534))
                .single { it.definition.id == TitleIds.ELITE }.unlocked,
        )
        assertTrue(
            evaluateTitles(achievements, levelProgressFromTotalXp(100_000))
                .single { it.definition.id == TitleIds.LEGENDARY }.unlocked,
        )
    }

    @Test
    fun deletionRevokesAchievementBasedTitle() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val sessions = (1..10).map { session(it.toLong(), it * 86_400_000L) }
        val sets = sessions.map { set(it.sessionId, it.startedAtMillis) }
        var achievements = evaluateAchievements(sessions, sets, zone)
        var titles = evaluateTitles(achievements, levelProgressFromTotalXp(0))
        assertTrue(titles.single { it.definition.id == TitleIds.TEN_WORKOUTS }.unlocked)

        val afterDelete = sessions.dropLast(1)
        val setsAfter = sets.filter { it.sessionId != 10L }
        achievements = evaluateAchievements(afterDelete, setsAfter, zone)
        titles = evaluateTitles(achievements, levelProgressFromTotalXp(0))
        assertFalse(titles.single { it.definition.id == TitleIds.TEN_WORKOUTS }.unlocked)
    }

    @Test
    fun sessionDeletion_doesNotRevokeLevelTitle_whenXpRemains() {
        val achievements = lockedAchievements()
        val level = levelProgressFromTotalXp(28_534) // level 25
        val titles = evaluateTitles(achievements, level)
        assertTrue(titles.single { it.definition.id == TitleIds.ELITE }.unlocked)
        // Simulating session delete does not change LevelProgress when XP ledger stays.
        assertTrue(
            evaluateTitles(achievements, level)
                .single { it.definition.id == TitleIds.ELITE }.unlocked,
        )
    }

    @Test
    fun resolveActiveTitleId_keepsValidAndClearsInvalidWithoutFallback() {
        val achievements = lockedAchievements().map {
            when (it.achievementId) {
                "FIRST_WORKOUT" -> it.copy(unlocked = true, current = 1)
                "WORKOUTS_10" -> it.copy(unlocked = true, current = 10)
                else -> it
            }
        }
        val titles = evaluateTitles(achievements, levelProgressFromTotalXp(0))
        assertEquals(
            TitleIds.BEGINNER,
            resolveActiveTitleId(TitleIds.BEGINNER, titles),
        )
        assertNull(resolveActiveTitleId(TitleIds.VETERAN, titles))
        assertNull(resolveActiveTitleId(null, titles))
        // Invalid preference must not resolve to another unlocked title.
        assertNull(resolveActiveTitleId(TitleIds.EXPLORER, titles))
    }

    private fun assertCondition(id: String, expected: TitleUnlockCondition) {
        assertEquals(expected, TitleCatalog.byId(id)!!.unlockCondition)
    }

    private fun lockedAchievements(): List<AchievementStatus> =
        AchievementCatalog.map {
            AchievementStatus(
                achievementId = it.id,
                unlocked = false,
                current = 0,
                target = it.target,
            )
        }

    private fun session(sessionId: Long, occurredAt: Long) = WorkoutHistoryItem(
        sessionId = sessionId,
        workoutName = "Treino",
        startedAtMillis = occurredAt,
        endedAtMillis = occurredAt,
        durationMillis = 1_000L,
        volume = 100.0,
        exerciseCount = 1,
        completedSetCount = 1,
        plannedSetCount = 1,
    )

    private fun set(sessionId: Long, occurredAt: Long) = CompletedSetRecord(
        sessionId = sessionId,
        occurredAtMillis = occurredAt,
        exerciseName = "Supino",
        reps = 8,
        weight = 10.0,
    )
}

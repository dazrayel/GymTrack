package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class AchievementCatalogMetadataTest {

    @Test
    fun catalog_hasEighteenUniqueIds() {
        val ids = AchievementCatalog.map { it.id }
        assertEquals(18, ids.size)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun catalog_preservesOriginalIdsAndTargets() {
        val expected = listOf(
            "FIRST_WORKOUT" to 1L,
            "WORKOUTS_10" to 10L,
            "WORKOUTS_25" to 25L,
            "WORKOUTS_50" to 50L,
            "VOLUME_10000" to 10_000L,
            "VOLUME_50000" to 50_000L,
            "VOLUME_100000" to 100_000L,
            "SETS_100" to 100L,
            "SETS_500" to 500L,
            "SETS_1000" to 1_000L,
            "EXERCISES_5" to 5L,
            "EXERCISES_10" to 10L,
            "EXERCISES_25" to 25L,
            "PR_FIRST" to 1L,
            "PR_EXERCISES_5" to 5L,
            "PR_EXERCISES_10" to 10L,
            "TRAINED_DAYS_5" to 5L,
            "TRAINED_DAYS_20" to 20L,
        )
        assertEquals(expected.map { it.first }, AchievementCatalog.map { it.id })
        expected.forEach { (id, target) ->
            assertEquals(id, target, AchievementCatalog.single { it.id == id }.target)
        }
    }

    @Test
    fun everyDefinition_hasCategoryAndRarity() {
        AchievementCatalog.forEach { definition ->
            // Accessing enums proves they are assigned; no nullables here.
            assertTrue(definition.id, definition.category.name.isNotEmpty())
            assertTrue(definition.id, definition.rarity.name.isNotEmpty())
        }
    }

    @Test
    fun categories_mapFromMetrics() {
        AchievementCatalog.forEach { definition ->
            val expected = when (definition.metric) {
                AchievementMetric.SESSION_COUNT -> AchievementCategory.MILESTONES
                AchievementMetric.TOTAL_VOLUME_KG -> AchievementCategory.VOLUME
                AchievementMetric.SET_COUNT -> AchievementCategory.EFFORT
                AchievementMetric.DISTINCT_EXERCISE_COUNT -> AchievementCategory.EXPLORATION
                AchievementMetric.PR_EXERCISE_COUNT -> AchievementCategory.RECORDS
                AchievementMetric.TRAINED_DAYS -> AchievementCategory.CONSISTENCY
            }
            assertEquals(definition.id, expected, definition.category)
        }
    }

    @Test
    fun rarity_matchesApprovedMapping() {
        val expected = mapOf(
            "FIRST_WORKOUT" to AchievementRarity.COMMON,
            "SETS_100" to AchievementRarity.COMMON,
            "EXERCISES_5" to AchievementRarity.COMMON,
            "PR_FIRST" to AchievementRarity.COMMON,
            "TRAINED_DAYS_5" to AchievementRarity.COMMON,
            "VOLUME_10000" to AchievementRarity.COMMON,
            "WORKOUTS_10" to AchievementRarity.UNCOMMON,
            "SETS_500" to AchievementRarity.UNCOMMON,
            "EXERCISES_10" to AchievementRarity.UNCOMMON,
            "PR_EXERCISES_5" to AchievementRarity.UNCOMMON,
            "TRAINED_DAYS_20" to AchievementRarity.UNCOMMON,
            "VOLUME_50000" to AchievementRarity.UNCOMMON,
            "WORKOUTS_25" to AchievementRarity.RARE,
            "SETS_1000" to AchievementRarity.RARE,
            "EXERCISES_25" to AchievementRarity.RARE,
            "PR_EXERCISES_10" to AchievementRarity.RARE,
            "VOLUME_100000" to AchievementRarity.RARE,
            "WORKOUTS_50" to AchievementRarity.EPIC,
        )
        assertEquals(expected.keys, AchievementCatalog.map { it.id }.toSet())
        expected.forEach { (id, rarity) ->
            assertEquals(id, rarity, AchievementCatalog.single { it.id == id }.rarity)
        }
        assertTrue(AchievementCatalog.none { it.rarity == AchievementRarity.LEGENDARY })
    }

    @Test
    fun workoutsFamily_tiersAreConsistentAndIdsUnchanged() {
        val workouts = AchievementCatalog.filter { it.familyId == AchievementFamilies.WORKOUTS }
        assertEquals(
            listOf("WORKOUTS_10", "WORKOUTS_25", "WORKOUTS_50"),
            workouts.sortedBy { it.tierIndex }.map { it.id },
        )
        assertEquals(listOf(1, 2, 3), workouts.sortedBy { it.tierIndex }.map { it.tierIndex })
        assertNull(AchievementCatalog.single { it.id == "FIRST_WORKOUT" }.familyId)
        assertNull(AchievementCatalog.single { it.id == "FIRST_WORKOUT" }.tierIndex)
    }

    @Test
    fun otherFamilies_haveConsistentTierIndexes() {
        assertFamilyTiers(
            AchievementFamilies.VOLUME,
            listOf("VOLUME_10000", "VOLUME_50000", "VOLUME_100000"),
        )
        assertFamilyTiers(
            AchievementFamilies.SETS,
            listOf("SETS_100", "SETS_500", "SETS_1000"),
        )
        assertFamilyTiers(
            AchievementFamilies.EXERCISES,
            listOf("EXERCISES_5", "EXERCISES_10", "EXERCISES_25"),
        )
        assertFamilyTiers(
            AchievementFamilies.PRS,
            listOf("PR_FIRST", "PR_EXERCISES_5", "PR_EXERCISES_10"),
        )
        assertFamilyTiers(
            AchievementFamilies.TRAINED_DAYS,
            listOf("TRAINED_DAYS_5", "TRAINED_DAYS_20"),
        )
    }

    @Test
    fun evaluatorRegression_emptyHistory_sameStatusShape() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val statuses = evaluateAchievements(emptyList(), emptyList(), zone)
        assertEquals(AchievementCatalog.map { it.id }, statuses.map { it.achievementId })
        statuses.forEach { status ->
            assertEquals(status.achievementId, false, status.unlocked)
            assertEquals(status.achievementId, 0L, status.current)
            assertEquals(
                status.achievementId,
                AchievementCatalog.single { it.id == status.achievementId }.target,
                status.target,
            )
        }
    }

    @Test
    fun evaluatorRegression_tenSessions_sameUnlocksAndProgress() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val sessions = (1..10).map { index ->
            WorkoutHistoryItem(
                sessionId = index.toLong(),
                workoutName = "Treino",
                startedAtMillis = index * 86_400_000L,
                endedAtMillis = index * 86_400_000L,
                durationMillis = 1_000L,
                volume = 100.0,
                exerciseCount = 1,
                completedSetCount = 1,
                plannedSetCount = 1,
            )
        }
        val sets = sessions.map { item ->
            CompletedSetRecord(
                sessionId = item.sessionId,
                occurredAtMillis = item.startedAtMillis,
                exerciseName = "Supino",
                reps = 8,
                weight = 10.0,
            )
        }
        val statuses = evaluateAchievements(sessions, sets, zone)
        assertStatus(statuses, "FIRST_WORKOUT", unlocked = true, current = 1, target = 1)
        assertStatus(statuses, "WORKOUTS_10", unlocked = true, current = 10, target = 10)
        assertStatus(statuses, "WORKOUTS_25", unlocked = false, current = 10, target = 25)
        assertStatus(statuses, "WORKOUTS_50", unlocked = false, current = 10, target = 50)
    }

    private fun assertFamilyTiers(familyId: String, expectedIds: List<String>) {
        val members = AchievementCatalog.filter { it.familyId == familyId }
        assertEquals(expectedIds, members.sortedBy { it.tierIndex }.map { it.id })
        assertEquals(
            expectedIds.indices.map { it + 1 },
            members.sortedBy { it.tierIndex }.map { it.tierIndex },
        )
    }

    private fun assertStatus(
        statuses: List<AchievementStatus>,
        id: String,
        unlocked: Boolean,
        current: Long,
        target: Long,
    ) {
        val status = statuses.single { it.achievementId == id }
        assertEquals(id, unlocked, status.unlocked)
        assertEquals(id, current, status.current)
        assertEquals(id, target, status.target)
    }
}

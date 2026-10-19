package com.gymtrack.domain.model

/**
 * Stable MVP achievement catalog. [nameKey] and [descriptionKey] are resource keys for the UI layer;
 * the domain does not resolve display strings.
 *
 * Phase 5.4 adds presentation metadata ([category], [rarity], [familyId], [tierIndex])
 * without changing evaluation criteria ([metric] / [target]).
 */
enum class AchievementMetric {
    SESSION_COUNT,
    TOTAL_VOLUME_KG,
    SET_COUNT,
    DISTINCT_EXERCISE_COUNT,
    PR_EXERCISE_COUNT,
    TRAINED_DAYS,
}

enum class AchievementCategory {
    MILESTONES,
    VOLUME,
    EFFORT,
    EXPLORATION,
    RECORDS,
    CONSISTENCY,
}

enum class AchievementRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY,
}

object AchievementFamilies {
    const val WORKOUTS = "WORKOUTS"
    const val VOLUME = "VOLUME"
    const val SETS = "SETS"
    const val EXERCISES = "EXERCISES"
    const val PRS = "PRS"
    const val TRAINED_DAYS = "TRAINED_DAYS"
}

data class AchievementDefinition(
    val id: String,
    val nameKey: String,
    val descriptionKey: String,
    val metric: AchievementMetric,
    val target: Long,
    val category: AchievementCategory,
    val rarity: AchievementRarity,
    /** Visual family only; null for standalone achievements. */
    val familyId: String? = null,
    /** Order within [familyId]; null when not in a family. */
    val tierIndex: Int? = null,
)

data class AchievementStatus(
    val achievementId: String,
    val unlocked: Boolean,
    val current: Long,
    val target: Long,
)

val AchievementCatalog: List<AchievementDefinition> = listOf(
    AchievementDefinition(
        id = "FIRST_WORKOUT",
        nameKey = "achievement.first_workout.name",
        descriptionKey = "achievement.first_workout.description",
        metric = AchievementMetric.SESSION_COUNT,
        target = 1,
        category = AchievementCategory.MILESTONES,
        rarity = AchievementRarity.COMMON,
    ),
    AchievementDefinition(
        id = "WORKOUTS_10",
        nameKey = "achievement.workouts_10.name",
        descriptionKey = "achievement.workouts_10.description",
        metric = AchievementMetric.SESSION_COUNT,
        target = 10,
        category = AchievementCategory.MILESTONES,
        rarity = AchievementRarity.UNCOMMON,
        familyId = AchievementFamilies.WORKOUTS,
        tierIndex = 1,
    ),
    AchievementDefinition(
        id = "WORKOUTS_25",
        nameKey = "achievement.workouts_25.name",
        descriptionKey = "achievement.workouts_25.description",
        metric = AchievementMetric.SESSION_COUNT,
        target = 25,
        category = AchievementCategory.MILESTONES,
        rarity = AchievementRarity.RARE,
        familyId = AchievementFamilies.WORKOUTS,
        tierIndex = 2,
    ),
    AchievementDefinition(
        id = "WORKOUTS_50",
        nameKey = "achievement.workouts_50.name",
        descriptionKey = "achievement.workouts_50.description",
        metric = AchievementMetric.SESSION_COUNT,
        target = 50,
        category = AchievementCategory.MILESTONES,
        rarity = AchievementRarity.EPIC,
        familyId = AchievementFamilies.WORKOUTS,
        tierIndex = 3,
    ),
    AchievementDefinition(
        id = "VOLUME_10000",
        nameKey = "achievement.volume_10000.name",
        descriptionKey = "achievement.volume_10000.description",
        metric = AchievementMetric.TOTAL_VOLUME_KG,
        target = 10_000,
        category = AchievementCategory.VOLUME,
        rarity = AchievementRarity.COMMON,
        familyId = AchievementFamilies.VOLUME,
        tierIndex = 1,
    ),
    AchievementDefinition(
        id = "VOLUME_50000",
        nameKey = "achievement.volume_50000.name",
        descriptionKey = "achievement.volume_50000.description",
        metric = AchievementMetric.TOTAL_VOLUME_KG,
        target = 50_000,
        category = AchievementCategory.VOLUME,
        rarity = AchievementRarity.UNCOMMON,
        familyId = AchievementFamilies.VOLUME,
        tierIndex = 2,
    ),
    AchievementDefinition(
        id = "VOLUME_100000",
        nameKey = "achievement.volume_100000.name",
        descriptionKey = "achievement.volume_100000.description",
        metric = AchievementMetric.TOTAL_VOLUME_KG,
        target = 100_000,
        category = AchievementCategory.VOLUME,
        rarity = AchievementRarity.RARE,
        familyId = AchievementFamilies.VOLUME,
        tierIndex = 3,
    ),
    AchievementDefinition(
        id = "SETS_100",
        nameKey = "achievement.sets_100.name",
        descriptionKey = "achievement.sets_100.description",
        metric = AchievementMetric.SET_COUNT,
        target = 100,
        category = AchievementCategory.EFFORT,
        rarity = AchievementRarity.COMMON,
        familyId = AchievementFamilies.SETS,
        tierIndex = 1,
    ),
    AchievementDefinition(
        id = "SETS_500",
        nameKey = "achievement.sets_500.name",
        descriptionKey = "achievement.sets_500.description",
        metric = AchievementMetric.SET_COUNT,
        target = 500,
        category = AchievementCategory.EFFORT,
        rarity = AchievementRarity.UNCOMMON,
        familyId = AchievementFamilies.SETS,
        tierIndex = 2,
    ),
    AchievementDefinition(
        id = "SETS_1000",
        nameKey = "achievement.sets_1000.name",
        descriptionKey = "achievement.sets_1000.description",
        metric = AchievementMetric.SET_COUNT,
        target = 1_000,
        category = AchievementCategory.EFFORT,
        rarity = AchievementRarity.RARE,
        familyId = AchievementFamilies.SETS,
        tierIndex = 3,
    ),
    AchievementDefinition(
        id = "EXERCISES_5",
        nameKey = "achievement.exercises_5.name",
        descriptionKey = "achievement.exercises_5.description",
        metric = AchievementMetric.DISTINCT_EXERCISE_COUNT,
        target = 5,
        category = AchievementCategory.EXPLORATION,
        rarity = AchievementRarity.COMMON,
        familyId = AchievementFamilies.EXERCISES,
        tierIndex = 1,
    ),
    AchievementDefinition(
        id = "EXERCISES_10",
        nameKey = "achievement.exercises_10.name",
        descriptionKey = "achievement.exercises_10.description",
        metric = AchievementMetric.DISTINCT_EXERCISE_COUNT,
        target = 10,
        category = AchievementCategory.EXPLORATION,
        rarity = AchievementRarity.UNCOMMON,
        familyId = AchievementFamilies.EXERCISES,
        tierIndex = 2,
    ),
    AchievementDefinition(
        id = "EXERCISES_25",
        nameKey = "achievement.exercises_25.name",
        descriptionKey = "achievement.exercises_25.description",
        metric = AchievementMetric.DISTINCT_EXERCISE_COUNT,
        target = 25,
        category = AchievementCategory.EXPLORATION,
        rarity = AchievementRarity.RARE,
        familyId = AchievementFamilies.EXERCISES,
        tierIndex = 3,
    ),
    AchievementDefinition(
        id = "PR_FIRST",
        nameKey = "achievement.pr_first.name",
        descriptionKey = "achievement.pr_first.description",
        metric = AchievementMetric.PR_EXERCISE_COUNT,
        target = 1,
        category = AchievementCategory.RECORDS,
        rarity = AchievementRarity.COMMON,
        familyId = AchievementFamilies.PRS,
        tierIndex = 1,
    ),
    AchievementDefinition(
        id = "PR_EXERCISES_5",
        nameKey = "achievement.pr_exercises_5.name",
        descriptionKey = "achievement.pr_exercises_5.description",
        metric = AchievementMetric.PR_EXERCISE_COUNT,
        target = 5,
        category = AchievementCategory.RECORDS,
        rarity = AchievementRarity.UNCOMMON,
        familyId = AchievementFamilies.PRS,
        tierIndex = 2,
    ),
    AchievementDefinition(
        id = "PR_EXERCISES_10",
        nameKey = "achievement.pr_exercises_10.name",
        descriptionKey = "achievement.pr_exercises_10.description",
        metric = AchievementMetric.PR_EXERCISE_COUNT,
        target = 10,
        category = AchievementCategory.RECORDS,
        rarity = AchievementRarity.RARE,
        familyId = AchievementFamilies.PRS,
        tierIndex = 3,
    ),
    AchievementDefinition(
        id = "TRAINED_DAYS_5",
        nameKey = "achievement.trained_days_5.name",
        descriptionKey = "achievement.trained_days_5.description",
        metric = AchievementMetric.TRAINED_DAYS,
        target = 5,
        category = AchievementCategory.CONSISTENCY,
        rarity = AchievementRarity.COMMON,
        familyId = AchievementFamilies.TRAINED_DAYS,
        tierIndex = 1,
    ),
    AchievementDefinition(
        id = "TRAINED_DAYS_20",
        nameKey = "achievement.trained_days_20.name",
        descriptionKey = "achievement.trained_days_20.description",
        metric = AchievementMetric.TRAINED_DAYS,
        target = 20,
        category = AchievementCategory.CONSISTENCY,
        rarity = AchievementRarity.UNCOMMON,
        familyId = AchievementFamilies.TRAINED_DAYS,
        tierIndex = 2,
    ),
)

/** Category display order for AchievementScreen. */
val AchievementCategoryOrder: List<AchievementCategory> = listOf(
    AchievementCategory.MILESTONES,
    AchievementCategory.VOLUME,
    AchievementCategory.EFFORT,
    AchievementCategory.EXPLORATION,
    AchievementCategory.RECORDS,
    AchievementCategory.CONSISTENCY,
)

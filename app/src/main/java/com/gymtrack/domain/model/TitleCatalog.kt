package com.gymtrack.domain.model

/**
 * Static RPG title catalog (Phase 5.5). Unlock is derived from achievements and level only.
 */
sealed class TitleUnlockCondition {
    data class AchievementUnlocked(val achievementId: String) : TitleUnlockCondition()
    data class MinLevel(val level: Int) : TitleUnlockCondition()
}

data class TitleDefinition(
    val id: String,
    val nameRes: Int,
    val descriptionRes: Int,
    val rarity: AchievementRarity,
    val unlockCondition: TitleUnlockCondition,
)

data class TitleStatus(
    val definition: TitleDefinition,
    val unlocked: Boolean,
)

object TitleIds {
    const val BEGINNER = "BEGINNER"
    const val TEN_WORKOUTS = "TEN_WORKOUTS"
    const val VETERAN = "VETERAN"
    const val TEN_TONS = "TEN_TONS"
    const val EXPLORER = "EXPLORER"
    const val RECORD_HOLDER = "RECORD_HOLDER"
    const val CONSISTENT = "CONSISTENT"
    const val ASCENDANT = "ASCENDANT"
    const val ELITE = "ELITE"
    const val LEGENDARY = "LEGENDARY"
}

object TitleCatalog {
    val all: List<TitleDefinition> = listOf(
        TitleDefinition(
            id = TitleIds.BEGINNER,
            nameRes = com.gymtrack.R.string.title_beginner_name,
            descriptionRes = com.gymtrack.R.string.title_beginner_description,
            rarity = AchievementRarity.COMMON,
            unlockCondition = TitleUnlockCondition.AchievementUnlocked("FIRST_WORKOUT"),
        ),
        TitleDefinition(
            id = TitleIds.TEN_WORKOUTS,
            nameRes = com.gymtrack.R.string.title_ten_workouts_name,
            descriptionRes = com.gymtrack.R.string.title_ten_workouts_description,
            rarity = AchievementRarity.UNCOMMON,
            unlockCondition = TitleUnlockCondition.AchievementUnlocked("WORKOUTS_10"),
        ),
        TitleDefinition(
            id = TitleIds.VETERAN,
            nameRes = com.gymtrack.R.string.title_veteran_name,
            descriptionRes = com.gymtrack.R.string.title_veteran_description,
            rarity = AchievementRarity.EPIC,
            unlockCondition = TitleUnlockCondition.AchievementUnlocked("WORKOUTS_50"),
        ),
        TitleDefinition(
            id = TitleIds.TEN_TONS,
            nameRes = com.gymtrack.R.string.title_ten_tons_name,
            descriptionRes = com.gymtrack.R.string.title_ten_tons_description,
            rarity = AchievementRarity.COMMON,
            unlockCondition = TitleUnlockCondition.AchievementUnlocked("VOLUME_10000"),
        ),
        TitleDefinition(
            id = TitleIds.EXPLORER,
            nameRes = com.gymtrack.R.string.title_explorer_name,
            descriptionRes = com.gymtrack.R.string.title_explorer_description,
            rarity = AchievementRarity.RARE,
            unlockCondition = TitleUnlockCondition.AchievementUnlocked("EXERCISES_25"),
        ),
        TitleDefinition(
            id = TitleIds.RECORD_HOLDER,
            nameRes = com.gymtrack.R.string.title_record_holder_name,
            descriptionRes = com.gymtrack.R.string.title_record_holder_description,
            rarity = AchievementRarity.RARE,
            unlockCondition = TitleUnlockCondition.AchievementUnlocked("PR_EXERCISES_10"),
        ),
        TitleDefinition(
            id = TitleIds.CONSISTENT,
            nameRes = com.gymtrack.R.string.title_consistent_name,
            descriptionRes = com.gymtrack.R.string.title_consistent_description,
            rarity = AchievementRarity.UNCOMMON,
            unlockCondition = TitleUnlockCondition.AchievementUnlocked("TRAINED_DAYS_20"),
        ),
        TitleDefinition(
            id = TitleIds.ASCENDANT,
            nameRes = com.gymtrack.R.string.title_ascendant_name,
            descriptionRes = com.gymtrack.R.string.title_ascendant_description,
            rarity = AchievementRarity.UNCOMMON,
            unlockCondition = TitleUnlockCondition.MinLevel(10),
        ),
        TitleDefinition(
            id = TitleIds.ELITE,
            nameRes = com.gymtrack.R.string.title_elite_name,
            descriptionRes = com.gymtrack.R.string.title_elite_description,
            rarity = AchievementRarity.RARE,
            unlockCondition = TitleUnlockCondition.MinLevel(25),
        ),
        TitleDefinition(
            id = TitleIds.LEGENDARY,
            nameRes = com.gymtrack.R.string.title_legendary_name,
            descriptionRes = com.gymtrack.R.string.title_legendary_description,
            rarity = AchievementRarity.LEGENDARY,
            unlockCondition = TitleUnlockCondition.MinLevel(50),
        ),
    )

    fun byId(id: String): TitleDefinition? = all.find { it.id == id }
}

/**
 * Derives title unlocks from existing achievement statuses and level progress.
 * O(catalog) — does not recompute session/volume/PR metrics.
 */
fun evaluateTitles(
    achievements: List<AchievementStatus>,
    levelProgress: LevelProgress,
): List<TitleStatus> {
    val unlockedById = achievements.associate { it.achievementId to it.unlocked }
    return TitleCatalog.all.map { definition ->
        val unlocked = when (val condition = definition.unlockCondition) {
            is TitleUnlockCondition.AchievementUnlocked ->
                unlockedById[condition.achievementId] == true
            is TitleUnlockCondition.MinLevel ->
                levelProgress.level >= condition.level
        }
        TitleStatus(definition = definition, unlocked = unlocked)
    }
}

/**
 * Returns [preferredId] only if that title is currently unlocked; otherwise null.
 * Does not pick a fallback title.
 */
fun resolveActiveTitleId(
    preferredId: String?,
    titles: List<TitleStatus>,
): String? {
    if (preferredId.isNullOrBlank()) return null
    val status = titles.find { it.definition.id == preferredId } ?: return null
    return if (status.unlocked) preferredId else null
}

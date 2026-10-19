package com.gymtrack.domain.model

/**
 * Mission kinds for Phase 5.3.
 */
enum class MissionType {
    DAILY,
    WEEKLY,
    LONG_TERM,
    ONE_TIME,
}

enum class MissionStatus {
    ACTIVE,
    COMPLETED,
}

/**
 * Static product definition. [target] is null for [MissionIds.FULL_WEEK]
 * (derived from weekly plan size at evaluation time).
 */
data class MissionDefinition(
    val id: String,
    val type: MissionType,
    val titleRes: Int,
    val descriptionRes: Int,
    val target: Int?,
    val xpReward: Int,
    val repeatable: Boolean,
)

object MissionIds {
    const val FIRST_STEP = "FIRST_STEP"
    const val TRAINING_DAY = "TRAINING_DAY"
    const val CONSISTENCY = "CONSISTENCY"
    const val FULL_WEEK = "FULL_WEEK"
    const val EVOLUTION = "EVOLUTION"
    const val START_JOURNEY = "START_JOURNEY"
    const val GROWING_STRENGTH = "GROWING_STRENGTH"
    const val VETERAN = "VETERAN"

    const val ONE_TIME_PERIOD_KEY = "ONCE"
}

object MissionCatalog {
    val all: List<MissionDefinition> = listOf(
        MissionDefinition(
            id = MissionIds.FIRST_STEP,
            type = MissionType.DAILY,
            titleRes = com.gymtrack.R.string.mission_first_step_title,
            descriptionRes = com.gymtrack.R.string.mission_first_step_description,
            target = 1,
            xpReward = 20,
            repeatable = true,
        ),
        MissionDefinition(
            id = MissionIds.TRAINING_DAY,
            type = MissionType.DAILY,
            titleRes = com.gymtrack.R.string.mission_training_day_title,
            descriptionRes = com.gymtrack.R.string.mission_training_day_description,
            target = 1,
            xpReward = 25,
            repeatable = true,
        ),
        MissionDefinition(
            id = MissionIds.CONSISTENCY,
            type = MissionType.WEEKLY,
            titleRes = com.gymtrack.R.string.mission_consistency_title,
            descriptionRes = com.gymtrack.R.string.mission_consistency_description,
            target = 3,
            xpReward = 75,
            repeatable = true,
        ),
        MissionDefinition(
            id = MissionIds.FULL_WEEK,
            type = MissionType.WEEKLY,
            titleRes = com.gymtrack.R.string.mission_full_week_title,
            descriptionRes = com.gymtrack.R.string.mission_full_week_description,
            target = null, // derived from planned workout count
            xpReward = 100,
            repeatable = true,
        ),
        MissionDefinition(
            id = MissionIds.EVOLUTION,
            type = MissionType.WEEKLY,
            titleRes = com.gymtrack.R.string.mission_evolution_title,
            descriptionRes = com.gymtrack.R.string.mission_evolution_description,
            target = 1,
            xpReward = 75,
            repeatable = true,
        ),
        MissionDefinition(
            id = MissionIds.START_JOURNEY,
            type = MissionType.ONE_TIME,
            titleRes = com.gymtrack.R.string.mission_start_journey_title,
            descriptionRes = com.gymtrack.R.string.mission_start_journey_description,
            target = 5,
            xpReward = 100,
            repeatable = false,
        ),
        MissionDefinition(
            id = MissionIds.GROWING_STRENGTH,
            type = MissionType.ONE_TIME,
            titleRes = com.gymtrack.R.string.mission_growing_strength_title,
            descriptionRes = com.gymtrack.R.string.mission_growing_strength_description,
            target = 5,
            xpReward = 150,
            repeatable = false,
        ),
        MissionDefinition(
            id = MissionIds.VETERAN,
            type = MissionType.ONE_TIME,
            titleRes = com.gymtrack.R.string.mission_veteran_title,
            descriptionRes = com.gymtrack.R.string.mission_veteran_description,
            target = 25,
            xpReward = 250,
            repeatable = false,
        ),
    )

    fun byId(id: String): MissionDefinition? = all.find { it.id == id }

    const val MAX_ACTIVE_DAILY = 2
    const val MAX_ACTIVE_WEEKLY = 3
}

data class MissionInstance(
    val id: Long = 0,
    val definitionId: String,
    val periodKey: String,
    val status: MissionStatus,
    val startedAtMillis: Long,
    val completedAtMillis: Long? = null,
)

/**
 * Resolved mission for UI: definition + instance + derived progress/target.
 */
data class MissionProgressView(
    val definition: MissionDefinition,
    val instance: MissionInstance,
    val progress: Int,
    val target: Int,
) {
    val isCompleted: Boolean
        get() = instance.status == MissionStatus.COMPLETED || progress >= target

    val progressFraction: Float
        get() = if (target <= 0) {
            0f
        } else {
            (progress.toFloat() / target.toFloat()).coerceIn(0f, 1f)
        }
}

fun missionPeriodKey(
    definition: MissionDefinition,
    nowMillis: Long,
    zoneId: java.time.ZoneId,
): String = when (definition.type) {
    MissionType.DAILY -> {
        java.time.Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate().toString()
    }
    MissionType.WEEKLY -> isoWeekKey(nowMillis, zoneId)
    MissionType.LONG_TERM, MissionType.ONE_TIME -> MissionIds.ONE_TIME_PERIOD_KEY
}

fun localDateKey(epochMillis: Long, zoneId: java.time.ZoneId): String =
    java.time.Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate().toString()

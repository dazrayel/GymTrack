package com.gymtrack.domain.model

/**
 * XP event kinds for Phase 5.2. Amounts are fixed product values.
 */
enum class XpEventType(val amount: Int) {
    WORKOUT_COMPLETED(100),
    PLANNED_WORKOUT_COMPLETED(25),
    FIRST_WORKOUT_OF_WEEK(25),
    PROGRESSION_WEIGHT(30),
    PROGRESSION_REPS(20),
    PERSONAL_RECORD(50),
    /** Amount comes from [MissionDefinition.xpReward], not this enum default. */
    MISSION_COMPLETED(0),
}

data class XpEvent(
    val id: Long = 0,
    val type: XpEventType,
    val amount: Int,
    val sourceKey: String,
    val createdAtMillis: Long,
)

object XpSourceKeys {
    fun workoutCompleted(sessionId: Long): String =
        "WORKOUT_COMPLETED:session:$sessionId"

    fun plannedWorkoutCompleted(sessionId: Long): String =
        "PLANNED_WORKOUT_COMPLETED:session:$sessionId"

    fun firstWorkoutOfWeek(isoWeekKey: String): String =
        "FIRST_WORKOUT_OF_WEEK:$isoWeekKey"

    fun progressionWeight(sessionId: Long, exerciseName: String): String =
        "PROGRESSION_WEIGHT:session:$sessionId:exercise:$exerciseName"

    fun progressionReps(sessionId: Long, exerciseName: String): String =
        "PROGRESSION_REPS:session:$sessionId:exercise:$exerciseName"

    fun personalRecord(sessionId: Long, exerciseName: String): String =
        "PERSONAL_RECORD:session:$sessionId:exercise:$exerciseName"

    fun missionCompleted(definitionId: String, periodKey: String): String =
        "MISSION_COMPLETED:$definitionId:$periodKey"
}

fun isoWeekKey(epochMillis: Long, zoneId: java.time.ZoneId): String {
    val date = java.time.Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDate()
    val weekFields = java.time.temporal.WeekFields.ISO
    val week = date.get(weekFields.weekOfWeekBasedYear())
    val year = date.get(weekFields.weekBasedYear())
    return "$year-W${week.toString().padStart(2, '0')}"
}

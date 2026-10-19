package com.gymtrack.presentation.workouts.summary

import com.gymtrack.domain.model.AchievementCatalog
import com.gymtrack.domain.model.AchievementStatus
import com.gymtrack.domain.model.CompletedSetRecord
import com.gymtrack.domain.model.ExerciseProgress
import com.gymtrack.domain.model.SessionMissionReward
import com.gymtrack.domain.model.SessionProgressionHighlight
import com.gymtrack.domain.model.SessionPrHighlight
import com.gymtrack.domain.model.SessionXpBreakdown
import com.gymtrack.domain.model.WorkoutSession
import com.gymtrack.domain.model.WorkoutSessionExercise
import com.gymtrack.domain.model.WorkoutSessionExerciseStatus
import com.gymtrack.domain.model.WorkoutSet
import com.gymtrack.domain.model.XpEvent
import com.gymtrack.domain.model.elapsedMillis
import com.gymtrack.domain.model.exerciseProgress
import com.gymtrack.domain.model.exerciseVolume
import com.gymtrack.domain.model.missionRewardsNearSessionEnd
import com.gymtrack.domain.model.newlyUnlockedAchievementIds
import com.gymtrack.domain.model.progressionHighlightsFromXp
import com.gymtrack.domain.model.workoutProgress
import com.gymtrack.domain.model.workoutVolume
import com.gymtrack.domain.model.xpEventsForSession
import com.gymtrack.domain.time.formatLocalDate
import com.gymtrack.domain.time.formatLocalTime
import java.time.ZoneId

data class SessionExerciseSummary(
    val exerciseName: String,
    val muscleGroup: String,
    val completedSets: Int,
    val plannedSets: Int,
    val sets: List<WorkoutSet>,
    val volume: Double,
    val progress: ExerciseProgress,
    val isComplete: Boolean,
    val isSkipped: Boolean,
)

data class WorkoutSessionSummaryUiState(
    val sessionId: Long,
    val session: WorkoutSession? = null,
    val exercises: List<WorkoutSessionExercise> = emptyList(),
    val setsByExerciseId: Map<Long, List<WorkoutSet>> = emptyMap(),
    val completedSetHistory: List<CompletedSetRecord> = emptyList(),
    val xpEvents: List<XpEvent> = emptyList(),
    val achievementsWithSession: List<AchievementStatus> = emptyList(),
    val achievementsWithoutSession: List<AchievementStatus> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val sessionNotFound: Boolean = false,
    val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    val sessionXp: SessionXpBreakdown
        get() = xpEventsForSession(xpEvents, sessionId)

    val missionRewards: List<SessionMissionReward>
        get() = missionRewardsNearSessionEnd(xpEvents, session?.endedAtMillis)

    val unlockedAchievementIds: List<String>
        get() = newlyUnlockedAchievementIds(
            withSession = achievementsWithSession,
            withoutSession = achievementsWithoutSession,
        )

    val unlockedAchievementDefinitions
        get() = unlockedAchievementIds.mapNotNull { id ->
            AchievementCatalog.find { it.id == id }
        }

    val progressionHighlights: List<SessionProgressionHighlight>
        get() = progressionHighlightsFromXp(xpEvents, sessionId)

    val prHighlights: List<SessionPrHighlight>
        get() = exerciseSummaries.mapNotNull { summary ->
            val weight = summary.progress.sessionBestWeight ?: return@mapNotNull null
            val reps = summary.progress.sessionBestReps ?: return@mapNotNull null
            val delta = summary.progress.weightDelta
            if (delta == null || delta <= 0.0) return@mapNotNull null
            SessionPrHighlight(
                exerciseName = summary.exerciseName,
                weight = weight,
                reps = reps,
            )
        }

    val hasRewardsSection: Boolean
        get() = !sessionXp.isEmpty ||
            missionRewards.isNotEmpty() ||
            unlockedAchievementIds.isNotEmpty() ||
            progressionHighlights.isNotEmpty() ||
            prHighlights.isNotEmpty()

    val durationMillis: Long
        get() = elapsedMillis(session, nowMillis = session?.endedAtMillis ?: 0L)

    val startedDate: String
        get() = session?.let { formatLocalDate(it.startedAtMillis, zoneId) }.orEmpty()

    val startedTime: String
        get() = session?.let { formatLocalTime(it.startedAtMillis, zoneId) }.orEmpty()

    val endedTime: String
        get() = session?.endedAtMillis?.let { formatLocalTime(it, zoneId) }.orEmpty()

    val progress
        get() = workoutProgress(exercises, setsByExerciseId)

    val volume: Double
        get() = workoutVolume(setsByExerciseId)

    val canNavigateToWorkout: Boolean
        get() = session?.workoutId != null

    val exerciseSummaries: List<SessionExerciseSummary>
        get() {
            val sid = session?.id ?: sessionId
            val occurredAt = session?.endedAtMillis ?: session?.startedAtMillis ?: 0L
            val currentRecords = exercises.flatMap { exercise ->
                setsByExerciseId[exercise.id].orEmpty().map { set ->
                    CompletedSetRecord(
                        sessionId = sid,
                        occurredAtMillis = occurredAt,
                        exerciseName = exercise.exerciseName,
                        reps = set.reps,
                        weight = set.weight,
                    )
                }
            }
            val history = completedSetHistory.filter { it.sessionId != sid } + currentRecords
            return exercises.map { exercise ->
                val sets = setsByExerciseId[exercise.id].orEmpty().sortedBy { it.setIndex }
                SessionExerciseSummary(
                    exerciseName = exercise.exerciseName,
                    muscleGroup = exercise.muscleGroup,
                    completedSets = sets.size,
                    plannedSets = exercise.plannedSets,
                    sets = sets,
                    volume = exerciseVolume(sets),
                    progress = exerciseProgress(
                        exerciseName = exercise.exerciseName,
                        currentSessionId = sid,
                        currentOccurredAtMillis = occurredAt,
                        history = history,
                    ),
                    isComplete = sets.size >= exercise.plannedSets,
                    isSkipped = exercise.status == WorkoutSessionExerciseStatus.SKIPPED &&
                        sets.size < exercise.plannedSets,
                )
            }
        }
}

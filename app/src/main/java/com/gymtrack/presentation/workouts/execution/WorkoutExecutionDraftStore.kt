package com.gymtrack.presentation.workouts.execution

import dagger.hilt.android.scopes.ActivityRetainedScoped
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

data class ExerciseInputDraft(
    val repsInput: String,
    val weightInput: String,
)

@ActivityRetainedScoped
class WorkoutExecutionDraftStore @Inject constructor() {

    private val drafts = ConcurrentHashMap<DraftKey, ExerciseInputDraft>()

    fun get(sessionId: Long, exerciseId: Long): ExerciseInputDraft? =
        drafts[DraftKey(sessionId, exerciseId)]

    fun put(sessionId: Long, exerciseId: Long, repsInput: String, weightInput: String) {
        drafts[DraftKey(sessionId, exerciseId)] = ExerciseInputDraft(repsInput, weightInput)
    }

    fun clear(sessionId: Long, exerciseId: Long) {
        drafts.remove(DraftKey(sessionId, exerciseId))
    }

    fun clearSession(sessionId: Long) {
        drafts.keys.filter { it.sessionId == sessionId }.forEach { drafts.remove(it) }
    }
}

private data class DraftKey(
    val sessionId: Long,
    val exerciseId: Long,
)

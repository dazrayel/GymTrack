package com.gymtrack.domain.repository

import com.gymtrack.domain.model.WeeklyWorkoutPlan
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

interface WeeklyPlanRepository {

    fun observePlans(): Flow<List<WeeklyWorkoutPlan>>

    /**
     * Assigns [workoutId] to [dayOfWeek], replacing any existing plan for that day.
     * Does not create workout sessions or touch history.
     */
    suspend fun assignWorkout(dayOfWeek: DayOfWeek, workoutId: Long)

    /**
     * Clears the plan for [dayOfWeek]. No-op if the day was empty.
     */
    suspend fun clearDay(dayOfWeek: DayOfWeek)
}

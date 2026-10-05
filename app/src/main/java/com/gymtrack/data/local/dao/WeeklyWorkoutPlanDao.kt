package com.gymtrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gymtrack.data.local.entity.WeeklyWorkoutPlanEntity
import com.gymtrack.data.local.entity.WeeklyWorkoutPlanRow
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyWorkoutPlanDao {

    @Query(
        """
        SELECT p.dayOfWeek AS dayOfWeek,
               p.workoutId AS workoutId,
               w.name AS workoutName
        FROM weekly_workout_plans AS p
        INNER JOIN workouts AS w ON w.id = p.workoutId
        ORDER BY p.dayOfWeek ASC
        """,
    )
    fun observeAllWithWorkoutNames(): Flow<List<WeeklyWorkoutPlanRow>>

    @Query("SELECT * FROM weekly_workout_plans WHERE dayOfWeek = :dayOfWeek LIMIT 1")
    fun getByDay(dayOfWeek: Int): WeeklyWorkoutPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entity: WeeklyWorkoutPlanEntity)

    @Query("DELETE FROM weekly_workout_plans WHERE dayOfWeek = :dayOfWeek")
    fun deleteByDay(dayOfWeek: Int): Int

    @Query("DELETE FROM weekly_workout_plans WHERE workoutId = :workoutId")
    fun deleteByWorkoutId(workoutId: Long): Int

    @Query("SELECT COUNT(*) FROM weekly_workout_plans")
    fun count(): Int
}

package com.gymtrack.data.repository

import com.gymtrack.data.local.dao.WeeklyWorkoutPlanDao
import com.gymtrack.data.local.entity.WeeklyWorkoutPlanEntity
import com.gymtrack.data.local.entity.WeeklyWorkoutPlanRow
import com.gymtrack.domain.model.WeeklyWorkoutPlan
import com.gymtrack.domain.repository.WeeklyPlanRepository
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class WeeklyPlanRepositoryImpl @Inject constructor(
    private val planDao: WeeklyWorkoutPlanDao,
) : WeeklyPlanRepository {

    override fun observePlans(): Flow<List<WeeklyWorkoutPlan>> =
        planDao.observeAllWithWorkoutNames().map { rows -> rows.map { it.toDomain() } }

    override suspend fun assignWorkout(dayOfWeek: DayOfWeek, workoutId: Long) {
        withContext(Dispatchers.IO) {
            planDao.upsert(
                WeeklyWorkoutPlanEntity(
                    dayOfWeek = dayOfWeek.value,
                    workoutId = workoutId,
                ),
            )
        }
    }

    override suspend fun clearDay(dayOfWeek: DayOfWeek) {
        withContext(Dispatchers.IO) {
            planDao.deleteByDay(dayOfWeek.value)
        }
    }
}

private fun WeeklyWorkoutPlanRow.toDomain() = WeeklyWorkoutPlan(
    dayOfWeek = DayOfWeek.of(dayOfWeek),
    workoutId = workoutId,
    workoutName = workoutName,
)

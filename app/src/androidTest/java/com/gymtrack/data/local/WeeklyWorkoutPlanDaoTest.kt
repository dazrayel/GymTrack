package com.gymtrack.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.data.local.dao.WeeklyWorkoutPlanDao
import com.gymtrack.data.local.dao.WorkoutDao
import com.gymtrack.data.local.entity.WeeklyWorkoutPlanEntity
import com.gymtrack.data.local.entity.WorkoutEntity
import com.gymtrack.data.repository.WeeklyPlanRepositoryImpl
import com.gymtrack.domain.model.WeeklyWorkoutPlan
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.DayOfWeek
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WeeklyWorkoutPlanDaoTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var db: GymTrackDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var planDao: WeeklyWorkoutPlanDao

    @Before
    fun setUp() {
        hiltRule.inject()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        workoutDao = db.workoutDao()
        planDao = db.weeklyWorkoutPlanDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsert_assignsWorkoutToDay() = runBlocking {
        val workoutId = workoutDao.insert(WorkoutEntity(name = "Peito", description = ""))
        planDao.upsert(WeeklyWorkoutPlanEntity(dayOfWeek = DayOfWeek.MONDAY.value, workoutId = workoutId))

        val rows = planDao.observeAllWithWorkoutNames().first()
        assertEquals(1, rows.size)
        assertEquals(DayOfWeek.MONDAY.value, rows.single().dayOfWeek)
        assertEquals("Peito", rows.single().workoutName)
    }

    @Test
    fun upsert_sameDay_replacesWithoutDuplicate() = runBlocking {
        val a = workoutDao.insert(WorkoutEntity(name = "A", description = ""))
        val b = workoutDao.insert(WorkoutEntity(name = "B", description = ""))
        planDao.upsert(WeeklyWorkoutPlanEntity(DayOfWeek.FRIDAY.value, a))
        planDao.upsert(WeeklyWorkoutPlanEntity(DayOfWeek.FRIDAY.value, b))

        assertEquals(1, planDao.count())
        assertEquals(b, planDao.getByDay(DayOfWeek.FRIDAY.value)?.workoutId)
    }

    @Test
    fun deleteByDay_clearsPlan() = runBlocking {
        val workoutId = workoutDao.insert(WorkoutEntity(name = "A", description = ""))
        planDao.upsert(WeeklyWorkoutPlanEntity(DayOfWeek.TUESDAY.value, workoutId))
        planDao.deleteByDay(DayOfWeek.TUESDAY.value)

        assertNull(planDao.getByDay(DayOfWeek.TUESDAY.value))
        assertEquals(0, planDao.count())
    }

    @Test
    fun deleteWorkout_cascadesAndRemovesPlan() = runBlocking {
        val workoutId = workoutDao.insert(WorkoutEntity(name = "A", description = ""))
        planDao.upsert(WeeklyWorkoutPlanEntity(DayOfWeek.WEDNESDAY.value, workoutId))
        assertEquals(1, planDao.count())

        workoutDao.deleteById(workoutId)

        assertEquals(0, planDao.count())
        assertTrue(planDao.observeAllWithWorkoutNames().first().isEmpty())
    }

    @Test
    fun repository_assignAndObserve_roundTrip() = runBlocking {
        val workoutId = workoutDao.insert(WorkoutEntity(name = "Costas", description = ""))
        val repository = WeeklyPlanRepositoryImpl(planDao)

        repository.assignWorkout(DayOfWeek.THURSDAY, workoutId)
        val plans: List<WeeklyWorkoutPlan> = repository.observePlans().first()

        assertEquals(1, plans.size)
        assertEquals(DayOfWeek.THURSDAY, plans.single().dayOfWeek)
        assertEquals(workoutId, plans.single().workoutId)
        assertEquals("Costas", plans.single().workoutName)

        repository.clearDay(DayOfWeek.THURSDAY)
        assertTrue(repository.observePlans().first().isEmpty())
    }
}

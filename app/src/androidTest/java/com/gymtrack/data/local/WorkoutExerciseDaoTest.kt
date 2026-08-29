package com.gymtrack.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.data.local.dao.ExerciseDao
import com.gymtrack.data.local.dao.WorkoutDao
import com.gymtrack.data.local.dao.WorkoutExerciseDao
import com.gymtrack.data.local.entity.ExerciseEntity
import com.gymtrack.data.local.entity.WorkoutEntity
import com.gymtrack.data.local.entity.WorkoutExerciseEntity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WorkoutExerciseDaoTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var db: GymTrackDatabase
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var workoutDao: WorkoutDao
    private lateinit var dao: WorkoutExerciseDao

    private var workoutId: Long = 0L
    private var exerciseId: Long = 0L

    @Before
    fun setUp() {
        hiltRule.inject()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        exerciseDao = db.exerciseDao()
        workoutDao = db.workoutDao()
        dao = db.workoutExerciseDao()

        workoutId = workoutDao.insert(WorkoutEntity(name = "Test Workout", description = ""))
        exerciseId = exerciseDao.insert(ExerciseEntity(name = "Bench Press", muscleGroup = "Chest", equipmentType = "Barbell"))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun makeEntity(position: Int = 0, exerciseId: Long = this.exerciseId) =
        WorkoutExerciseEntity(
            workoutId = workoutId,
            exerciseId = exerciseId,
            position = position,
            sets = 3,
            minRepetitions = 8,
            maxRepetitions = 12,
            weight = 60.0,
            restSeconds = 90,
            notes = "",
        )

    // ─── Inserção ────────────────────────────────────────────────────────────

    @Test
    fun insert_returnsPositiveRowId() {
        val id = dao.insert(makeEntity())
        assertTrue("Row ID should be positive", id > 0)
    }

    @Test
    fun insert_allFieldsPersistedCorrectly() = runBlocking {
        val entity = WorkoutExerciseEntity(
            workoutId = workoutId,
            exerciseId = exerciseId,
            position = 2,
            sets = 4,
            minRepetitions = 6,
            maxRepetitions = 10,
            weight = 100.5,
            restSeconds = 120,
            notes = "Última série até a falha",
        )

        val id = dao.insert(entity)
        val result = dao.getById(id).first()

        assertNotNull(result)
        assertEquals(id, result!!.id)
        assertEquals(workoutId, result.workoutId)
        assertEquals(exerciseId, result.exerciseId)
        assertEquals(2, result.position)
        assertEquals(4, result.sets)
        assertEquals(6, result.minRepetitions)
        assertEquals(10, result.maxRepetitions)
        assertEquals(100.5, result.weight, 0.001)
        assertEquals(120, result.restSeconds)
        assertEquals("Última série até a falha", result.notes)
    }

    // ─── Busca por workout ───────────────────────────────────────────────────

    @Test
    fun getByWorkoutId_emptyWorkout_returnsEmptyList() = runBlocking {
        val result = dao.getByWorkoutId(workoutId).first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun getByWorkoutId_returnsOnlyExercisesForThatWorkout() = runBlocking {
        val otherWorkoutId = workoutDao.insert(WorkoutEntity(name = "Other Workout", description = ""))

        dao.insert(makeEntity(position = 0))
        dao.insert(WorkoutExerciseEntity(
            workoutId = otherWorkoutId,
            exerciseId = exerciseId,
            position = 0,
            sets = 3, minRepetitions = 8, maxRepetitions = 12,
            weight = 60.0, restSeconds = 90,
        ))

        val result = dao.getByWorkoutId(workoutId).first()

        assertEquals(1, result.size)
        assertEquals(workoutId, result[0].workoutId)
    }

    @Test
    fun getByWorkoutId_isSortedByPositionAscending() = runBlocking {
        val ex2 = exerciseDao.insert(ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"))
        val ex3 = exerciseDao.insert(ExerciseEntity(name = "Deadlift", muscleGroup = "Back", equipmentType = "Barbell"))

        dao.insert(makeEntity(position = 2, exerciseId = exerciseId))
        dao.insert(makeEntity(position = 0, exerciseId = ex2))
        dao.insert(makeEntity(position = 1, exerciseId = ex3))

        val result = dao.getByWorkoutId(workoutId).first()

        assertEquals(3, result.size)
        assertEquals(0, result[0].position)
        assertEquals(1, result[1].position)
        assertEquals(2, result[2].position)
    }

    // ─── Busca por ID ────────────────────────────────────────────────────────

    @Test
    fun getById_returnsCorrectEntity() = runBlocking {
        dao.insert(makeEntity(position = 0))
        val id = dao.insert(makeEntity(position = 1))

        val result = dao.getById(id).first()

        assertNotNull(result)
        assertEquals(id, result!!.id)
        assertEquals(1, result.position)
    }

    @Test
    fun getById_forNonExistentId_returnsNull() = runBlocking {
        val result = dao.getById(999L).first()
        assertNull(result)
    }

    // ─── Atualização ─────────────────────────────────────────────────────────

    @Test
    fun insert_withExistingId_updatesEntity() = runBlocking {
        val id = dao.insert(makeEntity(position = 0))

        dao.insert(WorkoutExerciseEntity(
            id = id,
            workoutId = workoutId,
            exerciseId = exerciseId,
            position = 3,
            sets = 5,
            minRepetitions = 5,
            maxRepetitions = 8,
            weight = 80.0,
            restSeconds = 180,
            notes = "Drop set",
        ))

        val result = dao.getById(id).first()
        assertNotNull(result)
        assertEquals(3, result!!.position)
        assertEquals(5, result.sets)
        assertEquals(5, result.minRepetitions)
        assertEquals(8, result.maxRepetitions)
        assertEquals(80.0, result.weight, 0.001)
        assertEquals(180, result.restSeconds)
        assertEquals("Drop set", result.notes)
    }

    @Test
    fun insert_withExistingId_doesNotCreateDuplicate() = runBlocking {
        val id = dao.insert(makeEntity(position = 0))
        dao.insert(WorkoutExerciseEntity(
            id = id,
            workoutId = workoutId,
            exerciseId = exerciseId,
            position = 1,
            sets = 3, minRepetitions = 8, maxRepetitions = 12,
            weight = 60.0, restSeconds = 90,
        ))

        assertEquals(1, dao.getByWorkoutId(workoutId).first().size)
    }

    // ─── Exclusão por ID ─────────────────────────────────────────────────────

    @Test
    fun deleteById_removesOnlyTargetExercise() = runBlocking {
        val ex2 = exerciseDao.insert(ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"))
        val id1 = dao.insert(makeEntity(position = 0, exerciseId = exerciseId))
        dao.insert(makeEntity(position = 1, exerciseId = ex2))

        dao.deleteById(id1)

        val remaining = dao.getByWorkoutId(workoutId).first()
        assertEquals(1, remaining.size)
        assertEquals(1, remaining[0].position)
    }

    @Test
    fun deleteById_returnsAffectedRowCount() {
        val id = dao.insert(makeEntity())
        val deletedCount = dao.deleteById(id)
        assertEquals(1, deletedCount)
    }

    @Test
    fun deleteById_forNonExistentId_returnsZero() {
        val count = dao.deleteById(999L)
        assertEquals(0, count)
    }

    // ─── Exclusão de todos por workout ───────────────────────────────────────

    @Test
    fun deleteByWorkoutId_removesAllExercisesForWorkout() = runBlocking {
        val ex2 = exerciseDao.insert(ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"))
        dao.insert(makeEntity(position = 0, exerciseId = exerciseId))
        dao.insert(makeEntity(position = 1, exerciseId = ex2))

        dao.deleteByWorkoutId(workoutId)

        assertTrue(dao.getByWorkoutId(workoutId).first().isEmpty())
    }

    @Test
    fun deleteByWorkoutId_preservesExercisesOfOtherWorkouts() = runBlocking {
        val otherWorkoutId = workoutDao.insert(WorkoutEntity(name = "Other", description = ""))
        dao.insert(makeEntity(position = 0))
        dao.insert(WorkoutExerciseEntity(
            workoutId = otherWorkoutId, exerciseId = exerciseId, position = 0,
            sets = 3, minRepetitions = 8, maxRepetitions = 12, weight = 60.0, restSeconds = 90,
        ))

        dao.deleteByWorkoutId(workoutId)

        assertEquals(1, dao.getByWorkoutId(otherWorkoutId).first().size)
    }

    // ─── Foreign Keys e CASCADE ───────────────────────────────────────────────

    @Test
    fun foreignKey_workout_cascadeDeleteRemovesWorkoutExercises() = runBlocking {
        dao.insert(makeEntity(position = 0))
        assertEquals(1, dao.getByWorkoutId(workoutId).first().size)

        workoutDao.deleteById(workoutId)

        assertTrue(dao.getByWorkoutId(workoutId).first().isEmpty())
    }

    @Test
    fun foreignKey_exercise_cascadeDeleteRemovesAssociation() = runBlocking {
        dao.insert(makeEntity(position = 0))
        assertEquals(1, dao.getByWorkoutId(workoutId).first().size)

        exerciseDao.deleteById(exerciseId)

        assertTrue(dao.getByWorkoutId(workoutId).first().isEmpty())
    }

    @Test
    fun foreignKey_cascadeOnWorkout_preservesExercisesOfOtherWorkouts() = runBlocking {
        val otherWorkoutId = workoutDao.insert(WorkoutEntity(name = "Other", description = ""))
        dao.insert(makeEntity(position = 0))
        dao.insert(WorkoutExerciseEntity(
            workoutId = otherWorkoutId, exerciseId = exerciseId, position = 0,
            sets = 3, minRepetitions = 8, maxRepetitions = 12, weight = 60.0, restSeconds = 90,
        ))

        workoutDao.deleteById(workoutId)

        assertEquals(1, dao.getByWorkoutId(otherWorkoutId).first().size)
    }

    // ─── Atualização de position ─────────────────────────────────────────────

    @Test
    fun updatePosition_updatesOnlyPosition() = runBlocking {
        val id = dao.insert(
            WorkoutExerciseEntity(
                workoutId = workoutId,
                exerciseId = exerciseId,
                position = 0,
                sets = 4,
                minRepetitions = 6,
                maxRepetitions = 10,
                weight = 80.0,
                restSeconds = 120,
                notes = "Manter",
            ),
        )

        val affected = dao.updatePosition(id, 3)
        assertEquals(1, affected)

        val result = dao.getById(id).first()!!
        assertEquals(3, result.position)
        assertEquals(workoutId, result.workoutId)
        assertEquals(exerciseId, result.exerciseId)
        assertEquals(4, result.sets)
        assertEquals(6, result.minRepetitions)
        assertEquals(10, result.maxRepetitions)
        assertEquals(80.0, result.weight, 0.001)
        assertEquals(120, result.restSeconds)
        assertEquals("Manter", result.notes)
    }

    @Test
    fun updatePosition_returnsAffectedRowCount() {
        val id = dao.insert(makeEntity())
        assertEquals(1, dao.updatePosition(id, 5))
        assertEquals(0, dao.updatePosition(999L, 1))
    }

    @Test
    fun updatePositions_appliesAllMappingsAtomically() = runBlocking {
        val ex2 = exerciseDao.insert(ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"))
        val id0 = dao.insert(makeEntity(position = 0, exerciseId = exerciseId))
        val id1 = dao.insert(makeEntity(position = 1, exerciseId = ex2))

        dao.updatePositions(mapOf(id0 to 1, id1 to 0))

        val result = dao.getByWorkoutId(workoutId).first()
        assertEquals(2, result.size)
        assertEquals(id1, result[0].id)
        assertEquals(0, result[0].position)
        assertEquals(id0, result[1].id)
        assertEquals(1, result[1].position)
    }
}

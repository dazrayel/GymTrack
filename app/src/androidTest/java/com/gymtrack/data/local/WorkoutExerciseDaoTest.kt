package com.gymtrack.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.data.local.dao.ExerciseDao
import com.gymtrack.data.local.dao.WorkoutBlockDao
import com.gymtrack.data.local.dao.WorkoutDao
import com.gymtrack.data.local.dao.WorkoutExerciseDao
import com.gymtrack.data.local.entity.ExerciseEntity
import com.gymtrack.data.local.entity.WorkoutBlockEntity
import com.gymtrack.data.local.entity.WorkoutEntity
import com.gymtrack.data.local.entity.WorkoutExerciseEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutExerciseDaoTest {

    private lateinit var db: GymTrackDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var blockDao: WorkoutBlockDao
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var workoutExerciseDao: WorkoutExerciseDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        workoutDao = db.workoutDao()
        blockDao = db.workoutBlockDao()
        exerciseDao = db.exerciseDao()
        workoutExerciseDao = db.workoutExerciseDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndReadByBlockPreservesOrder() = runBlocking {
        val workoutId = workoutDao.insert(WorkoutEntity(name = "A"))
        val ex1 = exerciseDao.insert(ExerciseEntity(name = "E1", muscleGroup = "Peitoral", equipmentType = "Barra"))
        val ex2 = exerciseDao.insert(ExerciseEntity(name = "E2", muscleGroup = "Tríceps", equipmentType = "Corda"))
        val blockId = blockDao.insert(
            WorkoutBlockEntity(workoutId = workoutId, position = 0, type = "BI_SET", rounds = 3, restSeconds = 60),
        )
        workoutExerciseDao.insert(
            WorkoutExerciseEntity(blockId = blockId, exerciseId = ex1, positionInBlock = 0, minRepetitions = 8, maxRepetitions = 12, weight = 20.0),
        )
        workoutExerciseDao.insert(
            WorkoutExerciseEntity(blockId = blockId, exerciseId = ex2, positionInBlock = 1, minRepetitions = 10, maxRepetitions = 12, weight = 15.0),
        )

        val items = workoutExerciseDao.getByBlockIdOnce(blockId)
        assertEquals(2, items.size)
        assertEquals(0, items[0].positionInBlock)
        assertEquals(1, items[1].positionInBlock)
        assertEquals(ex1, items[0].exerciseId)
        assertEquals(ex2, items[1].exerciseId)
    }

    @Test
    fun deleteBlockCascadesExercises() = runBlocking {
        val workoutId = workoutDao.insert(WorkoutEntity(name = "A"))
        val exId = exerciseDao.insert(ExerciseEntity(name = "E1", muscleGroup = "Peitoral", equipmentType = "Barra"))
        val blockId = blockDao.insert(
            WorkoutBlockEntity(workoutId = workoutId, position = 0, type = "SINGLE", rounds = 3, restSeconds = 60),
        )
        workoutExerciseDao.insert(
            WorkoutExerciseEntity(blockId = blockId, exerciseId = exId, positionInBlock = 0, minRepetitions = 8, maxRepetitions = 10, weight = 0.0),
        )
        blockDao.deleteById(blockId)
        assertEquals(0, workoutExerciseDao.getByWorkoutId(workoutId).first().size)
    }
}

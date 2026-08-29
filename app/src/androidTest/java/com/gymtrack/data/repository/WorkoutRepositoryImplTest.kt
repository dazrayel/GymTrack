package com.gymtrack.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.data.local.GymTrackDatabase
import com.gymtrack.data.local.entity.ExerciseEntity
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutExercise
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

/**
 * Instrumented tests for WorkoutRepositoryImpl.
 *
 * Verifies the observable behavior of WorkoutRepository and validates that
 * Workout ↔ WorkoutEntity and WorkoutExercise ↔ WorkoutExerciseEntity mappings
 * are correct via round-trips. Mappers remain private and are tested indirectly.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryImplTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var db: GymTrackDatabase
    private lateinit var repository: WorkoutRepositoryImpl

    private var exerciseId: Long = 0L

    @Before
    fun setUp() {
        hiltRule.inject()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutRepositoryImpl(db.workoutDao(), db.workoutExerciseDao())

        exerciseId = db.exerciseDao().insert(
            ExerciseEntity(name = "Bench Press", muscleGroup = "Chest", equipmentType = "Barbell"),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ─── Workout: save / retrieve ─────────────────────────────────────────────

    @Test
    fun save_and_getAll_allFieldsMappedCorrectly() = runBlocking {
        val workout = Workout(name = "Push Day", description = "Chest and triceps")

        val savedId = repository.save(workout)

        val all = repository.getAll().first()
        assertEquals(1, all.size)
        val result = all[0]
        assertEquals(savedId, result.id)
        assertEquals("Push Day", result.name)
        assertEquals("Chest and triceps", result.description)
    }

    @Test
    fun save_withIdZero_generatesPositiveId() = runBlocking {
        val id = repository.save(Workout(id = 0, name = "Leg Day"))
        assertTrue("Generated ID should be positive", id > 0)
    }

    @Test
    fun save_withEmptyDescription_preservesEmptyString() = runBlocking {
        val id = repository.save(Workout(name = "Full Body", description = ""))

        val result = repository.getById(id).first()
        assertNotNull(result)
        assertEquals("", result!!.description)
    }

    @Test
    fun getById_returnsMappedWorkoutWithAllFields() = runBlocking {
        val id = repository.save(Workout(name = "Pull Day", description = "Back and biceps"))

        val result = repository.getById(id).first()

        assertNotNull(result)
        assertEquals(id, result!!.id)
        assertEquals("Pull Day", result.name)
        assertEquals("Back and biceps", result.description)
    }

    @Test
    fun getById_forNonExistentId_returnsNull() = runBlocking {
        val result = repository.getById(9999L).first()
        assertNull(result)
    }

    // ─── Workout: update ──────────────────────────────────────────────────────

    @Test
    fun update_changesWorkoutFields() = runBlocking {
        val id = repository.save(Workout(name = "Push Day", description = "Original"))

        repository.update(Workout(id = id, name = "Push Day Advanced", description = "Updated"))

        val result = repository.getById(id).first()
        assertNotNull(result)
        assertEquals("Push Day Advanced", result!!.name)
        assertEquals("Updated", result.description)
    }

    @Test
    fun update_doesNotCreateDuplicate() = runBlocking {
        val id = repository.save(Workout(name = "Push Day", description = ""))
        repository.update(Workout(id = id, name = "Push Day Updated", description = ""))

        assertEquals(1, repository.getAll().first().size)
    }

    @Test
    fun update_preservesId() = runBlocking {
        val id = repository.save(Workout(name = "Push Day", description = ""))
        repository.update(Workout(id = id, name = "New Name", description = "New Desc"))

        val result = repository.getById(id).first()
        assertNotNull(result)
        assertEquals(id, result!!.id)
    }

    // ─── Workout: delete ──────────────────────────────────────────────────────

    @Test
    fun delete_removesWorkoutFromStorage() = runBlocking {
        val id = repository.save(Workout(name = "Push Day"))
        val saved = repository.getById(id).first()!!

        repository.delete(saved)

        assertNull(repository.getById(id).first())
        assertTrue(repository.getAll().first().isEmpty())
    }

    @Test
    fun delete_preservesOtherWorkouts() = runBlocking {
        val id1 = repository.save(Workout(name = "Push Day"))
        repository.save(Workout(name = "Pull Day"))

        val toDelete = repository.getById(id1).first()!!
        repository.delete(toDelete)

        val remaining = repository.getAll().first()
        assertEquals(1, remaining.size)
        assertEquals("Pull Day", remaining[0].name)
    }

    // ─── WorkoutExercise: add / retrieve ─────────────────────────────────────

    @Test
    fun addExercise_and_getExercises_allFieldsMappedCorrectly() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        val we = WorkoutExercise(
            workoutId = workoutId,
            exerciseId = exerciseId,
            position = 0,
            sets = 3,
            minRepetitions = 8,
            maxRepetitions = 12,
            weight = 60.0,
            restSeconds = 90,
            notes = "Última série até a falha",
        )

        val savedId = repository.addExercise(we)

        val exercises = repository.getExercises(workoutId).first()
        assertEquals(1, exercises.size)
        val result = exercises[0]
        assertEquals(savedId, result.id)
        assertEquals(workoutId, result.workoutId)
        assertEquals(exerciseId, result.exerciseId)
        assertEquals(0, result.position)
        assertEquals(3, result.sets)
        assertEquals(8, result.minRepetitions)
        assertEquals(12, result.maxRepetitions)
        assertEquals(60.0, result.weight, 0.001)
        assertEquals(90, result.restSeconds)
        assertEquals("Última série até a falha", result.notes)
    }

    @Test
    fun getExercises_isSortedByPosition() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        val ex2 = db.exerciseDao().insert(ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"))

        repository.addExercise(WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, position = 2, sets = 3, minRepetitions = 8, maxRepetitions = 12, weight = 60.0, restSeconds = 90))
        repository.addExercise(WorkoutExercise(workoutId = workoutId, exerciseId = ex2, position = 0, sets = 3, minRepetitions = 8, maxRepetitions = 12, weight = 80.0, restSeconds = 120))

        val result = repository.getExercises(workoutId).first()
        assertEquals(2, result.size)
        assertEquals(0, result[0].position)
        assertEquals(2, result[1].position)
    }

    @Test
    fun getExercises_emptyWorkout_returnsEmptyList() = runBlocking {
        val workoutId = repository.save(Workout(name = "Empty Workout"))
        assertTrue(repository.getExercises(workoutId).first().isEmpty())
    }

    // ─── WorkoutExercise: update ──────────────────────────────────────────────

    @Test
    fun updateExercise_changesFields() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        val id = repository.addExercise(
            WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, position = 0, sets = 3, minRepetitions = 8, maxRepetitions = 12, weight = 60.0, restSeconds = 90),
        )

        repository.updateExercise(
            WorkoutExercise(id = id, workoutId = workoutId, exerciseId = exerciseId, position = 1, sets = 5, minRepetitions = 5, maxRepetitions = 8, weight = 80.0, restSeconds = 180, notes = "Drop set"),
        )

        val result = repository.getExercises(workoutId).first().first()
        assertEquals(1, result.position)
        assertEquals(5, result.sets)
        assertEquals(5, result.minRepetitions)
        assertEquals(8, result.maxRepetitions)
        assertEquals(80.0, result.weight, 0.001)
        assertEquals(180, result.restSeconds)
        assertEquals("Drop set", result.notes)
    }

    @Test
    fun updateExercise_doesNotCreateDuplicate() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        val id = repository.addExercise(
            WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, position = 0, sets = 3, minRepetitions = 8, maxRepetitions = 12, weight = 60.0, restSeconds = 90),
        )

        repository.updateExercise(
            WorkoutExercise(id = id, workoutId = workoutId, exerciseId = exerciseId, position = 1, sets = 4, minRepetitions = 6, maxRepetitions = 10, weight = 70.0, restSeconds = 120),
        )

        assertEquals(1, repository.getExercises(workoutId).first().size)
    }

    // ─── WorkoutExercise: remove ──────────────────────────────────────────────

    @Test
    fun removeExercise_removesItFromWorkout() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        val id = repository.addExercise(
            WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, position = 0, sets = 3, minRepetitions = 8, maxRepetitions = 12, weight = 60.0, restSeconds = 90),
        )
        val saved = repository.getExercises(workoutId).first()[0]

        repository.removeExercise(saved)

        assertTrue(repository.getExercises(workoutId).first().isEmpty())
    }

    @Test
    fun removeExerciseById_removesTargetExercise() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        val ex2 = db.exerciseDao().insert(ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"))
        val id1 = repository.addExercise(WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, position = 0, sets = 3, minRepetitions = 8, maxRepetitions = 12, weight = 60.0, restSeconds = 90))
        repository.addExercise(WorkoutExercise(workoutId = workoutId, exerciseId = ex2, position = 1, sets = 3, minRepetitions = 8, maxRepetitions = 12, weight = 80.0, restSeconds = 120))

        repository.removeExerciseById(id1)

        val remaining = repository.getExercises(workoutId).first()
        assertEquals(1, remaining.size)
        assertEquals(1, remaining[0].position)
    }

    // ─── Retorna domain objects, não entities ─────────────────────────────────

    @Test
    fun getAll_returnsDomainWorkouts() = runBlocking {
        repository.save(Workout(name = "Push Day"))

        val result = repository.getAll().first()
        assertTrue(result.all { it is Workout })
    }

    @Test
    fun getExercises_returnsDomainWorkoutExercises() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        repository.addExercise(WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, position = 0, sets = 3, minRepetitions = 8, maxRepetitions = 12, weight = 60.0, restSeconds = 90))

        val result = repository.getExercises(workoutId).first()
        assertTrue(result.all { it is WorkoutExercise })
    }

    // ─── WorkoutExercise: update positions ────────────────────────────────────

    @Test
    fun updateExercisePositions_updatesMultipleExercises() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        val ex2 = db.exerciseDao().insert(ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"))
        val id0 = repository.addExercise(
            WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, position = 0, sets = 3, minRepetitions = 8, maxRepetitions = 12, weight = 60.0, restSeconds = 90),
        )
        val id1 = repository.addExercise(
            WorkoutExercise(workoutId = workoutId, exerciseId = ex2, position = 1, sets = 4, minRepetitions = 6, maxRepetitions = 10, weight = 80.0, restSeconds = 120, notes = "Notas"),
        )

        repository.updateExercisePositions(mapOf(id0 to 1, id1 to 0))

        val result = repository.getExercises(workoutId).first()
        assertEquals(2, result.size)
        assertEquals(id1, result[0].id)
        assertEquals(0, result[0].position)
        assertEquals(id0, result[1].id)
        assertEquals(1, result[1].position)
    }

    @Test
    fun updateExercisePositions_preservesOtherFields() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        val id = repository.addExercise(
            WorkoutExercise(
                workoutId = workoutId,
                exerciseId = exerciseId,
                position = 0,
                sets = 4,
                minRepetitions = 6,
                maxRepetitions = 10,
                weight = 75.5,
                restSeconds = 90,
                notes = "Manter",
            ),
        )

        repository.updateExercisePositions(mapOf(id to 2))

        val result = repository.getExercises(workoutId).first().first()
        assertEquals(2, result.position)
        assertEquals(workoutId, result.workoutId)
        assertEquals(exerciseId, result.exerciseId)
        assertEquals(4, result.sets)
        assertEquals(6, result.minRepetitions)
        assertEquals(10, result.maxRepetitions)
        assertEquals(75.5, result.weight, 0.001)
        assertEquals(90, result.restSeconds)
        assertEquals("Manter", result.notes)
    }

    @Test
    fun updateExercisePositions_doesNotAffectOtherWorkout() = runBlocking {
        val workoutA = repository.save(Workout(name = "A"))
        val workoutB = repository.save(Workout(name = "B"))
        val ex2 = db.exerciseDao().insert(ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"))
        val idA = repository.addExercise(
            WorkoutExercise(workoutId = workoutA, exerciseId = exerciseId, position = 0, sets = 3, minRepetitions = 8, maxRepetitions = 12, weight = 60.0, restSeconds = 90),
        )
        val idB = repository.addExercise(
            WorkoutExercise(workoutId = workoutB, exerciseId = ex2, position = 0, sets = 5, minRepetitions = 5, maxRepetitions = 8, weight = 100.0, restSeconds = 180),
        )

        repository.updateExercisePositions(mapOf(idA to 3))

        val fromA = repository.getExercises(workoutA).first().first()
        val fromB = repository.getExercises(workoutB).first().first()
        assertEquals(3, fromA.position)
        assertEquals(idB, fromB.id)
        assertEquals(0, fromB.position)
        assertEquals(5, fromB.sets)
    }

    @Test
    fun updateExercisePositions_emptyMap_doesNotChangeData() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        repository.addExercise(
            WorkoutExercise(workoutId = workoutId, exerciseId = exerciseId, position = 0, sets = 3, minRepetitions = 8, maxRepetitions = 12, weight = 60.0, restSeconds = 90),
        )

        repository.updateExercisePositions(emptyMap())

        val result = repository.getExercises(workoutId).first()
        assertEquals(1, result.size)
        assertEquals(0, result[0].position)
        assertEquals(3, result[0].sets)
    }
}

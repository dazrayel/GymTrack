package com.gymtrack.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.data.local.GymTrackDatabase
import com.gymtrack.data.local.entity.ExerciseEntity
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.model.WorkoutSessionStatus
import com.gymtrack.domain.repository.WorkoutSessionRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class WorkoutSessionRepositoryImplTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var db: GymTrackDatabase
    private lateinit var workoutRepository: WorkoutRepositoryImpl
    private lateinit var exerciseRepository: ExerciseRepositoryImpl
    private lateinit var repository: WorkoutSessionRepository

    private var exerciseId: Long = 0L
    private var squatId: Long = 0L
    private var workoutId: Long = 0L

    @Before
    fun setUp() {
        hiltRule.inject()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        workoutRepository = WorkoutRepositoryImpl(db.workoutDao(), db.workoutExerciseDao())
        exerciseRepository = ExerciseRepositoryImpl(db.exerciseDao())
        repository = WorkoutSessionRepositoryImpl(
            db,
            db.workoutSessionDao(),
            db.workoutSessionExerciseDao(),
            db.workoutSetDao(),
            workoutRepository,
            exerciseRepository,
            com.gymtrack.domain.time.SystemTimeProvider(),
        )

        runBlocking {
            exerciseId = db.exerciseDao().insert(
                ExerciseEntity(name = "Bench Press", muscleGroup = "Chest", equipmentType = "Barbell"),
            )
            squatId = db.exerciseDao().insert(
                ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"),
            )
            workoutId = workoutRepository.save(Workout(name = "Push Day", description = "Chest focus"))
            workoutRepository.addExercise(
                WorkoutExercise(
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                    position = 0,
                    sets = 3,
                    minRepetitions = 8,
                    maxRepetitions = 12,
                    weight = 60.0,
                    restSeconds = 90,
                    notes = "pause",
                ),
            )
            workoutRepository.addExercise(
                WorkoutExercise(
                    workoutId = workoutId,
                    exerciseId = squatId,
                    position = 1,
                    sets = 4,
                    minRepetitions = 6,
                    maxRepetitions = 10,
                    weight = 80.0,
                    restSeconds = 120,
                    notes = "",
                ),
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun startSession_createsSession() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        val session = repository.getSession(sessionId)
        assertNotNull(session)
        assertEquals(sessionId, session!!.id)
        assertEquals(WorkoutSessionStatus.IN_PROGRESS, session.status)
        assertEquals(workoutId, session.workoutId)
    }

    @Test
    fun startSession_setsStartedAt() = runBlocking {
        val before = System.currentTimeMillis()
        val sessionId = repository.startSession(workoutId)
        val after = System.currentTimeMillis()
        val startedAt = repository.getSession(sessionId)!!.startedAtMillis
        assertTrue(startedAt in before..after)
        assertNull(repository.getSession(sessionId)!!.endedAtMillis)
    }

    @Test
    fun startSession_snapshotsWorkout() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        val session = repository.getSession(sessionId)!!
        assertEquals("Push Day", session.workoutName)
        assertEquals("Chest focus", session.workoutDescription)
    }

    @Test
    fun startSession_snapshotsAllExercises() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        val exercises = repository.observeSessionExercises(sessionId).first()
        assertEquals(2, exercises.size)
        val bench = exercises[0]
        assertEquals("Bench Press", bench.exerciseName)
        assertEquals("Chest", bench.muscleGroup)
        assertEquals("Barbell", bench.equipmentType)
        assertEquals(exerciseId, bench.exerciseId)
        assertEquals(3, bench.plannedSets)
        assertEquals(8, bench.minRepetitions)
        assertEquals(12, bench.maxRepetitions)
        assertEquals(60.0, bench.plannedWeight, 0.001)
        assertEquals(90, bench.restSeconds)
        assertEquals("pause", bench.notes)
        assertEquals("Squat", exercises[1].exerciseName)
        assertEquals(4, exercises[1].plannedSets)
    }

    @Test
    fun startSession_preservesExerciseOrder() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        val exercises = repository.observeSessionExercises(sessionId).first()
        assertEquals(listOf(0, 1), exercises.map { it.position })
        assertEquals(listOf("Bench Press", "Squat"), exercises.map { it.exerciseName })
    }

    @Test
    fun startSession_rejectsEmptyWorkout() = runBlocking {
        val emptyId = workoutRepository.save(Workout(name = "Empty", description = ""))
        try {
            repository.startSession(emptyId)
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("no exercises"))
        }
        assertNull(repository.observeInProgress().first())
    }

    @Test
    fun startSession_workoutNotFound_throws() = runBlocking {
        try {
            repository.startSession(999_999L)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("not found"))
        }
    }

    @Test
    fun startSession_doesNotCreateSecondInProgressSession() = runBlocking {
        val first = repository.startSession(workoutId)
        val otherWorkout = workoutRepository.save(Workout(name = "Other", description = ""))
        workoutRepository.addExercise(
            WorkoutExercise(
                workoutId = otherWorkout,
                exerciseId = exerciseId,
                position = 0,
                sets = 1,
                minRepetitions = 1,
                maxRepetitions = 1,
                weight = 1.0,
                restSeconds = 0,
            ),
        )
        val second = repository.startSession(otherWorkout)
        assertEquals(first, second)
        val exercises = repository.observeSessionExercises(first).first()
        assertEquals(2, exercises.size)
        assertEquals("Push Day", repository.getSession(first)!!.workoutName)
    }

    @Test
    fun completeSet_persistsSet() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        val sessionExerciseId = repository.observeSessionExercises(sessionId).first()[0].id
        val setId = repository.completeSet(sessionExerciseId, setIndex = 0, reps = 10, weight = 60.0)
        assertTrue(setId > 0)
        val sets = repository.observeSets(sessionExerciseId).first()
        assertEquals(1, sets.size)
        assertEquals(setId, sets[0].id)
    }

    @Test
    fun completeSet_persistsCorrectValues() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        val sessionExerciseId = repository.observeSessionExercises(sessionId).first()[0].id
        val before = System.currentTimeMillis()
        repository.completeSet(sessionExerciseId, setIndex = 1, reps = 8, weight = 62.5)
        val after = System.currentTimeMillis()
        val set = repository.observeSets(sessionExerciseId).first().single()
        assertEquals(sessionExerciseId, set.sessionExerciseId)
        assertEquals(1, set.setIndex)
        assertEquals(8, set.reps)
        assertEquals(62.5, set.weight, 0.001)
        assertTrue(set.completedAtMillis in before..after)
    }

    @Test
    fun finishSession_setsCompletedStatus() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        repository.finishSession(sessionId)
        assertEquals(WorkoutSessionStatus.COMPLETED, repository.getSession(sessionId)!!.status)
        assertNull(repository.observeInProgress().first())
    }

    @Test
    fun finishSession_setsEndedAt() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        val before = System.currentTimeMillis()
        repository.finishSession(sessionId)
        val after = System.currentTimeMillis()
        val endedAt = repository.getSession(sessionId)!!.endedAtMillis
        assertNotNull(endedAt)
        assertTrue(endedAt!! in before..after)
    }

    @Test
    fun finishSession_doesNotModifySnapshot() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        val beforeExercises = repository.observeSessionExercises(sessionId).first()
        repository.finishSession(sessionId)
        val session = repository.getSession(sessionId)!!
        assertEquals("Push Day", session.workoutName)
        assertEquals("Chest focus", session.workoutDescription)
        val afterExercises = repository.observeSessionExercises(sessionId).first()
        assertEquals(beforeExercises, afterExercises)
    }

    @Test
    fun startSession_snapshotUnchangedWhenTemplateIsEdited() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        val we = workoutRepository.getExercises(workoutId).first()[0]
        workoutRepository.updateExercise(we.copy(sets = 99, notes = "changed", weight = 1.0))

        val session = repository.getSession(sessionId)!!
        assertEquals("Push Day", session.workoutName)
        assertEquals("Chest focus", session.workoutDescription)
        val snapshot = repository.observeSessionExercises(sessionId).first()[0]
        assertEquals(3, snapshot.plannedSets)
        assertEquals("pause", snapshot.notes)
        assertEquals(60.0, snapshot.plannedWeight, 0.001)
        assertEquals(99, workoutRepository.getExercises(workoutId).first().first { it.id == we.id }.sets)
    }

    @Test
    fun startSession_snapshotSurvivesExerciseDeletion() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        exerciseRepository.delete(
            com.gymtrack.domain.model.Exercise(
                id = exerciseId,
                name = "Bench Press",
                muscleGroup = "Chest",
                equipmentType = "Barbell",
            ),
        )
        val snapshot = repository.observeSessionExercises(sessionId).first()
            .first { it.exerciseName == "Bench Press" }
        assertEquals("Bench Press", snapshot.exerciseName)
        assertEquals("Chest", snapshot.muscleGroup)
        assertNull(snapshot.exerciseId)
        assertEquals(3, snapshot.plannedSets)
    }

    @Test
    fun completeSet_isolatesSessions() = runBlocking {
        val sessionA = repository.startSession(workoutId)
        val exerciseA = repository.observeSessionExercises(sessionA).first()[0].id
        repository.completeSet(exerciseA, setIndex = 0, reps = 10, weight = 60.0)
        repository.finishSession(sessionA)

        val otherWorkout = workoutRepository.save(Workout(name = "Other", description = ""))
        workoutRepository.addExercise(
            WorkoutExercise(
                workoutId = otherWorkout,
                exerciseId = squatId,
                position = 0,
                sets = 2,
                minRepetitions = 5,
                maxRepetitions = 5,
                weight = 40.0,
                restSeconds = 30,
            ),
        )
        val sessionB = repository.startSession(otherWorkout)
        val exerciseB = repository.observeSessionExercises(sessionB).first()[0].id
        repository.completeSet(exerciseB, setIndex = 0, reps = 5, weight = 40.0)

        assertEquals(1, repository.observeSets(exerciseA).first().size)
        assertEquals(10, repository.observeSets(exerciseA).first()[0].reps)
        assertEquals(1, repository.observeSets(exerciseB).first().size)
        assertEquals(5, repository.observeSets(exerciseB).first()[0].reps)
        assertEquals("Push Day", repository.getSession(sessionA)!!.workoutName)
        assertEquals("Other", repository.getSession(sessionB)!!.workoutName)
    }

    @Test
    fun startRest_persistsAbsoluteTimestamp() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        val exerciseId = repository.observeSessionExercises(sessionId).first()[0].id
        val before = System.currentTimeMillis()
        repository.startRest(sessionId, exerciseId, afterSetIndex = 0, restSeconds = 90)
        val session = repository.getSession(sessionId)!!
        assertNotNull(session.restEndsAtMillis)
        assertTrue(session.restEndsAtMillis!! >= before + 80_000)
        assertNull(session.restPausedRemainingMillis)
        assertEquals(exerciseId, session.restSessionExerciseId)
        assertEquals(0, session.restAfterSetIndex)
        assertEquals(WorkoutSessionStatus.IN_PROGRESS, session.status)
    }

    @Test
    fun startRest_zeroSeconds_doesNotCreateRest() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        val exerciseId = repository.observeSessionExercises(sessionId).first()[0].id
        repository.startRest(sessionId, exerciseId, afterSetIndex = 0, restSeconds = 0)
        val session = repository.getSession(sessionId)!!
        assertNull(session.restEndsAtMillis)
        assertNull(session.restSessionExerciseId)
    }

    @Test
    fun pauseAndResumeRest_roundTrip() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        val exerciseId = repository.observeSessionExercises(sessionId).first()[0].id
        repository.startRest(sessionId, exerciseId, afterSetIndex = 1, restSeconds = 120)
        repository.pauseRest(sessionId)
        val paused = repository.getSession(sessionId)!!
        assertNull(paused.restEndsAtMillis)
        assertNotNull(paused.restPausedRemainingMillis)
        assertTrue(paused.restPausedRemainingMillis!! > 0)
        assertEquals(WorkoutSessionStatus.IN_PROGRESS, paused.status)
        repository.resumeRest(sessionId)
        val resumed = repository.getSession(sessionId)!!
        assertNotNull(resumed.restEndsAtMillis)
        assertNull(resumed.restPausedRemainingMillis)
        assertEquals(exerciseId, resumed.restSessionExerciseId)
    }

    @Test
    fun skipRest_clearsFields_keepsInProgress() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        val exerciseId = repository.observeSessionExercises(sessionId).first()[0].id
        repository.startRest(sessionId, exerciseId, afterSetIndex = 0, restSeconds = 30)
        repository.skipRest(sessionId)
        val session = repository.getSession(sessionId)!!
        assertNull(session.restEndsAtMillis)
        assertNull(session.restPausedRemainingMillis)
        assertNull(session.restSessionExerciseId)
        assertNull(session.restAfterSetIndex)
        assertEquals(WorkoutSessionStatus.IN_PROGRESS, session.status)
        assertNull(session.endedAtMillis)
    }

    @Test
    fun finishSession_preservesStartedAt() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        val startedAt = repository.getSession(sessionId)!!.startedAtMillis
        repository.finishSession(sessionId)
        assertEquals(startedAt, repository.getSession(sessionId)!!.startedAtMillis)
    }

    @Test
    fun finishSession_preservesSets() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        val exerciseId = repository.observeSessionExercises(sessionId).first()[0].id
        repository.completeSet(exerciseId, setIndex = 0, reps = 10, weight = 60.0)
        repository.finishSession(sessionId)
        val sets = repository.observeSets(exerciseId).first()
        assertEquals(1, sets.size)
        assertEquals(10, sets[0].reps)
        assertEquals(60.0, sets[0].weight, 0.001)
    }

    @Test
    fun finishSession_clearsRest() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        val exerciseId = repository.observeSessionExercises(sessionId).first()[0].id
        repository.startRest(sessionId, exerciseId, afterSetIndex = 0, restSeconds = 90)
        repository.finishSession(sessionId)
        val session = repository.getSession(sessionId)!!
        assertEquals(WorkoutSessionStatus.COMPLETED, session.status)
        assertNull(session.restEndsAtMillis)
        assertNull(session.restPausedRemainingMillis)
        assertNull(session.restSessionExerciseId)
        assertNull(session.restAfterSetIndex)
        assertNull(repository.observeInProgress().first())
    }

    @Test
    fun finishSession_partialSession_keepsPerformedSetsOnly() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        val exerciseId = repository.observeSessionExercises(sessionId).first()[0].id
        repository.completeSet(exerciseId, setIndex = 0, reps = 8, weight = 60.0)
        repository.finishSession(sessionId)
        val exercises = repository.observeSessionExercises(sessionId).first()
        assertEquals(2, exercises.size)
        assertEquals(1, repository.observeSets(exerciseId).first().size)
        assertEquals(0, repository.observeSets(exercises[1].id).first().size)
        assertEquals(WorkoutSessionStatus.COMPLETED, repository.getSession(sessionId)!!.status)
    }

    @Test
    fun finishSession_idempotent_doesNotChangeEndedAt() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        repository.finishSession(sessionId)
        val firstEndedAt = repository.getSession(sessionId)!!.endedAtMillis
        repository.finishSession(sessionId)
        val second = repository.getSession(sessionId)!!
        assertEquals(WorkoutSessionStatus.COMPLETED, second.status)
        assertEquals(firstEndedAt, second.endedAtMillis)
    }

    @Test
    fun finishSession_doesNotAffectOtherSession() = runBlocking {
        val sessionA = repository.startSession(workoutId)
        val exerciseA = repository.observeSessionExercises(sessionA).first()[0].id
        repository.completeSet(exerciseA, setIndex = 0, reps = 10, weight = 60.0)
        repository.finishSession(sessionA)

        val otherWorkout = workoutRepository.save(Workout(name = "Other", description = ""))
        workoutRepository.addExercise(
            WorkoutExercise(
                workoutId = otherWorkout,
                exerciseId = squatId,
                position = 0,
                sets = 2,
                minRepetitions = 5,
                maxRepetitions = 5,
                weight = 40.0,
                restSeconds = 30,
            ),
        )
        val sessionB = repository.startSession(otherWorkout)
        repository.finishSession(sessionB)

        assertEquals(WorkoutSessionStatus.COMPLETED, repository.getSession(sessionA)!!.status)
        assertEquals("Push Day", repository.getSession(sessionA)!!.workoutName)
        assertEquals(1, repository.observeSets(exerciseA).first().size)
        assertEquals("Other", repository.getSession(sessionB)!!.workoutName)
        assertEquals(WorkoutSessionStatus.COMPLETED, repository.getSession(sessionB)!!.status)
    }

    @Test
    fun finishSession_summarySurvivesDeletedTemplate() = runBlocking {
        val sessionId = repository.startSession(workoutId)
        val snapshot = repository.observeSessionExercises(sessionId).first()
        repository.completeSet(snapshot[0].id, setIndex = 0, reps = 8, weight = 60.0)
        repository.finishSession(sessionId)
        workoutRepository.delete(Workout(id = workoutId, name = "Push Day", description = ""))
        val session = repository.getSession(sessionId)!!
        assertEquals(WorkoutSessionStatus.COMPLETED, session.status)
        assertEquals("Push Day", session.workoutName)
        val exercises = repository.observeSessionExercises(sessionId).first()
        assertEquals("Bench Press", exercises[0].exerciseName)
        assertEquals(1, repository.observeSets(exercises[0].id).first().size)
    }
}

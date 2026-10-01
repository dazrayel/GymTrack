package com.gymtrack.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.data.local.GymTrackDatabase
import com.gymtrack.data.local.entity.ExerciseEntity
import com.gymtrack.domain.model.StartSessionResult
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutBlock
import com.gymtrack.domain.model.WorkoutBlockType
import com.gymtrack.domain.model.WorkoutExercise
import com.gymtrack.domain.model.WorkoutSessionStatus
import com.gymtrack.data.local.entity.WorkoutSessionEntity
import com.gymtrack.domain.repository.WorkoutSessionRepository
import com.gymtrack.domain.time.TimeProvider
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
        workoutRepository = WorkoutRepositoryImpl(
            db,
            db.workoutDao(),
            db.workoutBlockDao(),
            db.workoutExerciseDao(),
        )
        exerciseRepository = ExerciseRepositoryImpl(
            db,
            db.exerciseDao(),
            db.exerciseSecondaryMuscleDao(),
        )
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
            workoutRepository.addBlock(
                WorkoutBlock(workoutId = workoutId, position = 0, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 60),
                listOf(WorkoutExercise(blockId = 0, exerciseId = exerciseId, positionInBlock = 0, minRepetitions = 8, maxRepetitions = 12, weight = 60.0, notes = "pause")),
            )
            workoutRepository.addBlock(
                WorkoutBlock(workoutId = workoutId, position = 0, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 60),
                listOf(WorkoutExercise(blockId = 0, exerciseId = squatId, positionInBlock = 0, minRepetitions = 6, maxRepetitions = 10, weight = 80.0, notes = "")),
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun startSession_createsSession() = runBlocking {
        val result = repository.startSession(workoutId)
        assertTrue(result is StartSessionResult.Created)
        val sessionId = (result as StartSessionResult.Created).sessionId
        val session = repository.getSession(sessionId)
        assertNotNull(session)
        assertEquals(sessionId, session!!.id)
        assertEquals(WorkoutSessionStatus.IN_PROGRESS, session.status)
        assertEquals(workoutId, session.workoutId)
        assertEquals(1, db.workoutSessionDao().getByIdOnce(sessionId)!!.inProgressLock)
    }

    @Test
    fun startSession_setsStartedAt() = runBlocking {
        val before = System.currentTimeMillis()
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val after = System.currentTimeMillis()
        val startedAt = repository.getSession(sessionId)!!.startedAtMillis
        assertTrue(startedAt in before..after)
        assertNull(repository.getSession(sessionId)!!.endedAtMillis)
    }

    @Test
    fun startSession_usesTimeProviderForStartedAt() = runBlocking {
        val now = 1_700_000_000_000L
        val timedRepository = sessionRepositoryWith(FakeTimeProvider(now))
        val sessionId = timedRepository.startSession(workoutId).requireSessionId()
        assertEquals(now, timedRepository.getSession(sessionId)!!.startedAtMillis)
        assertNull(timedRepository.getSession(sessionId)!!.endedAtMillis)
    }

    @Test
    fun startSession_snapshotsWorkout() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val session = repository.getSession(sessionId)!!
        assertEquals("Push Day", session.workoutName)
        assertEquals("Chest focus", session.workoutDescription)
    }

    @Test
    fun startSession_snapshotsAllExercises() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
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
        assertEquals(emptyList<String>(), bench.secondaryMuscles)
        assertEquals(com.gymtrack.domain.model.WorkoutSessionExerciseStatus.PENDING, bench.status)
        assertEquals("Squat", exercises[1].exerciseName)
        assertEquals(4, exercises[1].plannedSets)
    }

    @Test
    fun skipSessionExercise_marksSkippedWithoutCreatingSet() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val bench = repository.observeSessionExercises(sessionId).first()[0]
        repository.skipSessionExercise(bench.id)
        val after = repository.observeSessionExercises(sessionId).first()[0]
        assertEquals(com.gymtrack.domain.model.WorkoutSessionExerciseStatus.SKIPPED, after.status)
        assertTrue(repository.observeSets(bench.id).first().isEmpty())
        assertEquals(3, after.plannedSets)
        assertEquals(8, after.minRepetitions)
        assertEquals(12, after.maxRepetitions)
    }

    @Test
    fun skipSessionExercise_keepsExistingSets() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val bench = repository.observeSessionExercises(sessionId).first()[0]
        repository.completeSet(bench.id, setIndex = 0, reps = 10, weight = 60.0)
        repository.completeSet(bench.id, setIndex = 1, reps = 8, weight = 60.0)
        repository.skipSessionExercise(bench.id)
        val after = repository.observeSessionExercises(sessionId).first()[0]
        assertEquals(com.gymtrack.domain.model.WorkoutSessionExerciseStatus.SKIPPED, after.status)
        assertEquals(2, repository.observeSets(bench.id).first().size)
        assertEquals(
            com.gymtrack.domain.model.WorkoutSessionExerciseStatus.SKIPPED,
            after.status,
        )
    }

    @Test
    fun resumeSessionExercise_marksInProgress() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val bench = repository.observeSessionExercises(sessionId).first()[0]
        repository.skipSessionExercise(bench.id)
        repository.resumeSessionExercise(bench.id)
        val after = repository.observeSessionExercises(sessionId).first()[0]
        assertEquals(com.gymtrack.domain.model.WorkoutSessionExerciseStatus.IN_PROGRESS, after.status)
    }

    @Test
    fun resumeSessionExercise_fromPending_doesNotSkipOthersOrCreateSets() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val exercises = repository.observeSessionExercises(sessionId).first()
        val bench = exercises[0]
        val squat = exercises[1]
        repository.resumeSessionExercise(squat.id)
        val after = repository.observeSessionExercises(sessionId).first()
        assertEquals(com.gymtrack.domain.model.WorkoutSessionExerciseStatus.PENDING, after[0].status)
        assertEquals(com.gymtrack.domain.model.WorkoutSessionExerciseStatus.IN_PROGRESS, after[1].status)
        assertTrue(repository.observeSets(bench.id).first().isEmpty())
        assertTrue(repository.observeSets(squat.id).first().isEmpty())
    }

    @Test
    fun resumeSessionExercise_persistsInProgress_afterNewRepositoryInstance() = runBlocking {
        val curlId = db.exerciseDao().insert(
            ExerciseEntity(name = "Curl", muscleGroup = "Arms", equipmentType = "Dumbbell"),
        )
        workoutRepository.addBlock(
                WorkoutBlock(workoutId = workoutId, position = 0, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 60),
                listOf(WorkoutExercise(blockId = 0, exerciseId = curlId, positionInBlock = 0, minRepetitions = 8, maxRepetitions = 12, weight = 10.0, notes = "")),
            )
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val exercises = repository.observeSessionExercises(sessionId).first()
        val curl = exercises[2]
        repository.resumeSessionExercise(curl.id)

        val reread = WorkoutSessionRepositoryImpl(
            db,
            db.workoutSessionDao(),
            db.workoutSessionExerciseDao(),
            db.workoutSetDao(),
            workoutRepository,
            exerciseRepository,
            com.gymtrack.domain.time.SystemTimeProvider(),
        )
        val after = reread.observeSessionExercises(sessionId).first()
        assertEquals(com.gymtrack.domain.model.WorkoutSessionExerciseStatus.PENDING, after[0].status)
        assertEquals(com.gymtrack.domain.model.WorkoutSessionExerciseStatus.PENDING, after[1].status)
        assertEquals(com.gymtrack.domain.model.WorkoutSessionExerciseStatus.IN_PROGRESS, after[2].status)
        assertEquals(1, after.count { it.status == com.gymtrack.domain.model.WorkoutSessionExerciseStatus.IN_PROGRESS })
        assertTrue(reread.observeSets(after[2].id).first().isEmpty())
    }

    @Test
    fun finishSession_doesNotRewriteMixedExerciseStatuses() = runBlocking {
        val curlId = db.exerciseDao().insert(
            ExerciseEntity(name = "Finish Curl", muscleGroup = "Arms", equipmentType = "Dumbbell"),
        )
        workoutRepository.addBlock(
                WorkoutBlock(workoutId = workoutId, position = 0, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 60),
                listOf(WorkoutExercise(blockId = 0, exerciseId = curlId, positionInBlock = 0, minRepetitions = 8, maxRepetitions = 12, weight = 10.0, notes = "")),
            )
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val exercises = repository.observeSessionExercises(sessionId).first()
        val bench = exercises[0]
        val squat = exercises[1]
        val curl = exercises[2]
        repeat(4) { index ->
            repository.completeSet(squat.id, setIndex = index, reps = 5, weight = 40.0)
        }
        repository.skipSessionExercise(curl.id)
        repository.finishSession(sessionId)

        val after = repository.observeSessionExercises(sessionId).first()
        assertEquals(com.gymtrack.domain.model.WorkoutSessionExerciseStatus.PENDING, after.first { it.id == bench.id }.status)
        assertEquals(com.gymtrack.domain.model.WorkoutSessionExerciseStatus.COMPLETED, after.first { it.id == squat.id }.status)
        assertEquals(com.gymtrack.domain.model.WorkoutSessionExerciseStatus.SKIPPED, after.first { it.id == curl.id }.status)
        assertEquals(WorkoutSessionStatus.COMPLETED, repository.getSession(sessionId)!!.status)
        assertTrue(repository.observeSets(bench.id).first().isEmpty())
        assertEquals(4, repository.observeSets(squat.id).first().size)
        assertTrue(repository.observeSets(curl.id).first().isEmpty())
    }

    @Test
    fun completeAllSets_marksCompletedNotSkipped() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val squat = repository.observeSessionExercises(sessionId).first()[1]
        repeat(4) { index ->
            repository.completeSet(squat.id, setIndex = index, reps = 5, weight = 40.0)
        }
        val after = repository.observeSessionExercises(sessionId).first()[1]
        assertEquals(com.gymtrack.domain.model.WorkoutSessionExerciseStatus.COMPLETED, after.status)
    }

    @Test
    fun startSession_preservesExerciseOrder() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
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
    fun startSession_sameWorkout_resumesExisting() = runBlocking {
        val created = repository.startSession(workoutId)
        assertTrue(created is StartSessionResult.Created)
        val sessionId = (created as StartSessionResult.Created).sessionId

        val resumed = repository.startSession(workoutId)
        assertTrue(resumed is StartSessionResult.Resumed)
        assertEquals(sessionId, (resumed as StartSessionResult.Resumed).sessionId)
        assertEquals(sessionId, repository.observeInProgress().first()!!.id)
        assertEquals(1, db.workoutSessionDao().getByIdOnce(sessionId)!!.inProgressLock)
    }

    @Test
    fun startSession_otherWorkout_isBlockedWithoutCreatingSession() = runBlocking {
        val created = repository.startSession(workoutId)
        assertTrue(created is StartSessionResult.Created)
        val sessionA = (created as StartSessionResult.Created).sessionId
        val otherWorkout = workoutRepository.save(Workout(name = "Other", description = ""))
        workoutRepository.addBlock(
                WorkoutBlock(workoutId = otherWorkout, position = 0, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 60),
                listOf(WorkoutExercise(blockId = 0, exerciseId = exerciseId, positionInBlock = 0, minRepetitions = 1, maxRepetitions = 1, weight = 1.0, notes = "")),
            )
        val blocked = repository.startSession(otherWorkout)
        assertTrue(blocked is StartSessionResult.BlockedOtherWorkout)
        blocked as StartSessionResult.BlockedOtherWorkout
        assertEquals(sessionA, blocked.sessionId)
        assertEquals("Push Day", blocked.workoutName)
        assertEquals(sessionA, repository.observeInProgress().first()!!.id)
        assertEquals("Push Day", repository.getSession(sessionA)!!.workoutName)
        assertEquals(2, repository.observeSessionExercises(sessionA).first().size)
        assertEquals(1, db.workoutSessionDao().getByIdOnce(sessionA)!!.inProgressLock)
    }

    @Test
    fun startSession_nullWorkoutId_isBlockedAsOtherWorkout() = runBlocking {
        val created = repository.startSession(workoutId) as StartSessionResult.Created
        db.openHelper.writableDatabase.execSQL(
            "UPDATE workout_sessions SET workoutId = NULL WHERE id = ${created.sessionId}",
        )
        val otherWorkout = workoutRepository.save(Workout(name = "Other", description = ""))
        workoutRepository.addBlock(
                WorkoutBlock(workoutId = otherWorkout, position = 0, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 60),
                listOf(WorkoutExercise(blockId = 0, exerciseId = exerciseId, positionInBlock = 0, minRepetitions = 1, maxRepetitions = 1, weight = 1.0, notes = "")),
            )
        val blocked = repository.startSession(otherWorkout)
        assertTrue(blocked is StartSessionResult.BlockedOtherWorkout)
        blocked as StartSessionResult.BlockedOtherWorkout
        assertEquals(created.sessionId, blocked.sessionId)
        assertEquals("Push Day", blocked.workoutName)
        assertNull(repository.getSession(created.sessionId)!!.workoutId)
    }

    @Test
    fun insert_secondInProgressLock_isRejected() = runBlocking {
        repository.startSession(workoutId).requireSessionId()
        try {
            db.workoutSessionDao().insert(
                WorkoutSessionEntity(
                    workoutId = workoutId,
                    workoutName = "Race",
                    workoutDescription = "",
                    startedAtMillis = 1L,
                    endedAtMillis = null,
                    status = WorkoutSessionEntity.STATUS_IN_PROGRESS,
                    inProgressLock = 1,
                ),
            )
            fail("Second IN_PROGRESS row with inProgressLock = 1 should be rejected")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            // expected
        }
    }

    @Test
    fun completeSet_persistsSet() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val sessionExerciseId = repository.observeSessionExercises(sessionId).first()[0].id
        val setId = repository.completeSet(sessionExerciseId, setIndex = 0, reps = 10, weight = 60.0)
        assertTrue(setId > 0)
        val sets = repository.observeSets(sessionExerciseId).first()
        assertEquals(1, sets.size)
        assertEquals(setId, sets[0].id)
    }

    @Test
    fun completeSet_persistsCorrectValues() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
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
    fun completeSet_usesTimeProviderForCompletedAt() = runBlocking {
        val now = 1_700_000_000_500L
        val timedRepository = sessionRepositoryWith(FakeTimeProvider(now))
        val sessionId = timedRepository.startSession(workoutId).requireSessionId()
        val sessionExerciseId = timedRepository.observeSessionExercises(sessionId).first()[0].id
        timedRepository.completeSet(sessionExerciseId, setIndex = 0, reps = 8, weight = 60.0)
        val set = timedRepository.observeSets(sessionExerciseId).first().single()
        assertEquals(now, set.completedAtMillis)
    }

    @Test
    fun finishSession_setsCompletedStatus() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
        repository.finishSession(sessionId)
        assertEquals(WorkoutSessionStatus.COMPLETED, repository.getSession(sessionId)!!.status)
        assertNull(repository.observeInProgress().first())
    }

    @Test
    fun finishSession_setsEndedAt() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val before = System.currentTimeMillis()
        repository.finishSession(sessionId)
        val after = System.currentTimeMillis()
        val endedAt = repository.getSession(sessionId)!!.endedAtMillis
        assertNotNull(endedAt)
        assertTrue(endedAt!! in before..after)
    }

    @Test
    fun finishSession_doesNotModifySnapshot() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
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
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val we = workoutRepository.getExercises(workoutId).first()[0]
        val block = workoutRepository.getBlocks(workoutId).first().first { it.id == we.blockId }
        workoutRepository.updateBlock(block.copy(rounds = 99))
        workoutRepository.updateExercise(we.copy(notes = "changed", weight = 1.0))

        val session = repository.getSession(sessionId)!!
        assertEquals("Push Day", session.workoutName)
        assertEquals("Chest focus", session.workoutDescription)
        val snapshot = repository.observeSessionExercises(sessionId).first()[0]
        assertEquals(3, snapshot.plannedSets)
        assertEquals("pause", snapshot.notes)
        assertEquals(60.0, snapshot.plannedWeight, 0.001)
        assertEquals(99, workoutRepository.getBlocks(workoutId).first().first { it.id == we.blockId }.rounds)
    }

    @Test
    fun startSession_snapshotOrderUnchangedWhenTemplateIsReordered() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val before = repository.observeSessionExercises(sessionId).first()
        assertEquals(listOf("Bench Press", "Squat"), before.map { it.exerciseName })
        assertEquals(listOf(0, 1), before.map { it.blockPosition })

        val blocks = workoutRepository.getBlocks(workoutId).first()
        workoutRepository.updateBlockPositions(
            mapOf(blocks[0].id to 1, blocks[1].id to 0),
        )
        val templateOrder = workoutRepository.getExercises(workoutId).first().map { it.exerciseId }
        assertEquals(listOf(squatId, exerciseId), templateOrder)

        val after = repository.observeSessionExercises(sessionId).first()
        assertEquals(listOf("Bench Press", "Squat"), after.map { it.exerciseName })
        assertEquals(listOf(0, 1), after.map { it.position })
        assertEquals(listOf(0, 1), after.map { it.blockPosition })
    }

    @Test
    fun startSession_snapshotSurvivesExerciseDeletion() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
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
        val sessionA = repository.startSession(workoutId).requireSessionId()
        val exerciseA = repository.observeSessionExercises(sessionA).first()[0].id
        repository.completeSet(exerciseA, setIndex = 0, reps = 10, weight = 60.0)
        repository.finishSession(sessionA)

        val otherWorkout = workoutRepository.save(Workout(name = "Other", description = ""))
        workoutRepository.addBlock(
                WorkoutBlock(workoutId = otherWorkout, position = 0, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 60),
                listOf(WorkoutExercise(blockId = 0, exerciseId = squatId, positionInBlock = 0, minRepetitions = 5, maxRepetitions = 5, weight = 40.0, notes = "")),
            )
        val sessionB = repository.startSession(otherWorkout).requireSessionId()
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
        val sessionId = repository.startSession(workoutId).requireSessionId()
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
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val exerciseId = repository.observeSessionExercises(sessionId).first()[0].id
        repository.startRest(sessionId, exerciseId, afterSetIndex = 0, restSeconds = 0)
        val session = repository.getSession(sessionId)!!
        assertNull(session.restEndsAtMillis)
        assertNull(session.restSessionExerciseId)
    }

    @Test
    fun pauseAndResumeRest_roundTrip() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
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
        val sessionId = repository.startSession(workoutId).requireSessionId()
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
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val startedAt = repository.getSession(sessionId)!!.startedAtMillis
        repository.finishSession(sessionId)
        assertEquals(startedAt, repository.getSession(sessionId)!!.startedAtMillis)
    }

    @Test
    fun finishSession_preservesSets() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
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
        val sessionId = repository.startSession(workoutId).requireSessionId()
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
        val sessionId = repository.startSession(workoutId).requireSessionId()
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
        val sessionId = repository.startSession(workoutId).requireSessionId()
        repository.finishSession(sessionId)
        val firstEndedAt = repository.getSession(sessionId)!!.endedAtMillis
        repository.finishSession(sessionId)
        val second = repository.getSession(sessionId)!!
        assertEquals(WorkoutSessionStatus.COMPLETED, second.status)
        assertEquals(firstEndedAt, second.endedAtMillis)
    }

    @Test
    fun finishSession_doesNotAffectOtherSession() = runBlocking {
        val sessionA = repository.startSession(workoutId).requireSessionId()
        val exerciseA = repository.observeSessionExercises(sessionA).first()[0].id
        repository.completeSet(exerciseA, setIndex = 0, reps = 10, weight = 60.0)
        repository.finishSession(sessionA)

        val otherWorkout = workoutRepository.save(Workout(name = "Other", description = ""))
        workoutRepository.addBlock(
                WorkoutBlock(workoutId = otherWorkout, position = 0, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 60),
                listOf(WorkoutExercise(blockId = 0, exerciseId = squatId, positionInBlock = 0, minRepetitions = 5, maxRepetitions = 5, weight = 40.0, notes = "")),
            )
        val sessionB = repository.startSession(otherWorkout).requireSessionId()
        repository.finishSession(sessionB)

        assertEquals(WorkoutSessionStatus.COMPLETED, repository.getSession(sessionA)!!.status)
        assertEquals("Push Day", repository.getSession(sessionA)!!.workoutName)
        assertEquals(1, repository.observeSets(exerciseA).first().size)
        assertEquals("Other", repository.getSession(sessionB)!!.workoutName)
        assertEquals(WorkoutSessionStatus.COMPLETED, repository.getSession(sessionB)!!.status)
    }

    @Test
    fun finishSession_summarySurvivesDeletedTemplate() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
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

    @Test
    fun observeCompletedSessions_excludesInProgress() = runBlocking {
        repository.startSession(workoutId).requireSessionId()
        assertTrue(repository.observeCompletedSessions().first().isEmpty())
    }

    @Test
    fun observeCompletedSessions_mapsAggregatesAndDuration() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val exerciseId = repository.observeSessionExercises(sessionId).first()[0].id
        repository.completeSet(exerciseId, setIndex = 0, reps = 8, weight = 60.0)
        repository.finishSession(sessionId)

        val item = repository.observeCompletedSessions().first().single()
        val session = repository.getSession(sessionId)!!
        assertEquals(sessionId, item.sessionId)
        assertEquals("Push Day", item.workoutName)
        assertEquals(session.startedAtMillis, item.startedAtMillis)
        assertEquals(session.endedAtMillis, item.endedAtMillis)
        assertEquals(session.endedAtMillis!! - session.startedAtMillis, item.durationMillis)
        assertEquals(8 * 60.0, item.volume, 0.001)
        assertEquals(2, item.exerciseCount)
        assertEquals(1, item.completedSetCount)
        assertEquals(7, item.plannedSetCount)
    }

    @Test
    fun observeCompletedSessions_ordersNewestFirst() = runBlocking {
        val firstId = repository.startSession(workoutId).requireSessionId()
        repository.finishSession(firstId)

        val otherWorkout = workoutRepository.save(Workout(name = "Other", description = ""))
        workoutRepository.addBlock(
                WorkoutBlock(workoutId = otherWorkout, position = 0, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 60),
                listOf(WorkoutExercise(blockId = 0, exerciseId = squatId, positionInBlock = 0, minRepetitions = 5, maxRepetitions = 5, weight = 40.0, notes = "")),
            )
        val secondId = repository.startSession(otherWorkout).requireSessionId()
        repository.finishSession(secondId)

        val ids = repository.observeCompletedSessions().first().map { it.sessionId }
        assertEquals(listOf(secondId, firstId), ids)
        assertEquals("Other", repository.observeCompletedSessions().first()[0].workoutName)
        assertEquals("Push Day", repository.observeCompletedSessions().first()[1].workoutName)
    }

    @Test
    fun observeCompletedSessions_snapshotSurvivesTemplateRename() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
        repository.finishSession(sessionId)
        workoutRepository.update(Workout(id = workoutId, name = "Renamed Push", description = "x"))

        val item = repository.observeCompletedSessions().first().single()
        assertEquals(sessionId, item.sessionId)
        assertEquals("Push Day", item.workoutName)
        assertEquals("Renamed Push", workoutRepository.getById(workoutId).first()!!.name)
    }

    @Test
    fun observeCompletedSessions_survivesExerciseDeletion() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val sessionExerciseId = repository.observeSessionExercises(sessionId).first()[0].id
        repository.completeSet(sessionExerciseId, setIndex = 0, reps = 10, weight = 50.0)
        repository.finishSession(sessionId)
        exerciseRepository.delete(
            com.gymtrack.domain.model.Exercise(
                id = exerciseId,
                name = "Bench Press",
                muscleGroup = "Chest",
                equipmentType = "Barbell",
            ),
        )

        val item = repository.observeCompletedSessions().first().single()
        assertEquals("Push Day", item.workoutName)
        assertEquals(1, item.completedSetCount)
        assertEquals(10 * 50.0, item.volume, 0.001)
        assertEquals(2, item.exerciseCount)
    }

    @Test
    fun observeCompletedSessions_isolatesSessions() = runBlocking {
        val firstId = repository.startSession(workoutId).requireSessionId()
        val firstExercise = repository.observeSessionExercises(firstId).first()[0].id
        repository.completeSet(firstExercise, setIndex = 0, reps = 8, weight = 60.0)
        repository.finishSession(firstId)

        val otherWorkout = workoutRepository.save(Workout(name = "Pull", description = ""))
        workoutRepository.addBlock(
                WorkoutBlock(workoutId = otherWorkout, position = 0, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 60),
                listOf(WorkoutExercise(blockId = 0, exerciseId = squatId, positionInBlock = 0, minRepetitions = 5, maxRepetitions = 5, weight = 40.0, notes = "")),
            )
        val secondId = repository.startSession(otherWorkout).requireSessionId()
        val secondExercise = repository.observeSessionExercises(secondId).first()[0].id
        repository.completeSet(secondExercise, setIndex = 0, reps = 5, weight = 40.0)
        repository.finishSession(secondId)

        val items = repository.observeCompletedSessions().first().associateBy { it.sessionId }
        assertEquals(8 * 60.0, items.getValue(firstId).volume, 0.001)
        assertEquals(5 * 40.0, items.getValue(secondId).volume, 0.001)
        assertEquals(7, items.getValue(firstId).plannedSetCount)
        assertEquals(2, items.getValue(secondId).plannedSetCount)
    }

    @Test
    fun observeCompletedSetHistory_excludesInProgress() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val sessionExerciseId = repository.observeSessionExercises(sessionId).first()[0].id
        repository.completeSet(sessionExerciseId, setIndex = 0, reps = 8, weight = 60.0)

        assertTrue(repository.observeCompletedSetHistory().first().isEmpty())

        repository.finishSession(sessionId)
        val history = repository.observeCompletedSetHistory().first()
        assertTrue(history.any { it.sessionId == sessionId && it.weight == 60.0 })
    }

    @Test
    fun observeCompletedSetHistory_keepsSnapshotNameAfterExerciseDelete() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val snapshot = repository.observeSessionExercises(sessionId).first()[0]
        repository.completeSet(snapshot.id, setIndex = 0, reps = 10, weight = 50.0)
        repository.finishSession(sessionId)
        exerciseRepository.delete(
            com.gymtrack.domain.model.Exercise(
                id = exerciseId,
                name = "Bench Press",
                muscleGroup = "Chest",
                equipmentType = "Barbell",
            ),
        )

        val rows = repository.observeCompletedSetHistory().first().filter { it.sessionId == sessionId }
        assertTrue(rows.any { it.exerciseName == "Bench Press" && it.reps == 10 && it.weight == 50.0 })
        assertNull(repository.observeSessionExercises(sessionId).first()[0].exerciseId)
    }

    @Test
    fun startSession_copiesSecondaryMusclesIntoSnapshot() = runBlocking {
        exerciseRepository.save(
            com.gymtrack.domain.model.Exercise(
                id = exerciseId,
                name = "Bench Press",
                muscleGroup = "Chest",
                equipmentType = "Barbell",
                secondaryMuscles = listOf("Ombros", "Tríceps"),
            ),
        )
        val sessionId = repository.startSession(workoutId).requireSessionId()
        val snapshot = repository.observeSessionExercises(sessionId).first()
            .first { it.exerciseName == "Bench Press" }
        assertEquals(listOf("Ombros", "Tríceps"), snapshot.secondaryMuscles)
    }

    @Test
    fun startSession_catalogSecondaryEditDoesNotChangeExistingSnapshot() = runBlocking {
        exerciseRepository.save(
            com.gymtrack.domain.model.Exercise(
                id = exerciseId,
                name = "Bench Press",
                muscleGroup = "Chest",
                equipmentType = "Barbell",
                secondaryMuscles = listOf("Ombros"),
            ),
        )
        val sessionId = repository.startSession(workoutId).requireSessionId()
        exerciseRepository.save(
            com.gymtrack.domain.model.Exercise(
                id = exerciseId,
                name = "Bench Press",
                muscleGroup = "Chest",
                equipmentType = "Barbell",
                secondaryMuscles = listOf("Tríceps", "Abdômen"),
            ),
        )
        val snapshot = repository.observeSessionExercises(sessionId).first()
            .first { it.exerciseName == "Bench Press" }
        assertEquals(listOf("Ombros"), snapshot.secondaryMuscles)
        val catalog = exerciseRepository.getById(exerciseId).first()!!
        assertEquals(listOf("Abdômen", "Tríceps"), catalog.secondaryMuscles)
    }

    @Test
    fun observeCompletedSetHistory_isolatesExerciseNames() = runBlocking {
        val firstId = repository.startSession(workoutId).requireSessionId()
        val firstExercises = repository.observeSessionExercises(firstId).first()
        repository.completeSet(firstExercises[0].id, setIndex = 0, reps = 8, weight = 60.0)
        repository.completeSet(firstExercises[1].id, setIndex = 0, reps = 5, weight = 40.0)
        repository.finishSession(firstId)

        val rows = repository.observeCompletedSetHistory().first().filter { it.sessionId == firstId }
        assertEquals(setOf("Bench Press", "Squat"), rows.map { it.exerciseName }.toSet())
        assertEquals(60.0, rows.single { it.exerciseName == "Bench Press" }.weight, 0.001)
        assertEquals(40.0, rows.single { it.exerciseName == "Squat" }.weight, 0.001)
    }

    @Test
    fun deleteCompletedSession_removesOnlyThatSessionAndDependents() = runBlocking {
        val firstId = repository.startSession(workoutId).requireSessionId()
        val firstExercises = repository.observeSessionExercises(firstId).first()
        repository.completeSet(firstExercises[0].id, setIndex = 0, reps = 8, weight = 60.0)
        repository.finishSession(firstId)

        val secondId = repository.startSession(workoutId).requireSessionId()
        val secondExercises = repository.observeSessionExercises(secondId).first()
        repository.completeSet(secondExercises[0].id, setIndex = 0, reps = 7, weight = 55.0)
        repository.finishSession(secondId)

        val firstExerciseIds = firstExercises.map { it.id }
        repository.deleteCompletedSession(firstId)

        assertNull(repository.getSession(firstId))
        assertNotNull(repository.getSession(secondId))
        assertEquals(secondId, repository.observeCompletedSessions().first().single().sessionId)
        assertTrue(db.workoutSessionExerciseDao().getBySessionId(firstId).first().isEmpty())
        assertTrue(db.workoutSetDao().getBySessionId(firstId).first().isEmpty())
        firstExerciseIds.forEach { sessionExerciseId ->
            assertTrue(db.workoutSetDao().getBySessionExerciseId(sessionExerciseId).first().isEmpty())
        }
        assertEquals(2, repository.observeSessionExercises(secondId).first().size)
        assertEquals(1, db.workoutSetDao().getBySessionId(secondId).first().size)
        assertNotNull(workoutRepository.getById(workoutId).first())
        assertNotNull(exerciseRepository.getById(exerciseId).first())
        assertTrue(
            repository.observeCompletedSetHistory().first().none { it.sessionId == firstId },
        )
        assertTrue(
            repository.observeCompletedSetHistory().first().any { it.sessionId == secondId },
        )
    }

    @Test
    fun deleteCompletedSession_doesNotDeleteInProgressSession() = runBlocking {
        val sessionId = repository.startSession(workoutId).requireSessionId()
        repository.deleteCompletedSession(sessionId)
        val session = repository.getSession(sessionId)
        assertNotNull(session)
        assertEquals(WorkoutSessionStatus.IN_PROGRESS, session!!.status)
        assertEquals(2, repository.observeSessionExercises(sessionId).first().size)
    }

    private fun StartSessionResult.requireSessionId(): Long = when (this) {
        is StartSessionResult.Created -> sessionId
        is StartSessionResult.Resumed -> sessionId
        is StartSessionResult.BlockedOtherWorkout ->
            error("Expected Created or Resumed, got BlockedOtherWorkout($workoutName)")
    }

    private fun sessionRepositoryWith(clock: TimeProvider): WorkoutSessionRepository {
        return WorkoutSessionRepositoryImpl(
            db,
            db.workoutSessionDao(),
            db.workoutSessionExerciseDao(),
            db.workoutSetDao(),
            workoutRepository,
            exerciseRepository,
            clock,
        )
    }

    private class FakeTimeProvider(private val now: Long) : TimeProvider {
        override fun nowMillis(): Long = now
    }
}

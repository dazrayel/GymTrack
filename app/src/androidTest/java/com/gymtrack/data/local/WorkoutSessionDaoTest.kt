package com.gymtrack.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.data.local.dao.WorkoutSessionDao
import com.gymtrack.data.local.dao.WorkoutSessionExerciseDao
import com.gymtrack.data.local.dao.WorkoutSetDao
import com.gymtrack.data.local.entity.ExerciseEntity
import com.gymtrack.data.local.entity.WorkoutEntity
import com.gymtrack.data.local.entity.WorkoutSessionEntity
import com.gymtrack.data.local.entity.WorkoutSessionExerciseEntity
import com.gymtrack.data.local.entity.WorkoutSetEntity
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
class WorkoutSessionDaoTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var db: GymTrackDatabase
    private lateinit var sessionDao: WorkoutSessionDao
    private lateinit var sessionExerciseDao: WorkoutSessionExerciseDao
    private lateinit var setDao: WorkoutSetDao

    private var workoutId: Long = 0L
    private var exerciseId: Long = 0L

    @Before
    fun setUp() {
        hiltRule.inject()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionDao = db.workoutSessionDao()
        sessionExerciseDao = db.workoutSessionExerciseDao()
        setDao = db.workoutSetDao()

        workoutId = db.workoutDao().insert(WorkoutEntity(name = "Push Day", description = "Chest"))
        exerciseId = db.exerciseDao().insert(
            ExerciseEntity(name = "Bench Press", muscleGroup = "Chest", equipmentType = "Barbell"),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun inProgressSession(
        name: String = "Push Day",
        startedAt: Long = 1_000L,
    ) = WorkoutSessionEntity(
        workoutId = workoutId,
        workoutName = name,
        workoutDescription = "Chest",
        startedAtMillis = startedAt,
        endedAtMillis = null,
        status = WorkoutSessionEntity.STATUS_IN_PROGRESS,
    )

    private fun sessionExercise(
        sessionId: Long,
        position: Int,
        name: String = "Bench Press",
        exerciseId: Long? = this.exerciseId,
    ) = WorkoutSessionExerciseEntity(
        sessionId = sessionId,
        exerciseId = exerciseId,
        position = position,
        exerciseName = name,
        muscleGroup = "Chest",
        equipmentType = "Barbell",
        plannedSets = 3,
        minRepetitions = 8,
        maxRepetitions = 12,
        plannedWeight = 60.0,
        restSeconds = 90,
        notes = "",
    )

    // ─── WorkoutSessionDao ───────────────────────────────────────────────────

    @Test
    fun sessionInsert_returnsPositiveRowId() {
        val id = sessionDao.insert(inProgressSession())
        assertTrue(id > 0)
    }

    @Test
    fun sessionGetById_returnsInsertedSession() = runBlocking {
        val id = sessionDao.insert(inProgressSession(startedAt = 2_500L))
        val result = sessionDao.getById(id).first()

        assertNotNull(result)
        assertEquals(id, result!!.id)
        assertEquals(workoutId, result.workoutId)
        assertEquals("Push Day", result.workoutName)
        assertEquals("Chest", result.workoutDescription)
        assertEquals(2_500L, result.startedAtMillis)
        assertNull(result.endedAtMillis)
        assertEquals(WorkoutSessionEntity.STATUS_IN_PROGRESS, result.status)
    }

    @Test
    fun sessionGetInProgress_returnsOnlyInProgressSession() = runBlocking {
        val activeId = sessionDao.insert(inProgressSession())
        sessionDao.insert(
            WorkoutSessionEntity(
                workoutId = workoutId,
                workoutName = "Old",
                workoutDescription = "",
                startedAtMillis = 100L,
                endedAtMillis = 200L,
                status = WorkoutSessionEntity.STATUS_COMPLETED,
            ),
        )

        val active = sessionDao.getInProgress().first()
        assertNotNull(active)
        assertEquals(activeId, active!!.id)
        assertEquals(WorkoutSessionEntity.STATUS_IN_PROGRESS, active.status)
    }

    @Test
    fun sessionGetInProgress_doesNotReturnCompletedSession() = runBlocking {
        sessionDao.insert(
            WorkoutSessionEntity(
                workoutId = workoutId,
                workoutName = "Done",
                workoutDescription = "",
                startedAtMillis = 100L,
                endedAtMillis = 200L,
                status = WorkoutSessionEntity.STATUS_COMPLETED,
            ),
        )

        assertNull(sessionDao.getInProgress().first())
    }

    @Test
    fun sessionUpdateCompletion_updatesOnlyEndedAtAndStatus() = runBlocking {
        val id = sessionDao.insert(inProgressSession(startedAt = 1_000L))

        val affected = sessionDao.updateCompletion(
            id = id,
            endedAtMillis = 9_000L,
            status = WorkoutSessionEntity.STATUS_COMPLETED,
        )
        assertEquals(1, affected)

        val result = sessionDao.getById(id).first()!!
        assertEquals(9_000L, result.endedAtMillis)
        assertEquals(WorkoutSessionEntity.STATUS_COMPLETED, result.status)
        assertEquals("Push Day", result.workoutName)
        assertEquals("Chest", result.workoutDescription)
        assertEquals(1_000L, result.startedAtMillis)
        assertEquals(workoutId, result.workoutId)
    }

    @Test
    fun sessionUpdateCompletion_clearsRestFields() = runBlocking {
        val id = sessionDao.insert(inProgressSession(startedAt = 1_000L))
        sessionDao.updateRestStarted(
            id = id,
            restEndsAtMillis = 5_000L,
            restSessionExerciseId = 42L,
            restAfterSetIndex = 1,
        )
        sessionDao.updateCompletion(
            id = id,
            endedAtMillis = 9_000L,
            status = WorkoutSessionEntity.STATUS_COMPLETED,
        )
        val result = sessionDao.getById(id).first()!!
        assertEquals(WorkoutSessionEntity.STATUS_COMPLETED, result.status)
        assertEquals(9_000L, result.endedAtMillis)
        assertEquals(1_000L, result.startedAtMillis)
        assertNull(result.restEndsAtMillis)
        assertNull(result.restPausedRemainingMillis)
        assertNull(result.restSessionExerciseId)
        assertNull(result.restAfterSetIndex)
    }

    @Test
    fun sessionUpdateCompletion_forMissingId_returnsZero() {
        assertEquals(
            0,
            sessionDao.updateCompletion(999L, 1L, WorkoutSessionEntity.STATUS_COMPLETED),
        )
    }

    @Test
    fun sessionUpdateRestStarted_persistsRestFieldsOnly() = runBlocking {
        val id = sessionDao.insert(inProgressSession(startedAt = 1_000L))
        val affected = sessionDao.updateRestStarted(
            id = id,
            restEndsAtMillis = 5_000L,
            restSessionExerciseId = 42L,
            restAfterSetIndex = 1,
        )
        assertEquals(1, affected)
        val result = sessionDao.getById(id).first()!!
        assertEquals(5_000L, result.restEndsAtMillis)
        assertNull(result.restPausedRemainingMillis)
        assertEquals(42L, result.restSessionExerciseId)
        assertEquals(1, result.restAfterSetIndex)
        assertEquals(WorkoutSessionEntity.STATUS_IN_PROGRESS, result.status)
        assertEquals(1_000L, result.startedAtMillis)
        assertEquals("Push Day", result.workoutName)
        assertNull(result.endedAtMillis)
    }

    @Test
    fun sessionUpdateRestPaused_clearsEndsAtAndStoresRemaining() = runBlocking {
        val id = sessionDao.insert(inProgressSession())
        sessionDao.updateRestStarted(id, 9_000L, 7L, 0)
        sessionDao.updateRestPaused(id, 3_500L)
        val result = sessionDao.getById(id).first()!!
        assertNull(result.restEndsAtMillis)
        assertEquals(3_500L, result.restPausedRemainingMillis)
        assertEquals(7L, result.restSessionExerciseId)
        assertEquals(0, result.restAfterSetIndex)
        assertEquals(WorkoutSessionEntity.STATUS_IN_PROGRESS, result.status)
    }

    @Test
    fun sessionUpdateRestResumed_setsEndsAtAndClearsPaused() = runBlocking {
        val id = sessionDao.insert(inProgressSession())
        sessionDao.updateRestStarted(id, 9_000L, 7L, 0)
        sessionDao.updateRestPaused(id, 2_000L)
        sessionDao.updateRestResumed(id, 11_000L)
        val result = sessionDao.getById(id).first()!!
        assertEquals(11_000L, result.restEndsAtMillis)
        assertNull(result.restPausedRemainingMillis)
        assertEquals(7L, result.restSessionExerciseId)
    }

    @Test
    fun sessionClearRest_nullsAllRestFields() = runBlocking {
        val id = sessionDao.insert(inProgressSession())
        sessionDao.updateRestStarted(id, 9_000L, 7L, 2)
        sessionDao.clearRest(id)
        val result = sessionDao.getById(id).first()!!
        assertNull(result.restEndsAtMillis)
        assertNull(result.restPausedRemainingMillis)
        assertNull(result.restSessionExerciseId)
        assertNull(result.restAfterSetIndex)
        assertEquals(WorkoutSessionEntity.STATUS_IN_PROGRESS, result.status)
        assertEquals("Push Day", result.workoutName)
    }

    // ─── WorkoutSessionExerciseDao ───────────────────────────────────────────

    @Test
    fun sessionExerciseInsert_persistsRow() = runBlocking {
        val sessionId = sessionDao.insert(inProgressSession())
        val id = sessionExerciseDao.insert(sessionExercise(sessionId, position = 0, name = "Bench"))

        val list = sessionExerciseDao.getBySessionId(sessionId).first()
        assertEquals(1, list.size)
        assertEquals(id, list[0].id)
        assertEquals("Bench", list[0].exerciseName)
        assertEquals(3, list[0].plannedSets)
        assertEquals(60.0, list[0].plannedWeight, 0.001)
    }

    @Test
    fun sessionExerciseInsertAll_insertsMultipleRows() = runBlocking {
        val sessionId = sessionDao.insert(inProgressSession())
        val ids = sessionExerciseDao.insertAll(
            listOf(
                sessionExercise(sessionId, position = 0, name = "A"),
                sessionExercise(sessionId, position = 1, name = "B"),
            ),
        )

        assertEquals(2, ids.size)
        assertEquals(2, sessionExerciseDao.getBySessionId(sessionId).first().size)
    }

    @Test
    fun sessionExerciseGetBySessionId_excludesOtherSessions() = runBlocking {
        val sessionA = sessionDao.insert(inProgressSession(name = "A"))
        val sessionB = sessionDao.insert(inProgressSession(name = "B"))
        sessionExerciseDao.insert(sessionExercise(sessionA, position = 0, name = "Only A"))
        sessionExerciseDao.insert(sessionExercise(sessionB, position = 0, name = "Only B"))

        val fromA = sessionExerciseDao.getBySessionId(sessionA).first()
        assertEquals(1, fromA.size)
        assertEquals("Only A", fromA[0].exerciseName)
    }

    @Test
    fun sessionExerciseGetBySessionId_isSortedByPositionAscending() = runBlocking {
        val sessionId = sessionDao.insert(inProgressSession())
        sessionExerciseDao.insert(sessionExercise(sessionId, position = 2, name = "C"))
        sessionExerciseDao.insert(sessionExercise(sessionId, position = 0, name = "A"))
        sessionExerciseDao.insert(sessionExercise(sessionId, position = 1, name = "B"))

        val result = sessionExerciseDao.getBySessionId(sessionId).first()
        assertEquals(listOf("A", "B", "C"), result.map { it.exerciseName })
        assertEquals(listOf(0, 1, 2), result.map { it.position })
    }

    // ─── WorkoutSetDao ───────────────────────────────────────────────────────

    @Test
    fun setInsert_returnsPositiveRowId() {
        val sessionId = sessionDao.insert(inProgressSession())
        val sessionExerciseId = sessionExerciseDao.insert(sessionExercise(sessionId, 0))
        val id = setDao.insert(
            WorkoutSetEntity(
                sessionExerciseId = sessionExerciseId,
                setIndex = 0,
                reps = 10,
                weight = 60.0,
                completedAtMillis = 2_000L,
            ),
        )
        assertTrue(id > 0)
    }

    @Test
    fun setGetBySessionExerciseId_isSortedBySetIndexAscending() = runBlocking {
        val sessionId = sessionDao.insert(inProgressSession())
        val sessionExerciseId = sessionExerciseDao.insert(sessionExercise(sessionId, 0))
        setDao.insert(
            WorkoutSetEntity(
                sessionExerciseId = sessionExerciseId,
                setIndex = 2,
                reps = 8,
                weight = 62.5,
                completedAtMillis = 3_000L,
            ),
        )
        setDao.insert(
            WorkoutSetEntity(
                sessionExerciseId = sessionExerciseId,
                setIndex = 0,
                reps = 10,
                weight = 60.0,
                completedAtMillis = 1_000L,
            ),
        )
        setDao.insert(
            WorkoutSetEntity(
                sessionExerciseId = sessionExerciseId,
                setIndex = 1,
                reps = 9,
                weight = 60.0,
                completedAtMillis = 2_000L,
            ),
        )

        val result = setDao.getBySessionExerciseId(sessionExerciseId).first()
        assertEquals(listOf(0, 1, 2), result.map { it.setIndex })
        assertEquals(listOf(10, 9, 8), result.map { it.reps })
    }

    @Test
    fun setGetBySessionExerciseId_doesNotMixExercises() = runBlocking {
        val sessionId = sessionDao.insert(inProgressSession())
        val first = sessionExerciseDao.insert(sessionExercise(sessionId, 0, name = "A"))
        val second = sessionExerciseDao.insert(sessionExercise(sessionId, 1, name = "B"))
        setDao.insert(
            WorkoutSetEntity(
                sessionExerciseId = first,
                setIndex = 0,
                reps = 10,
                weight = 60.0,
                completedAtMillis = 1L,
            ),
        )
        setDao.insert(
            WorkoutSetEntity(
                sessionExerciseId = second,
                setIndex = 0,
                reps = 5,
                weight = 80.0,
                completedAtMillis = 2L,
            ),
        )

        val forFirst = setDao.getBySessionExerciseId(first).first()
        assertEquals(1, forFirst.size)
        assertEquals(10, forFirst[0].reps)
    }

    @Test
    fun setGetBySessionId_returnsSetsForAllExercisesInOrder() = runBlocking {
        val sessionId = sessionDao.insert(inProgressSession())
        val first = sessionExerciseDao.insert(sessionExercise(sessionId, 0, name = "A"))
        val second = sessionExerciseDao.insert(sessionExercise(sessionId, 1, name = "B"))
        setDao.insert(
            WorkoutSetEntity(
                sessionExerciseId = second,
                setIndex = 0,
                reps = 5,
                weight = 80.0,
                completedAtMillis = 2L,
            ),
        )
        setDao.insert(
            WorkoutSetEntity(
                sessionExerciseId = first,
                setIndex = 1,
                reps = 8,
                weight = 60.0,
                completedAtMillis = 2L,
            ),
        )
        setDao.insert(
            WorkoutSetEntity(
                sessionExerciseId = first,
                setIndex = 0,
                reps = 10,
                weight = 60.0,
                completedAtMillis = 1L,
            ),
        )

        val result = setDao.getBySessionId(sessionId).first()
        assertEquals(3, result.size)
        assertEquals(listOf(first, first, second), result.map { it.sessionExerciseId })
        assertEquals(listOf(0, 1, 0), result.map { it.setIndex })
    }

    @Test
    fun setInsert_duplicateSessionExerciseIdAndSetIndex_isRejected() {
        val sessionId = sessionDao.insert(inProgressSession())
        val sessionExerciseId = sessionExerciseDao.insert(sessionExercise(sessionId, 0))
        setDao.insert(
            WorkoutSetEntity(
                sessionExerciseId = sessionExerciseId,
                setIndex = 0,
                reps = 10,
                weight = 60.0,
                completedAtMillis = 1L,
            ),
        )
        try {
            setDao.insert(
                WorkoutSetEntity(
                    sessionExerciseId = sessionExerciseId,
                    setIndex = 0,
                    reps = 8,
                    weight = 62.5,
                    completedAtMillis = 2L,
                ),
            )
            fail("UNIQUE (sessionExerciseId, setIndex) should reject duplicates")
        } catch (_: SQLiteConstraintException) {
            // expected
        }
    }

    // ─── CASCADE ─────────────────────────────────────────────────────────────

    @Test
    fun deleteSession_cascadesSessionExercises() = runBlocking {
        val sessionId = sessionDao.insert(inProgressSession())
        sessionExerciseDao.insert(sessionExercise(sessionId, 0))
        assertEquals(1, sessionExerciseDao.getBySessionId(sessionId).first().size)

        sessionDao.deleteById(sessionId)

        assertTrue(sessionExerciseDao.getBySessionId(sessionId).first().isEmpty())
        assertNull(sessionDao.getById(sessionId).first())
    }

    @Test
    fun deleteSessionExercise_cascadesSets() = runBlocking {
        val sessionId = sessionDao.insert(inProgressSession())
        val sessionExerciseId = sessionExerciseDao.insert(sessionExercise(sessionId, 0))
        setDao.insert(
            WorkoutSetEntity(
                sessionExerciseId = sessionExerciseId,
                setIndex = 0,
                reps = 10,
                weight = 60.0,
                completedAtMillis = 1L,
            ),
        )
        assertEquals(1, setDao.getBySessionExerciseId(sessionExerciseId).first().size)

        sessionExerciseDao.deleteById(sessionExerciseId)

        assertTrue(setDao.getBySessionExerciseId(sessionExerciseId).first().isEmpty())
        assertTrue(sessionExerciseDao.getBySessionId(sessionId).first().isEmpty())
    }
}

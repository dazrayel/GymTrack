package com.gymtrack.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gymtrack.data.local.GymTrackDatabase
import com.gymtrack.data.local.entity.ExerciseEntity
import com.gymtrack.domain.model.Workout
import com.gymtrack.domain.model.WorkoutBlock
import com.gymtrack.domain.model.WorkoutBlockType
import com.gymtrack.domain.model.WorkoutExercise
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for WorkoutRepositoryImpl under Workout → WorkoutBlock → WorkoutExercise.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryImplTest {

    private lateinit var db: GymTrackDatabase
    private lateinit var repository: WorkoutRepositoryImpl

    private var exerciseId: Long = 0L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GymTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WorkoutRepositoryImpl(
            db,
            db.workoutDao(),
            db.workoutBlockDao(),
            db.workoutExerciseDao(),
        )
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

    // ─── Blocks / exercises: add / retrieve (SINGLE) ──────────────────────────

    @Test
    fun addBlock_single_persistsOneExercise() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push"))
        val blockId = repository.addBlock(
            WorkoutBlock(
                workoutId = workoutId,
                position = 0,
                type = WorkoutBlockType.SINGLE,
                rounds = 4,
                restSeconds = 90,
            ),
            listOf(
                WorkoutExercise(
                    blockId = 0,
                    exerciseId = exerciseId,
                    positionInBlock = 0,
                    minRepetitions = 8,
                    maxRepetitions = 12,
                    weight = 60.0,
                    notes = "Última série até a falha",
                ),
            ),
        )
        val blocks = repository.getBlocks(workoutId).first()
        val exercises = repository.getExercisesForWorkout(workoutId).first()
        assertEquals(1, blocks.size)
        assertEquals(blockId, blocks.first().id)
        assertEquals(WorkoutBlockType.SINGLE, blocks.first().type)
        assertEquals(4, blocks.first().rounds)
        assertEquals(90, blocks.first().restSeconds)
        assertEquals(1, exercises.size)
        assertEquals(exerciseId, exercises.first().exerciseId)
        assertEquals(blockId, exercises.first().blockId)
        assertEquals(0, exercises.first().positionInBlock)
        assertEquals(8, exercises.first().minRepetitions)
        assertEquals(12, exercises.first().maxRepetitions)
        assertEquals(60.0, exercises.first().weight, 0.001)
        assertEquals("Última série até a falha", exercises.first().notes)
    }

    @Test
    fun getExercises_isSortedByBlockPosition() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        val ex2 = db.exerciseDao().insert(
            ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"),
        )

        repository.addBlock(
            WorkoutBlock(workoutId = workoutId, position = 2, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 90),
            listOf(singleExercise(exerciseId)),
        )
        repository.addBlock(
            WorkoutBlock(workoutId = workoutId, position = 0, type = WorkoutBlockType.SINGLE, rounds = 3, restSeconds = 120),
            listOf(singleExercise(ex2, weight = 80.0)),
        )

        val result = repository.getExercisesForWorkout(workoutId).first()
        assertEquals(2, result.size)
        assertEquals(ex2, result[0].exerciseId)
        assertEquals(exerciseId, result[1].exerciseId)

        val blocks = repository.getBlocks(workoutId).first()
        assertEquals(listOf(0, 2), blocks.map { it.position })
    }

    @Test
    fun getExercises_emptyWorkout_returnsEmptyList() = runBlocking {
        val workoutId = repository.save(Workout(name = "Empty Workout"))
        assertTrue(repository.getExercises(workoutId).first().isEmpty())
        assertTrue(repository.getBlocks(workoutId).first().isEmpty())
    }

    // ─── Blocks / exercises: update ───────────────────────────────────────────

    @Test
    fun updateBlockExercise_changesFields() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        val blockId = addSingleBlock(workoutId, exerciseId, position = 0, rounds = 3, restSeconds = 90)
        val exerciseRow = repository.getExercisesForWorkout(workoutId).first().first()

        repository.updateBlockExercise(
            exerciseRow.copy(
                positionInBlock = 0,
                minRepetitions = 5,
                maxRepetitions = 8,
                weight = 80.0,
                notes = "Drop set",
            ),
        )
        repository.updateBlock(
            WorkoutBlock(
                id = blockId,
                workoutId = workoutId,
                position = 1,
                type = WorkoutBlockType.SINGLE,
                rounds = 5,
                restSeconds = 180,
            ),
        )

        val result = repository.getExercisesForWorkout(workoutId).first().first()
        val block = repository.getBlocks(workoutId).first().first()
        assertEquals(5, result.minRepetitions)
        assertEquals(8, result.maxRepetitions)
        assertEquals(80.0, result.weight, 0.001)
        assertEquals("Drop set", result.notes)
        assertEquals(1, block.position)
        assertEquals(5, block.rounds)
        assertEquals(180, block.restSeconds)
    }

    @Test
    fun updateBlockExercise_doesNotCreateDuplicate() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        addSingleBlock(workoutId, exerciseId, position = 0)
        val exerciseRow = repository.getExercisesForWorkout(workoutId).first().first()

        repository.updateBlockExercise(
            exerciseRow.copy(minRepetitions = 6, maxRepetitions = 10, weight = 70.0),
        )

        assertEquals(1, repository.getExercisesForWorkout(workoutId).first().size)
        assertEquals(1, repository.getBlocks(workoutId).first().size)
    }

    // ─── Blocks / exercises: remove ───────────────────────────────────────────

    @Test
    fun removeBlock_deletesExercises() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push"))
        val blockId = addSingleBlock(workoutId, exerciseId, position = 0)
        repository.removeBlock(blockId)
        assertTrue(repository.getBlocks(workoutId).first().isEmpty())
        assertTrue(repository.getExercisesForWorkout(workoutId).first().isEmpty())
    }

    @Test
    fun removeExercise_removesItsBlockFromWorkout() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        addSingleBlock(workoutId, exerciseId, position = 0)
        val saved = repository.getExercisesForWorkout(workoutId).first()[0]

        repository.removeExercise(saved)

        assertTrue(repository.getExercises(workoutId).first().isEmpty())
        assertTrue(repository.getBlocks(workoutId).first().isEmpty())
    }

    @Test
    fun removeExerciseById_removesTargetBlock() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        val ex2 = db.exerciseDao().insert(
            ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"),
        )
        val block1 = addSingleBlock(workoutId, exerciseId, position = 0)
        addSingleBlock(workoutId, ex2, position = 1, weight = 80.0, restSeconds = 120)
        val id1 = repository.getExercisesForWorkout(workoutId).first().first { it.blockId == block1 }.id

        repository.removeExerciseById(id1)

        val remaining = repository.getExercisesForWorkout(workoutId).first()
        assertEquals(1, remaining.size)
        assertEquals(ex2, remaining[0].exerciseId)
        assertEquals(1, repository.getBlocks(workoutId).first().size)
    }

    // ─── Domain mapping ───────────────────────────────────────────────────────

    @Test
    fun getAll_returnsDomainWorkouts() = runBlocking {
        repository.save(Workout(name = "Push Day"))

        val result = repository.getAll().first()
        assertTrue(result.all { it is Workout })
    }

    @Test
    fun getExercises_returnsDomainWorkoutExercises() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        addSingleBlock(workoutId, exerciseId, position = 0)

        val result = repository.getExercises(workoutId).first()
        assertTrue(result.all { it is WorkoutExercise })
    }

    // ─── Block / exercise positions ───────────────────────────────────────────

    @Test
    fun updateBlockPositions_preservesOrder() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push"))
        val e2 = db.exerciseDao().insert(
            ExerciseEntity(name = "B", muscleGroup = "Costas", equipmentType = "Barra"),
        )
        val b1 = addSingleBlock(workoutId, exerciseId, position = 0)
        val b2 = addSingleBlock(workoutId, e2, position = 1)

        repository.updateBlockPositions(mapOf(b1 to 1, b2 to 0))
        val blocks = repository.getBlocks(workoutId).first()
        assertEquals(b2, blocks[0].id)
        assertEquals(b1, blocks[1].id)
    }

    @Test
    fun updateExercisePositions_updatesMultipleSingleBlocks() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        val ex2 = db.exerciseDao().insert(
            ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"),
        )
        val block0 = addSingleBlock(workoutId, exerciseId, position = 0)
        val block1 = addSingleBlock(workoutId, ex2, position = 1, rounds = 4, weight = 80.0, restSeconds = 120, notes = "Notas")
        val id0 = repository.getExercisesForWorkout(workoutId).first().first { it.blockId == block0 }.id
        val id1 = repository.getExercisesForWorkout(workoutId).first().first { it.blockId == block1 }.id

        repository.updateExercisePositions(mapOf(id0 to 1, id1 to 0))

        val result = repository.getExercisesForWorkout(workoutId).first()
        assertEquals(2, result.size)
        assertEquals(id1, result[0].id)
        assertEquals(id0, result[1].id)
        val blocks = repository.getBlocks(workoutId).first()
        assertEquals(block1, blocks[0].id)
        assertEquals(0, blocks[0].position)
        assertEquals(block0, blocks[1].id)
        assertEquals(1, blocks[1].position)
    }

    @Test
    fun updateExercisePositions_preservesOtherFields() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        val blockId = addSingleBlock(
            workoutId,
            exerciseId,
            position = 0,
            rounds = 4,
            restSeconds = 90,
            weight = 75.5,
            notes = "Manter",
            minReps = 6,
            maxReps = 10,
        )
        val id = repository.getExercisesForWorkout(workoutId).first().first().id

        repository.updateExercisePositions(mapOf(id to 2))

        val result = repository.getExercisesForWorkout(workoutId).first().first()
        val block = repository.getBlocks(workoutId).first().first()
        assertEquals(2, block.position)
        assertEquals(blockId, result.blockId)
        assertEquals(exerciseId, result.exerciseId)
        assertEquals(4, block.rounds)
        assertEquals(6, result.minRepetitions)
        assertEquals(10, result.maxRepetitions)
        assertEquals(75.5, result.weight, 0.001)
        assertEquals(90, block.restSeconds)
        assertEquals("Manter", result.notes)
    }

    @Test
    fun updateExercisePositions_doesNotAffectOtherWorkout() = runBlocking {
        val workoutA = repository.save(Workout(name = "A"))
        val workoutB = repository.save(Workout(name = "B"))
        val ex2 = db.exerciseDao().insert(
            ExerciseEntity(name = "Squat", muscleGroup = "Legs", equipmentType = "Barbell"),
        )
        val blockA = addSingleBlock(workoutA, exerciseId, position = 0)
        val blockB = addSingleBlock(workoutB, ex2, position = 0, rounds = 5, weight = 100.0, restSeconds = 180)
        val idA = repository.getExercisesForWorkout(workoutA).first().first { it.blockId == blockA }.id
        val idB = repository.getExercisesForWorkout(workoutB).first().first { it.blockId == blockB }.id

        repository.updateExercisePositions(mapOf(idA to 3))

        val fromA = repository.getBlocks(workoutA).first().first()
        val fromBExercise = repository.getExercisesForWorkout(workoutB).first().first()
        val fromBBlock = repository.getBlocks(workoutB).first().first()
        assertEquals(3, fromA.position)
        assertEquals(idB, fromBExercise.id)
        assertEquals(0, fromBBlock.position)
        assertEquals(5, fromBBlock.rounds)
    }

    @Test
    fun updateExercisePositions_emptyMap_doesNotChangeData() = runBlocking {
        val workoutId = repository.save(Workout(name = "Push Day"))
        addSingleBlock(workoutId, exerciseId, position = 0, rounds = 3, restSeconds = 90)

        repository.updateExercisePositions(emptyMap())

        val result = repository.getExercisesForWorkout(workoutId).first()
        val blocks = repository.getBlocks(workoutId).first()
        assertEquals(1, result.size)
        assertEquals(0, blocks[0].position)
        assertEquals(3, blocks[0].rounds)
    }

    // ─── Edit workout must preserve block / exercise associations ─────────────

    @Test
    fun update_nameOnly_keepsExistingExercises() = runBlocking {
        val workoutId = repository.save(Workout(name = "Peito", description = ""))
        val catalogIds = insertCatalogExercises("Supino", "Rosca", "Agachamento")
        attachSinglesInOrder(workoutId, catalogIds)

        repository.update(Workout(id = workoutId, name = "Peito v2", description = ""))

        assertEquals(
            listOf("Supino", "Rosca", "Agachamento"),
            catalogNamesInWorkoutOrder(workoutId),
        )
    }

    @Test
    fun addBlock_duringEdit_appendsWithoutDroppingExisting() = runBlocking {
        val workoutId = repository.save(Workout(name = "Peito", description = ""))
        val initial = insertCatalogExercises("Supino", "Rosca")
        attachSinglesInOrder(workoutId, initial)
        repository.update(Workout(id = workoutId, name = "Peito editado", description = ""))

        val squatId = db.exerciseDao().insert(
            ExerciseEntity(name = "Agachamento", muscleGroup = "Legs", equipmentType = "Barbell"),
        )
        addSingleBlock(workoutId, squatId, position = 2)

        assertEquals(
            listOf("Supino", "Rosca", "Agachamento"),
            catalogNamesInWorkoutOrder(workoutId),
        )
    }

    @Test
    fun removeExercise_duringEdit_removesOnlyThatBlock() = runBlocking {
        val workoutId = repository.save(Workout(name = "Peito", description = ""))
        val catalogIds = insertCatalogExercises("Supino", "Rosca", "Agachamento")
        val linkIds = attachSinglesInOrder(workoutId, catalogIds)
        repository.update(Workout(id = workoutId, name = "Peito editado", description = ""))

        repository.removeExerciseById(linkIds[1])

        assertEquals(
            listOf("Supino", "Agachamento"),
            catalogNamesInWorkoutOrder(workoutId),
        )
    }

    @Test
    fun update_nameOnly_preservesExerciseOrder() = runBlocking {
        val workoutId = repository.save(Workout(name = "ABC", description = ""))
        val catalogIds = insertCatalogExercises("A", "B", "C")
        attachSinglesInOrder(workoutId, catalogIds)

        repository.update(Workout(id = workoutId, name = "ABC v2", description = "Notas"))

        assertEquals(listOf("A", "B", "C"), catalogNamesInWorkoutOrder(workoutId))
        assertEquals(listOf(0, 1, 2), repository.getBlocks(workoutId).first().map { it.position })
    }

    @Test
    fun update_nameOnly_preservesExercisesWhenReloadedFromNewRepository() = runBlocking {
        val workoutId = repository.save(Workout(name = "Peito", description = ""))
        val catalogIds = insertCatalogExercises("Supino", "Rosca", "Agachamento")
        attachSinglesInOrder(workoutId, catalogIds)

        repository.update(Workout(id = workoutId, name = "Peito salvo", description = "Desc"))

        val reopened = WorkoutRepositoryImpl(
            db,
            db.workoutDao(),
            db.workoutBlockDao(),
            db.workoutExerciseDao(),
        )
        assertEquals("Peito salvo", reopened.getById(workoutId).first()!!.name)
        assertEquals(
            listOf("Supino", "Rosca", "Agachamento"),
            catalogNamesInWorkoutOrder(workoutId, reopened),
        )
    }

    // ─── BI_SET / TRI_SET (preserved coverage) ────────────────────────────────

    @Test
    fun addBlock_biSet_requiresTwoExercises() = runBlocking {
        val workoutId = repository.save(Workout(name = "Arms"))
        val e1 = db.exerciseDao().insert(ExerciseEntity(name = "A", muscleGroup = "Bíceps", equipmentType = "Barra"))
        val e2 = db.exerciseDao().insert(ExerciseEntity(name = "B", muscleGroup = "Tríceps", equipmentType = "Corda"))
        repository.addBlock(
            WorkoutBlock(workoutId = workoutId, position = 0, type = WorkoutBlockType.BI_SET, rounds = 3, restSeconds = 60),
            listOf(
                WorkoutExercise(blockId = 0, exerciseId = e1, positionInBlock = 0, minRepetitions = 8, maxRepetitions = 12, weight = 20.0),
                WorkoutExercise(blockId = 0, exerciseId = e2, positionInBlock = 1, minRepetitions = 10, maxRepetitions = 12, weight = 15.0),
            ),
        )
        val exercises = repository.getExercisesForWorkout(workoutId).first()
        assertEquals(2, exercises.size)
        assertEquals(0, exercises[0].positionInBlock)
        assertEquals(1, exercises[1].positionInBlock)
    }

    @Test
    fun updateBlockExercise_inBiSet_keepsMembershipAndSiblingUnchanged() = runBlocking {
        val workoutId = repository.save(Workout(name = "Arms"))
        val e1 = db.exerciseDao().insert(ExerciseEntity(name = "A", muscleGroup = "Bíceps", equipmentType = "Barra"))
        val e2 = db.exerciseDao().insert(ExerciseEntity(name = "B", muscleGroup = "Tríceps", equipmentType = "Corda"))
        val blockId = repository.addBlock(
            WorkoutBlock(workoutId = workoutId, position = 0, type = WorkoutBlockType.BI_SET, rounds = 3, restSeconds = 60),
            listOf(
                WorkoutExercise(blockId = 0, exerciseId = e1, positionInBlock = 0, minRepetitions = 8, maxRepetitions = 12, weight = 20.0),
                WorkoutExercise(blockId = 0, exerciseId = e2, positionInBlock = 1, minRepetitions = 10, maxRepetitions = 12, weight = 15.0),
            ),
        )
        val before = repository.getExercisesForWorkout(workoutId).first()
        val first = before[0]
        val second = before[1]

        repository.updateBlockExercise(
            first.copy(minRepetitions = 6, maxRepetitions = 10, weight = 30.0, notes = "pico"),
        )

        val after = repository.getExercisesForWorkout(workoutId).first()
        val blocks = repository.getBlocks(workoutId).first()
        assertEquals(1, blocks.size)
        assertEquals(blockId, blocks.single().id)
        assertEquals(WorkoutBlockType.BI_SET, blocks.single().type)
        assertEquals(3, blocks.single().rounds)
        assertEquals(60, blocks.single().restSeconds)
        assertEquals(2, after.size)
        assertEquals(first.id, after[0].id)
        assertEquals(blockId, after[0].blockId)
        assertEquals(0, after[0].positionInBlock)
        assertEquals(6, after[0].minRepetitions)
        assertEquals(10, after[0].maxRepetitions)
        assertEquals(30.0, after[0].weight, 0.001)
        assertEquals("pico", after[0].notes)
        assertEquals(second.id, after[1].id)
        assertEquals(blockId, after[1].blockId)
        assertEquals(1, after[1].positionInBlock)
        assertEquals(10, after[1].minRepetitions)
        assertEquals(12, after[1].maxRepetitions)
        assertEquals(15.0, after[1].weight, 0.001)
    }

    @Test
    fun updateBlock_doesNotCascadeDeleteExercises() = runBlocking {
        val workoutId = repository.save(Workout(name = "Arms"))
        val e1 = db.exerciseDao().insert(ExerciseEntity(name = "A", muscleGroup = "Bíceps", equipmentType = "Barra"))
        val e2 = db.exerciseDao().insert(ExerciseEntity(name = "B", muscleGroup = "Tríceps", equipmentType = "Corda"))
        val blockId = repository.addBlock(
            WorkoutBlock(workoutId = workoutId, position = 0, type = WorkoutBlockType.BI_SET, rounds = 3, restSeconds = 60),
            listOf(
                WorkoutExercise(blockId = 0, exerciseId = e1, positionInBlock = 0, minRepetitions = 8, maxRepetitions = 12, weight = 20.0),
                WorkoutExercise(blockId = 0, exerciseId = e2, positionInBlock = 1, minRepetitions = 10, maxRepetitions = 12, weight = 15.0),
            ),
        )
        val beforeIds = repository.getExercisesForWorkout(workoutId).first().map { it.id }

        repository.updateBlock(
            WorkoutBlock(
                id = blockId,
                workoutId = workoutId,
                position = 0,
                type = WorkoutBlockType.BI_SET,
                rounds = 5,
                restSeconds = 90,
            ),
        )

        val after = repository.getExercisesForWorkout(workoutId).first()
        val block = repository.getBlocks(workoutId).first().single()
        assertEquals(5, block.rounds)
        assertEquals(90, block.restSeconds)
        assertEquals(beforeIds, after.map { it.id })
        assertEquals(2, after.size)
        assertTrue(after.all { it.blockId == blockId })
    }

    @Test
    fun updateBlockExercise_inTriSet_keepsAllThreeMembership() = runBlocking {
        val workoutId = repository.save(Workout(name = "Shoulders"))
        val e1 = db.exerciseDao().insert(ExerciseEntity(name = "A", muscleGroup = "Ombros", equipmentType = "Halteres"))
        val e2 = db.exerciseDao().insert(ExerciseEntity(name = "B", muscleGroup = "Ombros", equipmentType = "Halteres"))
        val e3 = db.exerciseDao().insert(ExerciseEntity(name = "C", muscleGroup = "Ombros", equipmentType = "Halteres"))
        val blockId = repository.addBlock(
            WorkoutBlock(workoutId = workoutId, position = 0, type = WorkoutBlockType.TRI_SET, rounds = 3, restSeconds = 90),
            listOf(
                WorkoutExercise(blockId = 0, exerciseId = e1, positionInBlock = 0, minRepetitions = 10, maxRepetitions = 12, weight = 8.0),
                WorkoutExercise(blockId = 0, exerciseId = e2, positionInBlock = 1, minRepetitions = 8, maxRepetitions = 10, weight = 12.0),
                WorkoutExercise(blockId = 0, exerciseId = e3, positionInBlock = 2, minRepetitions = 12, maxRepetitions = 15, weight = 6.0),
            ),
        )
        val middle = repository.getExercisesForWorkout(workoutId).first()[1]
        repository.updateBlockExercise(middle.copy(weight = 14.5, minRepetitions = 9, maxRepetitions = 11))

        val after = repository.getExercisesForWorkout(workoutId).first()
        assertEquals(3, after.size)
        assertTrue(after.all { it.blockId == blockId })
        assertEquals(listOf(0, 1, 2), after.map { it.positionInBlock })
        assertEquals(14.5, after[1].weight, 0.001)
        assertEquals(9, after[1].minRepetitions)
        assertEquals(11, after[1].maxRepetitions)
        assertEquals(8.0, after[0].weight, 0.001)
        assertEquals(6.0, after[2].weight, 0.001)
    }

    @Test
    fun addBlock_biSetWithOneExercise_throws() = runBlocking {
        val workoutId = repository.save(Workout(name = "Arms"))
        val e1 = db.exerciseDao().insert(ExerciseEntity(name = "A", muscleGroup = "Bíceps", equipmentType = "Barra"))
        try {
            repository.addBlock(
                WorkoutBlock(workoutId = workoutId, position = 0, type = WorkoutBlockType.BI_SET, rounds = 3, restSeconds = 60),
                listOf(
                    WorkoutExercise(blockId = 0, exerciseId = e1, positionInBlock = 0, minRepetitions = 8, maxRepetitions = 12, weight = 20.0),
                ),
            )
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("BI_SET"))
        }
    }

    @Test
    fun addBlock_triSet_requiresThreeExercises() = runBlocking {
        val workoutId = repository.save(Workout(name = "Shoulders"))
        val e1 = db.exerciseDao().insert(ExerciseEntity(name = "A", muscleGroup = "Ombros", equipmentType = "Halteres"))
        val e2 = db.exerciseDao().insert(ExerciseEntity(name = "B", muscleGroup = "Ombros", equipmentType = "Halteres"))
        val e3 = db.exerciseDao().insert(ExerciseEntity(name = "C", muscleGroup = "Ombros", equipmentType = "Halteres"))
        repository.addBlock(
            WorkoutBlock(workoutId = workoutId, position = 0, type = WorkoutBlockType.TRI_SET, rounds = 3, restSeconds = 90),
            listOf(
                WorkoutExercise(blockId = 0, exerciseId = e1, positionInBlock = 0, minRepetitions = 10, maxRepetitions = 12, weight = 8.0),
                WorkoutExercise(blockId = 0, exerciseId = e2, positionInBlock = 1, minRepetitions = 8, maxRepetitions = 10, weight = 12.0),
                WorkoutExercise(blockId = 0, exerciseId = e3, positionInBlock = 2, minRepetitions = 12, maxRepetitions = 15, weight = 6.0),
            ),
        )
        val exercises = repository.getExercisesForWorkout(workoutId).first()
        val blocks = repository.getBlocks(workoutId).first()
        assertEquals(1, blocks.size)
        assertEquals(WorkoutBlockType.TRI_SET, blocks.first().type)
        assertEquals(3, exercises.size)
        assertEquals(listOf(0, 1, 2), exercises.map { it.positionInBlock })
    }

    @Test
    fun addBlock_triSetWithTwoExercises_throws() = runBlocking {
        val workoutId = repository.save(Workout(name = "Shoulders"))
        val e1 = db.exerciseDao().insert(ExerciseEntity(name = "A", muscleGroup = "Ombros", equipmentType = "Halteres"))
        val e2 = db.exerciseDao().insert(ExerciseEntity(name = "B", muscleGroup = "Ombros", equipmentType = "Halteres"))
        try {
            repository.addBlock(
                WorkoutBlock(workoutId = workoutId, position = 0, type = WorkoutBlockType.TRI_SET, rounds = 3, restSeconds = 90),
                listOf(
                    WorkoutExercise(blockId = 0, exerciseId = e1, positionInBlock = 0, minRepetitions = 10, maxRepetitions = 12, weight = 8.0),
                    WorkoutExercise(blockId = 0, exerciseId = e2, positionInBlock = 1, minRepetitions = 8, maxRepetitions = 10, weight = 12.0),
                ),
            )
            throw AssertionError("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message.orEmpty().contains("TRI_SET"))
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun singleExercise(
        catalogExerciseId: Long,
        weight: Double = 60.0,
        minReps: Int = 8,
        maxReps: Int = 12,
        notes: String = "",
    ) = WorkoutExercise(
        blockId = 0,
        exerciseId = catalogExerciseId,
        positionInBlock = 0,
        minRepetitions = minReps,
        maxRepetitions = maxReps,
        weight = weight,
        notes = notes,
    )

    private suspend fun addSingleBlock(
        workoutId: Long,
        catalogExerciseId: Long,
        position: Int,
        rounds: Int = 3,
        restSeconds: Int = 90,
        weight: Double = 60.0,
        notes: String = "",
        minReps: Int = 8,
        maxReps: Int = 12,
    ): Long = repository.addBlock(
        WorkoutBlock(
            workoutId = workoutId,
            position = position,
            type = WorkoutBlockType.SINGLE,
            rounds = rounds,
            restSeconds = restSeconds,
        ),
        listOf(singleExercise(catalogExerciseId, weight, minReps, maxReps, notes)),
    )

    private suspend fun insertCatalogExercises(vararg names: String): List<Long> =
        names.map { name ->
            db.exerciseDao().insert(
                ExerciseEntity(name = name, muscleGroup = "Test", equipmentType = "None"),
            )
        }

    private suspend fun attachSinglesInOrder(workoutId: Long, catalogIds: List<Long>): List<Long> =
        catalogIds.mapIndexed { index, catalogId ->
            val blockId = addSingleBlock(workoutId, catalogId, position = index, restSeconds = 60, weight = 0.0)
            repository.getExercisesForWorkout(workoutId).first().first { it.blockId == blockId }.id
        }

    private suspend fun catalogNamesInWorkoutOrder(
        workoutId: Long,
        repo: WorkoutRepositoryImpl = repository,
    ): List<String> =
        repo.getExercises(workoutId).first().map { we ->
            db.exerciseDao().getById(we.exerciseId).first()!!.name
        }
}

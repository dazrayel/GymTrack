package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSessionExerciseStatusTest {

    private val supino = sessionExercise(id = 1, position = 0, plannedSets = 3)
    private val crucifixo = sessionExercise(id = 2, position = 1, plannedSets = 3)
    private val triceps = sessionExercise(id = 3, position = 2, plannedSets = 3)

    @Test
    fun current_isFirstIncompleteWhenNoneSkipped() {
        val index = resolveCurrentExerciseIndex(
            listOf(supino, crucifixo),
            mapOf(1L to emptyList(), 2L to emptyList()),
        )
        assertEquals(0, index)
    }

    @Test
    fun current_skipsSkippedExercise() {
        val index = resolveCurrentExerciseIndex(
            listOf(supino.copy(status = WorkoutSessionExerciseStatus.SKIPPED), crucifixo),
            mapOf(1L to emptyList(), 2L to emptyList()),
        )
        assertEquals(1, index)
    }

    @Test
    fun current_prefersInProgressWhenEarlierIncompleteIsSkipped() {
        val index = resolveCurrentExerciseIndex(
            listOf(
                supino.copy(status = WorkoutSessionExerciseStatus.SKIPPED),
                crucifixo.copy(status = WorkoutSessionExerciseStatus.SKIPPED),
                triceps.copy(status = WorkoutSessionExerciseStatus.IN_PROGRESS),
            ),
            mapOf(1L to emptyList(), 2L to emptyList(), 3L to listOf(set(3, 0))),
        )
        assertEquals(2, index)
    }

    @Test
    fun current_honoursInProgressOnLaterBlock_forOutOfOrderSelection() {
        val index = resolveCurrentExerciseIndex(
            listOf(
                supino,
                crucifixo,
                triceps.copy(status = WorkoutSessionExerciseStatus.IN_PROGRESS),
            ),
            mapOf(1L to emptyList(), 2L to emptyList(), 3L to listOf(set(3, 0))),
        )
        assertEquals(2, index)
    }

    @Test
    fun current_biSetInProgressOnSecond_stillHonoursFirstForSameRound() {
        val a = sessionExercise(id = 11, position = 0, plannedSets = 4, blockPosition = 0, positionInBlock = 0)
        val b = sessionExercise(id = 12, position = 1, plannedSets = 4, blockPosition = 0, positionInBlock = 1)
            .copy(status = WorkoutSessionExerciseStatus.IN_PROGRESS)
        val index = resolveCurrentExerciseIndex(
            listOf(a, b),
            mapOf(11L to listOf(set(11, 0), set(11, 1), set(11, 2)), 12L to listOf(set(12, 0), set(12, 1), set(12, 2))),
        )
        // Both owe round 4; selection of B must keep A current for that round.
        assertEquals(0, index)
    }

    @Test
    fun current_ignoresCompletedAndSkipped_selectsNextPending() {
        val desenvolvimento = sessionExercise(id = 4, position = 3, plannedSets = 3)
        val index = resolveCurrentExerciseIndex(
            listOf(
                supino.copy(status = WorkoutSessionExerciseStatus.COMPLETED),
                crucifixo.copy(status = WorkoutSessionExerciseStatus.SKIPPED),
                triceps,
                desenvolvimento.copy(status = WorkoutSessionExerciseStatus.COMPLETED),
            ),
            mapOf(
                1L to listOf(set(1, 0), set(1, 1), set(1, 2)),
                2L to emptyList(),
                3L to emptyList(),
                4L to listOf(set(4, 0), set(4, 1), set(4, 2)),
            ),
        )
        assertEquals(2, index)
    }

    @Test
    fun current_isNoneWhenOnlyCompletedAndSkippedRemain() {
        val desenvolvimento = sessionExercise(id = 4, position = 3, plannedSets = 3)
        val exercises = listOf(
            supino.copy(status = WorkoutSessionExerciseStatus.COMPLETED),
            crucifixo.copy(status = WorkoutSessionExerciseStatus.SKIPPED),
            triceps.copy(status = WorkoutSessionExerciseStatus.SKIPPED),
            desenvolvimento.copy(status = WorkoutSessionExerciseStatus.COMPLETED),
        )
        val sets = mapOf(
            1L to listOf(set(1, 0), set(1, 1), set(1, 2)),
            2L to emptyList(),
            3L to emptyList(),
            4L to listOf(set(4, 0), set(4, 1), set(4, 2)),
        )
        assertEquals(-1, resolveCurrentExerciseIndex(exercises, sets))
        assertFalse(areAllSessionExercisesComplete(exercises, sets))
    }

    @Test
    fun current_isNoneWhenOnlySkippedRemain() {
        val index = resolveCurrentExerciseIndex(
            listOf(supino.copy(status = WorkoutSessionExerciseStatus.SKIPPED)),
            mapOf(1L to emptyList()),
        )
        assertEquals(-1, index)
        assertFalse(
            areAllSessionExercisesComplete(
                listOf(supino.copy(status = WorkoutSessionExerciseStatus.SKIPPED)),
                mapOf(1L to emptyList()),
            ),
        )
    }

    @Test
    fun skippedIsNotCompleteWithoutAllSets() {
        assertFalse(isSessionExerciseComplete(supino.copy(status = WorkoutSessionExerciseStatus.SKIPPED), 0))
        assertFalse(isSessionExerciseComplete(supino.copy(status = WorkoutSessionExerciseStatus.SKIPPED), 2))
        assertTrue(isSessionExerciseComplete(supino.copy(status = WorkoutSessionExerciseStatus.SKIPPED), 3))
    }

    @Test
    fun unknownStatus_defaultsToPending() {
        assertEquals(WorkoutSessionExerciseStatus.PENDING, parseWorkoutSessionExerciseStatus("NOPE"))
    }

    // ─── shouldRestAfterCompletingSet ────────────────────────────────────────

    @Test
    fun shouldRest_single_trueWhenSetDoneAndRestPositive() {
        val ex = sessionExercise(id = 10, position = 0, plannedSets = 3, restSeconds = 60, blockPosition = 0)
        val result = shouldRestAfterCompletingSet(
            completedExercise = ex,
            completedSetCountAfter = 1,
            exercises = listOf(ex),
            setsByExerciseId = mapOf(10L to emptyList()),
        )
        assertTrue(result)
    }

    @Test
    fun shouldRest_single_falseWhenRestZero() {
        val ex = sessionExercise(id = 10, position = 0, plannedSets = 3, restSeconds = 0, blockPosition = 0)
        assertFalse(
            shouldRestAfterCompletingSet(
                completedExercise = ex,
                completedSetCountAfter = 1,
                exercises = listOf(ex),
                setsByExerciseId = mapOf(10L to emptyList()),
            ),
        )
    }

    @Test
    fun shouldRest_single_falseAfterLastSetOfLastExercise() {
        val ex = sessionExercise(id = 10, position = 0, plannedSets = 3, restSeconds = 60, blockPosition = 0)
        assertFalse(
            shouldRestAfterCompletingSet(
                completedExercise = ex,
                completedSetCountAfter = 3,
                exercises = listOf(ex),
                setsByExerciseId = mapOf(10L to listOf(set(10, 0), set(10, 1))),
            ),
        )
    }

    @Test
    fun shouldRest_single_trueAfterLastSetWhenAnotherExerciseRemains() {
        val first = sessionExercise(id = 10, position = 0, plannedSets = 2, restSeconds = 60, blockPosition = 0)
        val second = sessionExercise(id = 11, position = 1, plannedSets = 2, restSeconds = 60, blockPosition = 1)
        assertTrue(
            shouldRestAfterCompletingSet(
                completedExercise = first,
                completedSetCountAfter = 2,
                exercises = listOf(first, second),
                setsByExerciseId = mapOf(10L to listOf(set(10, 0)), 11L to emptyList()),
            ),
        )
    }

    @Test
    fun shouldRest_biSet_fullThreeRoundCycle() {
        val a = sessionExercise(
            id = 11, position = 0, plannedSets = 3, restSeconds = 60,
            blockPosition = 0, positionInBlock = 0, blockType = WorkoutBlockType.BI_SET,
        )
        val b = sessionExercise(
            id = 12, position = 1, plannedSets = 3, restSeconds = 60,
            blockPosition = 0, positionInBlock = 1, blockType = WorkoutBlockType.BI_SET,
        )
        val next = sessionExercise(id = 13, position = 2, plannedSets = 2, restSeconds = 30, blockPosition = 1)
        val block = listOf(a, b, next)

        // Round 1: A → no rest; B → rest
        assertFalse(
            shouldRestAfterCompletingSet(a, 1, block, mapOf(11L to emptyList(), 12L to emptyList())),
        )
        assertTrue(
            shouldRestAfterCompletingSet(b, 1, block, mapOf(11L to listOf(set(11, 0)), 12L to emptyList())),
        )

        // Round 2: A → no rest; B → rest
        assertFalse(
            shouldRestAfterCompletingSet(
                a, 2, block,
                mapOf(11L to listOf(set(11, 0)), 12L to listOf(set(12, 0))),
            ),
        )
        assertTrue(
            shouldRestAfterCompletingSet(
                b, 2, block,
                mapOf(11L to listOf(set(11, 0), set(11, 1)), 12L to listOf(set(12, 0))),
            ),
        )

        // Round 3: A → no rest; B → rest because another block remains
        assertFalse(
            shouldRestAfterCompletingSet(
                a, 3, block,
                mapOf(11L to listOf(set(11, 0), set(11, 1)), 12L to listOf(set(12, 0), set(12, 1))),
            ),
        )
        assertTrue(
            shouldRestAfterCompletingSet(
                b, 3, block,
                mapOf(
                    11L to listOf(set(11, 0), set(11, 1), set(11, 2)),
                    12L to listOf(set(12, 0), set(12, 1)),
                ),
            ),
        )

        // After B round 3: advance to next block
        assertEquals(
            2,
            resolveCurrentExerciseIndex(
                block,
                mapOf(
                    11L to listOf(set(11, 0), set(11, 1), set(11, 2)),
                    12L to listOf(set(12, 0), set(12, 1), set(12, 2)),
                    13L to emptyList(),
                ),
            ),
        )
    }

    @Test
    fun shouldRest_biSet_falseAfterLastRoundWhenNoFurtherWork() {
        val a = sessionExercise(
            id = 11, position = 0, plannedSets = 2, restSeconds = 60,
            blockPosition = 0, positionInBlock = 0, blockType = WorkoutBlockType.BI_SET,
        )
        val b = sessionExercise(
            id = 12, position = 1, plannedSets = 2, restSeconds = 60,
            blockPosition = 0, positionInBlock = 1, blockType = WorkoutBlockType.BI_SET,
        )
        assertFalse(
            shouldRestAfterCompletingSet(
                b, 2, listOf(a, b),
                mapOf(
                    11L to listOf(set(11, 0), set(11, 1)),
                    12L to listOf(set(12, 0)),
                ),
            ),
        )
    }

    @Test
    fun shouldRest_biSet_falseWhenOtherExerciseHasNotFinishedRound() {
        val ex1 = sessionExercise(
            id = 11, position = 0, plannedSets = 3, restSeconds = 60,
            blockPosition = 5, positionInBlock = 0, blockType = WorkoutBlockType.BI_SET,
        )
        val ex2 = sessionExercise(
            id = 12, position = 1, plannedSets = 3, restSeconds = 60,
            blockPosition = 5, positionInBlock = 1, blockType = WorkoutBlockType.BI_SET,
        )
        val block = listOf(ex1, ex2)

        assertFalse(
            shouldRestAfterCompletingSet(
                completedExercise = ex1,
                completedSetCountAfter = 1,
                exercises = block,
                setsByExerciseId = mapOf(11L to emptyList(), 12L to emptyList()),
            ),
        )
    }

    @Test
    fun shouldRest_biSet_skippedSlotDoesNotBlockRoundRest() {
        val a = sessionExercise(
            id = 11, position = 0, plannedSets = 3, restSeconds = 60,
            blockPosition = 5, positionInBlock = 0, blockType = WorkoutBlockType.BI_SET,
        ).copy(status = WorkoutSessionExerciseStatus.SKIPPED)
        val b = sessionExercise(
            id = 12, position = 1, plannedSets = 3, restSeconds = 60,
            blockPosition = 5, positionInBlock = 1, blockType = WorkoutBlockType.BI_SET,
        )
        // Only B is active; completing round 1 should rest (not last round).
        assertTrue(
            shouldRestAfterCompletingSet(
                completedExercise = b,
                completedSetCountAfter = 1,
                exercises = listOf(a, b),
                setsByExerciseId = mapOf(11L to emptyList(), 12L to emptyList()),
            ),
        )
        // Completing last round of B alone: no rest.
        assertFalse(
            shouldRestAfterCompletingSet(
                completedExercise = b,
                completedSetCountAfter = 3,
                exercises = listOf(a, b),
                setsByExerciseId = mapOf(11L to emptyList(), 12L to listOf(set(12, 0), set(12, 1))),
            ),
        )
    }

    @Test
    fun shouldRest_triSet_fullThreeRoundCycle() {
        val a = sessionExercise(
            id = 21, position = 0, plannedSets = 3, restSeconds = 45,
            blockPosition = 0, positionInBlock = 0, blockType = WorkoutBlockType.TRI_SET,
        )
        val b = sessionExercise(
            id = 22, position = 1, plannedSets = 3, restSeconds = 45,
            blockPosition = 0, positionInBlock = 1, blockType = WorkoutBlockType.TRI_SET,
        )
        val c = sessionExercise(
            id = 23, position = 2, plannedSets = 3, restSeconds = 45,
            blockPosition = 0, positionInBlock = 2, blockType = WorkoutBlockType.TRI_SET,
        )
        val next = sessionExercise(id = 24, position = 3, plannedSets = 2, restSeconds = 30, blockPosition = 1)
        val exercises = listOf(a, b, c, next)

        // Round 1: A/B no rest; C → rest
        assertFalse(shouldRestAfterCompletingSet(a, 1, exercises, mapOf(21L to emptyList(), 22L to emptyList(), 23L to emptyList())))
        assertFalse(
            shouldRestAfterCompletingSet(
                b, 1, exercises,
                mapOf(21L to listOf(set(21, 0)), 22L to emptyList(), 23L to emptyList()),
            ),
        )
        assertTrue(
            shouldRestAfterCompletingSet(
                c, 1, exercises,
                mapOf(21L to listOf(set(21, 0)), 22L to listOf(set(22, 0)), 23L to emptyList()),
            ),
        )

        // Round 2: C → rest
        assertFalse(
            shouldRestAfterCompletingSet(
                a, 2, exercises,
                mapOf(21L to listOf(set(21, 0)), 22L to listOf(set(22, 0)), 23L to listOf(set(23, 0))),
            ),
        )
        assertFalse(
            shouldRestAfterCompletingSet(
                b, 2, exercises,
                mapOf(
                    21L to listOf(set(21, 0), set(21, 1)),
                    22L to listOf(set(22, 0)),
                    23L to listOf(set(23, 0)),
                ),
            ),
        )
        assertTrue(
            shouldRestAfterCompletingSet(
                c, 2, exercises,
                mapOf(
                    21L to listOf(set(21, 0), set(21, 1)),
                    22L to listOf(set(22, 0), set(22, 1)),
                    23L to listOf(set(23, 0)),
                ),
            ),
        )

        // Round 3: A/B no rest; C → rest because another block remains
        assertFalse(
            shouldRestAfterCompletingSet(
                a, 3, exercises,
                mapOf(
                    21L to listOf(set(21, 0), set(21, 1)),
                    22L to listOf(set(22, 0), set(22, 1)),
                    23L to listOf(set(23, 0), set(23, 1)),
                ),
            ),
        )
        assertFalse(
            shouldRestAfterCompletingSet(
                b, 3, exercises,
                mapOf(
                    21L to listOf(set(21, 0), set(21, 1), set(21, 2)),
                    22L to listOf(set(22, 0), set(22, 1)),
                    23L to listOf(set(23, 0), set(23, 1)),
                ),
            ),
        )
        assertTrue(
            shouldRestAfterCompletingSet(
                c, 3, exercises,
                mapOf(
                    21L to listOf(set(21, 0), set(21, 1), set(21, 2)),
                    22L to listOf(set(22, 0), set(22, 1), set(22, 2)),
                    23L to listOf(set(23, 0), set(23, 1)),
                ),
            ),
        )
        assertEquals(
            3,
            resolveCurrentExerciseIndex(
                exercises,
                mapOf(
                    21L to listOf(set(21, 0), set(21, 1), set(21, 2)),
                    22L to listOf(set(22, 0), set(22, 1), set(22, 2)),
                    23L to listOf(set(23, 0), set(23, 1), set(23, 2)),
                    24L to emptyList(),
                ),
            ),
        )
    }

    // ─── resolveCurrentExerciseIndex — block-aware ────────────────────────────

    @Test
    fun resolve_biSet_firstExerciseIsActiveInitially() {
        val ex1 = sessionExercise(
            id = 31, position = 0, plannedSets = 3,
            blockPosition = 0, positionInBlock = 0, blockType = WorkoutBlockType.BI_SET,
        )
        val ex2 = sessionExercise(
            id = 32, position = 1, plannedSets = 3,
            blockPosition = 0, positionInBlock = 1, blockType = WorkoutBlockType.BI_SET,
        )
        val idx = resolveCurrentExerciseIndex(
            listOf(ex1, ex2),
            mapOf(31L to emptyList(), 32L to emptyList()),
        )
        assertEquals(0, idx)
    }

    @Test
    fun resolve_biSet_secondExerciseActiveAfterFirstCompletesRound() {
        val ex1 = sessionExercise(
            id = 31, position = 0, plannedSets = 3,
            blockPosition = 0, positionInBlock = 0, blockType = WorkoutBlockType.BI_SET,
        )
        val ex2 = sessionExercise(
            id = 32, position = 1, plannedSets = 3,
            blockPosition = 0, positionInBlock = 1, blockType = WorkoutBlockType.BI_SET,
        )
        val idx = resolveCurrentExerciseIndex(
            listOf(ex1, ex2),
            mapOf(31L to listOf(set(31, 0)), 32L to emptyList()),
        )
        assertEquals(1, idx)
    }

    @Test
    fun resolve_triSet_advancesCorrectly() {
        val ex1 = sessionExercise(
            id = 41, position = 0, plannedSets = 2,
            blockPosition = 0, positionInBlock = 0, blockType = WorkoutBlockType.TRI_SET,
        )
        val ex2 = sessionExercise(
            id = 42, position = 1, plannedSets = 2,
            blockPosition = 0, positionInBlock = 1, blockType = WorkoutBlockType.TRI_SET,
        )
        val ex3 = sessionExercise(
            id = 43, position = 2, plannedSets = 2,
            blockPosition = 0, positionInBlock = 2, blockType = WorkoutBlockType.TRI_SET,
        )
        val exercises = listOf(ex1, ex2, ex3)

        assertEquals(0, resolveCurrentExerciseIndex(exercises, mapOf(41L to emptyList(), 42L to emptyList(), 43L to emptyList())))
        assertEquals(1, resolveCurrentExerciseIndex(exercises, mapOf(41L to listOf(set(41, 0)), 42L to emptyList(), 43L to emptyList())))
        assertEquals(2, resolveCurrentExerciseIndex(exercises, mapOf(41L to listOf(set(41, 0)), 42L to listOf(set(42, 0)), 43L to emptyList())))
        assertEquals(0, resolveCurrentExerciseIndex(exercises, mapOf(41L to listOf(set(41, 0)), 42L to listOf(set(42, 0)), 43L to listOf(set(43, 0)))))
    }

    @Test
    fun biSet_completingLastSetOfFirst_doesNotRestOrSkipSecondLastSet() {
        val squat = sessionExercise(
            id = 51, position = 0, plannedSets = 4, restSeconds = 60,
            blockPosition = 0, positionInBlock = 0, blockType = WorkoutBlockType.BI_SET,
        )
        val calf = sessionExercise(
            id = 52, position = 1, plannedSets = 4, restSeconds = 60,
            blockPosition = 0, positionInBlock = 1, blockType = WorkoutBlockType.BI_SET,
            status = WorkoutSessionExerciseStatus.IN_PROGRESS,
        )
        val next = sessionExercise(id = 53, position = 2, plannedSets = 2, restSeconds = 30, blockPosition = 1)
        val exercises = listOf(squat, calf, next)
        val setsAfterSquatLast = mapOf(
            51L to listOf(set(51, 0), set(51, 1), set(51, 2)),
            52L to listOf(set(52, 0), set(52, 1), set(52, 2)),
            53L to emptyList(),
        )

        assertFalse(
            shouldRestAfterCompletingSet(
                completedExercise = squat,
                completedSetCountAfter = 4,
                exercises = exercises,
                setsByExerciseId = setsAfterSquatLast,
            ),
        )
        assertFalse(
            isBlockRoundComplete(
                block = listOf(squat, calf),
                completedExerciseId = squat.id,
                completedSetCountAfter = 4,
                setsByExerciseId = setsAfterSquatLast,
            ),
        )

        val afterComplete = setsAfterSquatLast + (
            51L to listOf(set(51, 0), set(51, 1), set(51, 2), set(51, 3))
            )
        assertEquals(1, resolveCurrentExerciseIndex(exercises, afterComplete))
        assertEquals(calf.id, exercises[resolveCurrentExerciseIndex(exercises, afterComplete)].id)
    }

    @Test
    fun biSet_selectingSecondExercise_stillHonoursRoundOrder() {
        val squat = sessionExercise(
            id = 61, position = 0, plannedSets = 4, restSeconds = 60,
            blockPosition = 0, positionInBlock = 0, blockType = WorkoutBlockType.BI_SET,
        )
        val calf = sessionExercise(
            id = 62, position = 1, plannedSets = 4, restSeconds = 60,
            blockPosition = 0, positionInBlock = 1, blockType = WorkoutBlockType.BI_SET,
            status = WorkoutSessionExerciseStatus.IN_PROGRESS,
        )
        val exercises = listOf(squat, calf)
        // Both owe round 4 (3 sets each); selection of calf must not make calf current.
        val sets = mapOf(
            61L to listOf(set(61, 0), set(61, 1), set(61, 2)),
            62L to listOf(set(62, 0), set(62, 1), set(62, 2)),
        )
        assertEquals(0, resolveCurrentExerciseIndex(exercises, sets))
    }

    @Test
    fun biSet_afterFullRoundRest_nextIsFirstExerciseOfNextRound() {
        val squat = sessionExercise(
            id = 71, position = 0, plannedSets = 4, restSeconds = 60,
            blockPosition = 0, positionInBlock = 0, blockType = WorkoutBlockType.BI_SET,
        )
        val calf = sessionExercise(
            id = 72, position = 1, plannedSets = 4, restSeconds = 60,
            blockPosition = 0, positionInBlock = 1, blockType = WorkoutBlockType.BI_SET,
        )
        val exercises = listOf(squat, calf)
        val afterRound3 = mapOf(
            71L to listOf(set(71, 0), set(71, 1), set(71, 2)),
            72L to listOf(set(72, 0), set(72, 1), set(72, 2)),
        )
        assertTrue(
            shouldRestAfterCompletingSet(
                completedExercise = calf,
                completedSetCountAfter = 3,
                exercises = exercises,
                setsByExerciseId = mapOf(
                    71L to listOf(set(71, 0), set(71, 1), set(71, 2)),
                    72L to listOf(set(72, 0), set(72, 1)),
                ),
            ),
        )
        assertEquals(0, resolveCurrentExerciseIndex(exercises, afterRound3))
    }

    @Test
    fun triSet_completingOneLastSet_doesNotEndRoundWhilePartnersPending() {
        val a = sessionExercise(
            id = 81, position = 0, plannedSets = 3, restSeconds = 45,
            blockPosition = 0, positionInBlock = 0, blockType = WorkoutBlockType.TRI_SET,
        )
        val b = sessionExercise(
            id = 82, position = 1, plannedSets = 3, restSeconds = 45,
            blockPosition = 0, positionInBlock = 1, blockType = WorkoutBlockType.TRI_SET,
            status = WorkoutSessionExerciseStatus.IN_PROGRESS,
        )
        val c = sessionExercise(
            id = 83, position = 2, plannedSets = 3, restSeconds = 45,
            blockPosition = 0, positionInBlock = 2, blockType = WorkoutBlockType.TRI_SET,
        )
        val exercises = listOf(a, b, c)
        val sets = mapOf(
            81L to listOf(set(81, 0), set(81, 1)),
            82L to listOf(set(82, 0), set(82, 1)),
            83L to listOf(set(83, 0), set(83, 1)),
        )
        assertFalse(
            shouldRestAfterCompletingSet(
                completedExercise = a,
                completedSetCountAfter = 3,
                exercises = exercises,
                setsByExerciseId = sets,
            ),
        )
        val afterA = sets + (81L to listOf(set(81, 0), set(81, 1), set(81, 2)))
        assertEquals(1, resolveCurrentExerciseIndex(exercises, afterA))
    }

    @Test
    fun skippedPartner_isNotTreatedAsPendingForRestOrResolve() {
        val squat = sessionExercise(
            id = 91, position = 0, plannedSets = 2, restSeconds = 60,
            blockPosition = 0, positionInBlock = 0, blockType = WorkoutBlockType.BI_SET,
        )
        val calf = sessionExercise(
            id = 92, position = 1, plannedSets = 2, restSeconds = 60,
            blockPosition = 0, positionInBlock = 1, blockType = WorkoutBlockType.BI_SET,
            status = WorkoutSessionExerciseStatus.SKIPPED,
        )
        val next = sessionExercise(id = 93, position = 2, plannedSets = 1, restSeconds = 30, blockPosition = 1)
        val exercises = listOf(squat, calf, next)
        assertTrue(
            shouldRestAfterCompletingSet(
                completedExercise = squat,
                completedSetCountAfter = 1,
                exercises = exercises,
                setsByExerciseId = mapOf(91L to emptyList(), 92L to emptyList(), 93L to emptyList()),
            ),
        )
        val afterSquatDone = mapOf(
            91L to listOf(set(91, 0), set(91, 1)),
            92L to emptyList(),
            93L to emptyList(),
        )
        assertEquals(2, resolveCurrentExerciseIndex(exercises, afterSquatDone))
    }

    private fun sessionExercise(
        id: Long,
        position: Int,
        plannedSets: Int,
        restSeconds: Int = 0,
        blockPosition: Int = position,
        positionInBlock: Int = 0,
        blockType: WorkoutBlockType = WorkoutBlockType.SINGLE,
        status: WorkoutSessionExerciseStatus = WorkoutSessionExerciseStatus.PENDING,
    ) = WorkoutSessionExercise(
        id = id,
        sessionId = 1L,
        position = position,
        exerciseName = "E$id",
        muscleGroup = "Peito",
        equipmentType = "Barra",
        plannedSets = plannedSets,
        minRepetitions = 8,
        maxRepetitions = 12,
        plannedWeight = 40.0,
        restSeconds = restSeconds,
        status = status,
        blockType = blockType,
        blockPosition = blockPosition,
        positionInBlock = positionInBlock,
    )

    private fun set(sessionExerciseId: Long, setIndex: Int) = WorkoutSet(
        id = sessionExerciseId * 10 + setIndex,
        sessionExerciseId = sessionExerciseId,
        setIndex = setIndex,
        reps = 8,
        weight = 40.0,
        completedAtMillis = 1L,
    )
}

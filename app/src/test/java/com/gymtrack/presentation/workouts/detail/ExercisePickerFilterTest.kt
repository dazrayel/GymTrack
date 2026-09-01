package com.gymtrack.presentation.workouts.detail

import com.gymtrack.domain.model.Exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExercisePickerFilterTest {

    private val bench = Exercise(1, "Supino", "Peitoral", "Barra")
    private val curl = Exercise(2, "Rosca", "Bíceps", "Halter")
    private val squat = Exercise(3, "Agachamento", "Quadríceps", "Barra")
    private val all = listOf(bench, curl, squat)

    @Test
    fun emptyQuery_returnsFullListInOriginalOrder() {
        assertEquals(all, filterExercisesByName(all, ""))
        assertEquals(all, filterExercisesByName(all, "   "))
    }

    @Test
    fun query_filtersByNameSubstring() {
        assertEquals(listOf(curl), filterExercisesByName(all, "osc"))
    }

    @Test
    fun query_isCaseInsensitive() {
        assertEquals(listOf(bench), filterExercisesByName(all, "supino"))
        assertEquals(listOf(bench), filterExercisesByName(all, "SUPINO"))
        assertEquals(listOf(bench), filterExercisesByName(all, "SuPiNo"))
    }

    @Test
    fun query_noMatches_returnsEmptyList() {
        assertTrue(filterExercisesByName(all, "xyz").isEmpty())
    }

    @Test
    fun query_preservesRelativeOrder() {
        val result = filterExercisesByName(all, "a")
        assertEquals(listOf(curl, squat), result)
    }
}

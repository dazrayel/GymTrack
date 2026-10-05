package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class WeeklyWorkoutPlanTest {

    @Test
    fun buildSlots_startsWithConfiguredFirstDay() {
        val slots = buildWeeklyPlanSlots(emptyList(), DayOfWeek.SUNDAY)
        assertEquals(7, slots.size)
        assertEquals(DayOfWeek.SUNDAY, slots.first().dayOfWeek)
        assertEquals(DayOfWeek.SATURDAY, slots.last().dayOfWeek)
        assertTrue(slots.none { it.hasPlan })
    }

    @Test
    fun buildSlots_mergesPlansByDay() {
        val plans = listOf(
            WeeklyWorkoutPlan(DayOfWeek.MONDAY, 1L, "Peito"),
            WeeklyWorkoutPlan(DayOfWeek.FRIDAY, 2L, "Pernas"),
        )
        val slots = buildWeeklyPlanSlots(plans, DayOfWeek.MONDAY)

        assertEquals(1L, slots[0].plannedWorkoutId)
        assertEquals("Peito", slots[0].plannedWorkoutName)
        assertNull(slots[1].plannedWorkoutId)
        assertEquals(2L, slots[4].plannedWorkoutId)
        assertEquals(2, slots.count { it.hasPlan })
    }

    @Test
    fun buildSlots_onePlanPerDay_evenIfDuplicateInput() {
        val plans = listOf(
            WeeklyWorkoutPlan(DayOfWeek.TUESDAY, 1L, "A"),
            WeeklyWorkoutPlan(DayOfWeek.TUESDAY, 2L, "B"),
        )
        val slots = buildWeeklyPlanSlots(plans, DayOfWeek.SUNDAY)
        val tuesday = slots.first { it.dayOfWeek == DayOfWeek.TUESDAY }
        // associateBy keeps last
        assertEquals(2L, tuesday.plannedWorkoutId)
        assertEquals(1, slots.count { it.hasPlan })
    }

    @Test
    fun slot_withoutWorkout_isEmpty() {
        val slot = WeeklyPlanDaySlot(DayOfWeek.WEDNESDAY)
        assertFalse(slot.hasPlan)
        assertNull(slot.plannedWorkoutId)
        assertNull(slot.plannedWorkoutName)
    }
}

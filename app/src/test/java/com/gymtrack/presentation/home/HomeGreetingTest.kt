package com.gymtrack.presentation.home

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class HomeGreetingTest {

    @Test
    fun formatHomeGreeting_withoutUser_returnsBase() {
        assertEquals(
            "Bom dia",
            formatHomeGreeting("Bom dia", "Bom dia, %1\$s", null),
        )
    }

    @Test
    fun formatHomeGreeting_withDisplayName_appendsName() {
        assertEquals(
            "Bom dia, Danilo",
            formatHomeGreeting("Bom dia", "Bom dia, %1\$s", "Danilo"),
        )
    }

    @Test
    fun formatHomeGreeting_blankName_returnsBase() {
        assertEquals(
            "Boa tarde",
            formatHomeGreeting("Boa tarde", "Boa tarde, %1\$s", "   "),
        )
    }

    @Test
    fun homeGreetingStringResForHour_morningAfternoonEvening() {
        assertEquals(HomeGreetingPeriod.MORNING, homeGreetingStringResForHour(8))
        assertEquals(HomeGreetingPeriod.AFTERNOON, homeGreetingStringResForHour(14))
        assertEquals(HomeGreetingPeriod.EVENING, homeGreetingStringResForHour(20))
    }
}

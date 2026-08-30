package com.gymtrack.domain.time

import org.junit.Assert.assertEquals
import org.junit.Test

class ElapsedTimeTest {

    @Test
    fun formatsZeroAsMinutesAndSeconds() {
        assertEquals("00:00", formatElapsedMillis(0L))
    }

    @Test
    fun formatsOneSecond() {
        assertEquals("00:01", formatElapsedMillis(1_000L))
    }

    @Test
    fun formatsFiftyNineSeconds() {
        assertEquals("00:59", formatElapsedMillis(59_000L))
    }

    @Test
    fun formatsSixtySecondsAsOneMinute() {
        assertEquals("01:00", formatElapsedMillis(60_000L))
    }

    @Test
    fun formatsOneMinuteAndFiveSeconds() {
        assertEquals("01:05", formatElapsedMillis(65_000L))
    }

    @Test
    fun formatsOneHourTwoMinutesThreeSeconds() {
        assertEquals("01:02:03", formatElapsedMillis(3_723_000L))
    }
}

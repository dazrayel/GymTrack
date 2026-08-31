package com.gymtrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RepetitionTargetTest {

    @Test
    fun range_usesEnDashBetweenMinAndMax() {
        assertEquals("8–12", formatRepetitionTarget(8, 12))
    }

    @Test
    fun equalMinAndMax_isSingleValue() {
        assertEquals("8", formatRepetitionTarget(8, 8))
    }

    @Test
    fun invalidValues_areOmitted() {
        assertNull(formatRepetitionTarget(0, 12))
        assertNull(formatRepetitionTarget(8, 0))
        assertNull(formatRepetitionTarget(12, 8))
        assertNull(formatRepetitionTarget(-1, 8))
    }
}

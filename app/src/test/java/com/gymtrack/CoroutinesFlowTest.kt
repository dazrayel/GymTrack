package com.gymtrack

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CoroutinesFlowTest {

    @Test
    fun flow_emitsExpectedValue() = runTest {
        val source = flowOf("GymTrack")

        val result = source.first()

        assertEquals("GymTrack", result)
    }
}

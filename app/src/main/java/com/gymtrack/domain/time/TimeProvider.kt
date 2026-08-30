package com.gymtrack.domain.time

fun interface TimeProvider {
    fun nowMillis(): Long
}

class SystemTimeProvider : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

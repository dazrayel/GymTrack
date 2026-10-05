package com.gymtrack.presentation.home

fun formatHomeGreeting(
    baseGreeting: String,
    greetingWithName: String,
    displayName: String?,
): String {
    val name = displayName?.trim()?.takeIf { it.isNotEmpty() }
    return if (name == null) {
        baseGreeting
    } else {
        greetingWithName.format(name)
    }
}

fun homeGreetingStringResForHour(hour: Int): HomeGreetingPeriod = when {
    hour < 12 -> HomeGreetingPeriod.MORNING
    hour < 18 -> HomeGreetingPeriod.AFTERNOON
    else -> HomeGreetingPeriod.EVENING
}

enum class HomeGreetingPeriod {
    MORNING,
    AFTERNOON,
    EVENING,
}

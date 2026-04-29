package com.example.moves.data

data class Settings(
    val enabled: Boolean = true,
    val intervalMinutes: Int = 60,
    val moveDurationMinutes: Int = 2,
    val snoozeMinutes: Int = 10,
    val activeStartMinuteOfDay: Int = 9 * 60,
    val activeEndMinuteOfDay: Int = 18 * 60,
    val recentMovementSkipThresholdSteps: Int = 200,
)

data class ScheduleState(
    val lastFiredType: String = "Water",
    val nextScheduledAtEpochMillis: Long = 0L,
)

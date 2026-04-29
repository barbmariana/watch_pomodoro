package com.example.moves.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val INTERVAL_MIN = intPreferencesKey("interval_min")
        val MOVE_DURATION_MIN = intPreferencesKey("move_duration_min")
        val SNOOZE_MIN = intPreferencesKey("snooze_min")
        val ACTIVE_START_MIN = intPreferencesKey("active_start_min")
        val ACTIVE_END_MIN = intPreferencesKey("active_end_min")
        val SKIP_THRESHOLD = intPreferencesKey("skip_threshold_steps")
        val LAST_FIRED_TYPE = stringPreferencesKey("last_fired_type")
        val NEXT_SCHEDULED_AT = longPreferencesKey("next_scheduled_at")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        val defaults = Settings()
        Settings(
            enabled = prefs[Keys.ENABLED] ?: defaults.enabled,
            intervalMinutes = prefs[Keys.INTERVAL_MIN] ?: defaults.intervalMinutes,
            moveDurationMinutes = prefs[Keys.MOVE_DURATION_MIN] ?: defaults.moveDurationMinutes,
            snoozeMinutes = prefs[Keys.SNOOZE_MIN] ?: defaults.snoozeMinutes,
            activeStartMinuteOfDay = prefs[Keys.ACTIVE_START_MIN] ?: defaults.activeStartMinuteOfDay,
            activeEndMinuteOfDay = prefs[Keys.ACTIVE_END_MIN] ?: defaults.activeEndMinuteOfDay,
            recentMovementSkipThresholdSteps = prefs[Keys.SKIP_THRESHOLD] ?: defaults.recentMovementSkipThresholdSteps,
        )
    }

    val scheduleState: Flow<ScheduleState> = context.dataStore.data.map { prefs ->
        val defaults = ScheduleState()
        ScheduleState(
            lastFiredType = prefs[Keys.LAST_FIRED_TYPE] ?: defaults.lastFiredType,
            nextScheduledAtEpochMillis = prefs[Keys.NEXT_SCHEDULED_AT] ?: defaults.nextScheduledAtEpochMillis,
        )
    }

    suspend fun currentSettings(): Settings = settings.first()
    suspend fun currentScheduleState(): ScheduleState = scheduleState.first()

    suspend fun setEnabled(value: Boolean) = update { it[Keys.ENABLED] = value }
    suspend fun setInterval(min: Int) = update { it[Keys.INTERVAL_MIN] = min }
    suspend fun setMoveDuration(min: Int) = update { it[Keys.MOVE_DURATION_MIN] = min }
    suspend fun setSnooze(min: Int) = update { it[Keys.SNOOZE_MIN] = min }
    suspend fun setActiveStart(min: Int) = update { it[Keys.ACTIVE_START_MIN] = min }
    suspend fun setActiveEnd(min: Int) = update { it[Keys.ACTIVE_END_MIN] = min }

    suspend fun setLastFiredType(type: String) = update { it[Keys.LAST_FIRED_TYPE] = type }
    suspend fun setNextScheduledAt(epochMillis: Long) = update { it[Keys.NEXT_SCHEDULED_AT] = epochMillis }

    private suspend fun update(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { prefs -> block(prefs) }
    }
}

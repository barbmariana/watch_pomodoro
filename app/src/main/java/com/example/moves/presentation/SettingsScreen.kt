package com.example.moves.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.example.moves.R
import com.example.moves.data.Settings
import com.example.moves.data.SettingsRepository
import com.example.moves.domain.ReminderScheduler
import kotlinx.coroutines.launch

private val INTERVAL_OPTIONS = listOf(30, 45, 60, 90)
private val MOVE_DURATION_OPTIONS = listOf(1, 2, 3, 5)
private val SNOOZE_OPTIONS = listOf(5, 10, 15)
private val ACTIVE_HOUR_OPTIONS = (5..22).map { it * 60 }

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = Settings())
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        TimeText()
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Title(text = stringResource(R.string.settings_title))
            }
            item {
                CycleChip(
                    label = stringResource(R.string.settings_interval),
                    value = stringResource(R.string.unit_min, settings.intervalMinutes),
                ) {
                    val next = nextInList(settings.intervalMinutes, INTERVAL_OPTIONS)
                    scope.launch {
                        repo.setInterval(next)
                        ReminderScheduler.scheduleNext(context, repo)
                    }
                }
            }
            item {
                CycleChip(
                    label = stringResource(R.string.settings_move_duration),
                    value = stringResource(R.string.unit_min, settings.moveDurationMinutes),
                ) {
                    val next = nextInList(settings.moveDurationMinutes, MOVE_DURATION_OPTIONS)
                    scope.launch { repo.setMoveDuration(next) }
                }
            }
            item {
                CycleChip(
                    label = stringResource(R.string.settings_snooze),
                    value = stringResource(R.string.unit_min, settings.snoozeMinutes),
                ) {
                    val next = nextInList(settings.snoozeMinutes, SNOOZE_OPTIONS)
                    scope.launch { repo.setSnooze(next) }
                }
            }
            item {
                CycleChip(
                    label = stringResource(R.string.settings_active_start),
                    value = formatHourMin(settings.activeStartMinuteOfDay),
                ) {
                    val next = nextInList(settings.activeStartMinuteOfDay, ACTIVE_HOUR_OPTIONS)
                    scope.launch {
                        repo.setActiveStart(next)
                        ReminderScheduler.scheduleNext(context, repo)
                    }
                }
            }
            item {
                CycleChip(
                    label = stringResource(R.string.settings_active_end),
                    value = formatHourMin(settings.activeEndMinuteOfDay),
                ) {
                    val next = nextInList(settings.activeEndMinuteOfDay, ACTIVE_HOUR_OPTIONS)
                    scope.launch {
                        repo.setActiveEnd(next)
                        ReminderScheduler.scheduleNext(context, repo)
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.title3,
        color = MaterialTheme.colors.primary,
    )
}

@Composable
private fun CycleChip(label: String, value: String, onClick: () -> Unit) {
    Chip(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        label = { Text(label) },
        secondaryLabel = { Text(value) },
    )
}

private fun <T> nextInList(current: T, options: List<T>): T {
    val idx = options.indexOf(current)
    return if (idx < 0) options.first() else options[(idx + 1) % options.size]
}

private fun formatHourMin(minuteOfDay: Int): String {
    val h = minuteOfDay / 60
    val m = minuteOfDay % 60
    return "%02d:%02d".format(h, m)
}

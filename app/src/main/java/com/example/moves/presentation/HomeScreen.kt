package com.example.moves.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import com.example.moves.R
import com.example.moves.data.ScheduleState
import com.example.moves.data.Settings
import com.example.moves.data.SettingsRepository
import com.example.moves.domain.ReminderScheduler
import com.example.moves.domain.ReminderType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val pair by remember {
        combine(repo.settings, repo.scheduleState) { s, st -> s to st }
    }.collectAsStateWithLifecycle(initialValue = Settings() to ScheduleState())
    val (settings, state) = pair

    val scope = rememberCoroutineScope()

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    LaunchedEffect(settings.enabled, state.nextScheduledAtEpochMillis) {
        if (settings.enabled && state.nextScheduledAtEpochMillis <= nowMs) {
            ReminderScheduler.scheduleNext(context, repo)
        }
    }

    Scaffold(
        timeText = { TimeText() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.home_title),
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.primary,
                )
                Spacer(Modifier.height(2.dp))

                if (settings.enabled) {
                    val remainingMs = (state.nextScheduledAtEpochMillis - nowMs).coerceAtLeast(0L)
                    val nextType = ReminderType.fromString(state.lastFiredType).next()
                    Text(
                        text = stringResource(R.string.home_next_in, formatRemaining(remainingMs)),
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = nextType.displayLabel(),
                        style = MaterialTheme.typography.caption3,
                        color = MaterialTheme.colors.secondary,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.home_paused),
                        style = MaterialTheme.typography.body2,
                    )
                }

                Spacer(Modifier.height(8.dp))
                ToggleChip(
                    modifier = Modifier.fillMaxWidth(),
                    checked = settings.enabled,
                    onCheckedChange = { checked ->
                        scope.launch {
                            repo.setEnabled(checked)
                            if (checked) ReminderScheduler.scheduleNext(context, repo)
                            else ReminderScheduler.cancel(context)
                        }
                    },
                    label = {
                        Text(
                            stringResource(R.string.home_enabled),
                            style = MaterialTheme.typography.caption2,
                        )
                    },
                    toggleControl = {
                        Text(
                            if (settings.enabled) "On" else "Off",
                            style = MaterialTheme.typography.caption3,
                        )
                    },
                )
                Spacer(Modifier.height(4.dp))
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenSettings,
                    colors = ChipDefaults.secondaryChipColors(),
                    label = {
                        Text(
                            stringResource(R.string.home_settings),
                            style = MaterialTheme.typography.caption2,
                        )
                    },
                )
            }
        }
    }
}

private fun ReminderType.displayLabel(): String = when (this) {
    ReminderType.Move -> "Move"
    ReminderType.Water -> "Water"
}

private fun formatRemaining(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "${m}m ${s}s"
}

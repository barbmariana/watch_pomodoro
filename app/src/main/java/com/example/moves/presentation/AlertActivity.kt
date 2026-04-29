package com.example.moves.presentation

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.runtime.collectAsState
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
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.moves.R
import com.example.moves.data.SettingsRepository
import com.example.moves.domain.AlertNotifier
import com.example.moves.domain.MovementDetector
import com.example.moves.domain.ReminderScheduler
import com.example.moves.domain.ReminderType
import com.example.moves.presentation.theme.MovesTheme
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.Instant

class AlertActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TYPE = "extra_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)
                ?.requestDismissKeyguard(this, null)
        }
        // The full-screen-intent notification has launched us; dismiss it so it
        // doesn't sit in the watch's notification stream.
        AlertNotifier.dismiss(this)
        val type = ReminderType.fromString(intent.getStringExtra(EXTRA_TYPE))
        setContent {
            MovesTheme {
                AlertScreen(type = type, onClose = { finish() })
            }
        }
    }
}

@Composable
private fun AlertScreen(type: ReminderType, onClose: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    var moveDurationMin by remember { mutableLongStateOf(2L) }
    LaunchedEffect(Unit) {
        moveDurationMin = repo.currentSettings().moveDurationMinutes.toLong()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = when (type) {
                    ReminderType.Move -> stringResource(R.string.reminder_move)
                    ReminderType.Water -> stringResource(R.string.reminder_water)
                },
                style = MaterialTheme.typography.title2,
                color = MaterialTheme.colors.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))

            if (type == ReminderType.Move) {
                MoveProgress(durationMin = moveDurationMin.toInt())
            }

            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                scope.launch {
                    repo.setLastFiredType(type.name)
                    ReminderScheduler.scheduleNext(context, repo)
                    onClose()
                }
            }) { Text(stringResource(R.string.alert_done)) }

            Spacer(Modifier.height(6.dp))
            Button(onClick = {
                scope.launch {
                    val s = repo.currentSettings()
                    val nextAt = Instant.now().plusSeconds(s.snoozeMinutes * 60L)
                    // Snooze re-fires the SAME type. Receiver computes next = lastFired.next(),
                    // so set lastFired to type.next() to make next == type.
                    repo.setLastFiredType(type.next().name)
                    repo.setNextScheduledAt(nextAt.toEpochMilli())
                    ReminderScheduler.scheduleAt(context, nextAt)
                    onClose()
                }
            }) { Text(stringResource(R.string.alert_snooze)) }
        }
    }
}

@Composable
private fun MoveProgress(durationMin: Int) {
    val context = LocalContext.current
    val detector = remember { MovementDetector(context) }
    val stepsFlow = remember(detector) {
        if (detector.isAvailable()) detector.stepStream() else flowOf(0L)
    }
    val cumulative by stepsFlow.collectAsState(initial = 0L)
    var baseline by remember { mutableLongStateOf(-1L) }
    if (baseline < 0L && cumulative > 0L) baseline = cumulative
    val delta = (cumulative - baseline).coerceAtLeast(0L)
    val goal = (durationMin * 30).coerceAtLeast(10)

    Text(
        text = stringResource(R.string.alert_move_progress, durationMin),
        style = MaterialTheme.typography.body2,
        textAlign = TextAlign.Center,
    )
    Text(
        text = stringResource(R.string.alert_steps, delta.toInt(), goal),
        style = MaterialTheme.typography.caption1,
        color = MaterialTheme.colors.secondary,
    )
}

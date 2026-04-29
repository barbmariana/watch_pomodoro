package com.example.moves.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.moves.data.SettingsRepository
import com.example.moves.domain.AlertNotifier
import com.example.moves.domain.Buzzer
import com.example.moves.domain.MovementDetector
import com.example.moves.domain.ReminderScheduler
import com.example.moves.domain.ReminderType
import com.example.moves.tile.MovesTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                handle(appContext)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(context: Context) {
        val repo = SettingsRepository(context)
        val settings = repo.currentSettings()
        if (!settings.enabled) {
            ReminderScheduler.cancel(context)
            return
        }
        val state = repo.currentScheduleState()
        val nextType = ReminderType.fromString(state.lastFiredType).next()

        val shouldSkip = nextType == ReminderType.Move &&
            recentlyMoved(context, settings.recentMovementSkipThresholdSteps)

        if (!shouldSkip) {
            Buzzer.buzz(context, nextType)
            AlertNotifier.fire(context, nextType)
        }

        // Either way, advance the cycle and schedule the next alarm.
        repo.setLastFiredType(nextType.name)
        ReminderScheduler.scheduleNext(context, repo)
        MovesTileService.requestRefresh(context)
    }

    private suspend fun recentlyMoved(context: Context, threshold: Int): Boolean {
        val detector = MovementDetector(context)
        if (!detector.isAvailable()) return false
        // Sample over a 3-second window — the cumulative counter gives us recent activity
        // via reading the current value, but step counter total is since-boot, not recent.
        // We instead sample twice a few seconds apart to detect ACTIVE walking right now.
        val activeNowSteps = detector.stepsInWindow(3_000L)
        // Scale threshold to the short window: if user is walking briskly (~2 steps/sec),
        // 3s gives ~6 steps. Treat any nonzero recent movement above 5 as "actively moving".
        return activeNowSteps >= 5
    }

}

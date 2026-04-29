package com.example.moves.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.moves.data.SettingsRepository
import com.example.moves.domain.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                ReminderScheduler.scheduleNext(appContext, SettingsRepository(appContext))
            } finally {
                pending.finish()
            }
        }
    }
}

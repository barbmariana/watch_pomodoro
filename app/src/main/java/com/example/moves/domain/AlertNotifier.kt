package com.example.moves.domain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.moves.R
import com.example.moves.presentation.AlertActivity

/**
 * Posts a high-priority full-screen notification that launches AlertActivity.
 *
 * Why a notification instead of context.startActivity from a BroadcastReceiver?
 * Android 14+ Background Activity Launch rules block direct activity starts from
 * receivers. A notification with setFullScreenIntent is the supported escape hatch
 * for alarm-style reminders and works while the watch screen is locked.
 */
object AlertNotifier {

    private const val CHANNEL_ID = "moves.alerts"
    const val NOTIFICATION_ID = 1001

    fun fire(context: Context, type: ReminderType) {
        ensureChannel(context)

        val activityIntent = Intent(context, AlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(AlertActivity.EXTRA_TYPE, type.name)
        }
        val fullScreenPi = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = when (type) {
            ReminderType.Move -> context.getString(R.string.reminder_move)
            ReminderType.Water -> context.getString(R.string.reminder_water)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSilent(true)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(fullScreenPi)
            .setFullScreenIntent(fullScreenPi, true)
            .build()

        nm(context).notify(NOTIFICATION_ID, notification)
    }

    fun dismiss(context: Context) {
        nm(context).cancel(NOTIFICATION_ID)
    }

    private fun nm(context: Context): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = nm(context)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_alert),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_alert_desc)
            enableVibration(false)
            setSound(null, null)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }
}

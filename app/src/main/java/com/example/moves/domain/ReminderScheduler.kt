package com.example.moves.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.moves.data.Settings
import com.example.moves.data.SettingsRepository
import com.example.moves.receiver.ReminderReceiver
import com.example.moves.tile.MovesTileService
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {

    const val ACTION_REMINDER = "com.example.moves.REMINDER"
    private const val REQUEST_CODE = 1001

    /**
     * Pure: given current time, settings, and the type that fired most recently,
     * return when the next reminder should fire and what type it should be.
     *
     * The cycle alternates every interval/2 minutes (Move ↔ Water). If the next
     * tentative time falls outside the active window, advance to activeStart of
     * the next valid day.
     */
    fun nextReminder(
        now: Instant,
        settings: Settings,
        lastFiredType: ReminderType,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Pair<Instant, ReminderType> {
        val stepMinutes = (settings.intervalMinutes / 2).coerceAtLeast(1)
        val tentative = now.plusSeconds(stepMinutes * 60L).atZone(zone).toLocalDateTime()
        val clamped = clampToActiveWindow(tentative, settings)
        return clamped.atZone(zone).toInstant() to lastFiredType.next()
    }

    private fun clampToActiveWindow(at: LocalDateTime, settings: Settings): LocalDateTime {
        val startOfDay = LocalTime.of(settings.activeStartMinuteOfDay / 60, settings.activeStartMinuteOfDay % 60)
        val endOfDay = LocalTime.of(settings.activeEndMinuteOfDay / 60, settings.activeEndMinuteOfDay % 60)
        val startToday = LocalDateTime.of(at.toLocalDate(), startOfDay)
        val endToday = LocalDateTime.of(at.toLocalDate(), endOfDay)

        return when {
            // Wrap (e.g., night shift): start > end means window crosses midnight; for now treat as same-day.
            startOfDay >= endOfDay -> at // pathological config — fire as scheduled
            at.isBefore(startToday) -> startToday
            at.isAfter(endToday) -> LocalDateTime.of(at.toLocalDate().plusDays(1), startOfDay)
            else -> at
        }
    }

    /** Schedule the next exact alarm based on current settings + persisted state. */
    suspend fun scheduleNext(context: Context, repo: SettingsRepository) {
        val settings = repo.currentSettings()
        if (!settings.enabled) {
            cancel(context)
            repo.setNextScheduledAt(0L)
            MovesTileService.requestRefresh(context)
            return
        }
        val state = repo.currentScheduleState()
        val (at, _) = nextReminder(
            now = Instant.now(),
            settings = settings,
            lastFiredType = ReminderType.fromString(state.lastFiredType),
        )
        scheduleAt(context, at)
        repo.setNextScheduledAt(at.toEpochMilli())
        MovesTileService.requestRefresh(context)
    }

    fun scheduleAt(context: Context, at: Instant) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(context)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pi)
        } catch (_: SecurityException) {
            // No exact-alarm permission (rare on watch); fall back to inexact.
            am.set(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pi)
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMINDER)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

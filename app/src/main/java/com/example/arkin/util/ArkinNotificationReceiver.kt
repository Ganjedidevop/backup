package com.example.arkin.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Lightweight BroadcastReceiver for local academic reminders, homework deadlines,
 * and examination notifications.
 */
class ArkinNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra("EXTRA_NOTIFICATION_ID", 1001)
        val title = intent.getStringExtra("EXTRA_NOTIFICATION_TITLE") ?: "Arkin Reminder"
        val message = intent.getStringExtra("EXTRA_NOTIFICATION_MESSAGE") ?: "You have a scheduled academic event."
        val reminderId = intent.getLongExtra("EXTRA_REMINDER_ID", -1L).takeIf { it >= 0L }

        try {
            NotificationHelper.createNotificationChannels(context)
            NotificationHelper.showReminderNotification(context, id, title, message, reminderId)
        } catch (_: Throwable) {
            // Failsafe: Never crash background receiver
        }
    }
}

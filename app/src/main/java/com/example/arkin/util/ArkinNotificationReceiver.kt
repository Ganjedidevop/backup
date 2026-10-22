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

        try {
            NotificationHelper.createNotificationChannels(context)
            NotificationHelper.showReminderNotification(context, id, title, message)
        } catch (_: Throwable) {
            // Failsafe: Never crash background receiver
        }
    }
}

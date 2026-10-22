package com.example.arkin.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Isolated scheduler using standard Android AlarmManager.
 * Does not require external services or backend servers.
 * All scheduling is protected against permissions/exceptions.
 */
object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    fun scheduleReminderNotification(
        context: Context,
        reminderId: Long,
        title: String,
        dueText: String
    ) {
        val triggerTimeMs = parseDateTimeToEpoch(dueText) ?: return
        if (triggerTimeMs <= System.currentTimeMillis()) return

        val intent = Intent(context, ArkinNotificationReceiver::class.java).apply {
            putExtra("EXTRA_NOTIFICATION_ID", (reminderId % Int.MAX_VALUE).toInt())
            putExtra("EXTRA_NOTIFICATION_TITLE", "Reminder: $title")
            putExtra("EXTRA_NOTIFICATION_MESSAGE", "Scheduled for $dueText")
        }

        scheduleAlarm(context, (reminderId % Int.MAX_VALUE).toInt(), triggerTimeMs, intent)
    }

    fun scheduleHomeworkNotification(
        context: Context,
        homeworkId: Long,
        title: String,
        subject: String,
        dueDate: String,
        dueTime: String
    ) {
        val dueTimestampMs = parseDateTimeToEpoch("$dueDate $dueTime") ?: return
        if (dueTimestampMs <= System.currentTimeMillis()) return

        // 1. Alert 1 day before (if more than 24 hours away)
        val oneDayBefore = dueTimestampMs - (24 * 60 * 60 * 1000L)
        if (oneDayBefore > System.currentTimeMillis()) {
            val earlyIntent = Intent(context, ArkinNotificationReceiver::class.java).apply {
                putExtra("EXTRA_NOTIFICATION_ID", ((homeworkId + 100000) % Int.MAX_VALUE).toInt())
                putExtra("EXTRA_NOTIFICATION_TITLE", "Homework Due Tomorrow: $title")
                putExtra("EXTRA_NOTIFICATION_MESSAGE", "$subject assignment is due tomorrow at $dueTime")
            }
            scheduleAlarm(context, ((homeworkId + 100000) % Int.MAX_VALUE).toInt(), oneDayBefore, earlyIntent)
        }

        // 2. Alert at deadline
        val deadlineIntent = Intent(context, ArkinNotificationReceiver::class.java).apply {
            putExtra("EXTRA_NOTIFICATION_ID", (homeworkId % Int.MAX_VALUE).toInt())
            putExtra("EXTRA_NOTIFICATION_TITLE", "Homework Deadline: $title")
            putExtra("EXTRA_NOTIFICATION_MESSAGE", "Your $subject assignment is due now ($dueTime).")
        }
        scheduleAlarm(context, (homeworkId % Int.MAX_VALUE).toInt(), dueTimestampMs, deadlineIntent)
    }

    fun scheduleExamNotification(
        context: Context,
        examId: Long,
        subject: String,
        examDate: String,
        timeSlot: String
    ) {
        val examTimestampMs = parseDateTimeToEpoch("$examDate $timeSlot") ?: return
        if (examTimestampMs <= System.currentTimeMillis()) return

        // Alert 1 day before
        val oneDayBefore = examTimestampMs - (24 * 60 * 60 * 1000L)
        if (oneDayBefore > System.currentTimeMillis()) {
            val earlyIntent = Intent(context, ArkinNotificationReceiver::class.java).apply {
                putExtra("EXTRA_NOTIFICATION_ID", ((examId + 200000) % Int.MAX_VALUE).toInt())
                putExtra("EXTRA_NOTIFICATION_TITLE", "Upcoming Exam Tomorrow: $subject")
                putExtra("EXTRA_NOTIFICATION_MESSAGE", "Exam scheduled on $examDate ($timeSlot). Best of luck with revisions!")
            }
            scheduleAlarm(context, ((examId + 200000) % Int.MAX_VALUE).toInt(), oneDayBefore, earlyIntent)
        }

        // Alert 1 hour before exam
        val oneHourBefore = examTimestampMs - (60 * 60 * 1000L)
        if (oneHourBefore > System.currentTimeMillis()) {
            val hourIntent = Intent(context, ArkinNotificationReceiver::class.java).apply {
                putExtra("EXTRA_NOTIFICATION_ID", (examId % Int.MAX_VALUE).toInt())
                putExtra("EXTRA_NOTIFICATION_TITLE", "Exam Starting Soon: $subject")
                putExtra("EXTRA_NOTIFICATION_MESSAGE", "Starts at $timeSlot. Check your materials and admit card.")
            }
            scheduleAlarm(context, (examId % Int.MAX_VALUE).toInt(), oneHourBefore, hourIntent)
        }
    }

    fun cancelNotification(context: Context, notificationId: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ArkinNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                (notificationId % Int.MAX_VALUE).toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (_: Throwable) {}
    }

    private fun scheduleAlarm(context: Context, requestCode: Int, triggerTimeMs: Long, intent: Intent) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to schedule alarm: ${e.message}")
        }
    }

    fun parseDateTimeToEpoch(text: String): Long? {
        val clean = text.trim()
        if (clean.isBlank()) return null

        val formats = listOf(
            "dd/MM/yyyy, hh:mm a",
            "dd/MM/yyyy hh:mm a",
            "dd-MM-yyyy, hh:mm a",
            "dd-MM-yyyy hh:mm a",
            "yyyy-MM-dd, hh:mm a",
            "yyyy-MM-dd hh:mm a",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "yyyy-MM-dd",
            "MMM d, yyyy, hh:mm a",
            "MMM d, yyyy hh:mm a",
            "MMM d, hh:mm a"
        )

        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = true }
                val parsed = sdf.parse(clean)
                if (parsed != null) {
                    val cal = Calendar.getInstance().apply { time = parsed }
                    if (cal.get(Calendar.YEAR) < 2000) {
                        cal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
                    }
                    return cal.timeInMillis
                }
            } catch (_: Exception) {}
        }

        // Handle "Tomorrow, 10:00 AM" or "Today, 5:00 PM"
        if (clean.contains("tomorrow", ignoreCase = true) || clean.contains("today", ignoreCase = true)) {
            val cal = Calendar.getInstance()
            if (clean.contains("tomorrow", ignoreCase = true)) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            val timePart = clean.substringAfter(",").trim()
            val timeSdf = SimpleDateFormat("hh:mm a", Locale.US)
            try {
                val t = timeSdf.parse(timePart)
                if (t != null) {
                    val tCal = Calendar.getInstance().apply { time = t }
                    cal.set(Calendar.HOUR_OF_DAY, tCal.get(Calendar.HOUR_OF_DAY))
                    cal.set(Calendar.MINUTE, tCal.get(Calendar.MINUTE))
                    cal.set(Calendar.SECOND, 0)
                    return cal.timeInMillis
                }
            } catch (_: Exception) {}
        }

        return null
    }
}

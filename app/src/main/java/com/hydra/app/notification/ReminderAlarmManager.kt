package com.hydra.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

class ReminderAlarmManager(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    // --- 1. Behavioral Reminders ---
    fun scheduleReminder(sampleId: Long, delayMinutes: Int) {
        if (alarmManager == null) return

        val triggerAtMillis = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_SAMPLE_ID, sampleId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BEHAVIORAL,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(triggerAtMillis, pendingIntent)
        Log.d("ReminderAlarmManager", "Scheduled behavioral reminder in $delayMinutes min")
    }

    fun cancelReminder() {
        cancelAlarm(ReminderBroadcastReceiver.ACTION_REMINDER, REQUEST_CODE_BEHAVIORAL)
    }

    // --- 2. Periodic Water Reminders ---
    fun scheduleNextWaterReminder(
        intervalMinutes: Int,
        quietHoursStart: Int = 22,
        quietHoursEnd: Int = 8,
        delayFromNowMinutes: Int? = null
    ) {
        if (alarmManager == null) return

        val minutesToWait = delayFromNowMinutes ?: intervalMinutes
        var targetTimeMillis = System.currentTimeMillis() + (minutesToWait * 60 * 1000L)

        // Adjust for quiet hours (sleep time)
        targetTimeMillis = adjustForQuietHours(targetTimeMillis, quietHoursStart, quietHoursEnd)

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_WATER_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_PERIODIC_WATER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(targetTimeMillis, pendingIntent)
        Log.d("ReminderAlarmManager", "Scheduled periodic water reminder at $targetTimeMillis")
    }

    fun cancelWaterReminder() {
        cancelAlarm(ReminderBroadcastReceiver.ACTION_WATER_REMINDER, REQUEST_CODE_PERIODIC_WATER)
    }

    // --- 3. Periodic Food Reminders ---
    fun scheduleNextFoodReminder(
        intervalMinutes: Int,
        quietHoursStart: Int = 22,
        quietHoursEnd: Int = 8,
        delayFromNowMinutes: Int? = null
    ) {
        if (alarmManager == null) return

        val minutesToWait = delayFromNowMinutes ?: intervalMinutes
        var targetTimeMillis = System.currentTimeMillis() + (minutesToWait * 60 * 1000L)

        // Adjust for quiet hours
        targetTimeMillis = adjustForQuietHours(targetTimeMillis, quietHoursStart, quietHoursEnd)

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_FOOD_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_PERIODIC_FOOD,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(targetTimeMillis, pendingIntent)
        Log.d("ReminderAlarmManager", "Scheduled periodic food reminder at $targetTimeMillis")
    }

    fun cancelFoodReminder() {
        cancelAlarm(ReminderBroadcastReceiver.ACTION_FOOD_REMINDER, REQUEST_CODE_PERIODIC_FOOD)
    }

    // --- Internal Helpers ---
    private fun scheduleAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (alarmManager == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e("ReminderAlarmManager", "Error scheduling alarm", e)
        }
    }

    private fun cancelAlarm(action: String, requestCode: Int) {
        if (alarmManager == null) return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Checks if the scheduled targetTime falls within quiet hours (e.g. 22:00 to 08:00).
     * If so, pushes target time to quietHoursEnd on the morning of the next day.
     */
    private fun adjustForQuietHours(
        targetTimeMillis: Long,
        quietStart: Int,
        quietEnd: Int
    ): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = targetTimeMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        val inQuiet = if (quietStart > quietEnd) {
            // e.g. 22 to 8: (hour >= 22 || hour < 8)
            hour >= quietStart || hour < quietEnd
        } else {
            // e.g. 1 to 7: (hour >= 1 && hour < 7)
            hour in quietStart until quietEnd
        }

        if (inQuiet) {
            // Advance to quietEnd:00:00
            if (hour >= quietStart) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            cal.set(Calendar.HOUR_OF_DAY, quietEnd)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        return targetTimeMillis
    }

    companion object {
        private const val REQUEST_CODE_BEHAVIORAL = 901
        private const val REQUEST_CODE_PERIODIC_WATER = 902
        private const val REQUEST_CODE_PERIODIC_FOOD = 903
    }
}

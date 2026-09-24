package com.hydra.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hydra.app.data.local.AppDatabase
import com.hydra.app.data.local.EventEntity
import com.hydra.app.data.local.PreferencesManager
import com.hydra.app.data.model.EventSource
import com.hydra.app.data.model.EventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val sampleId = intent.getLongExtra(EXTRA_SAMPLE_ID, -1L)

        when (action) {
            // 1. Behavioral & Bathroom prediction reminder
            ACTION_REMINDER, ACTION_BATHROOM_REMINDER -> {
                if (sampleId != -1L) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getInstance(context)
                            val samples = db.predictionDao().getAllSamplesList()
                            val sample = samples.find { it.id == sampleId }
                            if (sample != null) {
                                NotificationHelper.showBathroomReminderNotification(
                                    context = context,
                                    sampleId = sampleId,
                                    waterMl = sample.waterAmount,
                                    calories = sample.caloriesAmount,
                                    urgeType = sample.urgeType,
                                    customText = if (sample.explanation.isNotBlank()) sample.explanation else null
                                )
                            } else {
                                NotificationHelper.showBathroomReminderNotification(context, sampleId)
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                } else {
                    NotificationHelper.showBathroomReminderNotification(context, sampleId)
                }
            }

            ACTION_DONE, ACTION_BATHROOM_DONE -> {
                NotificationHelper.dismissNotification(context, NotificationHelper.NOTIFICATION_ID_BATHROOM)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        // Log bathroom visit event
                        val event = EventEntity(
                            type = EventType.BATHROOM.name,
                            createdAt = System.currentTimeMillis(),
                            description = "دخول الحمام (تأكيد التذكير)",
                            source = EventSource.QUICK_ACTION.name,
                            confidence = 1.0
                        )
                        db.eventDao().insertEvent(event)

                        if (sampleId != -1L) {
                            val samples = db.predictionDao().getAllSamplesList()
                            val sample = samples.find { it.id == sampleId }
                            if (sample != null) {
                                val now = System.currentTimeMillis()
                                val elapsedMinutes = ((now - sample.createdAt) / (60 * 1000)).toInt().coerceAtLeast(1)
                                val updated = sample.copy(
                                    actualDelayMinutes = elapsedMinutes,
                                    feedbackGiven = true
                                )
                                db.predictionDao().updateSample(updated)
                            }
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_SNOOZE, ACTION_BATHROOM_SNOOZE -> {
                NotificationHelper.dismissNotification(context, NotificationHelper.NOTIFICATION_ID_BATHROOM)
                val alarmManager = ReminderAlarmManager(context)
                alarmManager.scheduleReminder(sampleId, 15) // Snooze for 15 minutes
            }

            ACTION_DISMISS, ACTION_BATHROOM_DISMISS -> {
                NotificationHelper.dismissNotification(context, NotificationHelper.NOTIFICATION_ID_BATHROOM)
                if (sampleId != -1L) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getInstance(context)
                            val samples = db.predictionDao().getAllSamplesList()
                            val sample = samples.find { it.id == sampleId }
                            if (sample != null) {
                                val updated = sample.copy(feedbackGiven = true)
                                db.predictionDao().updateSample(updated)
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            // 2. Periodic Water Reminder
            ACTION_WATER_REMINDER -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val prefsManager = PreferencesManager(context)
                        val prefs = prefsManager.userPreferencesFlow.first()
                        if (prefs.periodicWaterEnabled) {
                            val db = AppDatabase.getInstance(context)
                            val (startOfDay, endOfDay) = getTodayRange()
                            val todayEvents = db.eventDao().getEventsForDayList(startOfDay, endOfDay)
                            val todayWater = todayEvents.filter { it.type == EventType.WATER.name }
                                .sumOf { it.amount?.toInt() ?: 0 }

                            NotificationHelper.showPeriodicWaterNotification(
                                context,
                                todayWaterMl = todayWater,
                                goalMl = prefs.dailyWaterGoalMl
                            )

                            // Schedule next periodic reminder
                            val alarmManager = ReminderAlarmManager(context)
                            alarmManager.scheduleNextWaterReminder(
                                intervalMinutes = prefs.waterIntervalMinutes,
                                quietHoursStart = prefs.quietHoursStart,
                                quietHoursEnd = prefs.quietHoursEnd
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            // 3. Periodic Food Reminder
            ACTION_FOOD_REMINDER -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val prefsManager = PreferencesManager(context)
                        val prefs = prefsManager.userPreferencesFlow.first()
                        if (prefs.periodicFoodEnabled) {
                            val db = AppDatabase.getInstance(context)
                            val (startOfDay, endOfDay) = getTodayRange()
                            val todayEvents = db.eventDao().getEventsForDayList(startOfDay, endOfDay)
                            val todayFood = todayEvents.count { it.type == EventType.FOOD.name }

                            NotificationHelper.showPeriodicFoodNotification(
                                context,
                                todayFoodCount = todayFood
                            )

                            // Schedule next food reminder
                            val alarmManager = ReminderAlarmManager(context)
                            alarmManager.scheduleNextFoodReminder(
                                intervalMinutes = prefs.foodIntervalMinutes,
                                quietHoursStart = prefs.quietHoursStart,
                                quietHoursEnd = prefs.quietHoursEnd
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            // 4. Quick Action: +250ml Water from notification
            ACTION_QUICK_WATER_250 -> {
                NotificationHelper.dismissNotification(context, NotificationHelper.NOTIFICATION_ID_WATER)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        val event = EventEntity(
                            type = EventType.WATER.name,
                            createdAt = System.currentTimeMillis(),
                            amount = 250.0,
                            unit = "ml",
                            description = "ماء (إشعار سريع)",
                            source = EventSource.QUICK_ACTION.name,
                            confidence = 1.0
                        )
                        db.eventDao().insertEvent(event)

                        val prefsManager = PreferencesManager(context)
                        val now = System.currentTimeMillis()
                        prefsManager.recordWaterLogged(now)
                        val prefs = prefsManager.userPreferencesFlow.first()

                        if (prefs.periodicWaterEnabled) {
                            val alarmManager = ReminderAlarmManager(context)
                            alarmManager.scheduleNextWaterReminder(
                                intervalMinutes = prefs.waterIntervalMinutes,
                                quietHoursStart = prefs.quietHoursStart,
                                quietHoursEnd = prefs.quietHoursEnd
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            // 5. Quick Action: +500ml Water from notification
            ACTION_QUICK_WATER_500 -> {
                NotificationHelper.dismissNotification(context, NotificationHelper.NOTIFICATION_ID_WATER)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        val event = EventEntity(
                            type = EventType.WATER.name,
                            createdAt = System.currentTimeMillis(),
                            amount = 500.0,
                            unit = "ml",
                            description = "ماء (إشعار سريع)",
                            source = EventSource.QUICK_ACTION.name,
                            confidence = 1.0
                        )
                        db.eventDao().insertEvent(event)

                        val prefsManager = PreferencesManager(context)
                        val now = System.currentTimeMillis()
                        prefsManager.recordWaterLogged(now)
                        val prefs = prefsManager.userPreferencesFlow.first()

                        if (prefs.periodicWaterEnabled) {
                            val alarmManager = ReminderAlarmManager(context)
                            alarmManager.scheduleNextWaterReminder(
                                intervalMinutes = prefs.waterIntervalMinutes,
                                quietHoursStart = prefs.quietHoursStart,
                                quietHoursEnd = prefs.quietHoursEnd
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            // 6. Snooze Water reminder (20 min)
            ACTION_SNOOZE_WATER -> {
                NotificationHelper.dismissNotification(context, NotificationHelper.NOTIFICATION_ID_WATER)
                val alarmManager = ReminderAlarmManager(context)
                alarmManager.scheduleNextWaterReminder(
                    intervalMinutes = 20,
                    delayFromNowMinutes = 20
                )
            }

            // 7. Snooze Food reminder (30 min)
            ACTION_SNOOZE_FOOD -> {
                NotificationHelper.dismissNotification(context, NotificationHelper.NOTIFICATION_ID_FOOD)
                val alarmManager = ReminderAlarmManager(context)
                alarmManager.scheduleNextFoodReminder(
                    intervalMinutes = 30,
                    delayFromNowMinutes = 30
                )
            }

            // 8. Boot completed: reschedule alarms if enabled
            Intent.ACTION_BOOT_COMPLETED -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val prefsManager = PreferencesManager(context)
                        val prefs = prefsManager.userPreferencesFlow.first()
                        val alarmManager = ReminderAlarmManager(context)

                        if (prefs.periodicWaterEnabled) {
                            alarmManager.scheduleNextWaterReminder(
                                intervalMinutes = prefs.waterIntervalMinutes,
                                quietHoursStart = prefs.quietHoursStart,
                                quietHoursEnd = prefs.quietHoursEnd
                            )
                        }
                        if (prefs.periodicFoodEnabled) {
                            alarmManager.scheduleNextFoodReminder(
                                intervalMinutes = prefs.foodIntervalMinutes,
                                quietHoursStart = prefs.quietHoursStart,
                                quietHoursEnd = prefs.quietHoursEnd
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfDay = calendar.timeInMillis
        return Pair(startOfDay, endOfDay)
    }

    companion object {
        const val ACTION_REMINDER = "com.hydra.app.ACTION_REMINDER"
        const val ACTION_DONE = "com.hydra.app.ACTION_DONE"
        const val ACTION_SNOOZE = "com.hydra.app.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.hydra.app.ACTION_DISMISS"

        const val ACTION_BATHROOM_REMINDER = "com.hydra.app.ACTION_BATHROOM_REMINDER"
        const val ACTION_BATHROOM_DONE = "com.hydra.app.ACTION_BATHROOM_DONE"
        const val ACTION_BATHROOM_SNOOZE = "com.hydra.app.ACTION_BATHROOM_SNOOZE"
        const val ACTION_BATHROOM_DISMISS = "com.hydra.app.ACTION_BATHROOM_DISMISS"

        const val ACTION_WATER_REMINDER = "com.hydra.app.ACTION_WATER_REMINDER"
        const val ACTION_FOOD_REMINDER = "com.hydra.app.ACTION_FOOD_REMINDER"
        const val ACTION_QUICK_WATER_250 = "com.hydra.app.ACTION_QUICK_WATER_250"
        const val ACTION_QUICK_WATER_500 = "com.hydra.app.ACTION_QUICK_WATER_500"
        const val ACTION_SNOOZE_WATER = "com.hydra.app.ACTION_SNOOZE_WATER"
        const val ACTION_SNOOZE_FOOD = "com.hydra.app.ACTION_SNOOZE_FOOD"

        const val EXTRA_SAMPLE_ID = "EXTRA_SAMPLE_ID"
    }
}

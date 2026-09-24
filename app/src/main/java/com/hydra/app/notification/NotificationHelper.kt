package com.hydra.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hydra.app.MainActivity

object NotificationHelper {
    const val CHANNEL_ID_BEHAVIORAL = "hydra_behavioral_reminders"
    const val CHANNEL_ID_BATHROOM = "hydra_bathroom_reminders"
    const val CHANNEL_ID_WATER = "hydra_periodic_water"
    const val CHANNEL_ID_FOOD = "hydra_periodic_food"

    const val NOTIFICATION_ID_BEHAVIORAL = 2001
    const val NOTIFICATION_ID_WATER = 2002
    const val NOTIFICATION_ID_FOOD = 2003
    const val NOTIFICATION_ID_BATHROOM = 2004

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Behavioral channel
            val behavioralChannel = NotificationChannel(
                CHANNEL_ID_BEHAVIORAL,
                "تذكيرات Hydra الشخصية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تذكيرات سلوكية تقديرية لتسجيل الأحداث"
                enableVibration(true)
            }

            // 2. Smart Bathroom channel
            val bathroomChannel = NotificationChannel(
                CHANNEL_ID_BATHROOM,
                "تنبيهات دخول الحمام الذكية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات ذكية مبنية على خوارزمية حساب السعرات والماء ووقت الهضم والامتلاء"
                enableVibration(true)
            }

            // 3. Periodic Water channel
            val waterChannel = NotificationChannel(
                CHANNEL_ID_WATER,
                "تذكيرات شرب الماء الدورية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات دورية منتظمة لشرب الماء وترطيب الجسم"
                enableVibration(true)
            }

            // 4. Periodic Food channel
            val foodChannel = NotificationChannel(
                CHANNEL_ID_FOOD,
                "تذكيرات الوجبات والطعام",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات دورية منتظمة لتناول وجباتك والحفاظ على طاقتك"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(behavioralChannel)
            notificationManager.createNotificationChannel(bathroomChannel)
            notificationManager.createNotificationChannel(waterChannel)
            notificationManager.createNotificationChannel(foodChannel)
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            return permission == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun showReminderNotification(context: Context, sampleId: Long) {
        showBathroomReminderNotification(context, sampleId)
    }

    fun showBathroomReminderNotification(
        context: Context,
        sampleId: Long,
        waterMl: Int = 0,
        calories: Int = 0,
        urgeType: String = "COMBINED",
        customText: String? = null
    ) {
        if (!hasNotificationPermission(context)) return

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_SAMPLE_ID", sampleId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_BATHROOM_DONE
            putExtra(ReminderBroadcastReceiver.EXTRA_SAMPLE_ID, sampleId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_BATHROOM_SNOOZE
            putExtra(ReminderBroadcastReceiver.EXTRA_SAMPLE_ID, sampleId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_BATHROOM_DISMISS
            putExtra(ReminderBroadcastReceiver.EXTRA_SAMPLE_ID, sampleId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = customText ?: when {
            waterMl > 0 && calories > 0 -> {
                "حان الوقت المقدر لدخول الحمام بناءً على شرب $waterMl مل ماء ووجبة $calories سعرة حرارية."
            }
            calories > 0 -> {
                "حان الوقت المقدر لنشاط الهضم بعد وجبة $calories سعرة حرارية."
            }
            waterMl > 0 -> {
                "حان الوقت المقدر لامتلاء المثانة بعد شرب $waterMl مل ماء."
            }
            else -> {
                "قد يكون هذا وقتًا مناسباً لتسجيل دخول الحمام بناءً على خوارزمية الترطيب والهضم التقديرية."
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BATHROOM)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("تذكير دخول الحمام 🚻")
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "دخلت الحمام ✅", donePendingIntent)
            .addAction(0, "تأجيل 15 د ⏳", snoozePendingIntent)
            .addAction(0, "تجاهل", dismissPendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BATHROOM, notification)
    }

    fun showPeriodicWaterNotification(context: Context, todayWaterMl: Int = 0, goalMl: Int = 2500) {
        if (!hasNotificationPermission(context)) return

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            10,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Quick log +250ml
        val quick250Intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_QUICK_WATER_250
        }
        val quick250Pending = PendingIntent.getBroadcast(
            context,
            11,
            quick250Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Quick log +500ml
        val quick500Intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_QUICK_WATER_500
        }
        val quick500Pending = PendingIntent.getBroadcast(
            context,
            12,
            quick500Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 20m
        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SNOOZE_WATER
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            13,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val textBody = if (goalMl > 0) {
            val percent = ((todayWaterMl.toFloat() / goalMl) * 100).toInt()
            "تذكر ترطيب جسمك! شربت اليوم $todayWaterMl مل ($percent% من هدف $goalMl مل)."
        } else {
            "تذكر ترطيب جسمك! شرب كوب ماء ينعش تركيزك وطاقتك."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_WATER)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("تذكير بشرب الماء 💧")
            .setContentText(textBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(textBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "+250 مل", quick250Pending)
            .addAction(0, "+500 مل", quick500Pending)
            .addAction(0, "تأجيل 20 د", snoozePending)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_WATER, notification)
    }

    fun showPeriodicFoodNotification(context: Context, todayFoodCount: Int = 0) {
        if (!hasNotificationPermission(context)) return

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            20,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 30m
        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SNOOZE_FOOD
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            21,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val textBody = "حان وقت تناول وجبة أو سناك صحي للحفاظ على طاقتك ونشاطك. (سجلت اليوم $todayFoodCount وجبات)."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_FOOD)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("تذكير بالوجبات والطعام 🍽️")
            .setContentText(textBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(textBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "تسجيل وجبة", contentPendingIntent)
            .addAction(0, "تأجيل 30 د", snoozePending)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_FOOD, notification)
    }

    fun dismissNotification(context: Context, id: Int = NOTIFICATION_ID_BATHROOM) {
        NotificationManagerCompat.from(context).cancel(id)
        if (id == NOTIFICATION_ID_BATHROOM) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_BEHAVIORAL)
        }
    }
}

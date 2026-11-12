package app.ji.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import app.ji.MainActivity
import app.ji.R
import app.ji.data.model.Reminder

class ReminderAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "ji_reminders_channel"
        const val CHANNEL_NAME = "Ji Reminders"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_SNIPPET = "extra_snippet"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_DUE_TIME = "extra_due_time"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val snippet = intent.getStringExtra(EXTRA_SNIPPET) ?: return
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: Reminder.CATEGORY_PERSONAL

        createNotificationChannel(context)

        // Open app intent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Mark as Done" action intent
        val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_DONE
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId * 10 + 1).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Dismiss" action intent
        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId * 10 + 2).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val accentColor = when (category) {
            Reminder.CATEGORY_PROFESSIONAL -> Color.rgb(33, 150, 243) // Blue
            Reminder.CATEGORY_PERSONAL -> Color.rgb(76, 175, 80)      // Green
            Reminder.CATEGORY_PARCEL -> Color.rgb(255, 179, 0)        // Amber
            Reminder.CATEGORY_URGENT -> Color.rgb(244, 67, 54)        // Red
            else -> Color.rgb(76, 175, 80)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Ji Reminder • $category")
            .setContentText(snippet)
            .setStyle(NotificationCompat.BigTextStyle().bigText(snippet))
            .setColor(accentColor)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .addAction(0, "✓ Mark as Done", donePendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(reminderId.toInt(), notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager?.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Time-based reminder notifications"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 150, 300)
                }
                notificationManager?.createNotificationChannel(channel)
            }
        }
    }
}


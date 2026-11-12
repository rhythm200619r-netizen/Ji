package app.ji.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.ji.data.repository.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_DONE = "app.ji.action.MARK_DONE"
        const val ACTION_DISMISS = "app.ji.action.DISMISS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, -1L)
        if (reminderId < 0) return

        // Cancel notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(reminderId.toInt())

        val repository = ReminderRepository(context)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_MARK_DONE -> repository.markAsCompleted(reminderId)
                    ACTION_DISMISS -> repository.markAsDismissed(reminderId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}


package app.ji.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.ji.data.repository.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reschedules active pending alarms after device reboot (SPEC.md Phase 2).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val repository = ReminderRepository(context)
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.rescheduleAllPending()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}


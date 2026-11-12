package app.ji.data.repository

import android.content.Context
import app.ji.alarm.ReminderAlarmScheduler
import app.ji.data.db.DatabaseProvider
import app.ji.data.model.CategoryClassifier
import app.ji.data.model.Reminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class ReminderRepository(private val context: Context) {

    private val dao = DatabaseProvider.getDatabase(context).reminderDao()
    private val scheduler = ReminderAlarmScheduler(context)

    companion object {
        const val MAX_SNIPPET_LENGTH = 120
    }

    suspend fun createReminder(
        rawSnippet: String,
        dueTimeMillis: Long,
        categoryOverride: String? = null
    ): Reminder = withContext(Dispatchers.IO) {
        // Enforce Rule 5: snippet max 120 chars
        val trimmed = rawSnippet.trim().replace("\\s+".toRegex(), " ")
        val snippet = if (trimmed.length > MAX_SNIPPET_LENGTH) {
            trimmed.substring(0, MAX_SNIPPET_LENGTH - 3) + "..."
        } else {
            trimmed
        }

        val category = categoryOverride ?: CategoryClassifier.classify(snippet)

        val reminder = Reminder(
            snippet = snippet,
            dueTime = dueTimeMillis,
            category = category,
            status = Reminder.STATUS_PENDING,
            createdAt = System.currentTimeMillis()
        )

        val insertedId = dao.insert(reminder)
        val created = reminder.copy(id = insertedId)

        // Schedule exact alarm if due in the future
        if (dueTimeMillis > System.currentTimeMillis()) {
            scheduler.schedule(created)
        }

        created
    }

    fun getTodayReminders(): Flow<List<Reminder>> {
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val endOfDay = today.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
        return dao.getTodayAndOverduePending(endOfDay)
    }

    fun getAllReminders(): Flow<List<Reminder>> {
        return dao.getAllReminders()
    }

    suspend fun markAsCompleted(id: Long) = withContext(Dispatchers.IO) {
        dao.updateStatus(id, Reminder.STATUS_COMPLETED)
        scheduler.cancel(id)
    }

    suspend fun markAsDismissed(id: Long) = withContext(Dispatchers.IO) {
        dao.updateStatus(id, Reminder.STATUS_DISMISSED)
        scheduler.cancel(id)
    }

    suspend fun deleteReminder(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
        scheduler.cancel(id)
    }

    /**
     * Reschedules all pending future reminders (used by BootReceiver).
     */
    suspend fun rescheduleAllPending() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val pending = dao.getAllFuturePending(now)
        for (reminder in pending) {
            scheduler.schedule(reminder)
        }
    }

    /**
     * One-tap delete everything: wipes database, preferences, keystore keys and alarms (Rule 7).
     */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val futureReminders = dao.getAllFuturePending(now)
        for (r in futureReminders) {
            scheduler.cancel(r.id)
        }
        dao.deleteAll()
        DatabaseProvider.closeAndDeleteDatabase(context)
    }
}

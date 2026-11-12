package app.ji.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import app.ji.data.model.Reminder

@Database(entities = [Reminder::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
}


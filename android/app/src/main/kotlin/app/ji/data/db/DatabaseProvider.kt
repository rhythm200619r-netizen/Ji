package app.ji.data.db

import android.content.Context
import androidx.room.Room
import app.ji.data.security.SecurityKeyManager
import net.sqlcipher.database.SupportFactory

object DatabaseProvider {

    private const val DB_NAME = "ji_encrypted_reminders.db"

    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: buildDatabase(context.applicationContext).also { instance = it }
        }
    }

    private fun buildDatabase(context: Context): AppDatabase {
        val passphrase = SecurityKeyManager.getDatabasePassphrase(context)
        val factory = SupportFactory(passphrase)

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DB_NAME
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * Closes the active database connection and deletes the database file from disk (Rule 7).
     */
    @Synchronized
    fun closeAndDeleteDatabase(context: Context) {
        instance?.close()
        instance = null
        context.deleteDatabase(DB_NAME)
        SecurityKeyManager.wipeKeysAndCredentials(context)
    }
}


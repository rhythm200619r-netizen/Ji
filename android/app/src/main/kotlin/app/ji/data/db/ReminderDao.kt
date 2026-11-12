package app.ji.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import app.ji.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(reminder: Reminder): Long

    @Update
    fun update(reminder: Reminder): Int

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun getById(id: Long): Reminder?

    @Query("SELECT * FROM reminders WHERE status = 'PENDING' AND dueTime >= :startOfDay AND dueTime <= :endOfDay ORDER BY dueTime ASC")
    fun getTodayPending(startOfDay: Long, endOfDay: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE status = 'PENDING' AND dueTime <= :endOfDay ORDER BY dueTime ASC")
    fun getTodayAndOverduePending(endOfDay: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE status = 'PENDING' AND dueTime > :now ORDER BY dueTime ASC")
    fun getAllFuturePending(now: Long): List<Reminder>

    @Query("SELECT * FROM reminders ORDER BY dueTime ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("UPDATE reminders SET status = :status WHERE id = :id")
    fun updateStatus(id: Long, status: String): Int

    @Query("DELETE FROM reminders WHERE id = :id")
    fun deleteById(id: Long): Int

    @Query("DELETE FROM reminders")
    fun deleteAll(): Int
}

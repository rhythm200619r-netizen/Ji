package app.ji.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val snippet: String,
    val dueTime: Long,
    val category: String,
    val status: String = STATUS_PENDING,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_DISMISSED = "DISMISSED"

        const val CATEGORY_PROFESSIONAL = "Professional"
        const val CATEGORY_PERSONAL = "Personal"
        const val CATEGORY_PARCEL = "Parcel"
        const val CATEGORY_URGENT = "Urgent"
    }
}


package app.ji.ui.today

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.ji.data.model.Reminder
import app.ji.data.repository.ReminderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReminderRepository(application)

    val reminders: StateFlow<List<Reminder>> = repository.getTodayReminders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun markAsCompleted(id: Long) {
        viewModelScope.launch {
            repository.markAsCompleted(id)
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            repository.deleteReminder(id)
        }
    }

    fun addManualReminder(snippet: String, dueTimeMillis: Long, category: String) {
        viewModelScope.launch {
            repository.createReminder(
                rawSnippet = snippet,
                dueTimeMillis = dueTimeMillis,
                categoryOverride = category
            )
        }
    }
}


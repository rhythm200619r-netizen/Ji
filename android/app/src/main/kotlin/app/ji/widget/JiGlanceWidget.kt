package app.ji.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.ji.MainActivity
import app.ji.data.model.Reminder
import app.ji.data.repository.ReminderRepository
import app.ji.ui.theme.CategoryTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Glance Home-screen Widget for Ji: Today / Urgent / Upcoming reminders (SPEC Phase 4).
 */
class JiGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = ReminderRepository(context)
        val hideReminderText = app.ji.data.settings.JiSettings.isHideReminderText(context)
        val reminders = try {
            repository.getWidgetReminders()
        } catch (_: Throwable) {
            emptyList()
        }

        provideContent {
            GlanceTheme {
                JiWidgetContent(reminders, hideReminderText)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun JiWidgetContent(reminders: List<Reminder>, hideReminderText: Boolean) {
    val urgentCount = reminders.count { it.category == Reminder.CATEGORY_URGENT }
    val now = LocalDate.now()
    val todayCount = reminders.count {
        val date = LocalDateTime.ofInstant(Instant.ofEpochMilli(it.dueTime), ZoneId.systemDefault()).toLocalDate()
        date == now
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ji • Reminders",
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            if (urgentCount > 0) {
                Text(
                    text = "🚨 $urgentCount Urgent",
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.error
                    )
                )
            } else {
                Text(
                    text = "$todayCount Today",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = GlanceTheme.colors.primary
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        if (reminders.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓ All clear! No pending reminders",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
        } else {
            LazyColumn(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                items(reminders.take(6), itemId = { it.id }) { reminder ->
                    WidgetReminderItem(reminder, hideReminderText)
                    Spacer(modifier = GlanceModifier.height(6.dp))
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetReminderItem(reminder: Reminder, hideReminderText: Boolean) {
    val dueDateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(reminder.dueTime),
        ZoneId.systemDefault()
    )
    val now = LocalDateTime.now()
    val isOverdue = dueDateTime.isBefore(now)
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val timeStr = if (dueDateTime.toLocalDate() == now.toLocalDate()) {
        dueDateTime.format(timeFormatter)
    } else {
        dueDateTime.format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
    }

    val catColor = CategoryTheme.getColor(reminder.category, isDark = false).color

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(8.dp)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category indicator dot
        Box(
            modifier = GlanceModifier
                .size(8.dp)
                .background(ColorProvider(catColor))
                .cornerRadius(4.dp)
        ) {}

        Spacer(modifier = GlanceModifier.width(8.dp))

        val displaySnippet = if (hideReminderText) "Ji reminder" else reminder.snippet

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = displaySnippet,
                maxLines = 1,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
            Text(
                text = if (isOverdue) "⚠ $timeStr (Overdue)" else "⏰ $timeStr",
                style = TextStyle(
                    fontSize = 10.sp,
                    color = if (isOverdue) GlanceTheme.colors.error
                            else GlanceTheme.colors.onSurfaceVariant
                )
            )
        }
    }
}

package app.ji.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.ji.data.model.CategoryClassifier
import app.ji.data.model.Reminder
import app.ji.parser.TimePhraseParser
import app.ji.ui.theme.CategoryTheme
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onNavigateToOnboarding: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    sharedInitialText: String? = null,
    onClearSharedText: () -> Unit = {},
    viewModel: TodayViewModel = viewModel()
) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    var showManualAddDialog by remember { mutableStateOf(false) }

    val parsedShared = remember(sharedInitialText) {
        if (!sharedInitialText.isNullOrBlank()) {
            val parseResult = TimePhraseParser.parseDetailed(sharedInitialText, LocalDateTime.now())
            val dueMillis = parseResult?.resolvedDateTime?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
            val cat = CategoryClassifier.classify(sharedInitialText)
            val snippet = if (sharedInitialText.length > 120) sharedInitialText.take(117) + "..." else sharedInitialText
            Triple(snippet, dueMillis, cat)
        } else null
    }

    LaunchedEffect(sharedInitialText) {
        if (!sharedInitialText.isNullOrBlank()) {
            showManualAddDialog = true
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Ji • Today",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = onNavigateToPrivacy) {
                        Text(
                            text = "🛡",
                            fontSize = 18.sp
                        )
                    }
                    IconButton(onClick = onNavigateToOnboarding) {
                        Text(
                            text = "⌨",
                            fontSize = 20.sp
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showManualAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text("+", fontSize = 26.sp, fontWeight = FontWeight.Light)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (reminders.isEmpty()) {
                EmptyStateView(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reminders, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            onMarkDone = { viewModel.markAsCompleted(reminder.id) }
                        )
                    }
                }
            }

            if (showManualAddDialog) {
                ManualAddDialog(
                    initialSnippet = parsedShared?.first ?: "",
                    initialDueTimeMillis = parsedShared?.second,
                    initialCategory = parsedShared?.third,
                    onDismiss = {
                        showManualAddDialog = false
                        onClearSharedText()
                    },
                    onSave = { snippet, dueTimeMillis, category ->
                        viewModel.addManualReminder(snippet, dueTimeMillis, category)
                        showManualAddDialog = false
                        onClearSharedText()
                    }
                )
            }
        }
    }
}

@Composable
fun ReminderCard(
    reminder: Reminder,
    onMarkDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val categoryColors = CategoryTheme.getColor(reminder.category, isDark)

    val dueDateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(reminder.dueTime),
        ZoneId.systemDefault()
    )
    val now = LocalDateTime.now()
    val isOverdue = dueDateTime.isBefore(now)

    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val timeText = if (dueDateTime.toLocalDate() == now.toLocalDate()) {
        "Today at ${dueDateTime.format(timeFormatter)}"
    } else {
        dueDateTime.format(DateTimeFormatter.ofPattern("MMM d • h:mm a"))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Category tag pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(categoryColors.color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = reminder.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryColors.color,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Snippet text (max 120 chars)
                Text(
                    text = reminder.snippet,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Due time
                Text(
                    text = if (isOverdue) "⚠ $timeText (Overdue)" else "⏰ $timeText",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Done action button
            IconButton(
                onClick = onMarkDone,
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
            ) {
                Text(
                    text = "✓",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "✨",
                    fontSize = 32.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "All clear for today!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Type commitments like \"call you at 5\" using Ji Keyboard, or tap + below to add one manually.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

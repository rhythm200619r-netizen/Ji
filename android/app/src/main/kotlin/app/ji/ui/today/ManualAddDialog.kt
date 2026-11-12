package app.ji.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ji.data.model.CategoryClassifier
import app.ji.data.model.Reminder
import app.ji.data.repository.ReminderRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualAddDialog(
    onDismiss: () -> Unit,
    onSave: (snippet: String, dueTimeMillis: Long, category: String) -> Unit
) {
    var snippet by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Reminder.CATEGORY_PERSONAL) }
    var isCategoryOverridden by remember { mutableStateOf(false) }

    // Default to 1 hour from now
    var dueDateTime by remember {
        mutableStateOf(LocalDateTime.now().plusHours(1).withSecond(0).withNano(0))
    }

    val categories = listOf(
        Reminder.CATEGORY_PROFESSIONAL to Color(0xFF2196F3),
        Reminder.CATEGORY_PERSONAL to Color(0xFF4CAF50),
        Reminder.CATEGORY_PARCEL to Color(0xFFFFB300),
        Reminder.CATEGORY_URGENT to Color(0xFFF44336)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Reminder", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Task Snippet Input (Max 120 chars)
                Column {
                    OutlinedTextField(
                        value = snippet,
                        onValueChange = {
                            if (it.length <= ReminderRepository.MAX_SNIPPET_LENGTH) {
                                snippet = it
                                if (!isCategoryOverridden) {
                                    selectedCategory = CategoryClassifier.classify(it)
                                }
                            }
                        },
                        label = { Text("Task / Commitment") },
                        placeholder = { Text("e.g. Call client about project") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        supportingText = {
                            Text(
                                text = "${snippet.length} / ${ReminderRepository.MAX_SNIPPET_LENGTH}",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )
                }

                // Due Time display & quick presets
                Column {
                    Text(
                        text = "Due Time",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val formatter = DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a")
                    Text(
                        text = dueDateTime.format(formatter),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Quick Presets",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                dueDateTime = LocalDateTime.now().plusHours(1)
                            },
                            label = { Text("+1 Hour") }
                        )
                        SuggestionChip(
                            onClick = {
                                dueDateTime = LocalDate.now().atTime(14, 0)
                            },
                            label = { Text("Today 2 PM") }
                        )
                        SuggestionChip(
                            onClick = {
                                dueDateTime = LocalDate.now().atTime(18, 0)
                            },
                            label = { Text("Today 6 PM") }
                        )
                        SuggestionChip(
                            onClick = {
                                dueDateTime = LocalDate.now().atTime(21, 0)
                            },
                            label = { Text("Tonight 9 PM") }
                        )
                        SuggestionChip(
                            onClick = {
                                dueDateTime = LocalDate.now().plusDays(1).atTime(9, 0)
                            },
                            label = { Text("Tomorrow 9 AM") }
                        )
                    }
                }

                // Category Selection
                Column {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { (categoryName, color) ->
                            val isSelected = selectedCategory == categoryName
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategory = categoryName
                                    isCategoryOverridden = true
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                },
                                label = { Text(categoryName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = color.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dueMillis = dueDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    onSave(snippet, dueMillis, selectedCategory)
                },
                enabled = snippet.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


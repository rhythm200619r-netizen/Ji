package app.ji.keyboard

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object SentenceExtractor {

    private const val MAX_SNIPPET_LENGTH = 120

    /**
     * Extracts the current active sentence from the text before the cursor.
     * Looks back to the last sentence delimiter (. ? ! \n) or start of string.
     */
    fun extractCurrentSentence(textBeforeCursor: CharSequence?): String {
        if (textBeforeCursor.isNullOrEmpty()) return ""

        val text = textBeforeCursor.toString()
        val lastDelim = text.lastIndexOfAny(charArrayOf('.', '?', '!', '\n'))
        val rawSentence = if (lastDelim >= 0 && lastDelim < text.length - 1) {
            text.substring(lastDelim + 1)
        } else if (lastDelim == text.length - 1) {
            // Cursor is right at delimiter
            ""
        } else {
            text
        }

        return rawSentence.trim()
    }

    /**
     * Formats a concise task snippet (max 120 chars) from the sentence.
     */
    fun createSnippet(sentence: String): String {
        val trimmed = sentence.trim().replace("\\s+".toRegex(), " ")
        return if (trimmed.length > MAX_SNIPPET_LENGTH) {
            trimmed.substring(0, MAX_SNIPPET_LENGTH - 3) + "..."
        } else {
            trimmed
        }
    }

    /**
     * Formats resolved time nicely for chip display, e.g. "today at 5:00 PM" or "tomorrow at 9:00 AM".
     */
    fun formatTimeForChip(dateTime: LocalDateTime, referenceTime: LocalDateTime = LocalDateTime.now()): String {
        val targetDate = dateTime.toLocalDate()
        val refDate = referenceTime.toLocalDate()

        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
        val timeStr = dateTime.toLocalTime().format(timeFormatter)

        val dayStr = when {
            targetDate == refDate -> "today"
            targetDate == refDate.plusDays(1) -> "tomorrow"
            targetDate == refDate.plusDays(2) -> "in 2 days"
            targetDate.isAfter(refDate) && targetDate.isBefore(refDate.plusDays(7)) -> {
                targetDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            }
            else -> DateTimeFormatter.ofPattern("MMM d", Locale.US).format(targetDate)
        }

        return "$dayStr at $timeStr"
    }
}

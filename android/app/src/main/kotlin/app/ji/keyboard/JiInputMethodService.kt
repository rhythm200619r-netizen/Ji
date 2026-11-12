package app.ji.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import app.ji.data.repository.ReminderRepository
import app.ji.parser.TimePhraseParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class JiInputMethodService : InputMethodService(), JiKeyboardView.KeyActionListener {

    private var keyboardView: JiKeyboardView? = null
    private var isSensitiveField = false
    private var candidateSnippet: String? = null
    private var candidateDueTimeMillis: Long = 0L

    override fun onCreateInputView(): View {
        val view = JiKeyboardView(this)
        view.listener = this
        keyboardView = view
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Rule 4: Disable all detection for password, OTP, email and payment fields
        isSensitiveField = SensitivityFilter.isSensitiveField(info)
        keyboardView?.hideReminderChip()
        if (!isSensitiveField) {
            checkCurrentSentenceForTimePhrase()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        candidateSnippet = null
        candidateDueTimeMillis = 0L
        keyboardView?.hideReminderChip()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (!isSensitiveField) {
            checkCurrentSentenceForTimePhrase()
        }
    }

    override fun onTextKey(text: String) {
        currentInputConnection?.commitText(text, 1)
        if (!isSensitiveField) {
            checkCurrentSentenceForTimePhrase()
        }
    }

    override fun onDeleteKey() {
        val selected = currentInputConnection?.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            currentInputConnection?.commitText("", 1)
        } else {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
        if (!isSensitiveField) {
            checkCurrentSentenceForTimePhrase()
        }
    }

    override fun onSpaceKey() {
        currentInputConnection?.commitText(" ", 1)
        if (!isSensitiveField) {
            checkCurrentSentenceForTimePhrase()
        }
    }

    override fun onEnterKey() {
        val editorInfo = currentInputEditorInfo
        if (editorInfo != null && (editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION) != EditorInfo.IME_ACTION_NONE) {
            val action = editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION
            currentInputConnection?.performEditorAction(action)
        } else {
            sendKeyChar('\n')
        }
        keyboardView?.hideReminderChip()
    }

    override fun onChipAccepted(chipText: String) {
        val snippet = candidateSnippet
        val dueTime = candidateDueTimeMillis
        candidateSnippet = null
        candidateDueTimeMillis = 0L
        keyboardView?.hideReminderChip()

        if (snippet != null && dueTime > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                val repository = ReminderRepository(applicationContext)
                val created = repository.createReminder(snippet, dueTime)
                withContext(Dispatchers.Main) {
                    val timeFormatted = SentenceExtractor.formatTimeForChip(
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(created.dueTime), ZoneId.systemDefault())
                    )
                    Toast.makeText(
                        applicationContext,
                        "✓ Reminder scheduled for $timeFormatted",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onChipDismissed() {
        candidateSnippet = null
        candidateDueTimeMillis = 0L
        keyboardView?.hideReminderChip()
    }

    /**
     * Inspects text before the cursor strictly in memory.
     * Rule 3: Never log, persist, or transmit typed text.
     */
    private fun checkCurrentSentenceForTimePhrase() {
        val ic = currentInputConnection ?: return

        // Read up to 250 characters before cursor to extract active sentence
        val textBefore = ic.getTextBeforeCursor(250, 0) ?: return
        val sentence = SentenceExtractor.extractCurrentSentence(textBefore)

        if (sentence.isBlank()) {
            candidateSnippet = null
            candidateDueTimeMillis = 0L
            keyboardView?.hideReminderChip()
            return
        }

        // Run purely in-memory time phrase parser.
        // Wrapped in try/catch: if the parser throws (e.g. regex engine differences),
        // fail silently (no chip). Never log the exception with typed text.
        try {
            val result = TimePhraseParser.parseDetailed(sentence, LocalDateTime.now())
            val resolved = result?.resolvedDateTime
            if (resolved != null) {
                val snippet = SentenceExtractor.createSnippet(sentence)
                val timeFormatted = SentenceExtractor.formatTimeForChip(resolved)
                val dueMillis = resolved.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                candidateSnippet = snippet
                candidateDueTimeMillis = dueMillis
                keyboardView?.showReminderChip("Remind: $snippet, $timeFormatted?")
            } else {
                candidateSnippet = null
                candidateDueTimeMillis = 0L
                keyboardView?.hideReminderChip()
            }
        } catch (_: Throwable) {
            // Fail silently – do not log exception details alongside typed text (Rule 3).
            candidateSnippet = null
            candidateDueTimeMillis = 0L
            keyboardView?.hideReminderChip()
        }
    }
}

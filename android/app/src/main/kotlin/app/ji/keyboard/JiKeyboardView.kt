package app.ji.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class JiKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface KeyActionListener {
        fun onTextKey(text: String)
        fun onDeleteKey()
        fun onEnterKey()
        fun onSpaceKey()
        fun onChipAccepted(chipText: String)
        fun onChipDismissed()
    }

    var listener: KeyActionListener? = null

    private var isShifted = false
    private var isCapsLocked = false
    private var isSymbols = false
    private var lastShiftPressTime = 0L

    private val suggestionContainer: LinearLayout
    private val chipLayout: LinearLayout
    private val chipTextView: TextView
    private val chipDismissView: TextView
    private val keysLayout: LinearLayout

    private var currentChipText: String? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#1C1B1F")) // Material Dark Surface

        // 1. Suggestion Strip
        val stripScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(48))
        }

        suggestionContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        // Chip wrapper
        chipLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createChipBackground()
            setPadding(dpToPx(12), dpToPx(6), dpToPx(8), dpToPx(6))
            visibility = View.GONE
        }

        val clockIcon = TextView(context).apply {
            text = "⏰ "
            textSize = 14f
        }

        chipTextView = TextView(context).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#E6E1E5"))
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        chipDismissView = TextView(context).apply {
            text = "  ✕"
            textSize = 14f
            setTextColor(Color.parseColor("#CAC4D0"))
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
            setOnClickListener {
                hideReminderChip()
                listener?.onChipDismissed()
            }
        }

        chipLayout.addView(clockIcon)
        chipLayout.addView(chipTextView)
        chipLayout.addView(chipDismissView)

        chipLayout.setOnClickListener {
            currentChipText?.let { text ->
                performHaptic()
                listener?.onChipAccepted(text)
            }
        }

        suggestionContainer.addView(chipLayout)
        stripScroll.addView(suggestionContainer)
        addView(stripScroll)

        // 2. Keyboard Keys Container
        keysLayout = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(6))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        addView(keysLayout)

        rebuildKeys()
    }

    fun showReminderChip(chipText: String) {
        currentChipText = chipText
        chipTextView.text = chipText
        chipLayout.visibility = View.VISIBLE
    }

    fun hideReminderChip() {
        currentChipText = null
        chipLayout.visibility = View.GONE
    }

    private fun rebuildKeys() {
        keysLayout.removeAllViews()

        if (isSymbols) {
            buildSymbolsLayout()
        } else {
            buildAlphabetLayout()
        }
    }

    private fun buildAlphabetLayout() {
        // Number row
        val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        keysLayout.addView(createRow(numbers, keyHeight = 44, weight = 1f))

        // Row 1: q w e r t y u i o p
        val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").map {
            if (isShifted || isCapsLocked) it.uppercase() else it
        }
        keysLayout.addView(createRow(row1, keyHeight = 48, weight = 1f))

        // Row 2: a s d f g h j k l
        val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").map {
            if (isShifted || isCapsLocked) it.uppercase() else it
        }
        keysLayout.addView(createRow(row2, keyHeight = 48, weight = 1f, sidePaddingDp = 16))

        // Row 3: Shift + z x c v b n m + Backspace
        val row3 = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(48))
        }

        // Shift button
        val shiftColor = if (isCapsLocked) "#D0BCFF" else if (isShifted) "#9A82DB" else "#49454F"
        val shiftKey = createSpecialKey("⇧", 1.5f, shiftColor) {
            val now = System.currentTimeMillis()
            if (now - lastShiftPressTime < 300) {
                isCapsLocked = !isCapsLocked
                isShifted = isCapsLocked
            } else {
                if (isCapsLocked) {
                    isCapsLocked = false
                    isShifted = false
                } else {
                    isShifted = !isShifted
                }
            }
            lastShiftPressTime = now
            rebuildKeys()
        }
        row3.addView(shiftKey)

        val letters3 = listOf("z", "x", "c", "v", "b", "n", "m").map {
            if (isShifted || isCapsLocked) it.uppercase() else it
        }
        for (letter in letters3) {
            row3.addView(createCharKey(letter, 1f))
        }

        // Backspace button
        val deleteKey = createSpecialKey("⌫", 1.5f, "#49454F") {
            listener?.onDeleteKey()
        }
        row3.addView(deleteKey)
        keysLayout.addView(row3)

        // Bottom row: [?123] [,] [   Space   ] [.] [↵]
        val bottomRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(48))
        }

        val symKey = createSpecialKey("?123", 1.5f, "#49454F") {
            isSymbols = true
            rebuildKeys()
        }
        bottomRow.addView(symKey)

        bottomRow.addView(createCharKey(",", 1f))

        val spaceKey = createSpecialKey("Space", 4.5f, "#332D41") {
            listener?.onSpaceKey()
            if (isShifted && !isCapsLocked) {
                isShifted = false
                rebuildKeys()
            }
        }
        bottomRow.addView(spaceKey)

        bottomRow.addView(createCharKey(".", 1f))

        val enterKey = createSpecialKey("↵", 1.5f, "#6750A4") {
            listener?.onEnterKey()
        }
        bottomRow.addView(enterKey)

        keysLayout.addView(bottomRow)
    }

    private fun buildSymbolsLayout() {
        // Number row
        val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
        keysLayout.addView(createRow(numbers, keyHeight = 44, weight = 1f))

        // Row 1
        val row1 = listOf("@", "#", "$", "%", "&", "*", "-", "+", "(", ")")
        keysLayout.addView(createRow(row1, keyHeight = 48, weight = 1f))

        // Row 2
        val row2 = listOf("!", "\"", "'", ":", ";", "/", "?", "~", "_")
        keysLayout.addView(createRow(row2, keyHeight = 48, weight = 1f, sidePaddingDp = 12))

        // Row 3
        val row3 = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(48))
        }
        row3.addView(createCharKey("<", 1.2f))
        row3.addView(createCharKey(">", 1.2f))
        row3.addView(createCharKey("[", 1f))
        row3.addView(createCharKey("]", 1f))
        row3.addView(createCharKey("{", 1f))
        row3.addView(createCharKey("}", 1f))
        row3.addView(createCharKey("=", 1f))

        val deleteKey = createSpecialKey("⌫", 1.5f, "#49454F") {
            listener?.onDeleteKey()
        }
        row3.addView(deleteKey)
        keysLayout.addView(row3)

        // Bottom row: [ABC] [,] [   Space   ] [.] [↵]
        val bottomRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(48))
        }

        val abcKey = createSpecialKey("ABC", 1.5f, "#49454F") {
            isSymbols = false
            rebuildKeys()
        }
        bottomRow.addView(abcKey)

        bottomRow.addView(createCharKey(",", 1f))

        val spaceKey = createSpecialKey("Space", 4.5f, "#332D41") {
            listener?.onSpaceKey()
        }
        bottomRow.addView(spaceKey)

        bottomRow.addView(createCharKey(".", 1f))

        val enterKey = createSpecialKey("↵", 1.5f, "#6750A4") {
            listener?.onEnterKey()
        }
        bottomRow.addView(enterKey)

        keysLayout.addView(bottomRow)
    }

    private fun createRow(
        keys: List<String>,
        keyHeight: Int,
        weight: Float,
        sidePaddingDp: Int = 0
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(keyHeight))
            if (sidePaddingDp > 0) {
                setPadding(dpToPx(sidePaddingDp), 0, dpToPx(sidePaddingDp), 0)
            }
            for (k in keys) {
                addView(createCharKey(k, weight))
            }
        }
    }

    private fun createCharKey(char: String, weight: Float): View {
        return TextView(context).apply {
            text = char
            textSize = 18f
            setTextColor(Color.parseColor("#E6E1E5"))
            gravity = Gravity.CENTER
            background = createKeyBackground("#332D41")
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, weight).apply {
                setMargins(dpToPx(2), dpToPx(3), dpToPx(2), dpToPx(3))
            }
            setOnClickListener {
                performHaptic()
                listener?.onTextKey(char)
                if (isShifted && !isCapsLocked) {
                    isShifted = false
                    rebuildKeys()
                }
            }
        }
    }

    private fun createSpecialKey(
        label: String,
        weight: Float,
        bgColorHex: String,
        onClick: () -> Unit
    ): View {
        return TextView(context).apply {
            text = label
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#FFFFFF"))
            gravity = Gravity.CENTER
            background = createKeyBackground(bgColorHex)
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, weight).apply {
                setMargins(dpToPx(2), dpToPx(3), dpToPx(2), dpToPx(3))
            }
            setOnClickListener {
                performHaptic()
                onClick()
            }
        }
    }

    private fun performHaptic() {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun createKeyBackground(colorHex: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(8).toFloat()
            setColor(Color.parseColor(colorHex))
        }
    }

    private fun createChipBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(20).toFloat()
            setColor(Color.parseColor("#381E72")) // Deep purple container
            setStroke(dpToPx(1), Color.parseColor("#9A82DB"))
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}


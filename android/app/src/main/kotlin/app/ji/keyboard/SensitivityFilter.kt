package app.ji.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo

object SensitivityFilter {

    /**
     * Rule 4 (SPEC.md):
     * Disable all detection when the input field is a password, OTP, email-address,
     * or payment field. Also check IME_FLAG_NO_PERSONALIZED_LEARNING and incognito.
     *
     * Returns true if detection MUST be disabled.
     */
    fun isSensitiveField(editorInfo: EditorInfo?): Boolean {
        if (editorInfo == null) return true

        // 1. Check IME_FLAG_NO_PERSONALIZED_LEARNING
        if ((editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0) {
            return true
        }

        val inputType = editorInfo.inputType
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION

        // 2. Password fields (Text, Web, Number)
        if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        ) {
            return true
        }

        // 3. Email address fields
        if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
        ) {
            return true
        }

        // 4. Numeric payment, PIN, and OTP fields
        if (inputClass == InputType.TYPE_CLASS_NUMBER || inputClass == InputType.TYPE_CLASS_PHONE) {
            return true
        }

        return false
    }
}


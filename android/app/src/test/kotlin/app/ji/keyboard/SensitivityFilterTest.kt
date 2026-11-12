package app.ji.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitivityFilterTest {

    @Test
    fun testNullEditorInfoIsSensitive() {
        assertTrue(SensitivityFilter.isSensitiveField(null))
    }

    @Test
    fun testNoPersonalizedLearningFlag() {
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        }
        assertTrue(SensitivityFilter.isSensitiveField(info))
    }

    @Test
    fun testPasswordVariationsAreSensitive() {
        val textPassword = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        assertTrue(SensitivityFilter.isSensitiveField(textPassword))

        val webPassword = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        }
        assertTrue(SensitivityFilter.isSensitiveField(webPassword))

        val visiblePassword = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        assertTrue(SensitivityFilter.isSensitiveField(visiblePassword))

        val numberPassword = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        assertTrue(SensitivityFilter.isSensitiveField(numberPassword))
    }

    @Test
    fun testEmailVariationsAreSensitive() {
        val email = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        assertTrue(SensitivityFilter.isSensitiveField(email))

        val webEmail = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
        }
        assertTrue(SensitivityFilter.isSensitiveField(webEmail))
    }

    @Test
    fun testNumericOtpPaymentFieldsAreSensitive() {
        val numberField = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        assertTrue(SensitivityFilter.isSensitiveField(numberField))

        val phoneField = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_PHONE
        }
        assertTrue(SensitivityFilter.isSensitiveField(phoneField))
    }

    @Test
    fun testStandardTextFieldIsNotSensitive() {
        val standardText = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
        assertFalse(SensitivityFilter.isSensitiveField(standardText))
    }
}


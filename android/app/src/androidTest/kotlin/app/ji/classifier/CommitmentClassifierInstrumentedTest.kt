package app.ji.classifier

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import app.ji.data.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommitmentClassifierInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testTFLiteClassifier_InitializesAndClassifies() {
        val classifier = CommitmentClassifierProvider.getClassifier(context)
        assertNotNull(classifier)

        // Test commitment
        val res1 = classifier.classify("I will email the client deck by 5 pm")
        assertTrue("Expected commitment for 'I will email the client deck by 5 pm'", res1.isCommitment)
        assertEquals(Reminder.CATEGORY_PROFESSIONAL, res1.category)

        // Test non-commitment (Event)
        val res2 = classifier.classify("The train leaves at 5 pm")
        assertFalse("Expected non-commitment for 'The train leaves at 5 pm'", res2.isCommitment)

        // Test non-commitment (Question)
        val res3 = classifier.classify("What time is the meeting?")
        assertFalse("Expected non-commitment for 'What time is the meeting?'", res3.isCommitment)

        // Test non-commitment (Past tense)
        val res4 = classifier.classify("I called you yesterday at 5")
        assertFalse("Expected non-commitment for 'I called you yesterday at 5'", res4.isCommitment)
    }

    @Test
    fun testTFLiteClassifier_Categories() {
        val classifier = CommitmentClassifierProvider.getClassifier(context)

        val parcel = classifier.classify("I will collect the Amazon parcel at 3 pm")
        assertTrue(parcel.isCommitment)
        assertEquals(Reminder.CATEGORY_PARCEL, parcel.category)

        val urgent = classifier.classify("Urgent: I must submit this report before 4 pm")
        assertTrue(urgent.isCommitment)
        assertEquals(Reminder.CATEGORY_URGENT, urgent.category)

        val personal = classifier.classify("I'll call mom tomorrow at 6 pm")
        assertTrue(personal.isCommitment)
        assertEquals(Reminder.CATEGORY_PERSONAL, personal.category)
    }

    @Test
    fun testRuleBasedFallback() {
        val fallback = RuleBasedClassifier
        val res = fallback.classify("I'll call you tomorrow at 5")
        assertTrue(res.isCommitment)
        assertEquals(CommitmentClassifier.ClassifierSource.RULE_BASED_FALLBACK, res.source)
    }
}


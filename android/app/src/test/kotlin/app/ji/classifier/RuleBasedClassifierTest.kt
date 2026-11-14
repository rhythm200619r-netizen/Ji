package app.ji.classifier

import app.ji.data.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleBasedClassifierTest {

    private val classifier = RuleBasedClassifier

    @Test
    fun testCommitmentExamples_SPEC() {
        // SPEC: "I'll send it by 5" = yes
        val res1 = classifier.classify("I'll send it by 5")
        assertTrue("Expected commitment for 'I'll send it by 5'", res1.isCommitment)

        // SPEC: "the meeting is at 5" = no
        val res2 = classifier.classify("the meeting is at 5")
        assertFalse("Expected non-commitment for 'the meeting is at 5'", res2.isCommitment)
    }

    @Test
    fun testCommitmentCategories() {
        val prof = classifier.classify("I will submit the project report at 5 pm")
        assertTrue(prof.isCommitment)
        assertEquals(Reminder.CATEGORY_PROFESSIONAL, prof.category)

        val pers = classifier.classify("I'll call mom tomorrow at 6 pm")
        assertTrue(pers.isCommitment)
        assertEquals(Reminder.CATEGORY_PERSONAL, pers.category)

        val parcel = classifier.classify("I will pick up the Amazon parcel at 3 pm")
        assertTrue(parcel.isCommitment)
        assertEquals(Reminder.CATEGORY_PARCEL, parcel.category)

        val urgent = classifier.classify("Urgent: I must submit this assignment before 4 pm")
        assertTrue(urgent.isCommitment)
        assertEquals(Reminder.CATEGORY_URGENT, urgent.category)
    }

    @Test
    fun testHinglishCommitments() {
        val h1 = classifier.classify("kal shaam 5 baje bhej dunga")
        assertTrue(h1.isCommitment)

        val h2 = classifier.classify("main kal subah report send karunga")
        assertTrue(h2.isCommitment)
        assertEquals(Reminder.CATEGORY_PROFESSIONAL, h2.category)

        val h3 = classifier.classify("delivery wale ko 4 baje OTP dunga")
        assertTrue(h3.isCommitment)
        assertEquals(Reminder.CATEGORY_PARCEL, h3.category)
    }

    @Test
    fun testNonCommitments_QuestionsAndEvents() {
        val q1 = classifier.classify("What time is the meeting?")
        assertFalse(q1.isCommitment)

        val q2 = classifier.classify("Are you coming at 5 pm?")
        assertFalse(q2.isCommitment)

        val ev1 = classifier.classify("The train leaves at 4:30")
        assertFalse(ev1.isCommitment)

        val ev2 = classifier.classify("Movie starts at 7 pm")
        assertFalse(ev2.isCommitment)

        val ev3 = classifier.classify("Office timing is 9 am to 6 pm")
        assertFalse(ev3.isCommitment)
    }

    @Test
    fun testNonCommitments_PastTense() {
        val p1 = classifier.classify("I called you yesterday at 5")
        assertFalse(p1.isCommitment)

        val p2 = classifier.classify("He arrived 2 hours ago")
        assertFalse(p2.isCommitment)

        val p3 = classifier.classify("maine kal report bhej di thi")
        assertFalse(p3.isCommitment)
    }

    @Test
    fun testBlankAndCasual() {
        val b = classifier.classify("")
        assertFalse(b.isCommitment)

        val c = classifier.classify("Hello how are you doing")
        assertFalse(c.isCommitment)
    }
}


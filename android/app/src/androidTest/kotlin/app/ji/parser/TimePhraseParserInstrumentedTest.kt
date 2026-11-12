package app.ji.parser

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * Instrumented tests for TimePhraseParser running on Android's ICU regex engine.
 *
 * These tests exist because the JVM unit tests pass on desktop Java which supports
 * Pattern.UNICODE_CHARACTER_CLASS (the U flag), while Android's ICU-based regex engine
 * does not. This suite verifies that all regex patterns compile and match correctly
 * on a real Android device or emulator.
 *
 * Covers: English, Hindi (Devanagari), Hinglish (romanized), and past-tense negatives.
 */
@RunWith(AndroidJUnit4::class)
class TimePhraseParserInstrumentedTest {

    // Fixed reference time: Friday 15 Mar 2024, 10:00 AM
    private val ref = LocalDateTime.of(2024, 3, 15, 10, 0)

    // ── English phrases ───────────────────────────────────────────────────

    @Test
    fun english_tomorrowAt5() {
        val result = TimePhraseParser.parse("call you tomorrow at 5", ref)
        assertNotNull("tomorrow at 5 should resolve", result)
        assertEquals(2024, result!!.year)
        assertEquals(3, result.monthValue)
        assertEquals(16, result.dayOfMonth)
        assertEquals(17, result.hour) // bare 5 on future day → 5 PM
    }

    @Test
    fun english_in2Hours() {
        val result = TimePhraseParser.parse("meeting in 2 hours", ref)
        assertNotNull(result)
        assertEquals(12, result!!.hour)
        assertEquals(0, result.minute)
    }

    @Test
    fun english_at3pm() {
        val result = TimePhraseParser.parse("finish report at 3pm", ref)
        assertNotNull(result)
        assertEquals(15, result!!.hour)
    }

    @Test
    fun english_nextMonday() {
        val result = TimePhraseParser.parse("let's meet next Monday", ref)
        assertNotNull(result)
        // ref is Friday 15 Mar; next Monday = 18 Mar
        assertEquals(18, result!!.dayOfMonth)
    }

    @Test
    fun english_in30Minutes() {
        val result = TimePhraseParser.parse("call me in 30 minutes", ref)
        assertNotNull(result)
        assertEquals(10, result!!.hour)
        assertEquals(30, result.minute)
    }

    @Test
    fun english_dayAfterTomorrow() {
        val result = TimePhraseParser.parse("party day after tomorrow", ref)
        assertNotNull(result)
        assertEquals(17, result!!.dayOfMonth)
    }

    @Test
    fun english_morningDefault() {
        val result = TimePhraseParser.parse("tomorrow morning", ref)
        assertNotNull(result)
        assertEquals(16, result!!.dayOfMonth)
        assertEquals(9, result.hour) // morning default
    }

    @Test
    fun english_evening() {
        val result = TimePhraseParser.parse("tomorrow evening", ref)
        assertNotNull(result)
        assertEquals(18, result!!.hour) // evening default
    }

    @Test
    fun english_noon() {
        val result = TimePhraseParser.parse("tomorrow at noon", ref)
        assertNotNull(result)
        assertEquals(12, result!!.hour)
    }

    @Test
    fun english_at14Colon00() {
        val result = TimePhraseParser.parse("meeting at 14:00", ref)
        assertNotNull(result)
        assertEquals(14, result!!.hour)
        assertEquals(0, result.minute)
    }

    // ── Hindi (Devanagari) phrases ────────────────────────────────────────

    @Test
    fun hindi_kalSubah() {
        // कल सुबह = tomorrow morning
        val result = TimePhraseParser.parse("कल सुबह मिलते हैं", ref)
        assertNotNull("कल सुबह should resolve", result)
        assertEquals(16, result!!.dayOfMonth)
        assertEquals(9, result.hour)
    }

    @Test
    fun hindi_kalShaam() {
        // कल शाम = tomorrow evening
        val result = TimePhraseParser.parse("कल शाम को मिलेंगे", ref)
        assertNotNull(result)
        assertEquals(18, result!!.hour)
    }

    @Test
    fun hindi_devanagariDigit5Baje() {
        // ५ बजे = 5 baje (5 o'clock)
        val result = TimePhraseParser.parse("कल ५ बजे", ref)
        assertNotNull("५ बजे should resolve", result)
        assertEquals(17, result!!.hour) // 5 PM
    }

    @Test
    fun hindi_2GhantePehle_pastNegative() {
        // 2 घंटे पहले = 2 hours ago (past-tense → should return null)
        val result = TimePhraseParser.parse("2 घंटे पहले", ref)
        assertNull("past-tense Hindi should return null", result)
    }

    @Test
    fun hindi_parso() {
        // परसों = day after tomorrow
        val result = TimePhraseParser.parse("परसों मिलेंगे", ref)
        assertNotNull(result)
        assertEquals(17, result!!.dayOfMonth)
    }

    @Test
    fun hindi_kalDopahar() {
        // कल दोपहर = tomorrow afternoon
        val result = TimePhraseParser.parse("कल दोपहर", ref)
        assertNotNull(result)
        assertEquals(16, result!!.dayOfMonth)
        assertEquals(14, result.hour)
    }

    @Test
    fun hindi_kalRaat() {
        // कल रात = tomorrow night
        val result = TimePhraseParser.parse("कल रात", ref)
        assertNotNull(result)
        assertEquals(16, result!!.dayOfMonth)
        assertEquals(21, result.hour)
    }

    @Test
    fun hindi_kalBhejaTha_pastNegative() {
        val result = TimePhraseParser.parse("कल भेजा था", ref)
        assertNull(result)
    }

    // ── Hinglish (romanized) phrases ──────────────────────────────────────

    @Test
    fun hinglish_kalShaam5Baje() {
        val result = TimePhraseParser.parse("kal shaam 5 baje", ref)
        assertNotNull("kal shaam 5 baje should resolve", result)
        assertEquals(16, result!!.dayOfMonth)
        assertEquals(17, result.hour) // shaam + 5 → 5 PM
    }

    @Test
    fun hinglish_2GhanteMein() {
        val result = TimePhraseParser.parse("2 ghante mein", ref)
        assertNotNull(result)
        assertEquals(12, result!!.hour)
    }

    @Test
    fun hinglish_dedhGhanteMein() {
        val result = TimePhraseParser.parse("dedh ghante mein call karna", ref)
        assertNotNull(result)
        // 10:00 + 90 min = 11:30
        assertEquals(11, result!!.hour)
        assertEquals(30, result.minute)
    }

    @Test
    fun hinglish_parsoSubah() {
        val result = TimePhraseParser.parse("parso subah milte hain", ref)
        assertNotNull(result)
        assertEquals(17, result!!.dayOfMonth)
        assertEquals(9, result.hour)
    }

    @Test
    fun hinglish_aajRaat() {
        // aaj raat = tonight
        val refEvening = LocalDateTime.of(2024, 3, 15, 18, 0)
        val result = TimePhraseParser.parse("aaj raat milte hain", refEvening)
        assertNotNull(result)
        assertEquals(21, result!!.hour) // night default
    }

    @Test
    fun hinglish_saade4Baje() {
        val result = TimePhraseParser.parse("kal saade 4 baje", ref)
        assertNotNull(result)
        assertEquals(16, result!!.dayOfMonth)
        assertEquals(30, result.minute) // saade = :30
    }

    @Test
    fun hinglish_sawa3() {
        val result = TimePhraseParser.parse("kal sawa 3 baje", ref)
        assertNotNull(result)
        assertEquals(15, result!!.minute) // sawa = :15
    }

    @Test
    fun hinglish_paune4() {
        val result = TimePhraseParser.parse("kal paune 4 baje", ref)
        assertNotNull(result)
        assertEquals(45, result!!.minute) // paune 4 = 3:45
    }

    @Test
    fun hinglish_nextWeekFriday() {
        val result = TimePhraseParser.parse("next week friday ko call karna", ref)
        assertNotNull(result)
        assertEquals(22, result!!.dayOfMonth)
    }

    @Test
    fun hinglish_agleSomwar() {
        val result = TimePhraseParser.parse("agle somwar milte hain", ref)
        assertNotNull(result)
        assertEquals(18, result!!.dayOfMonth)
    }

    // ── Past-tense negatives ──────────────────────────────────────────────

    @Test
    fun negative_alreadyCalled() {
        val result = TimePhraseParser.parse("already called them", ref)
        assertNull(result)
    }

    @Test
    fun negative_2HoursAgo() {
        val result = TimePhraseParser.parse("2 hours ago", ref)
        assertNull("'2 hours ago' should return null", result)
    }

    @Test
    fun negative_yesterday() {
        val result = TimePhraseParser.parse("yesterday I called", ref)
        assertNull("'yesterday' should return null", result)
    }

    @Test
    fun negative_lastMonday() {
        val result = TimePhraseParser.parse("last Monday", ref)
        assertNull(result)
    }

    @Test
    fun negative_kalCallKiyaTha() {
        // "kal maine call kiya tha" = "I called yesterday" (past)
        val result = TimePhraseParser.parse("kal maine call kiya tha", ref)
        assertNull("past-tense Hinglish should return null", result)
    }

    @Test
    fun negative_2GhantePehle() {
        val result = TimePhraseParser.parse("2 ghante pehle", ref)
        assertNull("'2 ghante pehle' should return null", result)
    }

    @Test
    fun phraseWithToday_resolvesToNextDefaultSlot() {
        // "nice weather today huh" contains the time phrase "today" (Test #29 rule).
        // At 10:00 reference time, the next available default slot is 14:00 (2:00 PM).
        val result = TimePhraseParser.parse("nice weather today huh", ref)
        assertNotNull(result)
        assertEquals(2024, result!!.year)
        assertEquals(3, result.monthValue)
        assertEquals(15, result.dayOfMonth)
        assertEquals(14, result.hour)
    }

    @Test
    fun negative_blankAndIrrelevant() {
        assertNull(TimePhraseParser.parse("", ref))
        assertNull(TimePhraseParser.parse("   ", ref))
        assertNull(TimePhraseParser.parse("hello world", ref))
        assertNull(TimePhraseParser.parse("nice weather huh", ref))
        assertNull(TimePhraseParser.parse("👍🎉🔥", ref))
        assertNull(TimePhraseParser.parse("how are you doing", ref))
        assertNull(TimePhraseParser.parse("kya haal hai bhai", ref))
        assertNull(TimePhraseParser.parse("kaise ho aap", ref))
        assertNull(TimePhraseParser.parse("सब ठीक है", ref))
        assertNull(TimePhraseParser.parse("ok sounds good", ref))
    }
}


package app.ji.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class SentenceExtractorTest {

    @Test
    fun testExtractCurrentSentence() {
        assertEquals("call you tomorrow at 5", SentenceExtractor.extractCurrentSentence("Hey there! call you tomorrow at 5"))
        assertEquals("kal shaam 5 baje bhejta hoon", SentenceExtractor.extractCurrentSentence("Done with work. kal shaam 5 baje bhejta hoon"))
        assertEquals("I will send it by 6", SentenceExtractor.extractCurrentSentence("Okay? I will send it by 6"))
        assertEquals("", SentenceExtractor.extractCurrentSentence("Hello."))
        assertEquals("hello", SentenceExtractor.extractCurrentSentence("hello"))
        assertEquals("", SentenceExtractor.extractCurrentSentence(null))
        assertEquals("", SentenceExtractor.extractCurrentSentence(""))
    }

    @Test
    fun testCreateSnippetMaxLength() {
        val short = "call you tomorrow at 5"
        assertEquals("call you tomorrow at 5", SentenceExtractor.createSnippet(short))

        val long = "This is an extremely long commitment text that exceeds the maximum length permitted by the non-negotiable rules in the specification document for the Ji project which is 120 characters"
        val snippet = SentenceExtractor.createSnippet(long)
        assertTrue(snippet.length <= 120)
        assertTrue(snippet.endsWith("..."))
    }

    @Test
    fun testFormatTimeForChip() {
        val ref = LocalDateTime.of(2024, 3, 15, 10, 0)
        val todayTime = LocalDateTime.of(2024, 3, 15, 17, 0)
        assertEquals("today at 5:00 PM", SentenceExtractor.formatTimeForChip(todayTime, ref))

        val tomTime = LocalDateTime.of(2024, 3, 16, 9, 30)
        assertEquals("tomorrow at 9:30 AM", SentenceExtractor.formatTimeForChip(tomTime, ref))

        val datTime = LocalDateTime.of(2024, 3, 17, 14, 0)
        assertEquals("in 2 days at 2:00 PM", SentenceExtractor.formatTimeForChip(datTime, ref))
    }
}


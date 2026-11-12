package app.ji.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TimePhraseParserTest {

    private data class TestCase(
        val id: Int,
        val input: String,
        val reference: LocalDateTime,
        val expectedStr: String,
        val expected: LocalDateTime?,
        val comment: String
    )

    private fun findTestCasesFile(): File {
        val candidates = listOf(
            File("../../parser-tests/time_phrase_cases.txt"),
            File("../parser-tests/time_phrase_cases.txt"),
            File("parser-tests/time_phrase_cases.txt"),
            File("../../../parser-tests/time_phrase_cases.txt")
        )
        for (candidate in candidates) {
            if (candidate.exists()) {
                return candidate.canonicalFile
            }
        }
        var dir: File? = File(".").canonicalFile
        while (dir != null) {
            val check = File(dir, "parser-tests/time_phrase_cases.txt")
            if (check.exists()) return check
            dir = dir.parentFile
        }
        throw IllegalStateException("time_phrase_cases.txt not found in any parent directories")
    }

    private fun loadTestCases(): List<TestCase> {
        val file = findTestCasesFile()
        val cases = mutableListOf<TestCase>()
        file.forEachLine(Charsets.UTF_8) { rawLine ->
            val line = rawLine.trim()
            if (line.isNotEmpty() && !line.startsWith("#") && line.contains("|")) {
                val parts = line.split("|")
                if (parts.size >= 4) {
                    val id = parts[0].trim().toInt()
                    val input = parts[1].trim()
                    val ref = LocalDateTime.parse(parts[2].trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    val expStr = parts[3].trim()
                    val expected = if (expStr.equals("NONE", ignoreCase = true) ||
                        expStr.equals("null", ignoreCase = true) ||
                        expStr.equals("NEEDS_TIME", ignoreCase = true)
                    ) {
                        null
                    } else {
                        LocalDateTime.parse(expStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    }
                    val comment = if (parts.size > 4) parts[4].trim() else ""
                    cases.add(TestCase(id, input, ref, expStr, expected, comment))
                }
            }
        }
        return cases
    }

    @Test
    fun testAllCasesFromSpec() {
        val cases = loadTestCases()
        assertTrue("Test cases should not be empty", cases.isNotEmpty())
        println("Loaded ${cases.size} test cases from time_phrase_cases.txt")

        val failedCases = mutableListOf<String>()

        for (tc in cases) {
            val detailed = TimePhraseParser.parseDetailed(tc.input, tc.reference)

            if (tc.expectedStr.equals("NEEDS_TIME", ignoreCase = true)) {
                if (detailed == null) {
                    failedCases.add(
                        "Case #${tc.id} ('${tc.input}') expected NEEDS_TIME but got NULL (comment: ${tc.comment})"
                    )
                } else if (!detailed.needsTime) {
                    failedCases.add(
                        "Case #${tc.id} ('${tc.input}') expected NEEDS_TIME but got '${detailed.resolvedDateTime}' (comment: ${tc.comment})"
                    )
                }
            } else if (tc.expectedStr.equals("NONE", ignoreCase = true) || tc.expectedStr.equals("null", ignoreCase = true)) {
                if (detailed != null && (detailed.resolvedDateTime != null || detailed.needsTime)) {
                    failedCases.add(
                        "Case #${tc.id} ('${tc.input}') expected NONE but got '${detailed.resolvedDateTime ?: "NEEDS_TIME"}' (comment: ${tc.comment})"
                    )
                }
            } else {
                val actual = detailed?.resolvedDateTime
                if (actual == null) {
                    failedCases.add(
                        "Case #${tc.id} ('${tc.input}') expected '${tc.expected}' but got NULL (comment: ${tc.comment})"
                    )
                } else if (actual != tc.expected) {
                    failedCases.add(
                        "Case #${tc.id} ('${tc.input}') expected '${tc.expected}' but got '$actual' (comment: ${tc.comment})"
                    )
                }
            }
        }

        if (failedCases.isNotEmpty()) {
            println("FAILED ${failedCases.size} of ${cases.size} cases:")
            failedCases.forEach { println(" - $it") }
        } else {
            println("All ${cases.size} cases PASSED!")
        }

        assertTrue(
            "${failedCases.size} cases failed:\n" + failedCases.joinToString("\n"),
            failedCases.isEmpty()
        )
    }
}

package app.ji.parser

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.util.regex.Pattern

data class ParsedTimeResult(
    val resolvedDateTime: LocalDateTime?,
    val matchedText: String,
    val needsTime: Boolean = false,
    val startIndex: Int = 0,
    val endIndex: Int = 0
)

object TimePhraseParser {

    private val DEFAULT_SLOTS = listOf(
        LocalTime.of(9, 0),
        LocalTime.of(14, 0),
        LocalTime.of(18, 0),
        LocalTime.of(21, 0)
    )

    private val MORNING_DEFAULT = LocalTime.of(9, 0)
    private val AFTERNOON_DEFAULT = LocalTime.of(14, 0)
    private val EVENING_DEFAULT = LocalTime.of(18, 0)
    private val NIGHT_DEFAULT = LocalTime.of(21, 0)

    private enum class PeriodOfDay {
        MORNING, AFTERNOON, EVENING, NIGHT, NOON, EOD, MIDNIGHT
    }

    // ── Pre-compiled regex patterns (compiled once at class load) ──────────

    // Past-tense patterns
    // Note: \b is not Unicode-aware without the U flag on desktop JVM, and Android's ICU
    // engine doesn't support the U flag at all. We use explicit boundary groups
    // (?:^|\W) and (?:\W|$) for patterns containing Devanagari or mixed-script words.
    private val PAST_ENG = Pattern.compile(
        """\b(?:\d+\s*(?:hours?|hrs?|minutes?|mins?)\s+ago|an?\s+hour\s+ago|yesterday|last\s+(?:night|week|month|year|monday|tuesday|wednesday|thursday|friday|saturday|sunday)|already\s+(?:called|sent|done|met))\b""",
        Pattern.CASE_INSENSITIVE
    )
    private val PAST_HINDI_PEHLE = Pattern.compile(
        """(?i)(?:^|\W)(?:\d+|ek|aadhe|dedh|एक|आधे|डेढ़)\s*(?:ghante?|ghanta|घंटे|घंटा|minute?|mins?|मिनट|hours?)\s*(?:pehle|पहले)(?:\W|$)"""
    )
    private val PAST_HINDI_KAL = Pattern.compile(
        """(?i)(?:^|\W)(?:kal|kl|parso|कल|परसों)(?:\W).*?(?:tha|thi|the|था|थी|थे|gaya|gayi|gaye|hua|hui|hue|aaya|aayi|bheja|bheji|kiya|kiyi)(?:\W|$)"""
    )

    // Relative duration patterns
    private val REL_DAYS = Pattern.compile(
        """\bin\s+(\d+)\s*days?\b""", Pattern.CASE_INSENSITIVE
    )
    private val REL_ENG = Pattern.compile(
        """\bin\s+(?:(half\s+an?)\s*hour|(an?|1)\s*(?:hour|hr)|(\d+)\s*(?:hours?|hrs?|hr)|(\d+)\s*(?:minutes?|mins?|min))\b""",
        Pattern.CASE_INSENSITIVE
    )
    private val REL_HINDI = Pattern.compile(
        """(?i)\b(?:(dedh|डेढ़)\s*(?:ghante?|घंटे|घंटा)|(aadhe|आधे)\s*(?:ghante?|घंटे|घंटा)|(ek|1|एक)\s*(?:ghante?|घंटे|घंटा)|(\d+)\s*(?:ghante?|घंटे|घंटा)|(\d+)\s*(?:minute?|mins?|मिनट))\s*(?:mein|me|में)\b"""
    )

    // Day patterns
    private val DAY_NEXT_WEEK = Pattern.compile(
        """(?i)(?:^|[\s,.\?!])next\s+week(?:\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday))?(?:$|[\s,.\?!])"""
    )
    private val DAY_CALENDAR_DATE = Pattern.compile(
        """(?i)(?:^|[\s,.\?!])(?:on\s+)?(?:(\d{1,2})(?:st|nd|rd|th)?\s+(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)|(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\s+(\d{1,2})(?:st|nd|rd|th)?)(?:$|[\s,.\?!])"""
    )
    private val DAY_AFTER_TOMORROW = Pattern.compile(
        """(?i)(?:^|[\s,.\?!])(?:day\s+after\s+tomorrow|parso|parson|परसों)(?:$|[\s,.\?!])"""
    )
    private val DAY_TOMORROW = Pattern.compile(
        """(?i)(?:^|[\s,.\?!])(?:tomorrow|tmrw|kal|kl|कल)(?:$|[\s,.\?!])"""
    )
    private val DAY_TODAY = Pattern.compile(
        """(?i)(?:^|[\s,.\?!])(?:today|tonight|this\s+evening|this\s+afternoon|aaj|आज)(?:$|[\s,.\?!])"""
    )
    private val DAY_WEEKDAY = Pattern.compile(
        """(?i)(?:^|[\s,.\?!])(?:(next|this|on|agle|agla|अगले)\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday|somvaar|somwar|सोमवार|mangalvaar|mangalwar|मंगलवार|budhvaar|budhwar|बुधवार|guruvaar|guruwar|veervaar|वीरवार|गुरुवार|shukravaar|shukrawar|शुक्रवार|shanivaar|shaniwar|शनिवार|ravivaar|raviwar|aitvaar|aitwar|itwaar|itwar|रविवार|इतवार)(?:\s*(?:ko|को))?(?:$|[\s,.\?!])"""
    )

    // Period patterns
    private val PERIOD_REGEX = Pattern.compile(
        """(?i)(?:^|[\s,.\?!])(?:(morning|subah|सुबह)|(afternoon|dopahar|दोपहर)|(evening|shaam|sham|शाम)|(night|tonight|raat|रात)|(noon)|(end\s+of\s+day|eod)|(midnight))(?:$|[\s,.\?!])"""
    )

    // Clock patterns
    private val CLOCK_RANGE = Pattern.compile(
        """(?i)(?:between\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\s+and\s+(\d{1,2})|from\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\s+to\s+(\d{1,2})|(\d{1,2})(?::(\d{2}))?\s*se\s+(\d{1,2}))"""
    )
    private val CLOCK_IDIOM = Pattern.compile(
        """(?i)(?:^|[\s,.\?!])(?:(dedh|डेढ़)\s*(?:baje|बजे)?|(dhai|ढाई)\s*(?:baje|बजे)?|(saade|साढ़े)\s*(\d{1,2})\s*(?:baje|बजे)?|(sawa|सवा)\s*(\d{1,2})\s*(?:baje|बजे)?|(paune|पौने)\s*(\d{1,2})\s*(?:baje|बजे)?)(?:$|[\s,.\?!])"""
    )
    private val CLOCK_AMPM = Pattern.compile(
        """(?i)(?:^|[\s,.\?!])(?:(?:at|by)\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm|a\.m\.|p\.m\.)(?:$|[\s,.\?!])"""
    )
    private val CLOCK_BAJE = Pattern.compile(
        """(?i)(?:^|[\s,.\?!])(\d{1,2})(?::(\d{2}))?\s*(?:baje|बजे)(?:$|[\s,.\?!])"""
    )
    private val CLOCK_AT_BY = Pattern.compile(
        """(?i)(?:^|[\s,.\?!])(?:at|by)\s+(\d{1,2})(?::(\d{2}))?(?:$|[\s,.\?!])"""
    )

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Parses the input string for a time commitment phrase.
     * Returns the resolved LocalDateTime, or null if no valid future time phrase was found.
     */
    fun parse(text: String, referenceTime: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        return parseDetailed(text, referenceTime)?.resolvedDateTime
    }

    /**
     * Parses the input string and returns detailed information including matched text span and needsTime flag.
     */
    fun parseDetailed(text: String, referenceTime: LocalDateTime = LocalDateTime.now()): ParsedTimeResult? {
        if (text.isBlank()) return null

        // Strip leading/trailing quotation marks and punctuation
        val unquoted = text.trim('\"', '\'', ' ', '\t', '\n')
        val normalized = normalizeDigits(unquoted)
        val lower = normalized.lowercase()

        // 1. Check for past-tense negation
        if (isPastTense(lower)) {
            return null
        }

        // 2. Relative durations ("in 2 hours", "in 2 days", "dedh ghante mein", etc.)
        val relative = parseRelativeDuration(normalized, lower, referenceTime)
        if (relative != null) return relative

        // 3. Calendar & clock combinations
        return parseCalendarTime(normalized, lower, referenceTime)
    }

    // ── Internal methods ──────────────────────────────────────────────────

    private fun normalizeDigits(input: String): String {
        val devanagariDigits = "०१२३४५६७८९"
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val idx = devanagariDigits.indexOf(ch)
            if (idx >= 0) {
                sb.append(('0' + idx))
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun isPastTense(lower: String): Boolean {
        // English past indicators
        if (PAST_ENG.matcher(lower).find()) return true
        // Hindi / Hinglish: "2 ghante pehle", "2 घंटे पहले", etc.
        if (PAST_HINDI_PEHLE.matcher(lower).find()) return true
        // "kal … tha/thi/the" style past
        if (PAST_HINDI_KAL.matcher(lower).find()) return true
        return false
    }

    private fun parseRelativeDuration(
        rawText: String,
        lowerText: String,
        referenceTime: LocalDateTime
    ): ParsedTimeResult? {
        // English: "in X days"
        val daysMatcher = REL_DAYS.matcher(lowerText)
        if (daysMatcher.find()) {
            val count = daysMatcher.group(1).toLong()
            return ParsedTimeResult(
                resolvedDateTime = referenceTime.plusDays(count),
                matchedText = rawText.substring(daysMatcher.start(), daysMatcher.end()),
                startIndex = daysMatcher.start(),
                endIndex = daysMatcher.end()
            )
        }

        // English: "in X hours", "in X mins", "in an hour", "in half an hour", "in 1 hr"
        val engMatcher = REL_ENG.matcher(lowerText)
        if (engMatcher.find()) {
            val halfHour = engMatcher.group(1)
            val oneHour = engMatcher.group(2)
            val hours = engMatcher.group(3)
            val minutes = engMatcher.group(4)

            val resolved = when {
                halfHour != null -> referenceTime.plusMinutes(30)
                oneHour != null -> referenceTime.plusHours(1)
                hours != null -> referenceTime.plusHours(hours.toLong())
                minutes != null -> referenceTime.plusMinutes(minutes.toLong())
                else -> null
            }
            if (resolved != null) {
                return ParsedTimeResult(
                    resolvedDateTime = resolved,
                    matchedText = rawText.substring(engMatcher.start(), engMatcher.end()),
                    startIndex = engMatcher.start(),
                    endIndex = engMatcher.end()
                )
            }
        }

        // Hindi / Hinglish relative durations
        val hindiMatcher = REL_HINDI.matcher(lowerText)
        if (hindiMatcher.find()) {
            val dedh = hindiMatcher.group(1)
            val aadhe = hindiMatcher.group(2)
            val ek = hindiMatcher.group(3)
            val hours = hindiMatcher.group(4)
            val minutes = hindiMatcher.group(5)

            val resolved = when {
                dedh != null -> referenceTime.plusMinutes(90)
                aadhe != null -> referenceTime.plusMinutes(30)
                ek != null -> referenceTime.plusHours(1)
                hours != null -> referenceTime.plusHours(hours.toLong())
                minutes != null -> referenceTime.plusMinutes(minutes.toLong())
                else -> null
            }
            if (resolved != null) {
                return ParsedTimeResult(
                    resolvedDateTime = resolved,
                    matchedText = rawText.substring(hindiMatcher.start(), hindiMatcher.end()),
                    startIndex = hindiMatcher.start(),
                    endIndex = hindiMatcher.end()
                )
            }
        }

        return null
    }

    private data class DayMatch(
        val date: LocalDate,
        val isExplicitDay: Boolean,
        val isFutureDay: Boolean,
        val isTodayKeyword: Boolean = false,
        val startIndex: Int,
        val endIndex: Int
    )

    private data class PeriodMatch(
        val period: PeriodOfDay,
        val startIndex: Int,
        val endIndex: Int
    )

    private data class ClockMatch(
        val hour: Int,
        val minute: Int,
        val is12HourExplicit: Boolean,
        val isPm: Boolean?,
        val startIndex: Int,
        val endIndex: Int
    )

    private fun parseCalendarTime(
        rawText: String,
        lowerText: String,
        referenceTime: LocalDateTime
    ): ParsedTimeResult? {
        val clockMatch = findClock(lowerText)
        val periodMatch = findPeriod(lowerText)
        val dayMatch = findDay(lowerText, referenceTime, clockMatch, periodMatch)

        if (dayMatch == null && periodMatch == null && clockMatch == null) {
            return null
        }

        val targetDate: LocalDate
        val isFutureDay: Boolean
        val effectiveStartIndex: Int
        val effectiveEndIndex: Int

        if (dayMatch != null) {
            targetDate = dayMatch.date
            isFutureDay = dayMatch.isFutureDay
            effectiveStartIndex = minOf(
                dayMatch.startIndex,
                periodMatch?.startIndex ?: dayMatch.startIndex,
                clockMatch?.startIndex ?: dayMatch.startIndex
            )
            effectiveEndIndex = maxOf(
                dayMatch.endIndex,
                periodMatch?.endIndex ?: dayMatch.endIndex,
                clockMatch?.endIndex ?: dayMatch.endIndex
            )
        } else {
            targetDate = referenceTime.toLocalDate()
            isFutureDay = false
            effectiveStartIndex = minOf(periodMatch?.startIndex ?: Int.MAX_VALUE, clockMatch?.startIndex ?: Int.MAX_VALUE)
            effectiveEndIndex = maxOf(periodMatch?.endIndex ?: 0, clockMatch?.endIndex ?: 0)
        }

        val refTime = referenceTime.toLocalTime()
        val targetTime: LocalTime?

        if (clockMatch != null) {
            val h = clockMatch.hour
            val m = clockMatch.minute

            val resolvedHour: Int = when {
                h >= 13 -> h
                clockMatch.is12HourExplicit -> {
                    if (clockMatch.isPm == true) {
                        if (h == 12) 12 else h + 12
                    } else {
                        if (h == 12) 0 else h
                    }
                }
                periodMatch != null -> {
                    when (periodMatch.period) {
                        PeriodOfDay.MORNING -> if (h == 12) 0 else h
                        PeriodOfDay.AFTERNOON -> if (h < 12) h + 12 else h
                        PeriodOfDay.EVENING, PeriodOfDay.EOD -> if (h < 12) h + 12 else h
                        PeriodOfDay.NIGHT -> {
                            if (h in 7..11) h + 12
                            else if (h in 1..6) h + 12
                            else h
                        }
                        PeriodOfDay.NOON -> 12
                        PeriodOfDay.MIDNIGHT -> 0
                    }
                }
                else -> {
                    when {
                        h > 12 -> h // Already 24-hr format (e.g. 14:00, 18:30)
                        h in 1..6 -> h + 12
                        h == 12 -> 12
                        h in 7..11 -> {
                            if (isFutureDay) {
                                h
                            } else {
                                val candidateAm = LocalTime.of(h, m)
                                if (candidateAm.isAfter(refTime)) {
                                    h
                                } else {
                                    val candidatePm = LocalTime.of(h + 12, m)
                                    if (candidatePm.isAfter(refTime)) {
                                        h + 12
                                    } else {
                                        h
                                    }
                                }
                            }
                        }
                        else -> h
                    }
                }
            }
            targetTime = LocalTime.of(resolvedHour, m)
        } else if (periodMatch != null) {
            if (periodMatch.period == PeriodOfDay.MIDNIGHT) {
                targetTime = LocalTime.of(0, 0)
            } else {
                val baseTime = when (periodMatch.period) {
                    PeriodOfDay.MORNING -> MORNING_DEFAULT
                    PeriodOfDay.AFTERNOON -> AFTERNOON_DEFAULT
                    PeriodOfDay.EVENING, PeriodOfDay.EOD -> EVENING_DEFAULT
                    PeriodOfDay.NIGHT -> NIGHT_DEFAULT
                    PeriodOfDay.NOON -> LocalTime.of(12, 0)
                    PeriodOfDay.MIDNIGHT -> LocalTime.of(0, 0)
                }

                if (!isFutureDay && !baseTime.isAfter(refTime)) {
                    // Period today has passed
                    targetTime = null
                } else {
                    targetTime = baseTime
                }
            }
        } else if (dayMatch != null) {
            if (dayMatch.isTodayKeyword) {
                // "today" / "aaj" with no clock and no period:
                // Find next default slot strictly in future
                val nextSlot = DEFAULT_SLOTS.firstOrNull { it.isAfter(refTime) }
                targetTime = nextSlot
            } else {
                targetTime = MORNING_DEFAULT
            }
        } else {
            return null
        }

        val start = if (effectiveStartIndex in 0..rawText.length) effectiveStartIndex else 0
        val end = if (effectiveEndIndex in start..rawText.length) effectiveEndIndex else rawText.length
        val matched = rawText.substring(start, end).trim()

        if (targetTime == null) {
            // Needs time (e.g. today but all slots passed, or requested period passed today)
            return ParsedTimeResult(
                resolvedDateTime = null,
                matchedText = matched,
                needsTime = true,
                startIndex = start,
                endIndex = end
            )
        }

        var finalDateTime = if (periodMatch?.period == PeriodOfDay.MIDNIGHT) {
            LocalDateTime.of(targetDate.plusDays(1), LocalTime.of(0, 0))
        } else {
            LocalDateTime.of(targetDate, targetTime)
        }

        // If bare clock with no explicit day, and time has passed today, advance to tomorrow
        if (dayMatch == null && periodMatch?.period != PeriodOfDay.MIDNIGHT) {
            if (!finalDateTime.isAfter(referenceTime)) {
                finalDateTime = finalDateTime.plusDays(1)
            }
        }

        return ParsedTimeResult(
            resolvedDateTime = finalDateTime,
            matchedText = matched,
            needsTime = false,
            startIndex = start,
            endIndex = end
        )
    }

    private fun findDay(
        text: String,
        referenceTime: LocalDateTime,
        clock: ClockMatch?,
        period: PeriodMatch?
    ): DayMatch? {
        val refDate = referenceTime.toLocalDate()

        // 1. "next week" (with optional weekday, e.g. "next week Monday" or bare "next week")
        val nwMatcher = DAY_NEXT_WEEK.matcher(text)
        if (nwMatcher.find()) {
            val matched = nwMatcher.group().trim()
            val start = text.indexOf(matched, nwMatcher.start())
            val specificDay = nwMatcher.group(1)?.lowercase()
            val targetDow = if (specificDay != null) parseDayOfWeek(specificDay) else DayOfWeek.MONDAY
            val daysUntil = ((targetDow!!.value - refDate.dayOfWeek.value + 7) % 7).let { if (it == 0) 7 else it }
            val targetDate = refDate.plusDays(daysUntil.toLong() + (if (daysUntil <= 2 && specificDay == null) 7 else 0))
            return DayMatch(
                date = targetDate,
                isExplicitDay = true,
                isFutureDay = true,
                startIndex = start,
                endIndex = start + matched.length
            )
        }

        // 2. Specific calendar dates: "16th March", "March 16", "20th March"
        val dateMatcher = DAY_CALENDAR_DATE.matcher(text)
        if (dateMatcher.find()) {
            val matched = dateMatcher.group().trim()
            val start = text.indexOf(matched, dateMatcher.start())
            val day = (dateMatcher.group(1) ?: dateMatcher.group(4))?.toInt()
            val monthStr = (dateMatcher.group(2) ?: dateMatcher.group(3))?.lowercase()
            val month = parseMonth(monthStr ?: "")
            if (day != null && month != null) {
                var year = refDate.year
                var target = LocalDate.of(year, month, day)
                if (target.isBefore(refDate)) {
                    year++
                    target = LocalDate.of(year, month, day)
                }
                return DayMatch(
                    date = target,
                    isExplicitDay = true,
                    isFutureDay = target.isAfter(refDate),
                    startIndex = start,
                    endIndex = start + matched.length
                )
            }
        }

        // 3. "day after tomorrow" / "parso" / "parson" / "परसों"
        val datMatcher = DAY_AFTER_TOMORROW.matcher(text)
        if (datMatcher.find()) {
            val matched = datMatcher.group().trim()
            val start = text.indexOf(matched, datMatcher.start())
            return DayMatch(
                date = refDate.plusDays(2),
                isExplicitDay = true,
                isFutureDay = true,
                startIndex = start,
                endIndex = start + matched.length
            )
        }

        // 4. "tomorrow" / "tmrw" / "kal" / "kl" / "कल"
        val tomMatcher = DAY_TOMORROW.matcher(text)
        if (tomMatcher.find()) {
            val matched = tomMatcher.group().trim()
            val start = text.indexOf(matched, tomMatcher.start())
            return DayMatch(
                date = refDate.plusDays(1),
                isExplicitDay = true,
                isFutureDay = true,
                startIndex = start,
                endIndex = start + matched.length
            )
        }

        // 5. "today" / "tonight" / "aaj" / "आज"
        val todayMatcher = DAY_TODAY.matcher(text)
        if (todayMatcher.find()) {
            val matched = todayMatcher.group().trim()
            val start = text.indexOf(matched, todayMatcher.start())
            return DayMatch(
                date = refDate,
                isExplicitDay = true,
                isFutureDay = false,
                isTodayKeyword = true,
                startIndex = start,
                endIndex = start + matched.length
            )
        }

        // 6. Weekdays (English & Hindi / Hinglish)
        val wMatcher = DAY_WEEKDAY.matcher(text)
        if (wMatcher.find()) {
            val matched = wMatcher.group().trim()
            val start = text.indexOf(matched, wMatcher.start())
            val prefix = wMatcher.group(1)?.lowercase()
            val dayName = wMatcher.group(2).lowercase()
            val targetDow = parseDayOfWeek(dayName)
            if (targetDow != null) {
                val currentDow = refDate.dayOfWeek
                val isNextExplicit = prefix in listOf("next", "agle", "agla", "अगले")

                var daysAhead: Int
                if (targetDow == currentDow) {
                    if (isNextExplicit) {
                        daysAhead = 7
                    } else {
                        // Bare weekday that is today:
                        // If time is still ahead, means today; otherwise next week
                        val willBeFutureToday = if (clock != null) {
                            val candidateHour = if (clock.is12HourExplicit) {
                                if (clock.isPm == true && clock.hour < 12) clock.hour + 12 else clock.hour
                            } else {
                                if (clock.hour in 1..6) clock.hour + 12 else clock.hour
                            }
                            LocalTime.of(candidateHour, clock.minute).isAfter(referenceTime.toLocalTime())
                        } else if (period != null) {
                            val periodTime = when (period.period) {
                                PeriodOfDay.MORNING -> MORNING_DEFAULT
                                PeriodOfDay.AFTERNOON -> AFTERNOON_DEFAULT
                                PeriodOfDay.EVENING, PeriodOfDay.EOD -> EVENING_DEFAULT
                                PeriodOfDay.NIGHT -> NIGHT_DEFAULT
                                PeriodOfDay.NOON -> LocalTime.of(12, 0)
                                PeriodOfDay.MIDNIGHT -> LocalTime.of(23, 59)
                            }
                            periodTime.isAfter(referenceTime.toLocalTime())
                        } else {
                            MORNING_DEFAULT.isAfter(referenceTime.toLocalTime())
                        }

                        daysAhead = if (willBeFutureToday) 0 else 7
                    }
                } else {
                    daysAhead = targetDow.value - currentDow.value
                    if (daysAhead < 0) {
                        daysAhead += 7
                    }
                }

                val targetDate = refDate.plusDays(daysAhead.toLong())
                return DayMatch(
                    date = targetDate,
                    isExplicitDay = true,
                    isFutureDay = targetDate.isAfter(refDate),
                    startIndex = start,
                    endIndex = start + matched.length
                )
            }
        }

        return null
    }

    private fun parseMonth(str: String): Month? {
        return when {
            str.startsWith("jan") -> Month.JANUARY
            str.startsWith("feb") -> Month.FEBRUARY
            str.startsWith("mar") -> Month.MARCH
            str.startsWith("apr") -> Month.APRIL
            str.startsWith("may") -> Month.MAY
            str.startsWith("jun") -> Month.JUNE
            str.startsWith("jul") -> Month.JULY
            str.startsWith("aug") -> Month.AUGUST
            str.startsWith("sep") -> Month.SEPTEMBER
            str.startsWith("oct") -> Month.OCTOBER
            str.startsWith("nov") -> Month.NOVEMBER
            str.startsWith("dec") -> Month.DECEMBER
            else -> null
        }
    }

    private fun parseDayOfWeek(name: String): DayOfWeek? {
        return when (name) {
            "monday", "somvaar", "somwar", "सोमवार" -> DayOfWeek.MONDAY
            "tuesday", "mangalvaar", "mangalwar", "मंगलवार" -> DayOfWeek.TUESDAY
            "wednesday", "budhvaar", "budhwar", "बुधवार" -> DayOfWeek.WEDNESDAY
            "thursday", "guruvaar", "guruwar", "veervaar", "वीरवार", "गुरुवार" -> DayOfWeek.THURSDAY
            "friday", "shukravaar", "shukrawar", "शुक्रवार" -> DayOfWeek.FRIDAY
            "saturday", "shanivaar", "shaniwar", "शनिवार" -> DayOfWeek.SATURDAY
            "sunday", "ravivaar", "raviwar", "aitvaar", "aitwar", "itwaar", "itwar", "रविवार", "इतवार" -> DayOfWeek.SUNDAY
            else -> null
        }
    }

    private fun findPeriod(text: String): PeriodMatch? {
        val matcher = PERIOD_REGEX.matcher(text)
        if (matcher.find()) {
            val period = when {
                matcher.group(1) != null -> PeriodOfDay.MORNING
                matcher.group(2) != null -> PeriodOfDay.AFTERNOON
                matcher.group(3) != null -> PeriodOfDay.EVENING
                matcher.group(4) != null -> PeriodOfDay.NIGHT
                matcher.group(5) != null -> PeriodOfDay.NOON
                matcher.group(6) != null -> PeriodOfDay.EOD
                matcher.group(7) != null -> PeriodOfDay.MIDNIGHT
                else -> null
            }
            if (period != null) {
                val matched = matcher.group().trim()
                val start = text.indexOf(matched, matcher.start())
                return PeriodMatch(period, start, start + matched.length)
            }
        }
        return null
    }

    private fun findClock(text: String): ClockMatch? {
        // Pattern 0: Range / Interval (picks start time)
        val rangeMatcher = CLOCK_RANGE.matcher(text)
        if (rangeMatcher.find()) {
            val h = (rangeMatcher.group(1) ?: rangeMatcher.group(5) ?: rangeMatcher.group(9))?.toInt()
            val m = (rangeMatcher.group(2) ?: rangeMatcher.group(6) ?: rangeMatcher.group(10))?.toInt() ?: 0
            val ampm = (rangeMatcher.group(3) ?: rangeMatcher.group(7))?.lowercase()
            if (h != null && h in 1..24) {
                val matched = rangeMatcher.group().trim()
                val start = text.indexOf(matched, rangeMatcher.start())
                val isExplicit = ampm != null
                val isPm = ampm?.startsWith("p")
                return ClockMatch(
                    hour = h,
                    minute = m,
                    is12HourExplicit = isExplicit,
                    isPm = isPm,
                    startIndex = start,
                    endIndex = start + matched.length
                )
            }
        }

        // Pattern 1: Hindi/Hinglish quarter/half-hour idioms
        val idiomMatcher = CLOCK_IDIOM.matcher(text)
        if (idiomMatcher.find()) {
            val matched = idiomMatcher.group().trim()
            val start = text.indexOf(matched, idiomMatcher.start())

            val (hour, minute) = when {
                idiomMatcher.group(1) != null -> Pair(1, 30) // dedh = 1:30
                idiomMatcher.group(2) != null -> Pair(2, 30) // dhai = 2:30
                idiomMatcher.group(3) != null -> Pair(idiomMatcher.group(4).toInt(), 30) // saade X = X:30
                idiomMatcher.group(5) != null -> Pair(idiomMatcher.group(6).toInt(), 15) // sawa X = X:15
                idiomMatcher.group(7) != null -> Pair(idiomMatcher.group(8).toInt() - 1, 45) // paune X = (X-1):45
                else -> Pair(0, 0)
            }

            if (hour > 0) {
                return ClockMatch(
                    hour = hour,
                    minute = minute,
                    is12HourExplicit = false,
                    isPm = null,
                    startIndex = start,
                    endIndex = start + matched.length
                )
            }
        }

        // Pattern 2: explicit am/pm (e.g. "at 5pm", "5:30am", "3pm", "12pm", "12:30am")
        val amPmMatcher = CLOCK_AMPM.matcher(text)
        if (amPmMatcher.find()) {
            val h = amPmMatcher.group(1).toInt()
            val m = amPmMatcher.group(2)?.toInt() ?: 0
            val meridian = amPmMatcher.group(3).lowercase()
            val isPm = meridian.startsWith("p")
            if (h in 1..12 && m in 0..59) {
                val matched = amPmMatcher.group().trim()
                val start = text.indexOf(matched, amPmMatcher.start())
                return ClockMatch(
                    hour = h,
                    minute = m,
                    is12HourExplicit = true,
                    isPm = isPm,
                    startIndex = start,
                    endIndex = start + matched.length
                )
            }
        }

        // Pattern 3: Hindi/Hinglish "X baje" / "X बजे"
        val bajeMatcher = CLOCK_BAJE.matcher(text)
        if (bajeMatcher.find()) {
            val h = bajeMatcher.group(1).toInt()
            val m = bajeMatcher.group(2)?.toInt() ?: 0
            if (h in 1..24 && m in 0..59) {
                val matched = bajeMatcher.group().trim()
                val start = text.indexOf(matched, bajeMatcher.start())
                return ClockMatch(
                    hour = h,
                    minute = m,
                    is12HourExplicit = false,
                    isPm = null,
                    startIndex = start,
                    endIndex = start + matched.length
                )
            }
        }

        // Pattern 4: "at X" / "by X" / "at X:XX" / "by X:XX" / 24-hr "at 14:00"
        val atMatcher = CLOCK_AT_BY.matcher(text)
        if (atMatcher.find()) {
            val hourStr = atMatcher.group(1)
            val h = hourStr.toInt()
            val m = atMatcher.group(2)?.toInt() ?: 0
            val isExplicit24 = h > 12 || hourStr.startsWith("0")
            if (h in 0..24 && m in 0..59) {
                val matched = atMatcher.group().trim()
                val start = text.indexOf(matched, atMatcher.start())
                return ClockMatch(
                    hour = h,
                    minute = m,
                    is12HourExplicit = isExplicit24,
                    isPm = if (h > 12) true else if (hourStr.startsWith("0")) false else null,
                    startIndex = start,
                    endIndex = start + matched.length
                )
            }
        }

        return null
    }
}

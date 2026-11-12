package app.ji.data.model

object CategoryClassifier {

    private val URGENT_KEYWORDS = listOf(
        "urgent", "asap", "immediately", "emergency", "fast", "jaldi", "turant", "abhi",
        "important", "critical", "priority", "quick", "quicker"
    )

    private val PARCEL_KEYWORDS = listOf(
        "parcel", "courier", "delivery", "amazon", "flipkart", "order", "package", "post",
        "dispatch", "shipment", "track", "tracking", "zomato", "swiggy", "blinkit", "zepto"
    )

    private val PROFESSIONAL_KEYWORDS = listOf(
        "meeting", "call", "client", "office", "report", "email", "mail", "send", "submit",
        "presentation", "deck", "review", "project", "boss", "colleague", "interview",
        "resume", "cv", "sir", "bhejna", "kaam", "task", "invoice", "payment link", "pr",
        "deploy", "code", "sync", "standup"
    )

    private val PERSONAL_KEYWORDS = listOf(
        "mom", "dad", "bhai", "sister", "bro", "mother", "father", "home", "ghar",
        "doctor", "medicine", "dawa", "dinner", "lunch", "breakfast", "gym", "workout",
        "milk", "doodh", "groceries", "buy", "pay", "recharge", "bill", "movie", "party"
    )

    /**
     * Classifies a task snippet into one of: Urgent, Parcel, Professional, Personal.
     * Rule 10: Precision over recall. Default is Personal if no specific keyword matches.
     */
    fun classify(snippet: String): String {
        val lower = snippet.lowercase()

        // 1. Check Urgent first
        if (URGENT_KEYWORDS.any { lower.containsWord(it) }) {
            return Reminder.CATEGORY_URGENT
        }

        // 2. Check Parcel
        if (PARCEL_KEYWORDS.any { lower.containsWord(it) }) {
            return Reminder.CATEGORY_PARCEL
        }

        // 3. Check Professional
        if (PROFESSIONAL_KEYWORDS.any { lower.containsWord(it) }) {
            return Reminder.CATEGORY_PROFESSIONAL
        }

        // 4. Check Personal keywords or default to Personal
        return Reminder.CATEGORY_PERSONAL
    }

    private fun String.containsWord(word: String): Boolean {
        val regex = Regex("""(?i)(?:^|[\s,.\?!])${Regex.escape(word)}(?:$|[\s,.\?!])""")
        return regex.containsMatchIn(this)
    }
}


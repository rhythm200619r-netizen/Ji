package app.ji.classifier

import app.ji.data.model.CategoryClassifier
import java.util.regex.Pattern

/**
 * Heuristic rule-based fallback implementation of [CommitmentClassifier].
 * Distinguishes personal speaker commitments ("I'll call you at 5", "kal shaam bhej dunga")
 * from third-party statements, event schedules, questions, or past events.
 */
object RuleBasedClassifier : CommitmentClassifier {

    private val QUESTION_PATTERN = Pattern.compile(
        """(?i)\?|^(?:what|when|where|who|how|why|is|are|did|can|could|will\s+you|kya|kab|kahan|kaise)\b"""
    )

    private val PAST_TENSE_PATTERN = Pattern.compile(
        """(?i)\b(?:yesterday|ago|last\s+(?:night|week|month)|already|called|sent|arrived|received|completed|finished|tha|thi|pehle|ho\s+gaya)\b|(?:\b(?:gaye|mile|aaye|rahe)\s+the\b|\bthe\s*$)"""
    )

    private val THIRD_PARTY_OR_EVENT_PATTERN = Pattern.compile(
        """(?i)\b(?:the\s+train|the\s+flight|the\s+bus|the\s+movie|the\s+match|match\s+starts|store\s+closes|office\s+timing|bank\s+opens|traffic\s+is|weather\s+is|he\s+will|she\s+will|they\s+will|rohan|priya|amit|bhaiya|sir)\b"""
    )

    private val FIRST_PERSON_COMMITMENT_PATTERN = Pattern.compile(
        """(?i)\b(?:i'll|i\s+will|i'm\s+going\s+to|i\s+have\s+to|i\s+must|i\s+need\s+to|i\s+promise|will\s+(?:send|call|submit|meet|reach|finish|pay|review|update|pick|buy|share)|we\s+will|we'll|let\s+me|main\b|hum\b|karunga|karungi|karenge|bhejunga|bhejungi|dunga|dungi|jaunga|jaungi|milunga|milungi|bataunga|dikhauga|karta\s+hoon|bhejta\s+hoon|de\s+dunga|kar\s+dunga|kar\s+lunga|karna\s+hai|jaana\s+hai|urgent|emergency|asap)\b"""
    )

    override fun classify(text: String): CommitmentClassifier.ClassificationResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return CommitmentClassifier.ClassificationResult(
                isCommitment = false,
                confidence = 0.0f,
                category = CategoryClassifier.classify(trimmed),
                source = CommitmentClassifier.ClassifierSource.RULE_BASED_FALLBACK
            )
        }

        // 1. Check if it's a question
        if (QUESTION_PATTERN.matcher(trimmed).find()) {
            return CommitmentClassifier.ClassificationResult(
                isCommitment = false,
                confidence = 0.1f,
                category = CategoryClassifier.classify(trimmed),
                source = CommitmentClassifier.ClassifierSource.RULE_BASED_FALLBACK
            )
        }

        // 2. Check if it's past tense
        if (PAST_TENSE_PATTERN.matcher(trimmed).find()) {
            return CommitmentClassifier.ClassificationResult(
                isCommitment = false,
                confidence = 0.1f,
                category = CategoryClassifier.classify(trimmed),
                source = CommitmentClassifier.ClassifierSource.RULE_BASED_FALLBACK
            )
        }

        // 3. Check third-party or event schedule
        if (THIRD_PARTY_OR_EVENT_PATTERN.matcher(trimmed).find() && !FIRST_PERSON_COMMITMENT_PATTERN.matcher(trimmed).find()) {
            return CommitmentClassifier.ClassificationResult(
                isCommitment = false,
                confidence = 0.15f,
                category = CategoryClassifier.classify(trimmed),
                source = CommitmentClassifier.ClassifierSource.RULE_BASED_FALLBACK
            )
        }

        // 4. Check for first-person commitment
        val isFirstPerson = FIRST_PERSON_COMMITMENT_PATTERN.matcher(trimmed).find()
        val category = CategoryClassifier.classify(trimmed)

        return if (isFirstPerson) {
            CommitmentClassifier.ClassificationResult(
                isCommitment = true,
                confidence = 0.90f,
                category = category,
                source = CommitmentClassifier.ClassifierSource.RULE_BASED_FALLBACK
            )
        } else {
            // Default: if no first-person signal and contains time, precision over recall -> false
            CommitmentClassifier.ClassificationResult(
                isCommitment = false,
                confidence = 0.35f,
                category = category,
                source = CommitmentClassifier.ClassifierSource.RULE_BASED_FALLBACK
            )
        }
    }
}


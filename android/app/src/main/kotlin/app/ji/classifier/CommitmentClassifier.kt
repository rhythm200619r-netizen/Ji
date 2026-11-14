package app.ji.classifier

/**
 * Interface for classifying whether typed text constitutes a personal commitment
 * (e.g. "I'll call you tomorrow at 5") vs non-commitment (e.g. "The train leaves at 5"),
 * and determining its category (Professional, Personal, Parcel, Urgent).
 *
 * Implemented by [TFLiteCommitmentClassifier] with fallback to [RuleBasedClassifier].
 */
interface CommitmentClassifier {

    data class ClassificationResult(
        val isCommitment: Boolean,
        val confidence: Float,
        val category: String,
        val source: ClassifierSource
    )

    enum class ClassifierSource {
        TFLITE_ML,
        RULE_BASED_FALLBACK
    }

    /**
     * Evaluates the given input text.
     *
     * @param text The current typed sentence or snippet.
     * @return [ClassificationResult] containing isCommitment flag, confidence score, and category.
     */
    fun classify(text: String): ClassificationResult
}


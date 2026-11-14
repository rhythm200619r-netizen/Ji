package app.ji.classifier

import android.content.Context

/**
 * Provider / Factory for the [CommitmentClassifier] singleton.
 * Uses [TFLiteCommitmentClassifier] by default, gracefully falling back to [RuleBasedClassifier].
 */
object CommitmentClassifierProvider {

    @Volatile
    private var instance: CommitmentClassifier? = null

    fun getClassifier(context: Context): CommitmentClassifier {
        return instance ?: synchronized(this) {
            instance ?: try {
                TFLiteCommitmentClassifier(context.applicationContext)
            } catch (t: Throwable) {
                RuleBasedClassifier
            }.also { instance = it }
        }
    }
}


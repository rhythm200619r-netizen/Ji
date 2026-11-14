package app.ji.classifier

import android.content.Context
import app.ji.data.model.Reminder
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * TensorFlow Lite implementation of [CommitmentClassifier].
 * Evaluates on-device int8 quantized model for commitment detection and category classification.
 * Falls back to [RuleBasedClassifier] if initialization or inference encounters any error.
 */
class TFLiteCommitmentClassifier(context: Context) : CommitmentClassifier {

    private var interpreter: Interpreter? = null
    private var vocab: Map<String, Int> = emptyMap()
    private var threshold: Float = 0.5f
    private var maxSeqLen: Int = 32
    private var commitTensorIndex: Int = -1
    private var categoryTensorIndex: Int = -1

    private val categories = listOf(
        Reminder.CATEGORY_PROFESSIONAL,
        Reminder.CATEGORY_PERSONAL,
        Reminder.CATEGORY_PARCEL,
        Reminder.CATEGORY_URGENT
    )

    init {
        try {
            val modelBuffer = loadModelFile(context, "commitment_classifier.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(2)
            }
            val interp = Interpreter(modelBuffer, options)
            interpreter = interp

            // Determine output indices by tensor shape
            for (i in 0 until interp.outputTensorCount) {
                val shape = interp.getOutputTensor(i).shape()
                if (shape.isNotEmpty() && shape.last() == 1) {
                    commitTensorIndex = i
                } else if (shape.isNotEmpty() && shape.last() == 4) {
                    categoryTensorIndex = i
                }
            }

            // Load vocab
            val vocabStr = context.assets.open("vocab.json").bufferedReader().use { it.readText() }
            val vocabJson = JSONObject(vocabStr)
            val vMap = mutableMapOf<String, Int>()
            val keys = vocabJson.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                vMap[k] = vocabJson.getInt(k)
            }
            vocab = vMap

            // Load metadata
            val metaStr = context.assets.open("metadata.json").bufferedReader().use { it.readText() }
            val metaJson = JSONObject(metaStr)
            threshold = metaJson.optDouble("threshold", 0.5).toFloat()
            maxSeqLen = metaJson.optInt("max_seq_len", 32)

        } catch (t: Throwable) {
            // Graceful fallback to rule-based classifier
            interpreter = null
        }
    }

    override fun classify(text: String): CommitmentClassifier.ClassificationResult {
        val interp = interpreter
        if (interp == null || vocab.isEmpty() || commitTensorIndex == -1 || categoryTensorIndex == -1) {
            return RuleBasedClassifier.classify(text)
        }

        return try {
            val inputIds = encodeText(text)
            val inputs = arrayOf(arrayOf(inputIds)) // shape [1, maxSeqLen]

            val commitOutput = Array(1) { FloatArray(1) }
            val categoryOutput = Array(1) { FloatArray(4) }

            val outputs = mutableMapOf<Int, Any>(
                commitTensorIndex to commitOutput,
                categoryTensorIndex to categoryOutput
            )

            interp.runForMultipleInputsOutputs(inputs, outputs)

            val commitProb = commitOutput[0][0]
            val catProbs = categoryOutput[0]

            val isCommitment = commitProb >= threshold

            var maxCatIdx = 0
            var maxCatVal = catProbs[0]
            for (i in 1 until catProbs.size) {
                if (catProbs[i] > maxCatVal) {
                    maxCatVal = catProbs[i]
                    maxCatIdx = i
                }
            }
            val predictedCategory = categories.getOrElse(maxCatIdx) { Reminder.CATEGORY_PERSONAL }

            CommitmentClassifier.ClassificationResult(
                isCommitment = isCommitment,
                confidence = commitProb,
                category = predictedCategory,
                source = CommitmentClassifier.ClassifierSource.TFLITE_ML
            )
        } catch (t: Throwable) {
            // Fail safely to rule-based classifier on any exception
            RuleBasedClassifier.classify(text)
        }
    }

    private fun encodeText(text: String): IntArray {
        val cleaned = text.lowercase().replace("([.,!?()/:;])".toRegex(), " $1 ")
        val tokens = cleaned.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val ids = IntArray(maxSeqLen)
        val unkId = vocab["<UNK>"] ?: 1
        val padId = vocab["<PAD>"] ?: 0

        for (i in 0 until maxSeqLen) {
            if (i < tokens.size) {
                ids[i] = vocab[tokens[i]] ?: unkId
            } else {
                ids[i] = padId
            }
        }
        return ids
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}


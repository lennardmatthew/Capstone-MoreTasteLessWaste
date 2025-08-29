package prototype.one.mtlw.utils

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import androidx.core.graphics.scale


class TFLiteFoodClassifier(
    private val context: Context,
    private val modelAssetPath: String = "food_model.tflite",
    private val labelsAssetPath: String = "labels.txt",
    private val inputImageSize: Int = 224
) : AutoCloseable {

    private fun assetExists(path: String): Boolean {
        return try {
            context.assets.openFd(path).close()
            true
        } catch (_: Exception) {
            false
        }
    }

    private var selectedInputSize: Int = inputImageSize
    val inputSize: Int get() = selectedInputSize

    private val selectedModelPath: String by lazy {
        val candidates = listOf(
            "food_model_small.tflite" to 160,
            modelAssetPath to inputImageSize,
            "1.tflite" to 224
        )
        for ((path, size) in candidates) {
            if (assetExists(path)) {
                selectedInputSize = size
                return@lazy path
            }
        }
        // Fallback to requested path even if missing (will throw later)
        modelAssetPath
    }

    private val interpreter: Interpreter by lazy {
        val model = FileUtil.loadMappedFile(context, selectedModelPath)
        val options = Interpreter.Options().apply {
            setNumThreads(Runtime.getRuntime().availableProcessors()) // Use all available CPU cores
            setUseXNNPACK(true) // Enable XNNPACK for ARM optimization
            setUseNNAPI(true) // Enable Neural Network API if available
            setAllowFp16PrecisionForFp32(true) // Allow FP16 for speed
            setAllowFp16PrecisionForFp32(true) // Allow FP16 for speed
            setCancellable(true) // Allow cancellation for better performance
        }
        Interpreter(model, options)
    }

    private val labels: List<String> by lazy {
        // Try multiple common label filenames and formats (txt, tsv, csv)
        val labelCandidates = listOf(
            labelsAssetPath,
            "labels_food.txt",
            "labels.txt",
            "labels.tsv",
            "labels.csv"
        )
        for (candidate in labelCandidates) {
            try {
                if (!assetExists(candidate)) continue
                if (candidate.endsWith(".txt")) {
                    return@lazy FileUtil.loadLabels(context, candidate)
                }
                // TSV/CSV: read first column
                val input = context.assets.open(candidate).bufferedReader()
                val list = input.readLines().mapNotNull { line ->
                    line.split('\t', ',').firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
                }
                input.close()
                if (list.isNotEmpty()) return@lazy list
            } catch (_: Exception) {
                continue
            }
        }
        emptyList()
    }

    fun getSupportedLabels(): List<String> = labels
    
    fun getModelSize(): Long {
        return try {
            context.assets.openFd(selectedModelPath).use { fd ->
                fd.length
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    fun isLargeModel(): Boolean {
        return getModelSize() > 10 * 1024 * 1024 // > 10MB
    }
    
    fun getOptimizationInfo(): String {
        val sizeMB = getModelSize() / (1024 * 1024)
        val threadCount = Runtime.getRuntime().availableProcessors()
        return "Model: ${sizeMB}MB, Threads: $threadCount, Optimized: ${!isLargeModel()}"
    }

    fun classify(bitmap: Bitmap): Pair<String, Float>? {
        android.util.Log.d("TFLiteClassifier", "Starting classification with ${labels.size} labels")
        val resized = bitmap.scale(inputImageSize, inputImageSize)
        android.util.Log.d("TFLiteClassifier", "Bitmap resized to ${inputImageSize}x${inputImageSize}")

        val inputBuffer = convertBitmapToBuffer(resized)
        val outputArray = Array(1) { FloatArray(maxOf(labels.size, 1)) }

        try {
            android.util.Log.d("TFLiteClassifier", "Running interpreter...")
            interpreter.run(inputBuffer, outputArray)
            android.util.Log.d("TFLiteClassifier", "Interpreter run complete")
        } catch (e: Exception) {
            android.util.Log.e("TFLiteClassifier", "Classification error: ${e.message}")
            e.printStackTrace()
            return null
        }

        val scores = outputArray[0]
        if (scores.isEmpty()) return null

        // Ultra-fast early exit strategies
        var bestIndex = 0
        var bestScore = Float.NEGATIVE_INFINITY
        
        // Strategy 1: Check for very high confidence (instant exit)
        for (i in 0 until min(scores.size, labels.size)) {
            val s = scores[i]
            if (s > 0.95f) { // Ultra high confidence - return immediately
                val label = labels.getOrNull(i) ?: "Unknown"
                return Pair(label, s)
            }
            if (s > bestScore) {
                bestScore = s
                bestIndex = i
            }
        }
        
        // Strategy 2: Check for high confidence with early exit
        if (bestScore > 0.85f) {
            val label = labels.getOrNull(bestIndex) ?: "Unknown"
            return Pair(label, bestScore)
        }
        
        // Strategy 3: Return best result if above threshold
        if (bestScore > 0.6f) {
            val label = labels.getOrNull(bestIndex) ?: "Unknown"
            return Pair(label, bestScore)
        }
        
        // No confident result
        return null
    }

    private fun convertBitmapToBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        var idx = 0
        while (idx < pixels.size) {
            val c = pixels[idx]
            val r = ((c shr 16) and 0xFF) / 255f
            val g = ((c shr 8) and 0xFF) / 255f
            val b = (c and 0xFF) / 255f
            buffer.putFloat(r)
            buffer.putFloat(g)
            buffer.putFloat(b)
            idx++
        }
        buffer.rewind()
        return buffer
    }

    override fun close() {
        try {
            interpreter.close()
        } catch (_: Exception) {
        }
    }
}



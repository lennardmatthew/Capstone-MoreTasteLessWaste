package prototype.one.mtlw.utils

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class OCRResult(
    val text: String,
    val confidence: Float,
    val boundingBox: Rect?,
    val isLikelyDate: Boolean = false,
    val containsNumbers: Boolean = false,
    val containsDots: Boolean = false
)

class OCRHelper {
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun analyzeImage(bitmap: Bitmap): List<OCRResult> = withContext(Dispatchers.Default) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = textRecognizer.process(image).await()
            
            val ocrResults = mutableListOf<OCRResult>()
            
            for (block in result.textBlocks) {
                val text = block.text
                val confidence = calculateConfidence(block)
                val boundingBox = block.boundingBox
                
                val isLikelyDate = isLikelyDate(text)
                val containsNumbers = text.any { it.isDigit() }
                val containsDots = text.contains(".")
                
                ocrResults.add(
                    OCRResult(
                        text = text,
                        confidence = confidence,
                        boundingBox = boundingBox,
                        isLikelyDate = isLikelyDate,
                        containsNumbers = containsNumbers,
                        containsDots = containsDots
                    )
                )
                
                // Also analyze individual lines for better dotted text detection
                for (line in block.lines) {
                    val lineText = line.text
                    val lineConfidence = calculateConfidence(line)
                    val lineBoundingBox = line.boundingBox
                    
                    val lineIsLikelyDate = isLikelyDate(lineText)
                    val lineContainsNumbers = lineText.any { it.isDigit() }
                    val lineContainsDots = lineText.contains(".")
                    
                    ocrResults.add(
                        OCRResult(
                            text = lineText,
                            confidence = lineConfidence,
                            boundingBox = lineBoundingBox,
                            isLikelyDate = lineIsLikelyDate,
                            containsNumbers = lineContainsNumbers,
                            containsDots = lineContainsDots
                        )
                    )
                }
            }
            
            ocrResults
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun calculateConfidence(block: com.google.mlkit.vision.text.Text.TextBlock): Float {
        // Calculate confidence based on text quality indicators
        var confidence = 0.5f
        
        // Boost confidence for text with numbers
        if (block.text.any { it.isDigit() }) {
            confidence += 0.2f
        }
        
        // Boost confidence for text with dots (potential dates)
        if (block.text.contains(".")) {
            confidence += 0.15f
        }
        
        // Boost confidence for text that looks like a date
        if (isLikelyDate(block.text)) {
            confidence += 0.25f
        }
        
        // Reduce confidence for very short text (likely noise)
        if (block.text.length < 3) {
            confidence -= 0.2f
        }
        
        // Reduce confidence for very long text (likely not a date)
        if (block.text.length > 20) {
            confidence -= 0.1f
        }
        
        return confidence.coerceIn(0f, 1f)
    }

    private fun calculateConfidence(line: com.google.mlkit.vision.text.Text.Line): Float {
        var confidence = 0.5f
        
        if (line.text.any { it.isDigit() }) {
            confidence += 0.2f
        }
        
        if (line.text.contains(".")) {
            confidence += 0.15f
        }
        
        if (isLikelyDate(line.text)) {
            confidence += 0.25f
        }
        
        if (line.text.length < 3) {
            confidence -= 0.2f
        }
        
        if (line.text.length > 20) {
            confidence -= 0.1f
        }
        
        return confidence.coerceIn(0f, 1f)
    }

    private fun isLikelyDate(text: String): Boolean {
        val datePatterns = listOf(
            Regex("\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}"),
            Regex("\\d{4}[/.-]\\d{1,2}[/.-]\\d{1,2}"),
            Regex("\\d{1,2}[/.-]\\d{2}"),
            Regex("\\d{2}[/.-]\\d{1,2}"),
            Regex("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{1,2}"),
            Regex("\\d{1,2}\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)")
        )
        
        return datePatterns.any { it.containsMatchIn(text) }
    }

    fun getDebugInfo(ocrResults: List<OCRResult>): String {
        val debugInfo = StringBuilder()
        debugInfo.append("OCR Analysis Results:\n")
        debugInfo.append("Total text blocks found: ${ocrResults.size}\n\n")
        
        ocrResults.forEachIndexed { index, result ->
            debugInfo.append("Block ${index + 1}:\n")
            debugInfo.append("  Text: \"${result.text}\"\n")
            debugInfo.append("  Confidence: ${String.format("%.2f", result.confidence)}\n")
            debugInfo.append("  Contains numbers: ${result.containsNumbers}\n")
            debugInfo.append("  Contains dots: ${result.containsDots}\n")
            debugInfo.append("  Likely date: ${result.isLikelyDate}\n")
            debugInfo.append("  Bounding box: ${result.boundingBox}\n\n")
        }
        
        return debugInfo.toString()
    }

    fun getBestDateCandidates(ocrResults: List<OCRResult>): List<OCRResult> {
        return ocrResults
            .filter { it.isLikelyDate && it.confidence > 0.6f }
            .sortedByDescending { it.confidence }
    }

    fun close() {
        textRecognizer.close()
    }
}

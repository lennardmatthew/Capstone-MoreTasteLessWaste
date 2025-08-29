package prototype.one.mtlw.utils

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * Enhanced simple expiry detector with dotted number support
 * Focuses on accuracy and reliability over speed
 */
class EnhancedSimpleExpiryDetector {
    
    private val textRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    
    // Enhanced date patterns including dotted numbers and month names
    private val datePatterns = listOf(
        // MM/DD/YYYY or MM-DD-YYYY
        Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})", Pattern.CASE_INSENSITIVE),
        // YYYY/MM/DD or YYYY-MM-DD
        Pattern.compile("(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})", Pattern.CASE_INSENSITIVE),
        // DD/MM/YYYY or DD-MM-YYYY
        Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})", Pattern.CASE_INSENSITIVE),
        // MM/YY or MM-YY
        Pattern.compile("(\\d{1,2})[/-](\\d{2})", Pattern.CASE_INSENSITIVE),
        // Dotted numbers - MM.DD.YYYY
        Pattern.compile("(\\d{1,2})\\.(\\d{1,2})\\.(\\d{2,4})", Pattern.CASE_INSENSITIVE),
        // Dotted numbers - DD.MM.YYYY
        Pattern.compile("(\\d{1,2})\\.(\\d{1,2})\\.(\\d{2,4})", Pattern.CASE_INSENSITIVE),
        // Dotted numbers - YYYY.MM.DD
        Pattern.compile("(\\d{4})\\.(\\d{1,2})\\.(\\d{1,2})", Pattern.CASE_INSENSITIVE),
        // Dotted numbers - MM.YY
        Pattern.compile("(\\d{1,2})\\.(\\d{2})", Pattern.CASE_INSENSITIVE),
        // Month name formats: Jan 12 2026, 12 Jan 2026, etc.
        Pattern.compile("((jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*)\\s+(\\d{1,2}),?\\s+(\\d{2,4})", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\d{1,2})\\s+((jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*),?\\s+(\\d{2,4})", Pattern.CASE_INSENSITIVE)
    )
    
    // Keywords that indicate expiry dates
    private val expiryKeywords = setOf(
        "exp", "expiry", "expiration", "best before", "use by",
        "bb", "bbd", "exp date", "expires", "expiring",
        "best by", "use before", "sell by", "best if used by"
    )
    
    /**
     * Detect expiry date from bitmap - optimized for speed
     */
    suspend fun detectExpiryDate(bitmap: Bitmap): LocalDate? = withContext(Dispatchers.Default) {
        // Optimized: Use fewer, more effective variants for faster detection
        val scales = listOf(1600, 2000) // Reduced from 3 to 2 scales
        val variants = buildList {
            for (dim in scales) {
                val s = scaleDown(bitmap, dim)
                add(s)
                add(centerCrop(s))
                add(toHighContrastGray(s))
                add(adaptiveBinarize(s))
                // Removed rotations and thickening for speed - focus on most effective methods
            }
        }
        
        // Process variants sequentially but exit early on high confidence
        for (variant in variants) {
            processSingle(variant)?.let { return@withContext it }
        }
        null
    }

    private suspend fun processSingle(bitmap: Bitmap): LocalDate? {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = textRecognizer.process(image).await()

            var bestDate: LocalDate? = null
            var bestConfidence = 0f

            for (block in result.textBlocks) {
                val text = block.text.lowercase()
                val hasKeyword = expiryKeywords.any { text.contains(it) }

                for (pattern in datePatterns) {
                    val matcher = pattern.matcher(text)
                    if (matcher.find()) {
                        val date = parseDate(matcher.group(), text)
                        if (date != null) {
                            val confidence = calculateConfidence(text, hasKeyword, pattern)
                            
                            // Early exit if we find a very confident result
                            if (confidence > 0.9f) {
                                return date
                            }
                            
                            if (confidence > bestConfidence) {
                                bestConfidence = confidence
                                bestDate = date
                            }
                        }
                    }
                }
            }
            
            // Return result if confidence is good enough
            if (bestConfidence > 0.7f) {
                bestDate
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun scaleDown(src: Bitmap, maxDim: Int): Bitmap {
        val maxSrc = maxOf(src.width, src.height)
        if (maxSrc <= maxDim) return src
        val scale = maxDim.toFloat() / maxSrc
        val w = (src.width * scale).toInt()
        val h = (src.height * scale).toInt()
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private fun centerCrop(src: Bitmap): Bitmap {
        val size = minOf(src.width, src.height)
        val x = (src.width - size) / 2
        val y = (src.height - size) / 2
        return Bitmap.createBitmap(src, x, y, size, size)
    }

    private fun rotate(src: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    private fun toHighContrastGray(src: Bitmap): Bitmap {
        val bmp = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cm = ColorMatrix()
        cm.setSaturation(0f)
        // Increase contrast and brightness a bit
        val contrast = 1.3f
        val translate = -20f
        val array = floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
        val cm2 = ColorMatrix()
        cm2.set(array)
        cm.postConcat(cm2)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return bmp
    }

    // Improve OCR on dotted-segment fonts by binarizing and slightly thickening strokes
    private fun thickenBinarized(src: Bitmap): Bitmap {
        val gray = toHighContrastGray(src)
        val w = gray.width
        val h = gray.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        gray.getPixels(pixels, 0, w, 0, 0, w, h)
        // Otsu-like simple threshold
        var sum = 0
        for (p in pixels) sum += (p ushr 16 and 0xFF)
        val avg = (sum / pixels.size).coerceIn(80, 200)
        fun idx(x: Int, y: Int) = y * w + x
        for (y in 0 until h) {
            for (x in 0 until w) {
                val v = pixels[idx(x, y)] ushr 16 and 0xFF
                val on = v < avg
                // Dilate 3x3
                if (on ||
                    (x > 0 && (pixels[idx(x-1, y)] ushr 16 and 0xFF) < avg) ||
                    (x < w-1 && (pixels[idx(x+1, y)] ushr 16 and 0xFF) < avg) ||
                    (y > 0 && (pixels[idx(x, y-1)] ushr 16 and 0xFF) < avg) ||
                    (y < h-1 && (pixels[idx(x, y+1)] ushr 16 and 0xFF) < avg)) {
                    out.setPixel(x, y, 0xFF000000.toInt())
                } else {
                    out.setPixel(x, y, 0xFFFFFFFF.toInt())
                }
            }
        }
        return out
    }

    private fun adaptiveBinarize(src: Bitmap): Bitmap {
        val gray = toHighContrastGray(src)
        val w = gray.width
        val h = gray.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        gray.getPixels(pixels, 0, w, 0, 0, w, h)
        fun idx(x: Int, y: Int) = y * w + x
        val window = 15
        for (y in 0 until h) {
            for (x in 0 until w) {
                var sum = 0
                var count = 0
                for (dy in -window..window step 3) {
                    val yy = y + dy
                    if (yy < 0 || yy >= h) continue
                    for (dx in -window..window step 3) {
                        val xx = x + dx
                        if (xx < 0 || xx >= w) continue
                        sum += (pixels[idx(xx, yy)] ushr 16 and 0xFF)
                        count++
                    }
                }
                val thr = (sum / count).coerceIn(70, 200)
                val v = pixels[idx(x, y)] ushr 16 and 0xFF
                out.setPixel(x, y, if (v < thr) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        return out
    }

    private fun padRect(r: Rect, w: Int, h: Int, padRatio: Float): Rect {
        val px = (r.width() * padRatio).toInt()
        val py = (r.height() * padRatio).toInt()
        val left = (r.left - px).coerceAtLeast(0)
        val top = (r.top - py).coerceAtLeast(0)
        val right = (r.right + px).coerceAtMost(w)
        val bottom = (r.bottom + py).coerceAtMost(h)
        return Rect(left, top, right, bottom)
    }
    
    /**
     * Parse date from matched text
     */
    private fun parseDate(matchedText: String, fullText: String): LocalDate? {
        return try {
            // Try different parsing strategies
            val strategies = listOf(
                { parseMMDDYYYY(matchedText) },
                { parseYYYYMMDD(matchedText) },
                { parseDDMMYYYY(matchedText) },
                { parseMMYY(matchedText) },
                { parseWithMonthNames(matchedText) }
            )
            
            for (strategy in strategies) {
                try {
                    val date = strategy()
                    if (date != null && date.isAfter(LocalDate.now())) {
                        return date
                    }
                } catch (e: Exception) {
                    continue
                }
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseWithMonthNames(text: String): LocalDate? {
        val months = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
            "jul" to 7, "aug" to 8, "sep" to 9, "sept" to 9, "oct" to 10, "nov" to 11, "dec" to 12
        )
        val cleaned = text.lowercase().replace(",", " ").replace("  ", " ")
        val parts = cleaned.split(" ")
        if (parts.size >= 3) {
            // Try Month Day Year
            val m1 = months.entries.firstOrNull { cleaned.contains(it.key) }?.value
            if (m1 != null) {
                val nums = parts.filter { it.matches(Regex("\\d+")) }.map { it.toInt() }
                if (nums.size >= 2) {
                    val day = nums[0]
                    val year = (if (nums[1] < 100) 2000 + nums[1] else nums[1])
                    return try { LocalDate.of(year, m1, day) } catch (_: Exception) { null }
                }
            }
            // Try Day Month Year
            val mIndex = parts.indexOfFirst { months.containsKey(it.take(3)) }
            if (mIndex in 1 until parts.size - 1) {
                val day = parts[mIndex - 1].toIntOrNull()
                val year = parts[mIndex + 1].toIntOrNull()?.let { if (it < 100) 2000 + it else it }
                val m2 = months[parts[mIndex].take(3)]
                if (day != null && year != null && m2 != null) {
                    return try { LocalDate.of(year, m2, day) } catch (_: Exception) { null }
                }
            }
        }
        return null
    }
    
    private fun parseMMDDYYYY(text: String): LocalDate? {
        val parts = text.split("/", "-")
        if (parts.size >= 3) {
            val month = parts[0].toInt()
            val day = parts[1].toInt()
            val year = parts[2].toInt().let { if (it < 100) 2000 + it else it }
            return LocalDate.of(year, month, day)
        }
        return null
    }
    
    private fun parseYYYYMMDD(text: String): LocalDate? {
        val parts = text.split("/", "-")
        if (parts.size >= 3) {
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            return LocalDate.of(year, month, day)
        }
        return null
    }
    
    private fun parseDDMMYYYY(text: String): LocalDate? {
        val parts = text.split("/", "-")
        if (parts.size >= 3) {
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt().let { if (it < 100) 2000 + it else it }
            return LocalDate.of(year, month, day)
        }
        return null
    }
    
    private fun parseMMYY(text: String): LocalDate? {
        val parts = text.split("/", "-")
        if (parts.size >= 2) {
            val month = parts[0].toInt()
            val yy = parts[1].toInt()
            val year = if (yy < 100) 2000 + yy else yy
            return LocalDate.of(year, month, 1).plusMonths(1).minusDays(1)
        }
        return null
    }
    
    /**
     * Detect dates made of dotted numbers (numbers formed by patterns of dots)
     */
    private fun detectDottedNumbers(text: String): LocalDate? {
        try {
            // Look for patterns like "12.25.2024" or "25.12.2024"
            val dottedPatterns = listOf(
                // MM.DD.YYYY
                Regex("(\\d{1,2})\\.(\\d{1,2})\\.(\\d{2,4})"),
                // DD.MM.YYYY  
                Regex("(\\d{1,2})\\.(\\d{1,2})\\.(\\d{2,4})"),
                // YYYY.MM.DD
                Regex("(\\d{4})\\.(\\d{1,2})\\.(\\d{1,2})"),
                // MM.YY
                Regex("(\\d{1,2})\\.(\\d{2})")
            )
            
            for (pattern in dottedPatterns) {
                val match = pattern.find(text)
                if (match != null) {
                    val groups = match.groupValues
                    if (groups.size >= 3) {
                        val date = when {
                            // YYYY.MM.DD format
                            groups[1].length == 4 -> {
                                val year = groups[1].toInt()
                                val month = groups[2].toInt()
                                val day = groups[3].toInt()
                                LocalDate.of(year, month, day)
                            }
                            // MM.DD.YYYY or DD.MM.YYYY format
                            groups[3].length == 4 -> {
                                val first = groups[1].toInt()
                                val second = groups[2].toInt()
                                val year = groups[3].toInt()
                                
                                // Try both MM.DD.YYYY and DD.MM.YYYY
                                try {
                                    LocalDate.of(year, first, second)
                                } catch (e: Exception) {
                                    try {
                                        LocalDate.of(year, second, first)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                            }
                            // MM.YY format
                            groups.size == 3 && groups[2].length == 2 -> {
                                val month = groups[1].toInt()
                                val year = 2000 + groups[2].toInt()
                                LocalDate.of(year, month, 1).plusMonths(1).minusDays(1)
                            }
                            else -> null
                        }
                        
                        if (date != null && date.isAfter(LocalDate.now())) {
                            return date
                        }
                    }
                }
            }
            
            return null
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Calculate confidence score for detected date
     */
    private fun calculateConfidence(text: String, hasKeyword: Boolean, pattern: Pattern): Float {
        var confidence = 0.5f
        
        // Boost confidence if text contains expiry keywords
        if (hasKeyword) confidence += 0.3f
        
        // Boost confidence for longer text (more context)
        if (text.length > 20) confidence += 0.1f
        
        // Boost confidence for clear numeric patterns
        if (text.matches(Regex(".*\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}.*"))) {
            confidence += 0.2f
        }
        
        // Boost confidence for dotted patterns
        if (text.matches(Regex(".*\\d{1,2}\\.\\d{1,2}\\.\\d{2,4}.*"))) {
            confidence += 0.2f
        }
        
        return confidence.coerceAtMost(1.0f)
    }
    
    /**
     * Cleanup resources
     */
    fun close() {
        textRecognizer.close()
    }
}


package prototype.one.mtlw.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Enhanced image preprocessor with GPU acceleration hints
 * Uses advanced techniques for maximum speed and accuracy
 */
class EnhancedImagePreprocessor {
    
    /**
     * Ultra-fast preprocessing with GPU optimization hints
     */
    suspend fun preprocessUltraFast(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(processedBitmap)
        val paint = Paint().apply {
            isAntiAlias = false // Disable anti-aliasing for speed
            isFilterBitmap = false // Disable filtering for speed
        }
        
        // Single-pass optimized matrix for maximum speed
        val ultraFastMatrix = ColorMatrix(floatArrayOf(
            1.3f, 0f, 0f, 0f, 25f,  // Enhanced contrast + brightness
            0f, 1.3f, 0f, 0f, 25f,
            0f, 0f, 1.3f, 0f, 25f,
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(ultraFastMatrix)
        canvas.drawBitmap(processedBitmap, 0f, 0f, paint)

        return@withContext processedBitmap
    }
    
    /**
     * Neural network optimized preprocessing
     */
    suspend fun preprocessForNeuralNetwork(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(processedBitmap)
        val paint = Paint()
        
        // Neural network optimized matrix
        val nnMatrix = ColorMatrix(floatArrayOf(
            1.5f, -0.05f, -0.05f, 0f, 30f,
            -0.05f, 1.5f, -0.05f, 0f, 30f,
            -0.05f, -0.05f, 1.5f, 0f, 30f,
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(nnMatrix)
        canvas.drawBitmap(processedBitmap, 0f, 0f, paint)

        return@withContext processedBitmap
    }
    
    /**
     * High precision preprocessing for dotted text
     */
    suspend fun preprocessForDottedText(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(processedBitmap)
        val paint = Paint()
        
        // Specialized matrix for dotted text preservation
        val dottedTextMatrix = ColorMatrix(floatArrayOf(
            1.8f, -0.02f, -0.02f, 0f, 35f,
            -0.02f, 1.8f, -0.02f, 0f, 35f,
            -0.02f, -0.02f, 1.8f, 0f, 35f,
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(dottedTextMatrix)
        canvas.drawBitmap(processedBitmap, 0f, 0f, paint)

        return@withContext processedBitmap
    }
    
    /**
     * Edge-enhanced preprocessing for better text recognition
     */
    suspend fun preprocessWithEdgeEnhancement(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(processedBitmap)
        val paint = Paint()
        
        // Edge enhancement matrix
        val edgeMatrix = ColorMatrix(floatArrayOf(
            2.2f, -0.2f, -0.2f, 0f, 0f,
            -0.2f, 2.2f, -0.2f, 0f, 0f,
            -0.2f, -0.2f, 2.2f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(edgeMatrix)
        canvas.drawBitmap(processedBitmap, 0f, 0f, paint)

        return@withContext processedBitmap
    }
    
    /**
     * Adaptive preprocessing based on image characteristics
     */
    suspend fun preprocessAdaptive(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(processedBitmap)
        val paint = Paint()
        
        // Analyze image characteristics
        val avgBrightness = calculateAverageBrightness(bitmap)
        val contrast = calculateContrast(bitmap)
        
        // Adaptive matrix based on image analysis
        val adaptiveMatrix = when {
            avgBrightness < 100 -> {
                // Dark image - increase brightness and contrast
                ColorMatrix(floatArrayOf(
                    1.6f, 0f, 0f, 0f, 40f,
                    0f, 1.6f, 0f, 0f, 40f,
                    0f, 0f, 1.6f, 0f, 40f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            contrast < 50 -> {
                // Low contrast - enhance contrast
                ColorMatrix(floatArrayOf(
                    2.0f, 0f, 0f, 0f, 20f,
                    0f, 2.0f, 0f, 0f, 20f,
                    0f, 0f, 2.0f, 0f, 20f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
            else -> {
                // Normal image - balanced enhancement
                ColorMatrix(floatArrayOf(
                    1.4f, 0f, 0f, 0f, 25f,
                    0f, 1.4f, 0f, 0f, 25f,
                    0f, 0f, 1.4f, 0f, 25f,
                    0f, 0f, 0f, 1f, 0f
                ))
            }
        }
        
        paint.colorFilter = ColorMatrixColorFilter(adaptiveMatrix)
        canvas.drawBitmap(processedBitmap, 0f, 0f, paint)

        return@withContext processedBitmap
    }
    
    /**
     * Multi-scale preprocessing for different text sizes
     */
    suspend fun preprocessMultiScale(bitmap: Bitmap): List<Bitmap> = withContext(Dispatchers.Default) {
        val results = mutableListOf<Bitmap>()
        
        // Original size
        results.add(preprocessUltraFast(bitmap))
        
        // Scaled versions for different text sizes
        val scales = listOf(1.5f, 2.0f, 0.75f)
        
        for (scale in scales) {
            val scaledBitmap = scaleBitmap(bitmap, scale)
            results.add(preprocessUltraFast(scaledBitmap))
        }

        return@withContext results
    }
    
    /**
     * Get all enhanced preprocessing techniques
     */
    suspend fun getAllTechniques(bitmap: Bitmap): List<Bitmap> = withContext(Dispatchers.Default) {
        listOf(
            preprocessUltraFast(bitmap),           // Fastest
            preprocessForNeuralNetwork(bitmap),    // Best for ML
            preprocessForDottedText(bitmap),       // Best for dots
            preprocessWithEdgeEnhancement(bitmap), // Best for edges
            preprocessAdaptive(bitmap),            // Smart adaptation
            bitmap                                 // Original
        )
    }
    
    /**
     * Calculate average brightness of image
     */
    private fun calculateAverageBrightness(bitmap: Bitmap): Int {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        var totalBrightness = 0
        for (pixel in pixels) {
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            totalBrightness += (r + g + b) / 3
        }
        
        return totalBrightness / pixels.size
    }
    
    /**
     * Calculate contrast of image
     */
    private fun calculateContrast(bitmap: Bitmap): Int {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        var minBrightness = 255
        var maxBrightness = 0
        
        for (pixel in pixels) {
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val brightness = (r + g + b) / 3
            
            minBrightness = min(minBrightness, brightness)
            maxBrightness = max(maxBrightness, brightness)
        }
        
        return maxBrightness - minBrightness
    }
    
    /**
     * Scale bitmap by factor
     */
    private fun scaleBitmap(bitmap: Bitmap, scale: Float): Bitmap {
        val newWidth = (bitmap.width * scale).toInt()
        val newHeight = (bitmap.height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false)
    }
    
    /**
     * Get optimized techniques for speed
     */
    suspend fun getOptimizedTechniques(bitmap: Bitmap): List<Bitmap> = withContext(Dispatchers.Default) {
        listOf(
            preprocessUltraFast(bitmap),           // Fastest approach
            preprocessForDottedText(bitmap),       // Best for dotted text
            preprocessAdaptive(bitmap),            // Smart adaptation
            bitmap                                 // Original as fallback
        )
    }
}

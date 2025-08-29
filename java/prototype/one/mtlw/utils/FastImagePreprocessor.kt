package prototype.one.mtlw.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fast and optimized image preprocessor for OCR
 * Uses efficient algorithms for better performance
 */
class FastImagePreprocessor {
    
    /**
     * Fast preprocessing optimized for speed
     */
    suspend fun preprocessFast(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(processedBitmap)
        val paint = Paint()
        
        // Single-pass optimization: combine contrast, brightness, and sharpening
        val optimizedMatrix = ColorMatrix(floatArrayOf(
            1.4f, -0.1f, -0.1f, 0f, 20f,  // Enhanced contrast + brightness
            -0.1f, 1.4f, -0.1f, 0f, 20f,
            -0.1f, -0.1f, 1.4f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(optimizedMatrix)
        canvas.drawBitmap(processedBitmap, 0f, 0f, paint)

        return@withContext processedBitmap
    }
    
    /**
     * High contrast preprocessing for better text recognition
     */
    suspend fun preprocessHighContrast(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(processedBitmap)
        val paint = Paint()
        
        // High contrast matrix
        val contrastMatrix = ColorMatrix(floatArrayOf(
            2.0f, 0f, 0f, 0f, 30f,
            0f, 2.0f, 0f, 0f, 30f,
            0f, 0f, 2.0f, 0f, 30f,
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
        canvas.drawBitmap(processedBitmap, 0f, 0f, paint)

        return@withContext processedBitmap
    }
    
    /**
     * Grayscale preprocessing for better OCR accuracy
     */
    suspend fun preprocessGrayscale(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(processedBitmap)
        val paint = Paint()
        
        // Convert to grayscale
        val grayscaleMatrix = ColorMatrix().apply {
            setSaturation(0f)
        }
        
        paint.colorFilter = ColorMatrixColorFilter(grayscaleMatrix)
        canvas.drawBitmap(processedBitmap, 0f, 0f, paint)

        return@withContext processedBitmap
    }
    
    /**
     * Sharpen preprocessing for better edge detection
     */
    suspend fun preprocessSharpen(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(processedBitmap)
        val paint = Paint()
        
        // Sharpen matrix
        val sharpenMatrix = ColorMatrix(floatArrayOf(
            2.5f, -0.3f, -0.3f, 0f, 0f,
            -0.3f, 2.5f, -0.3f, 0f, 0f,
            -0.3f, -0.3f, 2.5f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(sharpenMatrix)
        canvas.drawBitmap(processedBitmap, 0f, 0f, paint)

        return@withContext processedBitmap
    }
    
    /**
     * Optimized preprocessing for dotted text
     */
    suspend fun preprocessForDottedText(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(processedBitmap)
        val paint = Paint()
        
        // Optimized matrix for dotted text preservation
        val dottedTextMatrix = ColorMatrix(floatArrayOf(
            1.6f, -0.05f, -0.05f, 0f, 25f,
            -0.05f, 1.6f, -0.05f, 0f, 25f,
            -0.05f, -0.05f, 1.6f, 0f, 25f,
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(dottedTextMatrix)
        canvas.drawBitmap(processedBitmap, 0f, 0f, paint)

        return@withContext processedBitmap
    }
    
    /**
     * Get optimized preprocessing techniques
     */
    suspend fun getOptimizedTechniques(bitmap: Bitmap): List<Bitmap> = withContext(Dispatchers.Default) {
        listOf(
            preprocessFast(bitmap),           // Fastest approach
            preprocessHighContrast(bitmap),   // Best for clear text
            preprocessGrayscale(bitmap),      // Best for OCR accuracy
            preprocessSharpen(bitmap),        // Best for edge detection
            preprocessForDottedText(bitmap),  // Best for dotted text
            bitmap                            // Original as fallback
        )
    }
    
    /**
     * Ultra-fast preprocessing for real-time detection
     */
    suspend fun preprocessUltraFast(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(processedBitmap)
        val paint = Paint()
        
        // Minimal processing for maximum speed
        val ultraFastMatrix = ColorMatrix(floatArrayOf(
            1.2f, 0f, 0f, 0f, 15f,
            0f, 1.2f, 0f, 0f, 15f,
            0f, 0f, 1.2f, 0f, 15f,
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(ultraFastMatrix)
        canvas.drawBitmap(processedBitmap, 0f, 0f, paint)

        return@withContext processedBitmap
    }
}

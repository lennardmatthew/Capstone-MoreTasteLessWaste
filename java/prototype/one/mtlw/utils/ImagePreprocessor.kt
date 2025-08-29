package prototype.one.mtlw.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ImagePreprocessor {
    
    /**
     * Preprocesses image specifically for dotted text recognition
     */
    suspend fun preprocessForDottedText(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(processedBitmap)
        val paint = Paint()

        // Step 1: Enhance contrast while preserving dots
        val contrast = 1.4f
        val brightness = 20f
        val contrastMatrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
        canvas.drawBitmap(processedBitmap, 0f, 0f, paint)

        // Step 2: Apply morphological operations to preserve small dots
        val width = processedBitmap.width
        val height = processedBitmap.height
        val pixels = IntArray(width * height)
        processedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Use very small block size to preserve dots
        val blockSize = 5
        val threshold = 3
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0
                var count = 0
                
                for (by in -blockSize/2..blockSize/2) {
                    for (bx in -blockSize/2..blockSize/2) {
                        val nx = x + bx
                        val ny = y + by
                        if (nx in 0 until width && ny in 0 until height) {
                            val pixel = pixels[ny * width + nx]
                            sum += Color.red(pixel)
                            count++
                        }
                    }
                }
                
                val mean = sum / count
                val pixel = pixels[y * width + x]
                val gray = Color.red(pixel)
                
                // Very conservative thresholding for dots
                pixels[y * width + x] = if (gray > mean - threshold) {
                    Color.rgb(255, 255, 255)
                } else {
                    Color.rgb(0, 0, 0)
                }
            }
        }
        
        processedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return@withContext processedBitmap
    }

    /**
     * Preprocesses image for number recognition
     */
    suspend fun preprocessForNumbers(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(processedBitmap)
        val paint = Paint()

        // High contrast preprocessing for numbers
        val contrast = 1.6f
        val brightness = 15f
        val contrastMatrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
        canvas.drawBitmap(processedBitmap, 0f, 0f, paint)

        // Sharpen the image
        val sharpenMatrix = ColorMatrix(floatArrayOf(
            2.2f, -0.3f, -0.3f, 0f, 0f,
            -0.3f, 2.2f, -0.3f, 0f, 0f,
            -0.3f, -0.3f, 2.2f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(sharpenMatrix)
        canvas.drawBitmap(processedBitmap, 0f, 0f, paint)

        return@withContext processedBitmap
    }

    /**
     * Preprocesses image for general text recognition
     */
    suspend fun preprocessForGeneralText(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(processedBitmap)
        val paint = Paint()

        // Convert to grayscale
        val grayscaleMatrix = ColorMatrix().apply {
            setSaturation(0f)
        }
        paint.colorFilter = ColorMatrixColorFilter(grayscaleMatrix)
        canvas.drawBitmap(processedBitmap, 0f, 0f, paint)

        // Apply adaptive thresholding
        val width = processedBitmap.width
        val height = processedBitmap.height
        val pixels = IntArray(width * height)
        processedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val blockSize = 15
        val threshold = 10
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0
                var count = 0
                
                for (by in -blockSize/2..blockSize/2) {
                    for (bx in -blockSize/2..blockSize/2) {
                        val nx = x + bx
                        val ny = y + by
                        if (nx in 0 until width && ny in 0 until height) {
                            val pixel = pixels[ny * width + nx]
                            sum += Color.red(pixel)
                            count++
                        }
                    }
                }
                
                val mean = sum / count
                val pixel = pixels[y * width + x]
                val gray = Color.red(pixel)
                
                pixels[y * width + x] = if (gray > mean - threshold) {
                    Color.rgb(255, 255, 255)
                } else {
                    Color.rgb(0, 0, 0)
                }
            }
        }
        
        processedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return@withContext processedBitmap
    }

    /**
     * Preprocesses image for low contrast text
     */
    suspend fun preprocessForLowContrast(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(processedBitmap)
        val paint = Paint()

        // Extreme contrast enhancement
        val contrast = 2.0f
        val brightness = 30f
        val contrastMatrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
        canvas.drawBitmap(processedBitmap, 0f, 0f, paint)

        return@withContext processedBitmap
    }

    /**
     * Specialized preprocessing for dotted numbers (numbers made of dots)
     */
    suspend fun preprocessForDottedNumbers(bitmap: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val width = processedBitmap.width
        val height = processedBitmap.height
        val pixels = IntArray(width * height)
        processedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Step 1: Convert to grayscale
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
            pixels[i] = Color.rgb(gray, gray, gray)
        }

        // Step 2: Apply dot enhancement - connect nearby dots
        val enhancedPixels = pixels.clone()
        val connectivityRadius = 2

        for (y in 0 until height) {
            for (x in 0 until width) {
                val centerPixel = pixels[y * width + x]
                val centerGray = Color.red(centerPixel)

                // If this is a dark pixel (potential dot)
                if (centerGray < 150) {
                    var minGray = centerGray
                    var hasNearbyDots = false

                    // Check for nearby dots
                    for (dy in -connectivityRadius..connectivityRadius) {
                        for (dx in -connectivityRadius..connectivityRadius) {
                            val nx = x + dx
                            val ny = y + dy
                            if (nx in 0 until width && ny in 0 until height && (dx != 0 || dy != 0)) {
                                val neighborPixel = pixels[ny * width + nx]
                                val neighborGray = Color.red(neighborPixel)
                                if (neighborGray < 150) {
                                    hasNearbyDots = true
                                    if (neighborGray < minGray) {
                                        minGray = neighborGray
                                    }
                                }
                            }
                        }
                    }

                    // If we found nearby dots, enhance this pixel
                    if (hasNearbyDots) {
                        enhancedPixels[y * width + x] = Color.rgb(minGray, minGray, minGray)
                    }
                }
            }
        }

        // Step 3: Apply adaptive thresholding with very small blocks
        val blockSize = 3
        val threshold = 5
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0
                var count = 0

                for (by in -blockSize/2..blockSize/2) {
                    for (bx in -blockSize/2..blockSize/2) {
                        val nx = x + bx
                        val ny = y + by
                        if (nx in 0 until width && ny in 0 until height) {
                            val pixel = enhancedPixels[ny * width + nx]
                            sum += Color.red(pixel)
                            count++
                        }
                    }
                }

                val mean = sum / count
                val pixel = enhancedPixels[y * width + x]
                val gray = Color.red(pixel)

                // Very conservative thresholding to preserve dots
                enhancedPixels[y * width + x] = if (gray < mean - threshold) {
                    Color.rgb(0, 0, 0) // Black for dots
                } else {
                    Color.rgb(255, 255, 255) // White for background
                }
            }
        }

        processedBitmap.setPixels(enhancedPixels, 0, width, 0, 0, width, height)
        return@withContext processedBitmap
    }

    /**
     * Applies multiple preprocessing techniques and returns the best result
     */
    suspend fun preprocessWithMultipleTechniques(bitmap: Bitmap): List<Bitmap> = withContext(Dispatchers.Default) {
        listOf(
            preprocessForDottedNumbers(bitmap), // Add specialized dotted number preprocessing
            preprocessForDottedText(bitmap),
            preprocessForNumbers(bitmap),
            preprocessForGeneralText(bitmap),
            preprocessForLowContrast(bitmap),
            bitmap // Original image as fallback
        )
    }
}

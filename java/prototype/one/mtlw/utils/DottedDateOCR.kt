package prototype.one.mtlw.utils

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Specialized OCR for dates printed using dots to form numbers
 * This handles cases where numbers are made up of small dots arranged in patterns
 */
class DottedDateOCR {
    
    /**
     * Detects dates that are printed using dots to form numbers
     */
    suspend fun detectDottedDate(bitmap: Bitmap): LocalDate? = withContext(Dispatchers.Default) {
        try {
            // Step 1: Preprocess image to enhance dots
            val processedBitmap = preprocessForDottedNumbers(bitmap)
            
            // Step 2: Find potential date regions
            val dateRegions = findDateRegions(processedBitmap)
            
            // Step 3: Analyze each region for dotted numbers
            for (region in dateRegions) {
                val croppedRegion = cropBitmap(processedBitmap, region)
                val detectedDate = analyzeDottedNumbers(croppedRegion)
                if (detectedDate != null && detectedDate.isAfter(LocalDate.now())) {
                    return@withContext detectedDate
                }
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Preprocesses image specifically for dotted number detection
     */
    private fun preprocessForDottedNumbers(bitmap: Bitmap): Bitmap {
        val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val width = processedBitmap.width
        val height = processedBitmap.height
        val pixels = IntArray(width * height)
        processedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Step 1: Convert to grayscale and enhance contrast
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
            pixels[i] = Color.rgb(gray, gray, gray)
        }
        
        // Step 2: Apply morphological operations to connect nearby dots
        val connectedPixels = pixels.clone()
        val connectivityRadius = 3
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val centerPixel = pixels[y * width + x]
                val centerGray = Color.red(centerPixel)
                
                // If this is a dark pixel (potential dot), look for nearby dots
                if (centerGray < 128) {
                    var maxGray = centerGray
                    
                    // Check surrounding area for connected dots
                    for (dy in -connectivityRadius..connectivityRadius) {
                        for (dx in -connectivityRadius..connectivityRadius) {
                            val nx = x + dx
                            val ny = y + dy
                            if (nx in 0 until width && ny in 0 until height) {
                                val neighborPixel = pixels[ny * width + nx]
                                val neighborGray = Color.red(neighborPixel)
                                if (neighborGray < maxGray) {
                                    maxGray = neighborGray
                                }
                            }
                        }
                    }
                    
                    // If we found connected dots, make this pixel darker
                    if (maxGray < centerGray) {
                        connectedPixels[y * width + x] = Color.rgb(maxGray, maxGray, maxGray)
                    }
                }
            }
        }
        
        // Step 3: Apply thresholding to create binary image
        val threshold = 100
        for (i in connectedPixels.indices) {
            val gray = Color.red(connectedPixels[i])
            connectedPixels[i] = if (gray < threshold) {
                Color.rgb(0, 0, 0) // Black for dots
            } else {
                Color.rgb(255, 255, 255) // White for background
            }
        }
        
        processedBitmap.setPixels(connectedPixels, 0, width, 0, 0, width, height)
        return processedBitmap
    }
    
    /**
     * Finds potential regions that might contain dates
     */
    private fun findDateRegions(bitmap: Bitmap): List<Rect> {
        val regions = mutableListOf<Rect>()
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Look for horizontal lines of dots that might form numbers
        val minLineLength = 20
        val minDotCount = 3
        
        for (y in 0 until height) {
            var lineStart = -1
            var dotCount = 0
            
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                val isDot = Color.red(pixel) < 128
                
                if (isDot) {
                    if (lineStart == -1) {
                        lineStart = x
                    }
                    dotCount++
                } else {
                    if (lineStart != -1 && dotCount >= minDotCount) {
                        val lineLength = x - lineStart
                        if (lineLength >= minLineLength) {
                            // Found a potential number line
                            val region = Rect(
                                lineStart - 5, // Add some padding
                                y - 10,
                                x + 5,
                                y + 10
                            )
                            regions.add(region)
                        }
                    }
                    lineStart = -1
                    dotCount = 0
                }
            }
        }
        
        return regions
    }
    
    /**
     * Analyzes a region for dotted numbers and attempts to recognize them
     */
    private fun analyzeDottedNumbers(bitmap: Bitmap): LocalDate? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Convert to binary pattern
        val pattern = Array(height) { row ->
            Array(width) { col ->
                val pixel = pixels[row * width + col]
                Color.red(pixel) < 128
            }
        }
        
        // Try to recognize individual digits
        val recognizedDigits = recognizeDigits(pattern)
        
        // Try to parse as different date formats
        return tryParseDate(recognizedDigits)
    }
    
    /**
     * Recognizes individual digits from dotted patterns
     */
    private fun recognizeDigits(pattern: Array<Array<Boolean>>): List<Int> {
        val digits = mutableListOf<Int>()
        val digitWidth = pattern[0].size / 8 // Assume roughly 8 digits max
        
        for (digitIndex in 0 until 8) {
            val startCol = digitIndex * digitWidth
            val endCol = minOf((digitIndex + 1) * digitWidth, pattern[0].size)
            
            if (startCol >= pattern[0].size) break
            
            val digitPattern = extractDigitPattern(pattern, startCol, endCol)
            val digit = matchDigitPattern(digitPattern)
            if (digit != -1) {
                digits.add(digit)
            }
        }
        
        return digits
    }
    
    /**
     * Extracts pattern for a single digit
     */
    private fun extractDigitPattern(pattern: Array<Array<Boolean>>, startCol: Int, endCol: Int): Array<Array<Boolean>> {
        val digitWidth = endCol - startCol
        val digitHeight = pattern.size
        val digitPattern = Array(digitHeight) { Array(digitWidth) { false } }
        
        for (row in 0 until digitHeight) {
            for (col in 0 until digitWidth) {
                val patternCol = startCol + col
                if (patternCol < pattern[row].size) {
                    digitPattern[row][col] = pattern[row][patternCol]
                }
            }
        }
        
        return digitPattern
    }
    
    /**
     * Matches a digit pattern against known dot patterns for numbers 0-9
     */
    private fun matchDigitPattern(pattern: Array<Array<Boolean>>): Int {
        // Define dot patterns for each digit (simplified)
        val digitPatterns = mapOf(
            0 to createDigitPattern(0),
            1 to createDigitPattern(1),
            2 to createDigitPattern(2),
            3 to createDigitPattern(3),
            4 to createDigitPattern(4),
            5 to createDigitPattern(5),
            6 to createDigitPattern(6),
            7 to createDigitPattern(7),
            8 to createDigitPattern(8),
            9 to createDigitPattern(9)
        )
        
        var bestMatch = -1
        var bestScore = 0.0
        
        for ((digit, expectedPattern) in digitPatterns) {
            val score = calculatePatternSimilarity(pattern, expectedPattern)
            if (score > bestScore && score > 0.7) { // 70% similarity threshold
                bestScore = score
                bestMatch = digit
            }
        }
        
        return bestMatch
    }
    
    /**
     * Creates expected dot patterns for each digit
     */
    private fun createDigitPattern(digit: Int): Array<Array<Boolean>> {
        // Simplified 5x3 dot patterns for each digit
        return when (digit) {
            0 -> arrayOf(
                arrayOf(true, true, true, true, true),
                arrayOf(true, false, false, false, true),
                arrayOf(true, false, false, false, true),
                arrayOf(true, false, false, false, true),
                arrayOf(true, true, true, true, true)
            )
            1 -> arrayOf(
                arrayOf(false, false, true, false, false),
                arrayOf(false, true, true, false, false),
                arrayOf(false, false, true, false, false),
                arrayOf(false, false, true, false, false),
                arrayOf(false, true, true, true, false)
            )
            2 -> arrayOf(
                arrayOf(true, true, true, true, true),
                arrayOf(false, false, false, false, true),
                arrayOf(true, true, true, true, true),
                arrayOf(true, false, false, false, false),
                arrayOf(true, true, true, true, true)
            )
            3 -> arrayOf(
                arrayOf(true, true, true, true, true),
                arrayOf(false, false, false, false, true),
                arrayOf(true, true, true, true, true),
                arrayOf(false, false, false, false, true),
                arrayOf(true, true, true, true, true)
            )
            4 -> arrayOf(
                arrayOf(true, false, false, false, true),
                arrayOf(true, false, false, false, true),
                arrayOf(true, true, true, true, true),
                arrayOf(false, false, false, false, true),
                arrayOf(false, false, false, false, true)
            )
            5 -> arrayOf(
                arrayOf(true, true, true, true, true),
                arrayOf(true, false, false, false, false),
                arrayOf(true, true, true, true, true),
                arrayOf(false, false, false, false, true),
                arrayOf(true, true, true, true, true)
            )
            6 -> arrayOf(
                arrayOf(true, true, true, true, true),
                arrayOf(true, false, false, false, false),
                arrayOf(true, true, true, true, true),
                arrayOf(true, false, false, false, true),
                arrayOf(true, true, true, true, true)
            )
            7 -> arrayOf(
                arrayOf(true, true, true, true, true),
                arrayOf(false, false, false, false, true),
                arrayOf(false, false, false, true, false),
                arrayOf(false, false, true, false, false),
                arrayOf(false, true, false, false, false)
            )
            8 -> arrayOf(
                arrayOf(true, true, true, true, true),
                arrayOf(true, false, false, false, true),
                arrayOf(true, true, true, true, true),
                arrayOf(true, false, false, false, true),
                arrayOf(true, true, true, true, true)
            )
            9 -> arrayOf(
                arrayOf(true, true, true, true, true),
                arrayOf(true, false, false, false, true),
                arrayOf(true, true, true, true, true),
                arrayOf(false, false, false, false, true),
                arrayOf(true, true, true, true, true)
            )
            else -> arrayOf()
        }
    }
    
    /**
     * Calculates similarity between two dot patterns
     */
    private fun calculatePatternSimilarity(pattern1: Array<Array<Boolean>>, pattern2: Array<Array<Boolean>>): Double {
        var matches = 0
        var total = 0
        
        val minRows = minOf(pattern1.size, pattern2.size)
        val minCols = minOf(pattern1[0].size, pattern2[0].size)
        
        for (row in 0 until minRows) {
            for (col in 0 until minCols) {
                if (pattern1[row][col] == pattern2[row][col]) {
                    matches++
                }
                total++
            }
        }
        
        return if (total > 0) matches.toDouble() / total else 0.0
    }
    
    /**
     * Tries to parse recognized digits as different date formats
     */
    private fun tryParseDate(digits: List<Int>): LocalDate? {
        if (digits.size < 6) return null
        
        // Try different date formats
        val dateFormats = listOf(
            // MM/DD/YYYY
            { d: List<Int> -> 
                if (d.size >= 8) {
                    val month = d[0] * 10 + d[1]
                    val day = d[2] * 10 + d[3]
                    val year = d[4] * 1000 + d[5] * 100 + d[6] * 10 + d[7]
                    LocalDate.of(year, month, day)
                } else null
            },
            // DD/MM/YYYY
            { d: List<Int> -> 
                if (d.size >= 8) {
                    val day = d[0] * 10 + d[1]
                    val month = d[2] * 10 + d[3]
                    val year = d[4] * 1000 + d[5] * 100 + d[6] * 10 + d[7]
                    LocalDate.of(year, month, day)
                } else null
            },
            // YYYY/MM/DD
            { d: List<Int> -> 
                if (d.size >= 8) {
                    val year = d[0] * 1000 + d[1] * 100 + d[2] * 10 + d[3]
                    val month = d[4] * 10 + d[5]
                    val day = d[6] * 10 + d[7]
                    LocalDate.of(year, month, day)
                } else null
            }
        )
        
        for (format in dateFormats) {
            try {
                val date = format(digits)
                if (date != null && date.isAfter(LocalDate.now())) {
                    return date
                }
            } catch (e: Exception) {
                continue
            }
        }
        
        return null
    }
    
    /**
     * Crops a bitmap to the specified region
     */
    private fun cropBitmap(bitmap: Bitmap, region: Rect): Bitmap {
        val x = maxOf(0, region.left)
        val y = maxOf(0, region.top)
        val width = minOf(region.width(), bitmap.width - x)
        val height = minOf(region.height(), bitmap.height - y)
        
        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }
}

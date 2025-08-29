package prototype.one.mtlw.utils

import java.time.LocalDate
import java.util.regex.Pattern

/**
 * Shared data classes and enums for OCR components
 * This file contains all shared types to avoid duplication
 */

/**
 * Enhanced date pattern with priority
 */
data class DatePattern(
    val pattern: String,
    val confidence: Float,
    val format: DateFormat,
    val priority: Int
) {
    val regex: Pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE)
}

/**
 * Cached result with timestamp
 */
data class CachedResult(
    val date: LocalDate?,
    val timestamp: Long
)

/**
 * Enhanced date result
 */
data class DateResult(
    val date: LocalDate,
    val confidence: Float
)

/**
 * Date format enum
 */
enum class DateFormat {
    MM_DD_YYYY,
    YYYY_MM_DD,
    MM_YY,
    MONTH_DD_YYYY,
    DD_MONTH_YYYY
}

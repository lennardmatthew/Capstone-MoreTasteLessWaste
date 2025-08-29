package prototype.one.mtlw.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Performance monitoring utility for OCR operations
 * Tracks processing times, success rates, and provides optimization insights
 */
class PerformanceMonitor {
    
    private val _performanceStats = MutableStateFlow(PerformanceStats())
    val performanceStats: StateFlow<PerformanceStats> = _performanceStats.asStateFlow()
    
    private var startTime: Long = 0
    private var processingStartTime: Long = 0
    
    /**
     * Start monitoring a new OCR operation
     */
    fun startOperation() {
        startTime = System.currentTimeMillis()
        processingStartTime = 0
    }
    
    /**
     * Start processing phase timing
     */
    fun startProcessing() {
        processingStartTime = System.currentTimeMillis()
    }
    
    /**
     * End processing phase and record timing
     */
    fun endProcessing(phase: ProcessingPhase) {
        if (processingStartTime > 0) {
            val duration = System.currentTimeMillis() - processingStartTime
            updatePhaseStats(phase, duration)
        }
    }
    
    /**
     * Complete operation and record results
     */
    fun completeOperation(success: Boolean, detectedDate: String? = null) {
        val totalTime = System.currentTimeMillis() - startTime
        updateOperationStats(success, totalTime, detectedDate)
        
        // Log performance data
        logPerformanceData(success, totalTime)
    }
    
    /**
     * Update phase-specific statistics
     */
    private fun updatePhaseStats(phase: ProcessingPhase, duration: Long) {
        _performanceStats.value = _performanceStats.value.copy(
            phaseStats = _performanceStats.value.phaseStats.toMutableMap().apply {
                val currentStats = get(phase) ?: PhaseStats()
                put(phase, currentStats.copy(
                    totalTime = currentStats.totalTime + duration,
                    count = currentStats.count + 1,
                    averageTime = (currentStats.totalTime + duration) / (currentStats.count + 1)
                ))
            }
        )
    }
    
    /**
     * Update operation-level statistics
     */
    private fun updateOperationStats(success: Boolean, totalTime: Long, detectedDate: String?) {
        val currentStats = _performanceStats.value
        _performanceStats.value = currentStats.copy(
            totalOperations = currentStats.totalOperations + 1,
            successfulOperations = currentStats.successfulOperations + (if (success) 1 else 0),
            totalProcessingTime = currentStats.totalProcessingTime + totalTime,
            averageProcessingTime = (currentStats.totalProcessingTime + totalTime) / (currentStats.totalOperations + 1),
            lastOperationTime = LocalDateTime.now(),
            lastDetectedDate = detectedDate,
            successRate = (currentStats.successfulOperations + (if (success) 1 else 0)).toFloat() / (currentStats.totalOperations + 1)
        )
    }
    
    /**
     * Log performance data for debugging
     */
    private fun logPerformanceData(success: Boolean, totalTime: Long) {
        val stats = _performanceStats.value
        Log.d("PerformanceMonitor", """
            OCR Performance:
            - Success: $success
            - Total Time: ${totalTime}ms
            - Average Time: ${stats.averageProcessingTime}ms
            - Success Rate: ${(stats.successRate * 100).toInt()}%
            - Total Operations: ${stats.totalOperations}
        """.trimIndent())
    }
    
    /**
     * Get performance insights
     */
    fun getPerformanceInsights(): PerformanceInsights {
        val stats = _performanceStats.value
        
        return PerformanceInsights(
            averageProcessingTime = stats.averageProcessingTime,
            successRate = stats.successRate,
            totalOperations = stats.totalOperations,
            slowestPhase = stats.phaseStats.maxByOrNull { it.value.averageTime }?.key,
            fastestPhase = stats.phaseStats.minByOrNull { it.value.averageTime }?.key,
            recommendations = generateRecommendations(stats)
        )
    }
    
    /**
     * Generate optimization recommendations
     */
    private fun generateRecommendations(stats: PerformanceStats): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (stats.averageProcessingTime > 2000) {
            recommendations.add("Consider using ultra-fast preprocessing for better performance")
        }
        
        if (stats.successRate < 0.7f) {
            recommendations.add("Low success rate detected. Try adjusting preprocessing parameters")
        }
        
        val slowestPhase = stats.phaseStats.maxByOrNull { it.value.averageTime }
        if (slowestPhase != null && slowestPhase.value.averageTime > 1000) {
            recommendations.add("${slowestPhase.key.name} phase is slow. Consider optimization")
        }
        
        if (stats.totalOperations > 10 && stats.successRate > 0.8f) {
            recommendations.add("Good performance! Consider enabling caching for even faster results")
        }
        
        return recommendations
    }
    
    /**
     * Reset performance statistics
     */
    fun resetStats() {
        _performanceStats.value = PerformanceStats()
    }
    
    /**
     * Export performance data
     */
    fun exportPerformanceData(): String {
        val stats = _performanceStats.value
        val insights = getPerformanceInsights()
        
        return """
            Performance Report - ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}
            
            Overall Statistics:
            - Total Operations: ${stats.totalOperations}
            - Successful Operations: ${stats.successfulOperations}
            - Success Rate: ${(stats.successRate * 100).toInt()}%
            - Average Processing Time: ${stats.averageProcessingTime}ms
            - Total Processing Time: ${stats.totalProcessingTime}ms
            
            Phase Statistics:
            ${stats.phaseStats.entries.joinToString("\n") { "- ${it.key.name}: ${it.value.averageTime}ms avg (${it.value.count} operations)" }}
            
            Performance Insights:
            - Slowest Phase: ${insights.slowestPhase?.name ?: "N/A"}
            - Fastest Phase: ${insights.fastestPhase?.name ?: "N/A"}
            
            Recommendations:
            ${insights.recommendations.joinToString("\n") { "- $it" }}
        """.trimIndent()
    }
}

/**
 * Performance statistics data class
 */
data class PerformanceStats(
    val totalOperations: Int = 0,
    val successfulOperations: Int = 0,
    val totalProcessingTime: Long = 0,
    val averageProcessingTime: Long = 0,
    val successRate: Float = 0f,
    val lastOperationTime: LocalDateTime? = null,
    val lastDetectedDate: String? = null,
    val phaseStats: Map<ProcessingPhase, PhaseStats> = emptyMap()
)

/**
 * Phase-specific statistics
 */
data class PhaseStats(
    val totalTime: Long = 0,
    val count: Int = 0,
    val averageTime: Long = 0
)

/**
 * Performance insights
 */
data class PerformanceInsights(
    val averageProcessingTime: Long,
    val successRate: Float,
    val totalOperations: Int,
    val slowestPhase: ProcessingPhase?,
    val fastestPhase: ProcessingPhase?,
    val recommendations: List<String>
)

/**
 * Processing phases for detailed monitoring
 */
enum class ProcessingPhase {
    IMAGE_PREPROCESSING,
    OCR_TEXT_RECOGNITION,
    DATE_PARSING,
    PATTERN_MATCHING,
    REGION_DETECTION
}

package com.fitness.pushuptracker.utils

/**
 * Performance Monitor
 * ===================
 * Monitors and logs performance metrics:
 * - Frame processing time (<30ms target)
 * - Memory usage (<128MB target)
 * - FPS calculation
 * - Battery impact estimation
 * - Performance warnings
 * 
 * Phase 7: Performance & Error Handling
 */
class PerformanceMonitor {
    
    // Frame timing
    private val frameTimings = mutableListOf<Long>()
    private var lastFrameTime = 0L
    private var frameCount = 0
    
    // FPS calculation
    private var fpsStartTime = 0L
    private var fpsFrameCount = 0
    private var currentFPS = 0f
    
    // Memory tracking
    private val runtime = Runtime.getRuntime()
    
    // Performance thresholds
    private val TARGET_FRAME_TIME_MS = 30L
    private val TARGET_MEMORY_MB = 128L
    private val TARGET_FPS = 30f
    
    data class PerformanceMetrics(
        val averageFrameTimeMs: Float,
        val currentFPS: Float,
        val memoryUsageMB: Long,
        val memoryPercentage: Float,
        val warnings: List<String>
    )
    
    /**
     * Start timing a frame
     */
    fun startFrame(): Long {
        return System.currentTimeMillis()
    }
    
    /**
     * End timing a frame and record metrics
     */
    fun endFrame(startTime: Long) {
        val frameTime = System.currentTimeMillis() - startTime
        frameTimings.add(frameTime)
        
        // Keep only last 30 frames
        if (frameTimings.size > 30) {
            frameTimings.removeAt(0)
        }
        
        frameCount++
        
        // Calculate FPS every second
        if (fpsStartTime == 0L) {
            fpsStartTime = System.currentTimeMillis()
        }
        
        fpsFrameCount++
        val fpsElapsed = System.currentTimeMillis() - fpsStartTime
        
        if (fpsElapsed >= 1000) {
            currentFPS = (fpsFrameCount * 1000f) / fpsElapsed
            fpsStartTime = System.currentTimeMillis()
            fpsFrameCount = 0
        }
        
        // Log performance every 100 frames
        if (frameCount % 100 == 0) {
            val metrics = getMetrics()
            logMetrics(metrics)
        }
    }
    
    /**
     * Get current performance metrics
     */
    fun getMetrics(): PerformanceMetrics {
        val warnings = mutableListOf<String>()
        
        // Calculate average frame time
        val avgFrameTime = if (frameTimings.isNotEmpty()) {
            frameTimings.average().toFloat()
        } else {
            0f
        }
        
        // Check frame time
        if (avgFrameTime > TARGET_FRAME_TIME_MS) {
            warnings.add("Frame processing slow: ${String.format("%.1f", avgFrameTime)}ms (target: ${TARGET_FRAME_TIME_MS}ms)")
        }
        
        // Calculate memory usage
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        val memoryPercentage = (usedMemory.toFloat() / maxMemory.toFloat()) * 100f
        
        // Check memory usage
        if (usedMemory > TARGET_MEMORY_MB) {
            warnings.add("High memory usage: ${usedMemory}MB (target: ${TARGET_MEMORY_MB}MB)")
        }
        
        if (memoryPercentage > 80f) {
            warnings.add("Memory usage critical: ${String.format("%.1f", memoryPercentage)}%")
        }
        
        // Check FPS
        if (currentFPS > 0 && currentFPS < 20f) {
            warnings.add("Low FPS: ${String.format("%.1f", currentFPS)} (target: ${TARGET_FPS})")
        }
        
        return PerformanceMetrics(
            averageFrameTimeMs = avgFrameTime,
            currentFPS = currentFPS,
            memoryUsageMB = usedMemory,
            memoryPercentage = memoryPercentage,
            warnings = warnings
        )
    }
    
    /**
     * Log performance metrics
     */
    private fun logMetrics(metrics: PerformanceMetrics) {
        android.util.Log.d("Performance", "📊 === Performance Metrics (Frame #$frameCount) ===")
        android.util.Log.d("Performance", "   Frame time: ${String.format("%.1f", metrics.averageFrameTimeMs)}ms (target: <${TARGET_FRAME_TIME_MS}ms)")
        android.util.Log.d("Performance", "   FPS: ${String.format("%.1f", metrics.currentFPS)} (target: ${TARGET_FPS})")
        android.util.Log.d("Performance", "   Memory: ${metrics.memoryUsageMB}MB / ${runtime.maxMemory() / 1024 / 1024}MB (${String.format("%.1f", metrics.memoryPercentage)}%)")
        
        if (metrics.warnings.isNotEmpty()) {
            android.util.Log.w("Performance", "⚠️ Warnings:")
            metrics.warnings.forEach { warning ->
                android.util.Log.w("Performance", "   - $warning")
            }
        } else {
            android.util.Log.d("Performance", "✅ All metrics within target ranges")
        }
    }
    
    /**
     * Estimate battery impact
     * Returns estimated battery drain percentage per hour
     */
    fun estimateBatteryImpact(): Float {
        // Rough estimation based on FPS and processing time
        // Higher FPS and longer processing = more battery drain
        val avgFrameTime = if (frameTimings.isNotEmpty()) {
            frameTimings.average().toFloat()
        } else {
            0f
        }
        
        // Baseline: 30 FPS at 20ms per frame = ~3% per hour
        // Scale based on actual metrics
        val baselineDrain = 3f
        val fpsMultiplier = currentFPS / 30f
        val timeMultiplier = avgFrameTime / 20f
        
        return baselineDrain * fpsMultiplier * timeMultiplier
    }
    
    /**
     * Check if performance is acceptable
     */
    fun isPerformanceAcceptable(): Boolean {
        val metrics = getMetrics()
        return metrics.warnings.isEmpty()
    }
    
    /**
     * Force garbage collection if memory is high
     */
    fun checkAndOptimizeMemory() {
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMemory = runtime.maxMemory() / 1024 / 1024
        val memoryPercentage = (usedMemory.toFloat() / maxMemory.toFloat()) * 100f
        
        if (memoryPercentage > 85f) {
            android.util.Log.w("Performance", "🧹 High memory usage (${String.format("%.1f", memoryPercentage)}%), suggesting GC...")
            System.gc()
        }
    }
    
    /**
     * Reset all metrics
     */
    fun reset() {
        frameTimings.clear()
        lastFrameTime = 0L
        frameCount = 0
        fpsStartTime = 0L
        fpsFrameCount = 0
        currentFPS = 0f
    }
}

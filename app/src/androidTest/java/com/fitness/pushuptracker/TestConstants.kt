package com.fitness.pushuptracker

/**
 * Test Constants for Pushup Counter Testing
 * Defines angle sequences, thresholds, and performance benchmarks
 */
object TestConstants {
    
    // ============================================
    // ANGLE SEQUENCES
    // ============================================
    
    /**
     * Perfect pushup angle sequence (30 frames total, ~1 second at 30fps)
     * Simulates smooth descent and ascent with proper holds
     */
    val PERFECT_PUSHUP_ANGLES = listOf(
        // Starting position (top)
        170f, 168f, 165f,
        // Descent
        160f, 150f, 140f, 130f, 120f, 110f, 100f,
        // Bottom hold (3 frames)
        90f, 85f, 80f,
        // Bottom hold continued
        75f, 80f, 85f,
        // Ascent
        90f, 100f, 110f, 120f, 130f, 140f, 150f, 160f,
        // Top position
        165f, 168f, 170f, 172f, 174f, 176f
    )
    
    /**
     * Half pushup sequence (doesn't reach proper depth)
     */
    val HALF_PUSHUP_ANGLES = listOf(
        170f, 165f, 160f, 150f, 140f, 130f, 120f, 110f, 100f,
        // Never goes below 100°
        100f, 110f, 120f, 130f, 140f, 150f, 160f, 165f, 170f
    )
    
    /**
     * Bouncing pushup sequence (too fast at bottom)
     */
    val BOUNCING_PUSHUP_ANGLES = listOf(
        170f, 160f, 140f, 120f, 100f, 85f,
        // Only 1 frame at bottom (bounce)
        75f,
        // Immediate ascent
        85f, 100f, 120f, 140f, 160f, 170f
    )
    
    /**
     * Too slow pushup sequence (exceeds max time)
     */
    val SLOW_PUSHUP_ANGLES = List(100) { index ->
        when {
            index < 30 -> 170f - (index * 3f) // Very slow descent
            index < 50 -> 80f // Long hold at bottom
            index < 80 -> 80f + ((index - 50) * 3f) // Very slow ascent
            else -> 170f
        }
    }
    
    /**
     * Too fast pushup sequence (below min time)
     */
    val FAST_PUSHUP_ANGLES = listOf(
        170f, 140f, 100f, 75f, 100f, 140f, 170f
    )
    
    // ============================================
    // FORM VALIDATION THRESHOLDS
    // ============================================
    
    const val ELBOW_UP_MIN = 160f
    const val ELBOW_DOWN_MAX = 90f
    const val BODY_ALIGNMENT_MIN = 160f
    const val HIP_SAG_MIN = 160f
    const val HIP_PIKE_MAX = 175f
    
    // ============================================
    // TIMING THRESHOLDS
    // ============================================
    
    const val MIN_REP_TIME = 0.5f  // seconds
    const val MAX_REP_TIME = 3.0f  // seconds
    const val STABILITY_FRAMES = 3
    
    // ============================================
    // PERFORMANCE BENCHMARKS
    // ============================================
    
    /**
     * Maximum frame processing time (30 FPS target)
     */
    const val MAX_FRAME_TIME_MS = 33L
    
    /**
     * Maximum memory usage during active session
     */
    const val MAX_MEMORY_MB = 150L
    
    /**
     * Maximum battery drain per minute of use
     */
    const val MAX_BATTERY_DRAIN_PERCENT_PER_MINUTE = 0.5
    
    /**
     * Minimum confidence for landmark detection
     */
    const val MIN_LANDMARK_CONFIDENCE = 0.7f
    
    /**
     * Maximum landmark jump between frames (stability check)
     */
    const val MAX_LANDMARK_JUMP = 0.15f
    
    // ============================================
    // TYPE DETECTION THRESHOLDS
    // ============================================
    
    const val WIDE_MULTIPLIER = 1.4f
    const val DIAMOND_MULTIPLIER = 0.6f
    const val KNEE_ANGLE_THRESHOLD = 120f
    const val HEIGHT_DIFF_THRESHOLD = 0.15f
    
    // ============================================
    // TEST SCENARIOS
    // ============================================
    
    /**
     * Number of perfect pushups to test counting accuracy
     */
    const val ACCURACY_TEST_REPS = 10
    
    /**
     * Number of frames to test for performance benchmarking
     */
    const val PERFORMANCE_TEST_FRAMES = 1000
    
    /**
     * Duration for continuous session test (seconds)
     */
    const val CONTINUOUS_SESSION_DURATION = 300 // 5 minutes
}

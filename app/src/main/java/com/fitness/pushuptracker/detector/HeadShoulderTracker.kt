package com.fitness.pushuptracker.detector

import kotlin.math.abs

/**
 * Head and Shoulder Movement Tracker
 * ===================================
 * Tracks vertical movement of head, shoulders, and hips to detect:
 * - Head-only movement (cheating)
 * - Proper full-body pushup motion
 * - Movement synchronization
 * 
 * Phase 2: Movement Tracking
 */
class HeadShoulderTracker {
    
    // History buffers (last 10 frames)
    private val headHistory = mutableListOf<Float>()
    private val shoulderHistory = mutableListOf<Float>()
    private val hipHistory = mutableListOf<Float>()
    
    private val maxHistorySize = 10
    
    /**
     * Track movement for current frame
     * @return true if movement is valid (not head-only)
     */
    fun trackMovement(landmarks: Map<String, FloatArray>): Boolean {
        val nose = landmarks["nose"]
        val leftShoulder = landmarks["left_shoulder"]
        val rightShoulder = landmarks["right_shoulder"]
        val leftHip = landmarks["left_hip"]
        val rightHip = landmarks["right_hip"]
        
        if (nose != null && leftShoulder != null && rightShoulder != null && 
            leftHip != null && rightHip != null) {
            
            // Track vertical positions (y-coordinate)
            headHistory.add(nose[1])
            shoulderHistory.add((leftShoulder[1] + rightShoulder[1]) / 2)
            hipHistory.add((leftHip[1] + rightHip[1]) / 2)
            
            // Keep only last 10 frames
            if (headHistory.size > maxHistorySize) headHistory.removeAt(0)
            if (shoulderHistory.size > maxHistorySize) shoulderHistory.removeAt(0)
            if (hipHistory.size > maxHistorySize) hipHistory.removeAt(0)
            
            return isValidPushupMovement()
        }
        
        return true // Default to valid if we can't check
    }
    
    /**
     * Check if movement pattern matches a valid pushup
     * Head and shoulders should move together (ratio 0.7-1.3)
     */
    private fun isValidPushupMovement(): Boolean {
        if (headHistory.size < 5) return true // Not enough data yet
        
        // Calculate movement ranges
        val headRange = headHistory.maxOrNull()!! - headHistory.minOrNull()!!
        val shoulderRange = shoulderHistory.maxOrNull()!! - shoulderHistory.minOrNull()!!
        
        // If very little movement, it's valid (person is stable)
        if (shoulderRange < 10f) return true
        
        // Check: Head and shoulders move similar amounts
        val ratio = headRange / shoulderRange
        
        // Head should move 70-130% of shoulder movement
        return ratio in 0.7f..1.3f
    }
    
    /**
     * Get feedback message if movement is invalid
     */
    fun getFeedback(): String {
        if (headHistory.size < 5) return ""
        
        val headRange = headHistory.maxOrNull()!! - headHistory.minOrNull()!!
        val shoulderRange = shoulderHistory.maxOrNull()!! - shoulderHistory.minOrNull()!!
        
        if (shoulderRange < 10f) return ""
        
        val ratio = headRange / shoulderRange
        
        return when {
            ratio > 1.3f -> "Only head moving! Use full body"
            ratio < 0.7f -> "Head not moving with body"
            else -> ""
        }
    }
    
    /**
     * Check if hips are moving with shoulders (not hip thrusting)
     */
    fun checkHipSynchronization(): Boolean {
        if (hipHistory.size < 5 || shoulderHistory.size < 5) return true
        
        val hipRange = hipHistory.maxOrNull()!! - hipHistory.minOrNull()!!
        val shoulderRange = shoulderHistory.maxOrNull()!! - shoulderHistory.minOrNull()!!
        
        if (shoulderRange < 10f) return true
        
        // Hips should move 50-100% of shoulder movement
        val ratio = hipRange / shoulderRange
        return ratio in 0.5f..1.0f
    }
    
    /**
     * Reset tracking history
     */
    fun reset() {
        headHistory.clear()
        shoulderHistory.clear()
        hipHistory.clear()
    }
}

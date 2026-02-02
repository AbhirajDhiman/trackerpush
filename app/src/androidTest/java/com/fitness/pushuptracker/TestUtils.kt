package com.fitness.pushuptracker

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

/**
 * Test Utilities for Pushup Counter Testing
 * Provides helper functions to create mock landmarks and simulate pushup sequences
 */
object TestUtils {
    
    /**
     * Create landmarks for a standard pushup with given elbow angle
     */
    fun createLandmarksWithElbowAngle(elbowAngle: Float): Map<String, FloatArray> {
        // Calculate wrist position based on elbow angle
        // For simplicity, we'll use basic trigonometry
        val shoulderY = 0.3f
        val elbowY = 0.5f
        val wristY = elbowY + 0.2f * (1 - (elbowAngle - 90f) / 90f)
        
        return mapOf(
            "nose" to floatArrayOf(0.5f, 0.1f, 0.95f),
            "left_shoulder" to floatArrayOf(0.3f, shoulderY, 0.95f),
            "right_shoulder" to floatArrayOf(0.7f, shoulderY, 0.95f),
            "left_elbow" to floatArrayOf(0.3f, elbowY, 0.95f),
            "right_elbow" to floatArrayOf(0.7f, elbowY, 0.95f),
            "left_wrist" to floatArrayOf(0.3f, wristY, 0.95f),
            "right_wrist" to floatArrayOf(0.7f, wristY, 0.95f),
            "left_hip" to floatArrayOf(0.3f, 0.6f, 0.95f),
            "right_hip" to floatArrayOf(0.7f, 0.6f, 0.95f),
            "left_knee" to floatArrayOf(0.3f, 0.8f, 0.90f),
            "right_knee" to floatArrayOf(0.7f, 0.8f, 0.90f),
            "left_ankle" to floatArrayOf(0.3f, 0.95f, 0.85f),
            "right_ankle" to floatArrayOf(0.7f, 0.95f, 0.85f)
        )
    }
    
    /**
     * Create landmarks for a standard pushup
     */
    fun createStandardPushupLandmarks(): Map<String, FloatArray> {
        return mapOf(
            "nose" to floatArrayOf(0.5f, 0.1f, 0.95f),
            "left_shoulder" to floatArrayOf(0.3f, 0.2f, 0.95f),
            "right_shoulder" to floatArrayOf(0.7f, 0.2f, 0.95f),
            "left_elbow" to floatArrayOf(0.3f, 0.4f, 0.95f),
            "right_elbow" to floatArrayOf(0.7f, 0.4f, 0.95f),
            "left_wrist" to floatArrayOf(0.3f, 0.6f, 0.95f),
            "right_wrist" to floatArrayOf(0.7f, 0.6f, 0.95f),
            "left_hip" to floatArrayOf(0.3f, 0.5f, 0.95f),
            "right_hip" to floatArrayOf(0.7f, 0.5f, 0.95f),
            "left_knee" to floatArrayOf(0.3f, 0.7f, 0.90f),
            "right_knee" to floatArrayOf(0.7f, 0.7f, 0.90f),
            "left_ankle" to floatArrayOf(0.3f, 0.9f, 0.85f),
            "right_ankle" to floatArrayOf(0.7f, 0.9f, 0.85f)
        )
    }
    
    /**
     * Create landmarks for a knee pushup (knees on ground)
     */
    fun createKneePushupLandmarks(): Map<String, FloatArray> {
        return createStandardPushupLandmarks().toMutableMap().apply {
            // Knees are on ground (higher Y value, lower in frame)
            put("left_knee", floatArrayOf(0.3f, 0.8f, 0.95f))
            put("right_knee", floatArrayOf(0.7f, 0.8f, 0.95f))
            // Ankles are less visible
            put("left_ankle", floatArrayOf(0.3f, 0.9f, 0.5f))
            put("right_ankle", floatArrayOf(0.7f, 0.9f, 0.5f))
        }
    }
    
    /**
     * Create landmarks for a wide pushup (hands wider than shoulders)
     */
    fun createWidePushupLandmarks(): Map<String, FloatArray> {
        return createStandardPushupLandmarks().toMutableMap().apply {
            // Wrists are wider apart
            put("left_wrist", floatArrayOf(0.15f, 0.6f, 0.95f))
            put("right_wrist", floatArrayOf(0.85f, 0.6f, 0.95f))
        }
    }
    
    /**
     * Create landmarks for a diamond pushup (hands close together)
     */
    fun createDiamondPushupLandmarks(): Map<String, FloatArray> {
        return createStandardPushupLandmarks().toMutableMap().apply {
            // Wrists are close together
            put("left_wrist", floatArrayOf(0.45f, 0.6f, 0.95f))
            put("right_wrist", floatArrayOf(0.55f, 0.6f, 0.95f))
        }
    }
    
    /**
     * Create landmarks with head-only movement (cheating)
     */
    fun createHeadOnlyMovementLandmarks(): Map<String, FloatArray> {
        return createStandardPushupLandmarks().toMutableMap().apply {
            // Head moves significantly, body doesn't
            put("nose", floatArrayOf(0.5f, 0.3f, 0.95f)) // Moved down
        }
    }
    
    /**
     * Create landmarks with hip sag (poor form)
     */
    fun createHipSagLandmarks(): Map<String, FloatArray> {
        return createStandardPushupLandmarks().toMutableMap().apply {
            // Hips sag down
            put("left_hip", floatArrayOf(0.3f, 0.65f, 0.95f))
            put("right_hip", floatArrayOf(0.7f, 0.65f, 0.95f))
        }
    }
    
    /**
     * Create landmarks with low confidence (poor lighting)
     */
    fun createLowConfidenceLandmarks(): Map<String, FloatArray> {
        return createStandardPushupLandmarks().mapValues { (_, value) ->
            floatArrayOf(value[0], value[1], 0.4f) // Low confidence
        }
    }
    
    /**
     * Create landmarks for multiple people (2 skeletons)
     */
    fun createMultiplePersonLandmarks(): Map<String, FloatArray> {
        // For simplicity, just return standard landmarks
        // In real implementation, this would have multiple sets
        return createStandardPushupLandmarks()
    }
    
    /**
     * Create landmarks with phone tilted at an angle
     */
    fun createTiltedLandmarks(angleDegrees: Float): Map<String, FloatArray> {
        // Apply rotation transformation to all landmarks
        val radians = Math.toRadians(angleDegrees.toDouble())
        val cos = Math.cos(radians).toFloat()
        val sin = Math.sin(radians).toFloat()
        
        return createStandardPushupLandmarks().mapValues { (_, value) ->
            val x = value[0]
            val y = value[1]
            val newX = x * cos - y * sin
            val newY = x * sin + y * cos
            floatArrayOf(newX, newY, value[2])
        }
    }
    
    /**
     * Simulate a complete pushup sequence with given angles
     */
    fun simulateFrameSequence(
        angleSequence: List<Float>,
        framesPerAngle: Int = 3
    ): List<Map<String, FloatArray>> {
        val frames = mutableListOf<Map<String, FloatArray>>()
        angleSequence.forEach { angle ->
            repeat(framesPerAngle) {
                frames.add(createLandmarksWithElbowAngle(angle))
            }
        }
        return frames
    }
}

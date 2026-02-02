package com.fitness.pushuptracker.detector

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Anti-Cheat Detector
 * ====================
 * Detects and rejects invalid pushup attempts:
 * 1. Head bobbing (head moves >20px while shoulders <5px)
 * 2. Hip thrusting (hips move opposite to shoulders)
 * 3. Partial reps (never reaches proper angles)
 * 4. Bouncing (< 3 frames at bottom)
 * 5. Arm dominance (one arm doing 70% of work)
 * 6. Camera tilting (shoulder angle changes >15°)
 * 7. Velocity checks (15-40 pixels/second)
 * 
 * Phase 5: Anti-Cheat Detection
 */
class AntiCheatDetector {
    
    // Movement history for velocity calculation
    private val shoulderYHistory = mutableListOf<Float>()
    private val headYHistory = mutableListOf<Float>()
    private val hipYHistory = mutableListOf<Float>()
    private val timestampHistory = mutableListOf<Long>()
    
    private val maxHistorySize = 10
    
    // Bottom position tracking
    private var framesAtBottom = 0
    private var lastElbowAngle = 180f
    
    data class CheatDetectionResult(
        val isValid: Boolean,
        val cheatType: String,
        val feedback: String
    )
    
    /**
     * Check for head bobbing
     * Head moves >20px while shoulders <5px
     */
    fun checkHeadBobbing(landmarks: Map<String, FloatArray>): CheatDetectionResult {
        val nose = landmarks["nose"] ?: return CheatDetectionResult(true, "", "")
        val leftShoulder = landmarks["left_shoulder"] ?: return CheatDetectionResult(true, "", "")
        val rightShoulder = landmarks["right_shoulder"] ?: return CheatDetectionResult(true, "", "")
        
        // Track positions
        val headY = nose[1]
        val shoulderY = (leftShoulder[1] + rightShoulder[1]) / 2
        
        headYHistory.add(headY)
        shoulderYHistory.add(shoulderY)
        
        if (headYHistory.size > maxHistorySize) headYHistory.removeAt(0)
        if (shoulderYHistory.size > maxHistorySize) shoulderYHistory.removeAt(0)
        
        if (headYHistory.size < 5) return CheatDetectionResult(true, "", "")
        
        // Calculate movement ranges
        val headMovement = headYHistory.maxOrNull()!! - headYHistory.minOrNull()!!
        val shoulderMovement = shoulderYHistory.maxOrNull()!! - shoulderYHistory.minOrNull()!!
        
        // Check for head bobbing
        if (headMovement > 20f && shoulderMovement < 5f) {
            return CheatDetectionResult(
                isValid = false,
                cheatType = "HEAD_BOBBING",
                feedback = "Only head moving! Use full body"
            )
        }
        
        return CheatDetectionResult(true, "", "")
    }
    
    /**
     * Check for hip thrusting
     * Hips move opposite direction to shoulders
     */
    fun checkHipThrusting(landmarks: Map<String, FloatArray>): CheatDetectionResult {
        val leftShoulder = landmarks["left_shoulder"] ?: return CheatDetectionResult(true, "", "")
        val rightShoulder = landmarks["right_shoulder"] ?: return CheatDetectionResult(true, "", "")
        val leftHip = landmarks["left_hip"] ?: return CheatDetectionResult(true, "", "")
        val rightHip = landmarks["right_hip"] ?: return CheatDetectionResult(true, "", "")
        
        val shoulderY = (leftShoulder[1] + rightShoulder[1]) / 2
        val hipY = (leftHip[1] + rightHip[1]) / 2
        
        hipYHistory.add(hipY)
        
        if (hipYHistory.size > maxHistorySize) hipYHistory.removeAt(0)
        
        if (shoulderYHistory.size < 5 || hipYHistory.size < 5) {
            return CheatDetectionResult(true, "", "")
        }
        
        // Calculate movement directions
        val shoulderDelta = shoulderYHistory.last() - shoulderYHistory.first()
        val hipDelta = hipYHistory.last() - hipYHistory.first()
        
        // Check if moving in opposite directions (significant movement)
        if (abs(shoulderDelta) > 10f && abs(hipDelta) > 10f) {
            if ((shoulderDelta > 0 && hipDelta < 0) || (shoulderDelta < 0 && hipDelta > 0)) {
                return CheatDetectionResult(
                    isValid = false,
                    cheatType = "HIP_THRUSTING",
                    feedback = "Hips moving opposite to shoulders!"
                )
            }
        }
        
        return CheatDetectionResult(true, "", "")
    }
    
    /**
     * Check for bouncing at bottom
     * Must hold for at least 3 frames
     */
    fun checkBouncing(elbowAngle: Float): CheatDetectionResult {
        // Check if at bottom position (< 95°)
        if (elbowAngle < 95f) {
            framesAtBottom++
        } else {
            // Left bottom position
            if (framesAtBottom > 0 && framesAtBottom < 3) {
                framesAtBottom = 0
                return CheatDetectionResult(
                    isValid = false,
                    cheatType = "BOUNCING",
                    feedback = "Bouncing at bottom! Hold for 0.1s"
                )
            }
            framesAtBottom = 0
        }
        
        lastElbowAngle = elbowAngle
        return CheatDetectionResult(true, "", "")
    }
    
    /**
     * Check for arm dominance
     * One arm doing >70% of work
     */
    fun checkArmDominance(leftAngle: Float, rightAngle: Float): CheatDetectionResult {
        val difference = abs(leftAngle - rightAngle)
        
        // Severe imbalance indicates one arm is dominant
        if (difference > 40f) {
            val dominantArm = if (leftAngle < rightAngle) "left" else "right"
            return CheatDetectionResult(
                isValid = false,
                cheatType = "ARM_DOMINANCE",
                feedback = "One arm dominant ($dominantArm doing most work)"
            )
        }
        
        return CheatDetectionResult(true, "", "")
    }
    
    /**
     * Check movement velocity
     * Should be 15-40 pixels/second
     */
    fun checkVelocity(landmarks: Map<String, FloatArray>, timestamp: Long): CheatDetectionResult {
        val leftShoulder = landmarks["left_shoulder"] ?: return CheatDetectionResult(true, "", "")
        val rightShoulder = landmarks["right_shoulder"] ?: return CheatDetectionResult(true, "", "")
        
        val shoulderY = (leftShoulder[1] + rightShoulder[1]) / 2
        
        timestampHistory.add(timestamp)
        
        if (timestampHistory.size > maxHistorySize) timestampHistory.removeAt(0)
        
        if (shoulderYHistory.size < 3 || timestampHistory.size < 3) {
            return CheatDetectionResult(true, "", "")
        }
        
        // Calculate velocity (pixels per second)
        val timeDelta = (timestampHistory.last() - timestampHistory.first()) / 1000f
        if (timeDelta < 0.1f) return CheatDetectionResult(true, "", "")
        
        val positionDelta = abs(shoulderYHistory.last() - shoulderYHistory.first())
        val velocity = positionDelta / timeDelta
        
        // Check if velocity is too high (jerky motion)
        if (velocity > 200f) {
            return CheatDetectionResult(
                isValid = false,
                cheatType = "JERKY_MOTION",
                feedback = "Movement too jerky! Slow down"
            )
        }
        
        return CheatDetectionResult(true, "", "")
    }
    
    /**
     * Check for camera tilting
     * Shoulder angle shouldn't change >15° during rep
     */
    private val shoulderAngleHistory = mutableListOf<Float>()
    
    fun checkCameraTilting(landmarks: Map<String, FloatArray>): CheatDetectionResult {
        val leftShoulder = landmarks["left_shoulder"] ?: return CheatDetectionResult(true, "", "")
        val rightShoulder = landmarks["right_shoulder"] ?: return CheatDetectionResult(true, "", "")
        
        // Calculate shoulder angle (horizontal line)
        val dx = rightShoulder[0] - leftShoulder[0]
        val dy = rightShoulder[1] - leftShoulder[1]
        val angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
        
        shoulderAngleHistory.add(angle)
        if (shoulderAngleHistory.size > maxHistorySize) shoulderAngleHistory.removeAt(0)
        
        if (shoulderAngleHistory.size < 5) return CheatDetectionResult(true, "", "")
        
        val angleRange = shoulderAngleHistory.maxOrNull()!! - shoulderAngleHistory.minOrNull()!!
        
        if (angleRange > 15f) {
            return CheatDetectionResult(
                isValid = false,
                cheatType = "CAMERA_TILTING",
                feedback = "Camera/body tilting detected"
            )
        }
        
        return CheatDetectionResult(true, "", "")
    }
    
    /**
     * Run all cheat detection checks
     */
    fun detectCheating(
        landmarks: Map<String, FloatArray>,
        leftElbowAngle: Float,
        rightElbowAngle: Float,
        timestamp: Long
    ): CheatDetectionResult {
        
        // Check head bobbing
        val headBobbing = checkHeadBobbing(landmarks)
        if (!headBobbing.isValid) return headBobbing
        
        // Check hip thrusting
        val hipThrusting = checkHipThrusting(landmarks)
        if (!hipThrusting.isValid) return hipThrusting
        
        // Check bouncing
        val avgAngle = (leftElbowAngle + rightElbowAngle) / 2
        val bouncing = checkBouncing(avgAngle)
        if (!bouncing.isValid) return bouncing
        
        // Check arm dominance
        val armDominance = checkArmDominance(leftElbowAngle, rightElbowAngle)
        if (!armDominance.isValid) return armDominance
        
        // Check velocity
        val velocity = checkVelocity(landmarks, timestamp)
        if (!velocity.isValid) return velocity
        
        // Check camera tilting
        val cameraTilting = checkCameraTilting(landmarks)
        if (!cameraTilting.isValid) return cameraTilting
        
        return CheatDetectionResult(true, "", "")
    }
    
    /**
     * Reset all tracking history
     */
    fun reset() {
        shoulderYHistory.clear()
        headYHistory.clear()
        hipYHistory.clear()
        timestampHistory.clear()
        shoulderAngleHistory.clear()
        framesAtBottom = 0
        lastElbowAngle = 180f
    }
}

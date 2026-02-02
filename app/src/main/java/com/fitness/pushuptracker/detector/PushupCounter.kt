package com.fitness.pushuptracker.detector

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

/**
 * PROFESSIONAL PUSHUP COUNTER - ANDROID PRODUCTION VERSION
 * ========================================================
 * Complete conversion from Python to Kotlin with Android optimizations
 * 
 * Features:
 * ✅ 6 Pushup Types Detection
 * ✅ Full Range of Motion Validation
 * ✅ Body Alignment Checks
 * ✅ Speed/Timing Control (0.5s - 3.0s)
 * ✅ Stability Validation
 * ✅ Advanced Anti-Cheat
 * ✅ Complete State Machine
 * ✅ Mobile Performance Optimized
 */

// ============================================
// ENUMS AND DATA CLASSES
// ============================================

enum class PushupType(val displayName: String) {
    STANDARD("Standard"),
    KNEE("Knee"),
    WIDE("Wide"),
    DIAMOND("Diamond"),
    DECLINE("Decline"),
    INCLINE("Incline"),
    UNKNOWN("Unknown")
}

enum class PushupState {
    IDLE,          // No person or not ready
    SETUP,         // Person detected, positioning
    READY,         // In starting position (top)
    DESCENDING,    // Going down
    BOTTOM,        // At bottom position
    ASCENDING,     // Going up
    TOP,          // Back at top
    COUNTED       // Rep validated and counted
}

data class Landmark2D(
    val x: Float,
    val y: Float,
    val confidence: Float
)

data class FrameValidation(
    val personVisible: Boolean = false,
    val landmarksConfident: Boolean = false,
    val cameraStable: Boolean = true,
    val lightingAdequate: Boolean = true,
    val singlePerson: Boolean = true,
    val correctOrientation: Boolean = false
) {
    fun isValid(): Boolean = personVisible && 
                            landmarksConfident && 
                            cameraStable && 
                            singlePerson && 
                            correctOrientation
}

data class PushupValidation(
    var fullRange: Boolean = false,
    var properForm: Boolean = false,
    var stableExecution: Boolean = false,
    var correctTiming: Boolean = false,
    var consistentType: Boolean = false,
    var noCheating: Boolean = false
) {
    fun isValid(): Boolean = fullRange && 
                            properForm && 
                            stableExecution && 
                            correctTiming && 
                            noCheating
}

data class PushupResult(
    val count: Int = 0,
    val pushupType: PushupType = PushupType.UNKNOWN,
    val formScore: Float = 0f,
    val feedback: List<String> = emptyList(),
    val partialReps: List<Float> = emptyList(),
    val invalidReasons: List<String> = emptyList(),
    val state: PushupState = PushupState.IDLE,
    val leftElbowAngle: Int = 0,
    val rightElbowAngle: Int = 0,
    val bodyAlignmentScore: Float = 0f,
    val repTime: Float = 0f
)

data class BodyMeasurements(
    var shoulderWidth: Float = 0f,
    var armLength: Float = 0f,
    var torsoLength: Float = 0f,
    var calibrated: Boolean = false
)

// ============================================
// CIRCULAR BUFFER FOR SMOOTHING
// ============================================

class CircularFloatBuffer(private val size: Int) {
    private val buffer = FloatArray(size)
    private var index = 0
    private var count = 0
    
    fun add(value: Float) {
        buffer[index] = value
        index = (index + 1) % size
        if (count < size) count++
    }
    
    fun average(): Float {
        if (count == 0) return 0f
        return buffer.take(count).average().toFloat()
    }
    
    fun size(): Int = count
    
    fun clear() {
        count = 0
        index = 0
    }
}

class LandmarkHistory(private val maxSize: Int = 10) {
    private val history = ArrayDeque<Map<String, FloatArray>>()
    
    fun add(landmarks: Map<String, FloatArray>) {
        if (history.size >= maxSize) {
            history.removeFirst()
        }
        history.addLast(landmarks.toMap())
    }
    
    fun get(index: Int): Map<String, FloatArray>? {
        return if (index < history.size) history[index] else null
    }
    
    fun size(): Int = history.size
    
    fun clear() {
        history.clear()
    }
    
    fun isEmpty(): Boolean = history.isEmpty()
}

// ============================================
// PROFESSIONAL PUSHUP COUNTER
// ============================================

class PushupCounter(private val strictMode: Boolean = false) {
    
    // State flows for UI updates
    private val _pushupResult = MutableStateFlow(PushupResult())
    val pushupResult: StateFlow<PushupResult> = _pushupResult.asStateFlow()
    
    // Internal state
    private var state = PushupState.IDLE
    private var count = 0
    private var pushupType = PushupType.UNKNOWN
    
    // Thresholds (biomechanically correct)
    private val ELBOW_UP_MIN = if (strictMode) 165f else 160f
    private val ELBOW_DOWN_MAX = if (strictMode) 85f else 90f
    private val BODY_ALIGNMENT_MIN = 160f
    private val HIP_SAG_MIN = 160f
    private val HIP_PIKE_MAX = 175f
    
    // Timing thresholds
    private val MIN_REP_TIME = 0.5f  // seconds
    private val MAX_REP_TIME = 3.0f  // seconds
    
    // Stability requirements
    private val STABILITY_FRAMES = 3
    private val MAX_LANDMARK_JUMP = 0.15f  // 15% of frame
    
    // Type detection thresholds
    private val WIDE_MULTIPLIER = 1.4f
    private val DIAMOND_MULTIPLIER = 0.6f
    
    // History buffers
    private val angleHistory = CircularFloatBuffer(30)  // 1 second at 30fps
    private val landmarkHistory = LandmarkHistory(10)
    private val stateHistory = CircularFloatBuffer(30)
    
    // Timing
    private var stateStartTime = System.currentTimeMillis()
    private var repStartTime = 0L
    private var bottomHoldFrames = 0
    private var topHoldFrames = 0
    
    // Calibration
    private val measurements = BodyMeasurements()
    private var calibrationFrames = 0
    
    // Current frame data
    private var currentLandmarks = mapOf<String, FloatArray>()
    private var currentAngles = mapOf<String, Float>()
    private var lastFrameTime = System.currentTimeMillis()
    
    // Validation
    private var frameValidation = FrameValidation()
    private val pushupValidation = PushupValidation()
    
    // Feedback
    private val feedbackMessages = mutableListOf<String>()
    private val invalidReasons = mutableListOf<String>()
    
    // ========================================
    // CORE ANGLE CALCULATIONS
    // ========================================
    
    private fun calculateAngle(a: FloatArray, b: FloatArray, c: FloatArray): Float {
        val ax = a[0]
        val ay = a[1]
        val bx = b[0]
        val by = b[1]
        val cx = c[0]
        val cy = c[1]
        
        val radians = atan2(cy - by, cx - bx) - atan2(ay - by, ax - bx)
        var angle = abs(radians * 180f / PI.toFloat())
        
        if (angle > 180f) {
            angle = 360f - angle
        }
        
        return angle
    }
    
    private fun calculateDistance(p1: FloatArray, p2: FloatArray): Float {
        val dx = p1[0] - p2[0]
        val dy = p1[1] - p2[1]
        return sqrt(dx * dx + dy * dy)
    }
    
    // ========================================
    // PUSHUP TYPE DETECTION
    // ========================================
    
    private fun detectPushupType(landmarks: Map<String, FloatArray>): PushupType {
        return try {
            val lShoulder = landmarks["left_shoulder"] ?: return PushupType.UNKNOWN
            val rShoulder = landmarks["right_shoulder"] ?: return PushupType.UNKNOWN
            val lWrist = landmarks["left_wrist"] ?: return PushupType.UNKNOWN
            val rWrist = landmarks["right_wrist"] ?: return PushupType.UNKNOWN
            val lKnee = landmarks["left_knee"] ?: return PushupType.UNKNOWN
            val rKnee = landmarks["right_knee"] ?: return PushupType.UNKNOWN
            val lAnkle = landmarks["left_ankle"] ?: return PushupType.UNKNOWN
            val rAnkle = landmarks["right_ankle"] ?: return PushupType.UNKNOWN
            val lHip = landmarks["left_hip"] ?: return PushupType.UNKNOWN
            val rHip = landmarks["right_hip"] ?: return PushupType.UNKNOWN
            
            // Calculate measurements
            val shoulderWidth = calculateDistance(lShoulder, rShoulder)
            val handWidth = calculateDistance(lWrist, rWrist)
            
            // Check if hands or feet are elevated
            val shoulderY = (lShoulder[1] + rShoulder[1]) / 2
            val wristY = (lWrist[1] + rWrist[1]) / 2
            val ankleY = (lAnkle[1] + rAnkle[1]) / 2
            
            val heightDiffHands = abs(wristY - shoulderY)
            val heightDiffFeet = abs(ankleY - lHip[1])
            
            // Check knee position
            val kneeAngleL = calculateAngle(lHip, lKnee, lAnkle)
            val kneeAngleR = calculateAngle(rHip, rKnee, rAnkle)
            val avgKneeAngle = (kneeAngleL + kneeAngleR) / 2
            
            // Detection logic
            when {
                avgKneeAngle < 120f -> PushupType.KNEE
                handWidth > shoulderWidth * WIDE_MULTIPLIER -> PushupType.WIDE
                handWidth < shoulderWidth * DIAMOND_MULTIPLIER -> PushupType.DIAMOND
                heightDiffFeet > 0.15f -> PushupType.DECLINE
                heightDiffHands > 0.15f -> PushupType.INCLINE
                else -> PushupType.STANDARD
            }
        } catch (e: Exception) {
            PushupType.UNKNOWN
        }
    }
    
    // ========================================
    // FORM VALIDATION
    // ========================================
    
    private data class AlignmentResult(
        val isAligned: Boolean,
        val angle: Float,
        val feedback: String
    )
    
    private fun checkBodyAlignment(landmarks: Map<String, FloatArray>): AlignmentResult {
        return try {
            val lShoulder = landmarks["left_shoulder"] ?: return AlignmentResult(false, 0f, "Can't detect body position")
            val lHip = landmarks["left_hip"] ?: return AlignmentResult(false, 0f, "Can't detect body position")
            val lAnkle = landmarks["left_ankle"] ?: return AlignmentResult(false, 0f, "Can't detect body position")
            
            val bodyAngle = calculateAngle(lShoulder, lHip, lAnkle)
            
            when {
                bodyAngle < HIP_SAG_MIN -> AlignmentResult(false, bodyAngle, "Hips sagging! Engage core")
                bodyAngle > HIP_PIKE_MAX -> AlignmentResult(false, bodyAngle, "Hips too high! Lower hips")
                bodyAngle >= BODY_ALIGNMENT_MIN -> AlignmentResult(true, bodyAngle, "Perfect alignment!")
                else -> AlignmentResult(false, bodyAngle, "Keep body straight")
            }
        } catch (e: Exception) {
            AlignmentResult(false, 0f, "Can't detect body position")
        }
    }
    
    private data class BalanceResult(val isBalanced: Boolean, val feedback: String)
    
    private fun checkElbowBalance(leftAngle: Float, rightAngle: Float): BalanceResult {
        val difference = abs(leftAngle - rightAngle)
        
        return when {
            difference > 30f -> BalanceResult(false, "Unbalanced arms detected")
            difference > 20f -> BalanceResult(true, "Keep arms balanced")
            else -> BalanceResult(true, "Good arm balance")
        }
    }
    
    private data class CheatResult(val isValid: Boolean, val feedback: String)
    
    private fun checkHeadMovementOnly(landmarks: Map<String, FloatArray>): CheatResult {
        if (landmarkHistory.size() < 5) {
            return CheatResult(true, "")
        }
        
        return try {
            val prev = landmarkHistory.get(0) ?: return CheatResult(true, "")
            
            val nose = landmarks["nose"] ?: return CheatResult(true, "")
            val prevNose = prev["nose"] ?: return CheatResult(true, "")
            
            val lShoulder = landmarks["left_shoulder"] ?: return CheatResult(true, "")
            val rShoulder = landmarks["right_shoulder"] ?: return CheatResult(true, "")
            val prevLShoulder = prev["left_shoulder"] ?: return CheatResult(true, "")
            val prevRShoulder = prev["right_shoulder"] ?: return CheatResult(true, "")
            
            val noseMovement = calculateDistance(nose, prevNose)
            val shoulderMovement = (
                calculateDistance(lShoulder, prevLShoulder) +
                calculateDistance(rShoulder, prevRShoulder)
            ) / 2
            
            if (shoulderMovement > 0 && noseMovement / shoulderMovement > 2.0f) {
                CheatResult(false, "Only head moving! Use full body")
            } else {
                CheatResult(true, "")
            }
        } catch (e: Exception) {
            CheatResult(true, "")
        }
    }
    
    private fun checkHipMovement(landmarks: Map<String, FloatArray>): CheatResult {
        if (landmarkHistory.size() < 5) {
            return CheatResult(true, "")
        }
        
        return try {
            val prev = landmarkHistory.get(0) ?: return CheatResult(true, "")
            
            val lShoulder = landmarks["left_shoulder"] ?: return CheatResult(true, "")
            val lHip = landmarks["left_hip"] ?: return CheatResult(true, "")
            val prevLShoulder = prev["left_shoulder"] ?: return CheatResult(true, "")
            val prevLHip = prev["left_hip"] ?: return CheatResult(true, "")
            
            val shoulderMovement = calculateDistance(lShoulder, prevLShoulder)
            val hipMovement = calculateDistance(lHip, prevLHip)
            
            if (shoulderMovement > 0.05f && hipMovement / shoulderMovement < 0.3f) {
                CheatResult(false, "Hips not moving! Full body required")
            } else {
                CheatResult(true, "")
            }
        } catch (e: Exception) {
            CheatResult(true, "")
        }
    }
    
    private fun checkStability(landmarks: Map<String, FloatArray>): CheatResult {
        if (landmarkHistory.size() < 2) {
            return CheatResult(true, "")
        }
        
        return try {
            val prev = landmarkHistory.get(landmarkHistory.size() - 1) ?: return CheatResult(true, "")
            
            val keys = listOf("left_wrist", "right_wrist", "left_shoulder", "right_shoulder")
            
            for (key in keys) {
                val current = landmarks[key] ?: continue
                val previous = prev[key] ?: continue
                
                val movement = calculateDistance(current, previous)
                
                if (movement > MAX_LANDMARK_JUMP) {
                    return CheatResult(false, "Unstable movement detected")
                }
            }
            
            CheatResult(true, "")
        } catch (e: Exception) {
            CheatResult(true, "")
        }
    }
    
    // ========================================
    // CALIBRATION
    // ========================================
    
    private fun calibrate(landmarks: Map<String, FloatArray>) {
        try {
            val lShoulder = landmarks["left_shoulder"] ?: return
            val rShoulder = landmarks["right_shoulder"] ?: return
            val lWrist = landmarks["left_wrist"] ?: return
            val rWrist = landmarks["right_wrist"] ?: return
            val lHip = landmarks["left_hip"] ?: return
            
            val shoulderWidth = calculateDistance(lShoulder, rShoulder)
            val armLengthL = calculateDistance(lShoulder, lWrist)
            val armLengthR = calculateDistance(rShoulder, rWrist)
            val armLength = (armLengthL + armLengthR) / 2
            val torsoLength = calculateDistance(lShoulder, lHip)
            
            if (!measurements.calibrated) {
                measurements.shoulderWidth = shoulderWidth
                measurements.armLength = armLength
                measurements.torsoLength = torsoLength
                calibrationFrames = 1
            } else {
                val n = calibrationFrames.toFloat()
                measurements.shoulderWidth = (measurements.shoulderWidth * n + shoulderWidth) / (n + 1)
                measurements.armLength = (measurements.armLength * n + armLength) / (n + 1)
                measurements.torsoLength = (measurements.torsoLength * n + torsoLength) / (n + 1)
                calibrationFrames++
            }
            
            if (calibrationFrames >= 30) {
                measurements.calibrated = true
            }
        } catch (e: Exception) {
            // Calibration failed for this frame
        }
    }
    
    // ========================================
    // STATE MACHINE
    // ========================================
    
    private fun updateStateMachine(landmarks: Map<String, FloatArray>, elbowAvg: Float): PushupState {
        val currentState = state
        var newState = currentState
        
        val alignmentResult = checkBodyAlignment(landmarks)
        val isAligned = alignmentResult.isAligned
        
        when (currentState) {
            PushupState.IDLE -> {
                if (frameValidation.personVisible) {
                    newState = PushupState.SETUP
                    calibrate(landmarks)
                }
            }
            
            PushupState.SETUP -> {
                if (elbowAvg > ELBOW_UP_MIN && isAligned) {
                    topHoldFrames++
                    if (topHoldFrames >= STABILITY_FRAMES) {
                        newState = PushupState.READY
                        repStartTime = System.currentTimeMillis()
                        topHoldFrames = 0
                    }
                } else {
                    topHoldFrames = 0
                    calibrate(landmarks)
                }
            }
            
            PushupState.READY -> {
                if (elbowAvg < ELBOW_UP_MIN) {
                    newState = PushupState.DESCENDING
                }
            }
            
            PushupState.DESCENDING -> {
                if (elbowAvg <= ELBOW_DOWN_MAX) {
                    newState = PushupState.BOTTOM
                    bottomHoldFrames = 0
                } else if (elbowAvg > ELBOW_UP_MIN) {
                    invalidReasons.add("Didn't go deep enough")
                    newState = PushupState.SETUP
                }
            }
            
            PushupState.BOTTOM -> {
                if (elbowAvg <= ELBOW_DOWN_MAX) {
                    bottomHoldFrames++
                    if (bottomHoldFrames >= STABILITY_FRAMES) {
                        newState = PushupState.ASCENDING
                    }
                } else if (elbowAvg > ELBOW_DOWN_MAX + 10) {
                    if (bottomHoldFrames < 2) {
                        invalidReasons.add("Bounced at bottom")
                        newState = PushupState.SETUP
                    } else {
                        newState = PushupState.ASCENDING
                    }
                }
            }
            
            PushupState.ASCENDING -> {
                if (elbowAvg >= ELBOW_UP_MIN) {
                    newState = PushupState.TOP
                    topHoldFrames = 0
                } else if (elbowAvg < ELBOW_DOWN_MAX) {
                    invalidReasons.add("Went down before finishing rep")
                    newState = PushupState.SETUP
                }
            }
            
            PushupState.TOP -> {
                if (elbowAvg >= ELBOW_UP_MIN && isAligned) {
                    topHoldFrames++
                    if (topHoldFrames >= STABILITY_FRAMES) {
                        val repTime = (System.currentTimeMillis() - repStartTime) / 1000f
                        
                        if (validateRep(repTime)) {
                            newState = PushupState.COUNTED
                        } else {
                            newState = PushupState.SETUP
                        }
                    }
                } else {
                    if (elbowAvg < ELBOW_UP_MIN - 10) {
                        newState = PushupState.DESCENDING
                        repStartTime = System.currentTimeMillis()
                    }
                }
            }
            
            PushupState.COUNTED -> {
                count++
                feedbackMessages.add("Rep counted! ✓")
                newState = PushupState.READY
                repStartTime = System.currentTimeMillis()
            }
        }
        
        if (newState != currentState) {
            stateStartTime = System.currentTimeMillis()
        }
        
        return newState
    }
    
    private fun validateRep(repTime: Float): Boolean {
        val reasons = mutableListOf<String>()
        
        if (repTime < MIN_REP_TIME) {
            reasons.add("Too fast (${String.format("%.1f", repTime)}s)")
            pushupValidation.correctTiming = false
        } else if (repTime > MAX_REP_TIME) {
            reasons.add("Too slow (${String.format("%.1f", repTime)}s)")
            pushupValidation.correctTiming = false
        } else {
            pushupValidation.correctTiming = true
        }
        
        return if (pushupValidation.isValid()) {
            true
        } else {
            invalidReasons.addAll(reasons)
            false
        }
    }
    
    // ========================================
    // LANDMARK EXTRACTION
    // ========================================
    
    private fun extractLandmarks(result: PoseLandmarkerResult, width: Int, height: Int): Map<String, FloatArray>? {
        if (result.landmarks().isEmpty()) {
            return null
        }
        
        val landmarks = mutableMapOf<String, FloatArray>()
        val lm = result.landmarks()[0]
        
        val landmarkMap = mapOf(
            "nose" to 0,
            "left_shoulder" to 11, "right_shoulder" to 12,
            "left_elbow" to 13, "right_elbow" to 14,
            "left_wrist" to 15, "right_wrist" to 16,
            "left_hip" to 23, "right_hip" to 24,
            "left_knee" to 25, "right_knee" to 26,
            "left_ankle" to 27, "right_ankle" to 28
        )
        
        for ((name, idx) in landmarkMap) {
            if (idx < lm.size) {
                val landmark = lm[idx]
                landmarks[name] = floatArrayOf(
                    landmark.x() * width,
                    landmark.y() * height,
                    landmark.visibility().orElse(0f)
                )
            }
        }
        
        return landmarks
    }
    
    private fun validateFrame(landmarks: Map<String, FloatArray>?): FrameValidation {
        if (landmarks == null) {
            return FrameValidation()
        }
        
        val personVisible = true
        
        val confidences = landmarks.values.map { it[2] }
        val landmarksConfident = confidences.average() > 0.7
        
        val lShoulder = landmarks["left_shoulder"]
        val rShoulder = landmarks["right_shoulder"]
        
        val correctOrientation = if (lShoulder != null && rShoulder != null) {
            val shoulderAngle = abs(
                atan2(
                    rShoulder[1] - lShoulder[1],
                    rShoulder[0] - lShoulder[0]
                ) * 180f / PI.toFloat()
            )
            shoulderAngle < 20f || shoulderAngle > 160f
        } else {
            false
        }
        
        return FrameValidation(
            personVisible = personVisible,
            landmarksConfident = landmarksConfident,
            cameraStable = true,
            lightingAdequate = true,
            singlePerson = true,
            correctOrientation = correctOrientation
        )
    }
    
    // ========================================
    // MAIN PROCESSING
    // ========================================
    
    fun processFrame(result: PoseLandmarkerResult, width: Int, height: Int) {
        val currentTime = System.currentTimeMillis()
        
        // Extract landmarks
        val landmarks = extractLandmarks(result, width, height)
        
        // Validate frame
        frameValidation = validateFrame(landmarks)
        
        // Reset feedback
        feedbackMessages.clear()
        invalidReasons.clear()
        
        if (!frameValidation.isValid()) {
            _pushupResult.value = PushupResult(
                count = count,
                state = PushupState.IDLE,
                feedback = listOf("Position yourself in frame"),
                pushupType = pushupType
            )
            return
        }
        
        landmarks?.let { lm ->
            // Store landmarks
            currentLandmarks = lm
            landmarkHistory.add(lm.toMap())
            
            // Calculate elbow angles
            val lShoulder = lm["left_shoulder"] ?: return
            val lElbow = lm["left_elbow"] ?: return
            val lWrist = lm["left_wrist"] ?: return
            val rShoulder = lm["right_shoulder"] ?: return
            val rElbow = lm["right_elbow"] ?: return
            val rWrist = lm["right_wrist"] ?: return
            
            val leftAngle = calculateAngle(lShoulder, lElbow, lWrist)
            val rightAngle = calculateAngle(rShoulder, rElbow, rWrist)
            val elbowAvg = (leftAngle + rightAngle) / 2
            
            currentAngles = mapOf(
                "left_elbow" to leftAngle,
                "right_elbow" to rightAngle,
                "avg" to elbowAvg
            )
            
            // Detect pushup type
            val detectedType = detectPushupType(lm)
            if (state in listOf(PushupState.SETUP, PushupState.READY)) {
                pushupType = detectedType
            }
            
            // Validate form
            val alignmentResult = checkBodyAlignment(lm)
            val balanceResult = checkElbowBalance(leftAngle, rightAngle)
            val stabilityResult = checkStability(lm)
            val headResult = checkHeadMovementOnly(lm)
            val hipResult = checkHipMovement(lm)
            
            // Update pushup validation
            pushupValidation.properForm = alignmentResult.isAligned && balanceResult.isBalanced
            pushupValidation.stableExecution = stabilityResult.isValid
            pushupValidation.noCheating = headResult.isValid && hipResult.isValid
            
            // Collect feedback
            if (!alignmentResult.isAligned) {
                feedbackMessages.add(alignmentResult.feedback)
            }
            if (!balanceResult.isBalanced) {
                feedbackMessages.add(balanceResult.feedback)
            }
            if (!stabilityResult.isValid) {
                feedbackMessages.add(stabilityResult.feedback)
            }
            if (!headResult.isValid) {
                feedbackMessages.add(headResult.feedback)
            }
            if (!hipResult.isValid) {
                feedbackMessages.add(hipResult.feedback)
            }
            
            // Update state machine
            state = updateStateMachine(lm, elbowAvg)
            
            // Calculate form score
            val formScore = listOf(
                alignmentResult.isAligned,
                balanceResult.isBalanced,
                stabilityResult.isValid,
                headResult.isValid,
                hipResult.isValid
            ).count { it } / 5f
            
            // Calculate rep time
            val repTime = if (state in listOf(
                    PushupState.DESCENDING,
                    PushupState.BOTTOM,
                    PushupState.ASCENDING,
                    PushupState.TOP
                )) {
                (currentTime - repStartTime) / 1000f
            } else {
                0f
            }
            
            // Update result
            _pushupResult.value = PushupResult(
                count = count,
                pushupType = pushupType,
                formScore = formScore,
                feedback = feedbackMessages.toList(),
                invalidReasons = invalidReasons.toList(),
                state = state,
                leftElbowAngle = leftAngle.toInt(),
                rightElbowAngle = rightAngle.toInt(),
                bodyAlignmentScore = if (alignmentResult.isAligned) alignmentResult.angle / 180f else 0f,
                repTime = repTime
            )
        }
    }
    
    fun reset() {
        count = 0
        state = PushupState.IDLE
        pushupType = PushupType.UNKNOWN
        angleHistory.clear()
        landmarkHistory.clear()
        feedbackMessages.clear()
        invalidReasons.clear()
        calibrationFrames = 0
        measurements.calibrated = false
        
        _pushupResult.value = PushupResult()
    }
}

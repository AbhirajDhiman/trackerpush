package com.fitness.pushuptracker.detector

/**
 * Calibration Manager
 * ===================
 * Handles initial and dynamic calibration:
 * - Initial calibration (first 30 frames)
 * - Body measurements (shoulder width, arm length, torso length)
 * - Baseline angle establishment
 * - Pushup type determination
 * - Dynamic recalibration (every 100 frames)
 * - Person change detection
 * 
 * Phase 6: Calibration System
 */
class CalibrationManager {
    
    data class BodyMeasurements(
        var shoulderWidth: Float = 0f,
        var armLength: Float = 0f,
        var torsoLength: Float = 0f,
        var baselineElbowAngle: Float = 0f,
        var baselineBodyAlignment: Float = 0f,
        var pushupType: PushupType = PushupType.UNKNOWN,
        var calibrated: Boolean = false
    )
    
    private val measurements = BodyMeasurements()
    private var calibrationFrames = 0
    private var totalFrames = 0
    private var lastRecalibrationFrame = 0
    
    // Thresholds for recalibration
    private val INITIAL_CALIBRATION_FRAMES = 30
    private val RECALIBRATION_INTERVAL = 100
    
    /**
     * Process frame for calibration
     */
    fun calibrate(landmarks: Map<String, FloatArray>): BodyMeasurements {
        totalFrames++
        
        // Initial calibration (first 30 frames)
        if (!measurements.calibrated && calibrationFrames < INITIAL_CALIBRATION_FRAMES) {
            performInitialCalibration(landmarks)
            calibrationFrames++
            
            if (calibrationFrames >= INITIAL_CALIBRATION_FRAMES) {
                measurements.calibrated = true
                android.util.Log.d("Calibration", "✅ Initial calibration complete!")
                android.util.Log.d("Calibration", "   Shoulder width: ${String.format("%.1f", measurements.shoulderWidth)}px")
                android.util.Log.d("Calibration", "   Arm length: ${String.format("%.1f", measurements.armLength)}px")
                android.util.Log.d("Calibration", "   Pushup type: ${measurements.pushupType.displayName}")
            }
        }
        
        // Dynamic recalibration (every 100 frames)
        if (measurements.calibrated && totalFrames - lastRecalibrationFrame >= RECALIBRATION_INTERVAL) {
            performDynamicRecalibration(landmarks)
            lastRecalibrationFrame = totalFrames
        }
        
        return measurements
    }
    
    /**
     * Initial calibration - average measurements over first 30 frames
     */
    private fun performInitialCalibration(landmarks: Map<String, FloatArray>) {
        try {
            val lShoulder = landmarks["left_shoulder"] ?: return
            val rShoulder = landmarks["right_shoulder"] ?: return
            val lWrist = landmarks["left_wrist"] ?: return
            val rWrist = landmarks["right_wrist"] ?: return
            val lHip = landmarks["left_hip"] ?: return
            val rHip = landmarks["right_hip"] ?: return
            
            // Calculate measurements for this frame
            val shoulderWidth = calculateDistance(lShoulder, rShoulder)
            val armLengthL = calculateDistance(lShoulder, lWrist)
            val armLengthR = calculateDistance(rShoulder, rWrist)
            val armLength = (armLengthL + armLengthR) / 2
            val torsoLength = calculateDistance(
                floatArrayOf((lShoulder[0] + rShoulder[0]) / 2, (lShoulder[1] + rShoulder[1]) / 2),
                floatArrayOf((lHip[0] + rHip[0]) / 2, (lHip[1] + rHip[1]) / 2)
            )
            
            // Running average
            if (calibrationFrames == 0) {
                measurements.shoulderWidth = shoulderWidth
                measurements.armLength = armLength
                measurements.torsoLength = torsoLength
            } else {
                val n = calibrationFrames.toFloat()
                measurements.shoulderWidth = (measurements.shoulderWidth * n + shoulderWidth) / (n + 1)
                measurements.armLength = (measurements.armLength * n + armLength) / (n + 1)
                measurements.torsoLength = (measurements.torsoLength * n + torsoLength) / (n + 1)
            }
            
            // Determine pushup type (on last calibration frame)
            if (calibrationFrames == INITIAL_CALIBRATION_FRAMES - 1) {
                measurements.pushupType = determinePushupType(landmarks)
            }
            
        } catch (e: Exception) {
            android.util.Log.w("Calibration", "Frame ${calibrationFrames} calibration failed: ${e.message}")
        }
    }
    
    /**
     * Dynamic recalibration - adjust for changes in lighting, distance, etc.
     */
    private fun performDynamicRecalibration(landmarks: Map<String, FloatArray>) {
        try {
            val lShoulder = landmarks["left_shoulder"] ?: return
            val rShoulder = landmarks["right_shoulder"] ?: return
            val lWrist = landmarks["left_wrist"] ?: return
            val rWrist = landmarks["right_wrist"] ?: return
            
            val currentShoulderWidth = calculateDistance(lShoulder, rShoulder)
            val currentArmLength = (calculateDistance(lShoulder, lWrist) + calculateDistance(rShoulder, rWrist)) / 2
            
            // Check if person changed (shoulder width changed by >20%)
            val shoulderWidthChange = kotlin.math.abs(currentShoulderWidth - measurements.shoulderWidth) / measurements.shoulderWidth
            
            if (shoulderWidthChange > 0.2f) {
                android.util.Log.w("Calibration", "⚠️ Person change detected! Resetting calibration...")
                reset()
                return
            }
            
            // Gradual adjustment (10% weight to new measurement)
            measurements.shoulderWidth = measurements.shoulderWidth * 0.9f + currentShoulderWidth * 0.1f
            measurements.armLength = measurements.armLength * 0.9f + currentArmLength * 0.1f
            
            android.util.Log.d("Calibration", "🔄 Dynamic recalibration at frame $totalFrames")
            
        } catch (e: Exception) {
            android.util.Log.w("Calibration", "Dynamic recalibration failed: ${e.message}")
        }
    }
    
    /**
     * Determine pushup type based on hand and feet positioning
     */
    private fun determinePushupType(landmarks: Map<String, FloatArray>): PushupType {
        try {
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
            
            val shoulderWidth = calculateDistance(lShoulder, rShoulder)
            val handWidth = calculateDistance(lWrist, rWrist)
            
            // Check knee position (for knee pushups)
            val lKneeAngle = calculateAngle(lHip, lKnee, lAnkle)
            val rKneeAngle = calculateAngle(rHip, rKnee, rAnkle)
            val avgKneeAngle = (lKneeAngle + rKneeAngle) / 2
            
            // Check hand elevation
            val shoulderY = (lShoulder[1] + rShoulder[1]) / 2
            val wristY = (lWrist[1] + rWrist[1]) / 2
            val ankleY = (lAnkle[1] + rAnkle[1]) / 2
            val hipY = (lHip[1] + rHip[1]) / 2
            
            return when {
                avgKneeAngle < 120f -> PushupType.KNEE
                handWidth > shoulderWidth * 1.4f -> PushupType.WIDE
                handWidth < shoulderWidth * 0.6f -> PushupType.DIAMOND
                ankleY < hipY - 50f -> PushupType.DECLINE
                wristY < shoulderY - 50f -> PushupType.INCLINE
                else -> PushupType.STANDARD
            }
            
        } catch (e: Exception) {
            return PushupType.UNKNOWN
        }
    }
    
    /**
     * Calculate distance between two points
     */
    private fun calculateDistance(p1: FloatArray, p2: FloatArray): Float {
        val dx = p1[0] - p2[0]
        val dy = p1[1] - p2[1]
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Calculate angle between three points
     */
    private fun calculateAngle(a: FloatArray, b: FloatArray, c: FloatArray): Float {
        val ba = floatArrayOf(a[0] - b[0], a[1] - b[1])
        val bc = floatArrayOf(c[0] - b[0], c[1] - b[1])
        
        val dot = ba[0] * bc[0] + ba[1] * bc[1]
        val magBA = kotlin.math.sqrt(ba[0] * ba[0] + ba[1] * ba[1])
        val magBC = kotlin.math.sqrt(bc[0] * bc[0] + bc[1] * bc[1])
        
        if (magBA == 0f || magBC == 0f) return 0f
        
        val cosAngle = (dot / (magBA * magBC)).coerceIn(-1f, 1f)
        return Math.toDegrees(kotlin.math.acos(cosAngle.toDouble())).toFloat()
    }
    
    /**
     * Get current measurements
     */
    fun getMeasurements(): BodyMeasurements = measurements
    
    /**
     * Check if calibrated
     */
    fun isCalibrated(): Boolean = measurements.calibrated
    
    /**
     * Reset calibration
     */
    fun reset() {
        measurements.shoulderWidth = 0f
        measurements.armLength = 0f
        measurements.torsoLength = 0f
        measurements.baselineElbowAngle = 0f
        measurements.baselineBodyAlignment = 0f
        measurements.pushupType = PushupType.UNKNOWN
        measurements.calibrated = false
        calibrationFrames = 0
        totalFrames = 0
        lastRecalibrationFrame = 0
    }
}

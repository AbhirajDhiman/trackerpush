package com.fitness.pushuptracker.detector

import android.util.Log
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

// Necessary Enums and Data Classes for ViewModel compatibility
enum class PushupState {
    IDLE, READY, DESCENDING, BOTTOM, ASCENDING, COUNTED
}

enum class PushupType(val displayName: String) {
    STANDARD("Standard"), KNEE("Knee"), WIDE("Wide"), DIAMOND("Diamond"), 
    DECLINE("Decline"), INCLINE("Incline"), UNKNOWN("Unknown")
}

data class PushupResult(
    val count: Int = 0,
    val state: PushupState = PushupState.IDLE,
    val leftElbowAngle: Float = 0f,
    val rightElbowAngle: Float = 0f,
    val formScore: Float = 0f,
    val feedback: String = "",
    val isValid: Boolean = true,
    val currentType: PushupType = PushupType.UNKNOWN
)

class PushupCounter(private val strictMode: Boolean = false) {
    
    private var count = 0
    private var internalState = "IDLE" // String state from user's sample
    private var wasDown = false
    
    private val _pushupResult = MutableStateFlow(PushupResult())
    val pushupResult: StateFlow<PushupResult> = _pushupResult.asStateFlow()
    
    fun processFrame(result: PoseLandmarkerResult, width: Int, height: Int) {
        try {
            if (result.landmarks().isEmpty()) return
            val landmarks = result.landmarks()[0]
            if (landmarks.size < 16) return
            
            // Indices: Shoulder(11), Elbow(13), Wrist(15)
            val shoulder = landmarks[11]
            val elbow = landmarks[13]
            val wrist = landmarks[15]
            
            val angle = calculateAngle(shoulder.x(), shoulder.y(), elbow.x(), elbow.y(), wrist.x(), wrist.y())
            val rightAngle = angle // Duplicate for simplicity
            
            Log.d("SIMPLE", "📐 Angle: ${angle.toInt()}° | State: $internalState | Count: $count")
            
            // User's State Machine
            when (internalState) {
                "IDLE" -> if (angle > 150) internalState = "TOP"
                "TOP" -> if (angle < 140) internalState = "DOWN"
                "DOWN" -> {
                    if (angle < 100) wasDown = true
                    if (angle > 130 && wasDown) internalState = "UP"
                }
                "UP" -> if (angle > 150) { internalState = "COUNTED"; count++ }
                "COUNTED" -> { internalState = "TOP"; wasDown = false }
            }
            
            // Map to Enum
            val enumState = when(internalState) {
                "IDLE" -> PushupState.IDLE
                "TOP" -> PushupState.READY
                "DOWN" -> PushupState.DESCENDING // Simplifying
                "UP" -> PushupState.ASCENDING
                "COUNTED" -> PushupState.COUNTED
                else -> PushupState.IDLE
            }
            
            _pushupResult.value = PushupResult(
                count = count,
                state = enumState,
                leftElbowAngle = angle,
                rightElbowAngle = rightAngle
            )
            
        } catch (e: Exception) {
            Log.e("SIMPLE", "Error: ${e.message}")
        }
    }
    
    private fun calculateAngle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Float {
        val BAx = x1 - x2; val BAy = y1 - y2
        val BCx = x3 - x2; val BCy = y3 - y2
        val dot = BAx * BCx + BAy * BCy
        val magBA = sqrt((BAx*BAx + BAy*BAy).toDouble())
        val magBC = sqrt((BCx*BCx + BCy*BCy).toDouble())
        if (magBA < 0.001 || magBC < 0.001) return 180f
        val cos = (dot / (magBA * magBC)).toFloat()
        return Math.toDegrees(acos(cos.toDouble().coerceIn(-1.0, 1.0))).toFloat()
    }
    
    fun getCurrentResult() = _pushupResult.value
    fun toggleStrictMode() {}
    fun reset() { count = 0; internalState = "IDLE"; wasDown = false; _pushupResult.value = PushupResult() }
}

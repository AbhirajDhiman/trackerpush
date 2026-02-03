package com.fitness.pushuptracker.detector

import android.util.Log
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

// Necessary Enums and Data Classes for ViewModel compatibility
enum class PushupState {
    IDLE, SETUP, READY, DESCENDING, BOTTOM, ASCENDING, COUNTED
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
    val pushupType: PushupType = PushupType.UNKNOWN,
    val currentType: PushupType = PushupType.UNKNOWN
)

class PushupCounter(private val strictMode: Boolean = false) {
    
    private var count = 0
    private var internalState = "SETUP" // Start in SETUP
    private var wasDown = false
    
    // Normalized depth (0.0 = top, 1.0 = bottom) for UI visualization
    private var currentDepth = 0f
    
    private val _pushupResult = MutableStateFlow(PushupResult())
    val pushupResult: StateFlow<PushupResult> = _pushupResult.asStateFlow()
    
    fun processFrame(result: PoseLandmarkerResult, width: Int, height: Int) {
        try {
            if (result.landmarks().isEmpty()) {
                // Return empty result if no landmarks, but keep previous count
                 _pushupResult.value = PushupResult(
                    count = count,
                    state = PushupState.IDLE,
                    feedback = "No person detected"
                )
                return
            }
            
            val landmarks = result.landmarks()[0]
            if (landmarks.size < 16) return
            
            // ===== BILATERAL ARM DETECTION =====
            val leftShoulder = landmarks[11]
            val leftElbow = landmarks[13]
            val leftWrist = landmarks[15]
            
            val rightShoulder = landmarks[12]
            val rightElbow = landmarks[14]
            val rightWrist = landmarks[16]
            
            // Calculate angles for both arms
            val leftAngle = calculateAngle(
                leftShoulder.x(), leftShoulder.y(),
                leftElbow.x(), leftElbow.y(),
                leftWrist.x(), leftWrist.y()
            )
            
            val rightAngle = calculateAngle(
                rightShoulder.x(), rightShoulder.y(),
                rightElbow.x(), rightElbow.y(),
                rightWrist.x(), rightWrist.y()
            )
            
            // Get visibility scores (0.0 to 1.0)
            val leftVisibility = leftElbow.visibility().orElse(0f)
            val rightVisibility = rightElbow.visibility().orElse(0f)
            
            // Choose the arm with better visibility
            val (angle, armSide) = if (leftVisibility >= rightVisibility) {
                Pair(leftAngle, "LEFT")
            } else {
                Pair(rightAngle, "RIGHT")
            }

            // Calculate normalized depth for UI (roughly 170° is top, 70° is bottom)
            // 0.0 = 170° (Top), 1.0 = 80° (Bottom)
            currentDepth = ((170f - angle) / 90f).coerceIn(0f, 1.0f)
            
            Log.d("PUSHUP", "📐 $armSide Angle: ${angle.toInt()}° | Depth: ${String.format("%.2f", currentDepth)} | State: $internalState | Count: $count")
            
            var feedback = ""

            // ===== STATE MACHINE (RELAXED) =====
            when (internalState) {
                "SETUP" -> {
                    feedback = "Get into pushup position"
                    if (angle > 150) { // Arms straight(ish)
                        internalState = "TOP"
                        Log.d("PUSHUP", "✅ State: SETUP → TOP")
                    } else if (angle < 100) {
                        feedback = "Straighten your arms"
                    }
                }
                "IDLE" -> {
                     internalState = "SETUP" // Auto-transition to SETUP
                }
                "TOP" -> {
                    feedback = "Go Down"
                    if (angle < 140) { // Started descending
                        internalState = "DOWN"
                        Log.d("PUSHUP", "✅ State: TOP → DOWN")
                    }
                }
                "DOWN" -> {
                    feedback = "Lower..."
                    if (angle < 100) {  // Good depth
                        wasDown = true
                        feedback = "Push Up!"
                        Log.d("PUSHUP", "✅ Bottom reached at ${angle.toInt()}°")
                    } else if (angle < 80) { // Deep
                         feedback = "Perfect Depth!"
                         wasDown = true
                    }
                    
                    if (angle > 140) { // Coming up
                        if (wasDown) {
                            internalState = "UP"
                            Log.d("PUSHUP", "✅ State: DOWN → UP")
                        } else {
                            // Aborted rep
                            internalState = "TOP"
                             feedback = "Rep too shallow"
                        }
                    }
                }
                "UP" -> {
                    feedback = "Extend fully"
                    if (angle > 160) {
                        internalState = "COUNTED"
                        count++
                        Log.d("PUSHUP", "🎉 PUSHUP COUNTED! Total: $count")
                    }
                }
                "COUNTED" -> {
                    internalState = "TOP"
                    wasDown = false
                    feedback = "Ready"
                    Log.d("PUSHUP", "✅ State: COUNTED → TOP (Ready for next rep)")
                }
            }
            
            // Map to Enum
            val enumState = when(internalState) {
                "SETUP" -> PushupState.SETUP
                "IDLE" -> PushupState.IDLE
                "TOP" -> PushupState.READY
                "DOWN" -> PushupState.DESCENDING
                "UP" -> PushupState.ASCENDING
                "COUNTED" -> PushupState.COUNTED
                else -> PushupState.IDLE
            }
            
            _pushupResult.value = PushupResult(
                count = count,
                state = enumState,
                leftElbowAngle = leftAngle,
                rightElbowAngle = rightAngle,
                formScore = currentDepth, // HACK: Passing depth as formScore for now so UI can use it
                feedback = feedback
            )
            
        } catch (e: Exception) {
            Log.e("PUSHUP", "❌ Error: ${e.message}", e)
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
    fun reset() { count = 0; internalState = "SETUP"; wasDown = false; _pushupResult.value = PushupResult() }
}

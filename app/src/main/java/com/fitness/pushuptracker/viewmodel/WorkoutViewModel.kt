package com.fitness.pushuptracker.viewmodel

import android.app.Application
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.fitness.pushuptracker.detector.MediaPipePoseDetector
import com.fitness.pushuptracker.detector.PushupCounter
import com.fitness.pushuptracker.detector.PushupResult
import com.fitness.pushuptracker.detector.PushupState
import com.fitness.pushuptracker.detector.PushupType
import com.fitness.pushuptracker.camera.CameraProcessor
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * COMPLETE FIXED WorkoutViewModel
 * ===============================
 * ✅ Fixes:
 * 1. Properly connects Camera → MediaPipe → PushupCounter
 * 2. Uses correct MediaPipe initialization
 * 3. Handles errors properly
 * 4. Thread-safe state management
 */

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "WorkoutViewModel"
    }
    
    // ===== COMPONENTS =====
    private lateinit var pushupCounter: PushupCounter
    private lateinit var poseDetector: MediaPipePoseDetector
    private lateinit var cameraProcessor: CameraProcessor
    
    // ===== STATE FLOWS =====
    private val _pushupResult = MutableStateFlow(PushupResult())
    val pushupResult: StateFlow<PushupResult> = _pushupResult.asStateFlow()
    
    private val _isWorkoutActive = MutableStateFlow(false)
    val isWorkoutActive: StateFlow<Boolean> = _isWorkoutActive.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps.asStateFlow()
    
    private val _workoutDuration = MutableStateFlow(0L)
    val workoutDuration: StateFlow<Long> = _workoutDuration.asStateFlow()
    
    private val _isCameraInitialized = MutableStateFlow(false)
    val isCameraInitialized: StateFlow<Boolean> = _isCameraInitialized.asStateFlow()
    
    // ===== PERFORMANCE TRACKING =====
    private var lastFrameTime = 0L
    private var frameCount = 0
    private var workoutStartTime = 0L
    
    // ===== INITIALIZATION =====
    init {
        android.util.Log.d(TAG, "🔄 Initializing WorkoutViewModel...")
        initializeComponents()
    }
    
    private fun initializeComponents() {
        try {
            // Initialize pushup counter
            pushupCounter = PushupCounter(strictMode = false)
            
            // Initialize pose detector
            poseDetector = MediaPipePoseDetector(
                context = getApplication(),
                onResult = { result, width, height ->
                    handlePoseResult(result, width, height)
                },
                onError = { error ->
                    handleError(error)
                }
            )
            
            poseDetector.initialize()
            
            // Initialize camera processor (Correct Constructor)
            cameraProcessor = CameraProcessor(
                context = getApplication(),
                poseDetector = poseDetector
            )
            
            android.util.Log.d(TAG, "✅ Components initialized")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to initialize components", e)
            _errorMessage.value = "Initialization failed: ${e.message}"
        }
    }
    
    /**
     * ✅ Start camera - MUST be called from UI
     */
    fun startCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner
    ) {
        try {
            android.util.Log.d(TAG, "🎥 Starting camera...")
            
            // Call startCamera on existing processor
            cameraProcessor.startCamera(
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                onError = { error ->
                    handleError(error)
                }
            )
            
            _isCameraInitialized.value = true
            android.util.Log.d(TAG, "✅ Camera started")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to start camera", e)
            _errorMessage.value = "Camera failed: ${e.message}"
        }
    }

    fun processFrame(result: PoseLandmarkerResult?, width: Int, height: Int) {
        handlePoseResult(result, width, height)
    }
    
    /**
     * ✅ Handle pose detection results - THE KEY CONNECTION!
     */
    private fun handlePoseResult(
        result: PoseLandmarkerResult?,
        width: Int,
        height: Int
    ) {
        viewModelScope.launch {
            try {
                // Update FPS
                updateFPS()
                
                // Update workout duration
                if (_isWorkoutActive.value && workoutStartTime > 0) {
                    _workoutDuration.value = System.currentTimeMillis() - workoutStartTime
                }
                
                // Process with pushup counter
                if (result != null) {
                    // Debug: Check if person detected
                    val landmarks = result.landmarks()
                    if (landmarks.isNotEmpty()) {
                        // Process frame
                        pushupCounter.processFrame(result, width, height)
                        
                        val currentRes = pushupCounter.getCurrentResult()
                        
                        // Heartbeat log every 30 frames
                        if (frameCount % 30 == 0) {
                            android.util.Log.d(TAG, "💓 ALIVE | State: ${currentRes.state} | Angles: L=${currentRes.leftElbowAngle}° R=${currentRes.rightElbowAngle}° | Count: ${currentRes.count}")
                        }
                        
                        // Update UI with latest result
                        _pushupResult.value = currentRes
                        
                        // Update workout active state based on pushup state
                        _isWorkoutActive.value = currentRes.state != PushupState.IDLE
                    } else {
                        if (frameCount % 30 == 0) android.util.Log.d(TAG, "⚠️ No person in frame")
                        // Still update with empty result
                        _pushupResult.value = pushupCounter.getCurrentResult()
                    }
                } else {
                    android.util.Log.d(TAG, "⚠️ Null result from detector")
                }
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Error processing pose result", e)
            }
        }
    }
    
    /**
     * Update FPS calculation
     */
    private fun updateFPS() {
        val currentTime = System.currentTimeMillis()
        frameCount++
        
        if (currentTime - lastFrameTime >= 1000) {
            _fps.value = frameCount
            frameCount = 0
            lastFrameTime = currentTime
        }
    }
    
    /**
     * Start workout session
     */
    fun startWorkout() {
        android.util.Log.d(TAG, "▶️ Starting workout")
        
        _isWorkoutActive.value = true
        workoutStartTime = System.currentTimeMillis()
        _workoutDuration.value = 0L
        
        pushupCounter.reset()
        clearError()
    }
    
    /**
     * Pause workout
     */
    fun pauseWorkout() {
        android.util.Log.d(TAG, "⏸️ Pausing workout")
        _isWorkoutActive.value = false
    }
    
    /**
     * Resume workout
     */
    fun resumeWorkout() {
        android.util.Log.d(TAG, "▶️ Resuming workout")
        _isWorkoutActive.value = true
        
        if (workoutStartTime == 0L) {
            workoutStartTime = System.currentTimeMillis()
        }
    }
    
    /**
     * End workout and get summary
     */
    fun endWorkout(): WorkoutSummary {
        val result = _pushupResult.value
        val duration = _workoutDuration.value
        
        _isWorkoutActive.value = false
        workoutStartTime = 0L
        
        return WorkoutSummary(
            totalReps = result.count,
            duration = duration,
            pushupType = result.pushupType,
            averageFormScore = result.formScore,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Reset counter
     */
    fun resetCounter() {
        android.util.Log.d(TAG, "🔄 Resetting counter")
        
        pushupCounter.reset()
        workoutStartTime = 0L
        _workoutDuration.value = 0L
        _pushupResult.value = PushupResult()
    }
    
    /**
     * Handle errors
     */
    fun handleError(error: String) {
        android.util.Log.e(TAG, "❌ Error: $error")
        _errorMessage.value = error
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Get current workout statistics
     */
    fun getCurrentStats(): WorkoutStats {
        val result = _pushupResult.value
        
        return WorkoutStats(
            count = result.count,
            formScore = result.formScore,
            currentState = result.state,
            feedback = if (result.feedback.isNotEmpty()) listOf(result.feedback) else emptyList(),
            leftElbowAngle = result.leftElbowAngle.toInt(),
            rightElbowAngle = result.rightElbowAngle.toInt(),
            repTime = 0f // repTime temporarily removed from PushupResult
        )
    }
    
    /**
     * Toggle strict mode
     */
    fun toggleStrictMode() {
        pushupCounter.toggleStrictMode()
    }
    
    /**
     * Clean up resources
     */
    override fun onCleared() {
        super.onCleared()
        android.util.Log.d(TAG, "🔒 Cleaning up resources...")
        
        try {
            cameraProcessor.stopCamera()
            poseDetector.close()
            android.util.Log.d(TAG, "✅ Resources cleaned up")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error during cleanup", e)
        }
    }
}

/**
 * Workout Statistics - Real-time data
 */
data class WorkoutStats(
    val count: Int,
    val formScore: Float,
    val currentState: PushupState,
    val feedback: List<String>,
    val leftElbowAngle: Int,
    val rightElbowAngle: Int,
    val repTime: Float
)

/**
 * Workout Summary - Final results
 */
data class WorkoutSummary(
    val totalReps: Int,
    val duration: Long,
    val pushupType: PushupType,
    val averageFormScore: Float,
    val timestamp: Long
) {
    fun getDurationSeconds(): Int = (duration / 1000).toInt()
    
    fun getDurationFormatted(): String {
        val seconds = getDurationSeconds()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }
}
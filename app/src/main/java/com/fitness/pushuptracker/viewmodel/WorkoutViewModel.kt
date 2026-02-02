package com.fitness.pushuptracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.pushuptracker.detector.PushupCounter
import com.fitness.pushuptracker.detector.PushupResult
import com.fitness.pushuptracker.detector.PushupState
import com.fitness.pushuptracker.detector.PushupType
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Workout ViewModel - State Management
 * ====================================
 * Manages workout state and coordinates between camera and counter
 * - Thread-safe state updates
 * - Lifecycle-aware processing
 * - Performance monitoring
 */

class WorkoutViewModel : ViewModel() {
    
    // Pushup counter instance
    private val pushupCounter = PushupCounter(strictMode = false)
    
    // Workout state flows (UI observes these)
    private val _pushupResult = MutableStateFlow(PushupResult())
    val pushupResult: StateFlow<PushupResult> = _pushupResult.asStateFlow()
    
    private val _isWorkoutActive = MutableStateFlow(false)
    val isWorkoutActive: StateFlow<Boolean> = _isWorkoutActive.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps.asStateFlow()
    
    // Performance tracking
    private val frameTimestamps = ArrayDeque<Long>(30)
    private var lastFrameTime = 0L
    
    // Workout session data
    private val _workoutDuration = MutableStateFlow(0L)
    val workoutDuration: StateFlow<Long> = _workoutDuration.asStateFlow()
    
    private var workoutStartTime = 0L
    
    init {
        // Observe counter results
        viewModelScope.launch {
            pushupCounter.pushupResult.collect { result ->
                _pushupResult.value = result
                
                // Update workout active state based on pushup state
                _isWorkoutActive.value = result.state != PushupState.IDLE
            }
        }
    }
    
    /**
     * Process a frame from MediaPipe
     * Called from camera background thread
     */
    fun processFrame(result: PoseLandmarkerResult, width: Int, height: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                // Debug logs
                val landmarks = result.landmarks()
                val count = landmarks.size
                if (count > 0) {
                    // Use presence() for detection confidence (score)
                    val confidence = landmarks.firstOrNull()?.firstOrNull()?.presence()?.orElse(0f) ?: 0f
                    // Only log every few frames or on state change to avoid spam
                     android.util.Log.d("ViewModelDebug", "Processed frame: $count landmarks, confidence: $confidence")
                }

                // Track performance
                updateFPS()
                
                // Process with counter
                pushupCounter.processFrame(result, width, height)
                
                // Update workout duration
                if (_isWorkoutActive.value && workoutStartTime > 0) {
                    _workoutDuration.value = System.currentTimeMillis() - workoutStartTime
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Processing error: ${e.message}"
                }
            }
        }
    }
    
    /**
     * Start a new workout session
     */
    fun startWorkout() {
        viewModelScope.launch {
            pushupCounter.reset()
            workoutStartTime = System.currentTimeMillis()
            _workoutDuration.value = 0L
            _isWorkoutActive.value = true
            clearError()
        }
    }
    
    /**
     * Pause the current workout
     */
    fun pauseWorkout() {
        _isWorkoutActive.value = false
    }
    
    /**
     * Resume the workout
     */
    fun resumeWorkout() {
        if (workoutStartTime == 0L) {
            workoutStartTime = System.currentTimeMillis()
        }
        _isWorkoutActive.value = true
    }
    
    /**
     * End the workout and get final stats
     */
    fun endWorkout(): WorkoutSummary {
        val result = _pushupResult.value
        val duration = _workoutDuration.value
        
        _isWorkoutActive.value = false
        
        return WorkoutSummary(
            totalReps = result.count,
            duration = duration,
            pushupType = result.pushupType,
            averageFormScore = result.formScore,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Reset the counter
     */
    fun resetCounter() {
        viewModelScope.launch {
            pushupCounter.reset()
            workoutStartTime = 0L
            _workoutDuration.value = 0L
        }
    }
    
    /**
     * Toggle strict mode
     */
    fun toggleStrictMode() {
        // Would need to recreate counter with new mode
        // For now, just reset
        resetCounter()
    }
    
    /**
     * Update FPS calculation
     */
    private fun updateFPS() {
        val currentTime = System.currentTimeMillis()
        
        if (lastFrameTime > 0) {
            frameTimestamps.addLast(currentTime)
            
            // Keep only last 30 frames
            while (frameTimestamps.size > 30) {
                frameTimestamps.removeFirst()
            }
            
            // Calculate FPS
            if (frameTimestamps.size >= 2) {
                val timeDiff = frameTimestamps.last() - frameTimestamps.first()
                if (timeDiff > 0) {
                    _fps.value = ((frameTimestamps.size - 1) * 1000 / timeDiff).toInt()
                }
            }
        }
        
        lastFrameTime = currentTime
    }
    
    /**
     * Handle errors from camera or detector
     */
    fun handleError(error: String) {
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
            feedback = result.feedback,
            leftElbowAngle = result.leftElbowAngle,
            rightElbowAngle = result.rightElbowAngle,
            repTime = result.repTime
        )
    }
    
    /**
     * Cleanup when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        // Cleanup if needed
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

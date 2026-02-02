package com.fitness.pushuptracker.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.fitness.pushuptracker.detector.MediaPipePoseDetector
import kotlinx.coroutines.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraX Processor - Android Camera Management
 * ==============================================
 * Handles camera lifecycle, preview, and frame analysis
 * - Uses CameraX for lifecycle-aware camera management
 * - Optimized resolution for mobile (480x640)
 * - Processes frames on background executor
 * - Automatic camera switching (front/back)
 */

class CameraProcessor(
    private val context: Context,
    private val poseDetector: MediaPipePoseDetector
) {
    
    private var cameraExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private var isProcessing = false
    
    companion object {
        // MediaPipe works best with 640x480
        private val TARGET_RESOLUTION = Size(640, 480)
        
        // Frame processing strategy
        private const val BACKPRESSURE_STRATEGY = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
    }
    
    /**
     * Start the camera and bind to lifecycle
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onError: (String) -> Unit
    ) {
        // Create background executor for camera operations
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView)
            } catch (e: Exception) {
                onError("Failed to start camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    /**
     * Bind camera use cases (preview + analysis)
     */
    private fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val cameraProvider = cameraProvider ?: return
        
        // Unbind all use cases before rebinding
        cameraProvider.unbindAll()
        
        // Select camera (front or back)
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        
        // Preview use case
        preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
        
        // Image analysis use case
        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480)) // MediaPipe optimized
            .setBackpressureStrategy(BACKPRESSURE_STRATEGY)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888) // Simplify pipeline
            .build()
            .also {
                it.setAnalyzer(
                    cameraExecutor!!,
                    PoseAnalyzer(poseDetector)
                )
            }
        
        try {
            // Bind use cases to lifecycle
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            
            isProcessing = true
            
        } catch (e: Exception) {
            throw Exception("Failed to bind camera: ${e.message}")
        }
    }
    
    /**
     * Switch between front and back camera
     */
    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        
        // Rebind with new camera
        bindCameraUseCases(lifecycleOwner, previewView)
    }
    
    /**
     * Pause frame processing (save battery)
     */
    fun pauseProcessing() {
        isProcessing = false
    }
    
    /**
     * Resume frame processing
     */
    fun resumeProcessing() {
        isProcessing = true
    }
    
    /**
     * Stop camera and cleanup
     */
    fun stopCamera() {
        isProcessing = false
        cameraProvider?.unbindAll()
        cameraExecutor?.shutdown()
        cameraExecutor = null
    }
    
    /**
     * Get current camera state
     */
    fun isProcessingFrames(): Boolean = isProcessing
    
    /**
     * Image Analyzer - Processes each camera frame
     */
    private inner class PoseAnalyzer(
        private val detector: MediaPipePoseDetector
    ) : ImageAnalysis.Analyzer {
        
        private var frameCount = 0
        private var lastProcessedTime = 0L
        private val minFrameInterval = 33L  // ~30 FPS max
        
        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            // Skip processing if paused
            if (!isProcessing) {
                imageProxy.close()
                return
            }
            
            // Throttle frame processing for performance
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastProcessedTime < minFrameInterval) {
                imageProxy.close()
                return
            }
            
            lastProcessedTime = currentTime
            frameCount++
            
            // Log frame captured (sample)
            if (frameCount % 30 == 0) {
                 android.util.Log.e("REAL_TEST", "📸 Camera frame: ${imageProxy.width}x${imageProxy.height} Format:${imageProxy.format}")
            }
            
            // Process frame with MediaPipe
            try {
                detector.detectAsync(imageProxy)
                // ImageProxy.close() is called inside detectAsync()
            } catch (e: Exception) {
                imageProxy.close()
            }
        }
    }
}

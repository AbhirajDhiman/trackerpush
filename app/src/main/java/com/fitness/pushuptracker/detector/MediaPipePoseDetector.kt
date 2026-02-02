package com.fitness.pushuptracker.detector

import android.content.Context
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * MediaPipe Pose Detector - Android Optimized
 * ===========================================
 * Handles pose detection using MediaPipe Android SDK
 * - Uses GPU delegate for performance
 * - Processes frames asynchronously
 * - Manages memory efficiently
 */

class MediaPipePoseDetector(
    private val context: Context,
    private val onResult: (PoseLandmarkerResult, Int, Int) -> Unit,
    private val onError: (String) -> Unit
) {
    
    private var poseLandmarker: PoseLandmarker? = null
    private var isInitialized = false
    
    companion object {
        private const val MODEL_NAME = "pose_landmarker_lite.task"
        private const val MIN_CONFIDENCE = 0.7f
    }
    
    /**
     * Initialize the pose detector
     * Must be called before processing frames
     */
    fun initialize() {
        try {
            android.util.Log.d("MediaPipeDebug", "Initializing MediaPipe...")
            
            // 1. Check if asset file exists
            val assetManager = context.assets
            try {
                assetManager.open(MODEL_NAME).use {
                    android.util.Log.d("MediaPipeDebug", "Model file found: ${it.available()} bytes")
                }
            } catch (e: Exception) {
                android.util.Log.e("MediaPipeDebug", "Model file NOT FOUND in assets: $MODEL_NAME", e)
                onError("Model file missing: $MODEL_NAME")
                return
            }
            
            // 2. Initialize options
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_NAME)
                .setDelegate(Delegate.GPU)  // Use GPU for mobile performance
                .build()
            
            val options = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setMinPoseDetectionConfidence(MIN_CONFIDENCE)
                .setMinPosePresenceConfidence(MIN_CONFIDENCE)
                .setMinTrackingConfidence(MIN_CONFIDENCE)
                .setResultListener { result, image ->
                    handleResult(result, image)
                }
                .setErrorListener { error ->
                    android.util.Log.e("MediaPipeDebug", "MediaPipe Error: ${error.message}")
                    onError("Detection error: ${error.message}")
                }
                .build()
            
            // 3. Create detector
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            isInitialized = true
            android.util.Log.d("MediaPipeDebug", "MediaPipe initialized successfully")
            
        } catch (e: Exception) {
            android.util.Log.e("MediaPipeDebug", "Failed to initialize pose detector", e)
            onError("Failed to initialize pose detector: ${e.message}")
        }
    }
    
    /**
     * Process a camera frame
     * Called from ImageAnalysis.Analyzer on background thread
     */
    fun detectAsync(imageProxy: ImageProxy) {
        if (!isInitialized) {
            imageProxy.close()
            return
        }
        
        try {
            // Log frame arrival (sample every 30 frames to avoid spam)
            if (System.currentTimeMillis() % 1000 < 50) {
                 android.util.Log.d("MediaPipeDebug", "Processing frame: ${imageProxy.width}x${imageProxy.height}")
            }

            // Convert ImageProxy to Bitmap
            val bitmap = imageProxyToBitmap(imageProxy)
            
            // Convert to MPImage
            val mpImage = BitmapImageBuilder(bitmap).build()
            
            // Get timestamp in milliseconds
            val timestampMs = System.currentTimeMillis()
            
            // Detect pose asynchronously
            poseLandmarker?.detectAsync(mpImage, timestampMs)
            
        } catch (e: Exception) {
            android.util.Log.e("MediaPipeDebug", "Frame processing error", e)
            onError("Frame processing error: ${e.message}")
        } finally {
            // CRITICAL: Always close ImageProxy to prevent memory leaks
            imageProxy.close()
        }
    }
    
    /**
     * Handle detection result
     * Called by MediaPipe on result thread
     */
    private fun handleResult(result: PoseLandmarkerResult, image: MPImage) {
        // Log result stats
        val landmarks = result.landmarks()
        if (landmarks.isNotEmpty()) {
             // android.util.Log.d("MediaPipeDebug", "Result received: ${landmarks.size} pose(s) detected")
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val width = image.width
                val height = image.height
                onResult(result, width, height)
            } catch (e: Exception) {
                onError("Result handling error: ${e.message}")
            }
        }
    }
    
    /**
     * Convert ImageProxy to Bitmap
     * Handles rotation and format conversion
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        // Handle RGBA_8888 format (from CameraX RGBA output)
        if (imageProxy.format == android.graphics.PixelFormat.RGBA_8888) {
            val bitmap = Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )
            imageProxy.planes[0].buffer.rewind()
            bitmap.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
            
            // Handle rotation
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            return if (rotationDegrees != 0) {
                rotateBitmap(bitmap, rotationDegrees.toFloat())
            } else {
                bitmap
            }
        }
    
        // Fallback for YUV_420_888
        val bitmap = Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )
        
        // Convert YUV to RGB
        val yuvToRgbConverter = YuvToRgbConverter(context)
        yuvToRgbConverter.yuvToRgb(imageProxy.image!!, bitmap)
        
        // Handle rotation
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            rotateBitmap(bitmap, rotationDegrees.toFloat())
        } else {
            bitmap
        }
    }
    
    /**
     * Rotate bitmap to correct orientation
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply {
            postRotate(degrees)
        }
        
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        ).also {
            // Recycle old bitmap to free memory
            if (it != bitmap) {
                bitmap.recycle()
            }
        }
    }
    
    /**
     * Cleanup resources
     * Call in onDestroy()
     */
    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
        isInitialized = false
    }
}

/**
 * YUV to RGB Converter
 * Converts camera YUV_420_888 format to RGB bitmap
 */
class YuvToRgbConverter(private val context: Context) {
    
    fun yuvToRgb(yuvImage: android.media.Image, outputBitmap: Bitmap) {
        try {
            val yBuffer = yuvImage.planes[0].buffer
            val uBuffer = yuvImage.planes[1].buffer
            val vBuffer = yuvImage.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)

            // U and V are swapped
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage420 = android.graphics.YuvImage(
                nv21,
                android.graphics.ImageFormat.NV21,
                yuvImage.width,
                yuvImage.height,
                null
            )
            
            val out = java.io.ByteArrayOutputStream()
            yuvImage420.compressToJpeg(
                android.graphics.Rect(0, 0, yuvImage.width, yuvImage.height),
                100,
                out
            )
            
            val imageBytes = out.toByteArray()
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            
            // Copy to output bitmap
            val canvas = android.graphics.Canvas(outputBitmap)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            bitmap.recycle()
            
        } catch (e: Exception) {
            // If conversion fails, fill with black (better than crash)
            outputBitmap.eraseColor(android.graphics.Color.BLACK)
        }
    }
}

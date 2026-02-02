package com.fitness.pushuptracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fitness.pushuptracker.camera.CameraProcessor
import com.fitness.pushuptracker.detector.MediaPipePoseDetector
import com.fitness.pushuptracker.ui.WorkoutScreen
import com.fitness.pushuptracker.ui.theme.PushupTrackerTheme
import com.fitness.pushuptracker.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

/**
 * Main Activity - Android App Entry Point
 * =======================================
 * Handles:
 * - Camera permissions
 * - Lifecycle management
 * - Component initialization
 * - UI rendering
 */

class MainActivity : ComponentActivity() {

    // ViewModel for workout state
    private val viewModel: WorkoutViewModel by viewModels()

    // Camera and detector components
    private var cameraProcessor: CameraProcessor? = null
    private var poseDetector: MediaPipePoseDetector? = null

    // Permission state
    private var hasCameraPermission = false

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            initializeComponents()
        } else {
            viewModel.handleError("Camera permission is required for pushup tracking")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check and request camera permission
        checkCameraPermission()

        // Set up UI with Compose
        setContent {
            PushupTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (hasCameraPermission) {
                        WorkoutScreen(
                            viewModel = viewModel,
                            onCameraReady = { previewView ->
                                startCamera(previewView)
                            }
                        )
                    } else {
                        PermissionRequiredScreen(
                            onRequestPermission = {
                                requestCameraPermission()
                            }
                        )
                    }
                }
            }
        }

        // Handle lifecycle states
        // Handle lifecycle states - Resume when STARTED, Pause when STOPPED
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // App is visible - resume processing
                cameraProcessor?.resumeProcessing()
                
                try {
                    // Keep the coroutine alive until the lifecycle falls below STARTED (i.e., onStop)
                    kotlinx.coroutines.awaitCancellation()
                } finally {
                    // App is in background - pause processing (save battery)
                    cameraProcessor?.pauseProcessing()
                }
            }
        }
    }

    /**
     * Check if camera permission is granted
     */
    private fun checkCameraPermission() {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            initializeComponents()
        }
    }

    /**
     * Request camera permission from user
     */
    private fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    /**
     * Initialize MediaPipe and camera components
     */
    private fun initializeComponents() {
        try {
            // Initialize MediaPipe pose detector
            poseDetector = MediaPipePoseDetector(
                context = this,
                onResult = { result, width, height ->
                    // Process result in ViewModel
                    viewModel.processFrame(result, width, height)
                },
                onError = { error ->
                    viewModel.handleError(error)
                }
            )

            poseDetector?.initialize()

            // Initialize camera processor
            cameraProcessor = CameraProcessor(
                context = this,
                poseDetector = poseDetector!!
            )

        } catch (e: Exception) {
            viewModel.handleError("Initialization failed: ${e.message}")
        }
    }

    /**
     * Start camera with preview
     */
    private fun startCamera(previewView: androidx.camera.view.PreviewView) {
        cameraProcessor?.startCamera(
            lifecycleOwner = this,
            previewView = previewView,
            onError = { error ->
                viewModel.handleError(error)
            }
        )
    }

    /**
     * Handle configuration changes (rotation, etc.)
     */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Camera will automatically handle rotation
    }

    /**
     * Cleanup when activity is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        cameraProcessor?.stopCamera()
        poseDetector?.close()
    }

    /**
     * Handle low memory situations
     */
    override fun onLowMemory() {
        super.onLowMemory()
        // Reduce processing if needed
        cameraProcessor?.pauseProcessing()
    }
}

/**
 * Permission Required Screen
 * Shows when camera permission is not granted
 */
@Composable
fun PermissionRequiredScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "This app needs camera access to track your pushups and provide real-time form feedback.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "• No video recording\n• No image storage\n• Privacy respected",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
package com.fitness.pushuptracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fitness.pushuptracker.ui.WorkoutScreen
import com.fitness.pushuptracker.ui.theme.PushupTrackerTheme
import com.fitness.pushuptracker.viewmodel.WorkoutViewModel

/**
 * Main Activity - Android App Entry Point
 * =======================================
 * Handles:
 * - Camera permissions
 * - UI rendering
 */
class MainActivity : ComponentActivity() {

    // ViewModel for workout state
    private val viewModel: WorkoutViewModel by viewModels()

    // Permission state
    private var hasCameraPermission by mutableStateOf(false)

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
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
                                viewModel.startCamera(previewView, this)
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
    }

    /**
     * Check if camera permission is granted
     */
    private fun checkCameraPermission() {
        hasCameraPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request camera permission from user
     */
    private fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
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
            text = """• No video recording
• No image storage
• Privacy respected""",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
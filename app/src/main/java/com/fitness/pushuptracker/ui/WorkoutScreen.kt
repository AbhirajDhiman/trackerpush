package com.fitness.pushuptracker.ui

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fitness.pushuptracker.detector.PushupState
import com.fitness.pushuptracker.viewmodel.WorkoutViewModel

/**
 * Workout Screen - Main UI
 * ========================
 * Displays:
 * - Camera preview
 * - Pushup count
 * - Form feedback
 * - Real-time metrics
 * - Controls
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onCameraReady: (PreviewView) -> Unit
) {
    val pushupResult by viewModel.pushupResult.collectAsState()
    val isWorkoutActive by viewModel.isWorkoutActive.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val fps by viewModel.fps.collectAsState()
    val workoutDuration by viewModel.workoutDuration.collectAsState()
    
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pushup Tracker") },
                actions = {
                    // FPS indicator
                    Text(
                        text = "$fps FPS",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Camera Preview (full screen)
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                        onCameraReady(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Overlay UI
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top info panel
                TopInfoPanel(
                    count = pushupResult.count,
                    type = pushupResult.pushupType.displayName,
                    state = pushupResult.state,
                    duration = workoutDuration
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Feedback panel
                if (pushupResult.feedback.isNotEmpty() || pushupResult.invalidReasons.isNotEmpty()) {
                    FeedbackPanel(
                        feedback = pushupResult.feedback,
                        invalidReasons = pushupResult.invalidReasons
                    )
                }
                
                // Metrics panel
                MetricsPanel(
                    leftElbowAngle = pushupResult.leftElbowAngle,
                    rightElbowAngle = pushupResult.rightElbowAngle,
                    formScore = pushupResult.formScore,
                    repTime = pushupResult.repTime
                )
                
                // Control buttons
                ControlPanel(
                    isWorkoutActive = isWorkoutActive,
                    onStart = { viewModel.startWorkout() },
                    onPause = { viewModel.pauseWorkout() },
                    onResume = { viewModel.resumeWorkout() },
                    onReset = { viewModel.resetCounter() }
                )
            }
            
            // Error message
            errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

/**
 * Top Info Panel - Count and Status
 */
@Composable
fun TopInfoPanel(
    count: Int,
    type: String,
    state: PushupState,
    duration: Long
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Count
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = Color(0xFF00FF00),
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "PUSHUPS",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Type
            Text(
                text = type,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            
            // State
            StateChip(state = state)
            
            // Duration
            if (duration > 0) {
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * State Chip - Shows current pushup state
 */
@Composable
fun StateChip(state: PushupState) {
    val (color, text) = when (state) {
        PushupState.IDLE -> Color.Gray to "Position Yourself"
        PushupState.SETUP -> Color(0xFFFF9800) to "Setting Up"
        PushupState.READY -> Color(0xFF00FFFF) to "Ready"
        PushupState.DESCENDING -> Color(0xFFFF00FF) to "Going Down"
        PushupState.BOTTOM -> Color.Red to "At Bottom"
        PushupState.ASCENDING -> Color.Yellow to "Going Up"
        PushupState.TOP -> Color.Green to "At Top"
        PushupState.COUNTED -> Color.Green to "Rep Counted!"
    }
    
    Surface(
        modifier = Modifier.padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.8f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black
        )
    }
}

/**
 * Feedback Panel - Form feedback and errors
 */
@Composable
fun FeedbackPanel(
    feedback: List<String>,
    invalidReasons: List<String>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Valid feedback
            if (feedback.isNotEmpty()) {
                Text(
                    text = "FEEDBACK",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00FFFF),
                    fontWeight = FontWeight.Bold
                )
                
                feedback.take(3).forEach { msg ->
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• ",
                            color = Color.White
                        )
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Invalid reasons
            if (invalidReasons.isNotEmpty()) {
                if (feedback.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color.White.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Text(
                    text = "INVALID REP",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
                
                invalidReasons.take(2).forEach { reason ->
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✗ ",
                            color = Color.Red
                        )
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Metrics Panel - Angles and form score
 */
@Composable
fun MetricsPanel(
    leftElbowAngle: Int,
    rightElbowAngle: Int,
    formScore: Float,
    repTime: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Elbow angles
            MetricItem(
                label = "Left",
                value = "${leftElbowAngle}°"
            )
            
            MetricItem(
                label = "Right",
                value = "${rightElbowAngle}°"
            )
            
            // Form score
            MetricItem(
                label = "Form",
                value = "${(formScore * 100).toInt()}%",
                color = when {
                    formScore > 0.8f -> Color.Green
                    formScore > 0.5f -> Color.Yellow
                    else -> Color.Red
                }
            )
            
            // Rep time
            if (repTime > 0) {
                MetricItem(
                    label = "Time",
                    value = "${String.format("%.1f", repTime)}s"
                )
            }
        }
    }
}

/**
 * Metric Item - Individual metric display
 */
@Composable
fun MetricItem(
    label: String,
    value: String,
    color: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

/**
 * Control Panel - Workout controls
 */
@Composable
fun ControlPanel(
    isWorkoutActive: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Start/Pause button
            if (!isWorkoutActive) {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Green
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start")
                }
            } else {
                Button(
                    onClick = onPause,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pause")
                }
            }
            
            // Reset button
            OutlinedButton(
                onClick = onReset,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset")
            }
        }
    }
}

/**
 * Format duration in MM:SS
 */
fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}

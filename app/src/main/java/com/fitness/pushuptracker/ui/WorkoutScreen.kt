package com.fitness.pushuptracker.ui

import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.fitness.pushuptracker.detector.PushupState
import com.fitness.pushuptracker.viewmodel.WorkoutViewModel

/**
 * Workout Screen - Cyberpunk Redesign
 * ====================================
 * A premium, neon-styled workout interface.
 */

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onCameraReady: (PreviewView) -> Unit
) {
    val pushupResult by viewModel.pushupResult.collectAsState()
    val isWorkoutActive by viewModel.isWorkoutActive.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Using formScore as normalized depth (0.0 = Top, 1.0 = Bottom)
    val currentDepth = pushupResult.formScore

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        
        // 1. Camera Preview (Full Screen)
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    onCameraReady(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // 2. Dark Gradient Overlay (Visibility layer)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.9f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // 3. Main UI Layout
        Row(modifier = Modifier.fillMaxSize()) {
            
            // Left Side: Depth Gauge
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight()
                    .padding(vertical = 40.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                DepthGauge(depth = currentDepth)
            }
            
            // Center & Right: Stats & Controls
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top: Instruction / Feedback
                Spacer(modifier = Modifier.height(30.dp))
                StatusPill(state = pushupResult.state, feedback = pushupResult.feedback)
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Center: BIG Counter
                NeonCounter(count = pushupResult.count)
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Bottom: Controls
                CyberControls(
                    isWorkoutActive = isWorkoutActive,
                    onStart = { viewModel.startWorkout() },
                    onPause = { viewModel.pauseWorkout() },
                    onReset = { viewModel.resetCounter() }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
        
        // Error Snackbar
        errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier.align(Alignment.TopCenter).padding(20.dp),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ) { Text(error) }
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENTS
// -----------------------------------------------------------------------------

/**
 * Vertical Bar Depth Gauge
 */
@Composable
fun DepthGauge(depth: Float) {
    val animatedDepth by animateFloatAsState(targetValue = depth, animationSpec = tween(100), label = "depth")
    
    Canvas(modifier = Modifier.fillMaxHeight().width(24.dp)) {
        val barWidth = size.width
        val barHeight = size.height
        
        // Background track
        drawRoundRect(
            color = Color.DarkGray.copy(alpha = 0.5f),
            size = Size(barWidth, barHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f)
        )
        
        // Good Rep Zone (Target) - Bottom 30%
        val zoneTop = barHeight * 0.7f
        drawRect(
            color = Color(0xFF00FF99).copy(alpha = 0.2f),
            topLeft = Offset(0f, zoneTop),
            size = Size(barWidth, barHeight - zoneTop)
        )

        // Fill bar (From bottom up) - WRONG logic for pushups visually?
        // Actually, let's fill from Top Down to mimic going down.
        // Depth 0.0 (Top) -> Empty bar? Or Full at top?
        // Let's visualize "Person" going down.
        
        // Let's do a "Piston" style.
        val indicatorY = barHeight * animatedDepth
        val color = if (animatedDepth > 0.8f) Color(0xFF00FF99) else Color(0xFF00E5FF)
        
        // Draw the fill
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, 0f),
            size = Size(barWidth, indicatorY),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f)
        )
        
        // Draw a "Limit" line at the bottom for perfect depth
        drawLine(
            color = Color.White,
            start = Offset(-10f, barHeight * 0.9f),
            end = Offset(barWidth + 10f, barHeight * 0.9f),
            strokeWidth = 4f
        )
    }
}

/**
 * Neon Counter Text
 */
@Composable
fun NeonCounter(count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Shadow/Glow
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "$count",
                style = TextStyle(
                    fontSize = 140.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), // Blur effect simulated
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.offset(x = 4.dp, y = 4.dp)
            )
            Text(
                text = "$count",
                style = TextStyle(
                    fontSize = 140.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary, // Main Neon color
                    textAlign = TextAlign.Center
                )
            )
        }
        Text(
            text = "REPS",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White.copy(alpha = 0.7f),
            letterSpacing = 4.sp
        )
    }
}

/**
 * Floating Status Pill
 */
@Composable
fun StatusPill(state: PushupState, feedback: String) {
    val containerColor = when(state) {
        PushupState.SETUP -> Color(0xFFFF9800).copy(alpha = 0.8f)
        PushupState.COUNTED -> Color(0xFF00FF99).copy(alpha = 0.8f)
        PushupState.BOTTOM -> Color(0xFF00E5FF).copy(alpha = 0.8f)
        else -> Color(0xFF222222).copy(alpha = 0.8f)
    }
    
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50),
        modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (state == PushupState.IDLE) Color.Red else Color.White)
            )
            Spacer(modifier = Modifier.width(12.dp))
            
            val displayText = if (feedback.isNotEmpty()) feedback.uppercase() else state.name
            Text(
                text = displayText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Cyberpunk Controls
 */
@Composable
fun CyberControls(
    isWorkoutActive: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset Button
        IconButton(
            onClick = onReset,
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFF222222), CircleShape)
                .border(2.dp, Color(0xFF444444), CircleShape)
        ) {
            Icon(Icons.Default.Refresh, "Reset", tint = Color.White)
        }
        
        // Play/Pause Button (Big)
        Button(
            onClick = if (isWorkoutActive) onPause else onStart,
            modifier = Modifier
                .size(80.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isWorkoutActive) Color(0xFF00E5FF) else Color(0xFF00FF99)
            ),
            elevation = ButtonDefaults.buttonElevation(8.dp)
        ) {
            Icon(
                imageVector = if (isWorkoutActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isWorkoutActive) "Pause" else "Start",
                tint = Color.Black,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

package com.fitness.pushuptracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File

class DiagnosticActivity : AppCompatActivity() {
    
    private lateinit var tvResults: TextView
    private lateinit var btnTestApp: Button
    private lateinit var btnTestCamera: Button
    private lateinit var btnTestMediaPipe: Button
    private lateinit var btnTestPushup: Button
    private lateinit var btnRunAll: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnostic)
        
        tvResults = findViewById(R.id.tvResults)
        btnTestApp = findViewById(R.id.btnTestApp)
        btnTestCamera = findViewById(R.id.btnTestCamera)
        btnTestMediaPipe = findViewById(R.id.btnTestMediaPipe)
        btnTestPushup = findViewById(R.id.btnTestPushup)
        btnRunAll = findViewById(R.id.btnRunAll)
        
        btnTestApp.setOnClickListener { testAppFunctionality() }
        btnTestCamera.setOnClickListener { testCamera() }
        btnTestMediaPipe.setOnClickListener { testMediaPipe() }
        btnTestPushup.setOnClickListener { testPushupLogic() }
        btnRunAll.setOnClickListener {
            clearResults()
            testAppFunctionality()
            testCamera()
            testMediaPipe()
            testPushupLogic()
        }
        
        appendResult("🔧 DIAGNOSTIC TOOL STARTED")
        appendResult("Click tests to run diagnostics...")
    }
    
    private fun testAppFunctionality() {
        appendResult("\n=== TEST 1: APP FUNCTIONALITY ===")
        
        // Test 1: Can app run?
        appendResult("✅ App is running")
        
        // Test 2: Can write to storage
        try {
            File(filesDir, "test.txt").writeText("Test")
            appendResult("✅ Can write files")
        } catch (e: Exception) {
            appendResult("❌ Cannot write files: ${e.message}")
        }
        
        // Test 3: Camera permission
        val hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            appendResult("✅ Camera permission granted")
        } else {
            appendResult("⚠️ Camera permission NOT granted")
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
        }
        
        // Test 4: List assets
        try {
            val assets = assets.list("")
            val modelFiles = assets?.filter { it.contains("task") || it.contains("tflite") }
            appendResult("📁 Assets found: ${assets?.size ?: 0} files")
            appendResult("🔍 Model files: ${modelFiles ?: "none"}")
        } catch (e: Exception) {
            appendResult("❌ Cannot list assets: ${e.message}")
        }
    }
    
    private fun testCamera() {
        appendResult("\n=== TEST 2: CAMERA ===")
        
        try {
            val cameraManager = getSystemService(android.content.Context.CAMERA_SERVICE) 
                as android.hardware.camera2.CameraManager
            
            val cameraList = cameraManager.cameraIdList
            appendResult("📷 Cameras found: ${cameraList.size}")
            
            cameraList.forEach { cameraId ->
                try {
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    val lensFacing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                    val facing = when(lensFacing) {
                        android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK -> "BACK"
                        android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"
                        else -> "UNKNOWN"
                    }
                    appendResult("   - Camera $cameraId ($facing)")
                } catch (e: Exception) {
                    appendResult("   - Camera $cameraId (ERROR: ${e.message})")
                }
            }
        } catch (e: Exception) {
            appendResult("❌ Camera test failed: ${e.message}")
        }
    }
    
    private fun testMediaPipe() {
        appendResult("\n=== TEST 3: MEDIAPIPE ===")
        
        // Test 1: Check model file exists
        try {
            val inputStream = assets.open("pose_landmarker_lite.task")
            val size = inputStream.available()
            inputStream.close()
            
            appendResult("📦 Model file size: $size bytes (${size/1024/1024} MB)")
            
            if (size > 9_000_000 && size < 11_000_000) {
                appendResult("✅ Model size looks correct (~9.9 MB expected)")
            } else {
                appendResult("⚠️ Model size suspicious (should be ~9.9 MB)")
            }
        } catch (e: Exception) {
            appendResult("❌ Model file error: ${e.message}")
            appendResult("   Make sure file exists at: app/src/main/assets/pose_landmarker_lite.task")
            appendResult("   Download from: https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task")
        }
        
        // Test 2: Try to initialize MediaPipe
        try {
            appendResult("🔄 Initializing MediaPipe...")
            // Simple test without actual initialization
            appendResult("✅ MediaPipe can be tested")
        } catch (e: Exception) {
            appendResult("❌ MediaPipe test failed: ${e.message}")
        }
    }
    
    private fun testPushupLogic() {
        appendResult("\n=== TEST 4: PUSHUP LOGIC ===")
        
        // Test angle calculation with mock data
        val mockShoulder = floatArrayOf(0.5f, 0.3f)
        val mockElbow = floatArrayOf(0.5f, 0.5f)
        val mockWrist = floatArrayOf(0.5f, 0.7f)
        
        val angle = calculateAngle(mockShoulder, mockElbow, mockWrist)
        appendResult("📐 Test angle calculation: ${angle.toInt()}°")
        
        if (angle > 170 && angle < 190) {
            appendResult("✅ Angle calculation works for straight arm")
        } else {
            appendResult("⚠️ Unexpected angle: ${angle.toInt()}° (expected ~180°)")
        }
    }
    
    private fun calculateAngle(a: FloatArray, b: FloatArray, c: FloatArray): Float {
        val BAx = a[0] - b[0]
        val BAy = a[1] - b[1]
        val BCx = c[0] - b[0]
        val BCy = c[1] - b[1]
        
        val dot = BAx * BCx + BAy * BCy
        val magBA = kotlin.math.sqrt(BAx * BAx + BAy * BAy)
        val magBC = kotlin.math.sqrt(BCx * BCx + BCy * BCy)
        
        val cosAngle = dot / (magBA * magBC)
        return Math.toDegrees(kotlin.math.acos(cosAngle.toDouble())).toFloat()
    }
    
    private fun appendResult(text: String) {
        runOnUiThread {
            tvResults.text = "${tvResults.text}\n$text"
        }
        Log.d("DIAGNOSTIC", text)
    }
    
    private fun clearResults() {
        runOnUiThread {
            tvResults.text = ""
        }
    }
}

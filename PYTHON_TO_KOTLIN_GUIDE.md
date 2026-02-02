# Python to Kotlin Conversion Reference

This document explains the key differences between the original Python code and the Android Kotlin implementation.

## 🔄 Core Conversions

### 1. Data Structures

| Python | Kotlin | Notes |
|--------|--------|-------|
| `dataclass` | `data class` | Similar syntax |
| `Enum` | `enum class` | Different declaration |
| `deque(maxlen=N)` | `CircularFloatBuffer` | Custom implementation |
| `dict` | `Map<String, FloatArray>` | Typed collections |
| `list` | `List<T>` / `MutableList<T>` | Immutable vs mutable |

```python
# Python
@dataclass
class PushupResult:
    count: int = 0
    feedback: List[str] = field(default_factory=list)
```

```kotlin
// Kotlin
data class PushupResult(
    val count: Int = 0,
    val feedback: List<String> = emptyList()
)
```

### 2. MediaPipe Integration

| Python | Kotlin Android |
|--------|----------------|
| `mp.solutions.pose` | `com.google.mediapipe:tasks-vision` |
| `mp_pose.Pose()` | `PoseLandmarker.createFromOptions()` |
| `pose.process(frame)` | `poseLandmarker.detectAsync()` |
| `results.pose_landmarks` | `result.landmarks()` |
| Synchronous processing | Async with callbacks |

```python
# Python
self.pose = mp_pose.Pose(
    static_image_mode=False,
    model_complexity=0
)
results = self.pose.process(rgb_frame)
```

```kotlin
// Kotlin
val options = PoseLandmarker.PoseLandmarkerOptions.builder()
    .setRunningMode(RunningMode.LIVE_STREAM)
    .setResultListener { result, image -> 
        handleResult(result, image)
    }
    .build()

poseLandmarker = PoseLandmarker.createFromOptions(context, options)
poseLandmarker.detectAsync(mpImage, timestampMs)
```

### 3. Camera Handling

| Python | Kotlin Android |
|--------|----------------|
| `cv2.VideoCapture(0)` | CameraX with ImageAnalysis |
| `cap.read()` | ImageProxy in analyzer |
| `while cap.isOpened()` | Lifecycle-aware processing |
| Blocking loop | Async callback pattern |

```python
# Python
cap = cv2.VideoCapture(0)
while cap.isOpened():
    ret, frame = cap.read()
    result = counter.process_frame(frame)
```

```kotlin
// Kotlin
class PoseAnalyzer(
    private val detector: MediaPipePoseDetector
) : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy) {
        detector.detectAsync(imageProxy)
    }
}

imageAnalyzer = ImageAnalysis.Builder()
    .setAnalyzer(cameraExecutor, PoseAnalyzer(detector))
    .build()
```

### 4. Threading Model

| Python | Kotlin Android |
|--------|----------------|
| Single-threaded | Multi-threaded |
| Main loop | Coroutines + Dispatchers |
| No lifecycle | Lifecycle-aware |
| Blocking operations | Suspending functions |

```python
# Python
def process_frame(self, frame):
    # Runs on main thread
    results = self.pose.process(frame)
    # Process synchronously
```

```kotlin
// Kotlin
fun processFrame(result: PoseLandmarkerResult, width: Int, height: Int) {
    viewModelScope.launch(Dispatchers.Default) {
        // Runs on background thread
        pushupCounter.processFrame(result, width, height)
        
        // Update UI on main thread
        withContext(Dispatchers.Main) {
            _pushupResult.value = result
        }
    }
}
```

### 5. State Management

| Python | Kotlin Android |
|--------|----------------|
| Direct variable updates | StateFlow/MutableStateFlow |
| No observers | Flow collectors |
| Manual UI updates | Reactive UI updates |

```python
# Python
self.count = 0

def update_count(self):
    self.count += 1
    # Manual UI update
```

```kotlin
// Kotlin
private val _count = MutableStateFlow(0)
val count: StateFlow<Int> = _count.asStateFlow()

fun updateCount() {
    _count.value += 1
    // UI automatically updates
}

// In Composable
@Composable
fun CountDisplay(viewModel: WorkoutViewModel) {
    val count by viewModel.count.collectAsState()
    Text("Count: $count")  // Auto-updates
}
```

### 6. UI Rendering

| Python | Kotlin Android |
|--------|----------------|
| `cv2.imshow()` | Jetpack Compose |
| `cv2.rectangle()` | Compose Canvas/Shapes |
| `cv2.putText()` | Text composables |
| OpenCV drawing | Material 3 components |

```python
# Python
cv2.rectangle(frame, (10, 10), (400, 280), (0, 0, 0), -1)
cv2.putText(frame, f'COUNT: {count}', (20, 50),
           cv2.FONT_HERSHEY_SIMPLEX, 1.5, (0, 255, 0), 3)
cv2.imshow('Pushup Counter', frame)
```

```kotlin
// Kotlin
@Composable
fun CountDisplay(count: Int) {
    Card(
        modifier = Modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.displayLarge,
            color = Color.Green
        )
    }
}
```

### 7. Math Operations

| Python | Kotlin |
|--------|--------|
| `numpy.array()` | `FloatArray` |
| `np.arctan2()` | `kotlin.math.atan2()` |
| `np.linalg.norm()` | Manual sqrt calculation |
| `np.abs()` | `kotlin.math.abs()` |

```python
# Python
import numpy as np

a = np.array([x, y])
angle = np.arctan2(c[1]-b[1], c[0]-b[0])
distance = np.linalg.norm(a - b)
```

```kotlin
// Kotlin
import kotlin.math.*

val a = floatArrayOf(x, y)
val angle = atan2(c[1] - b[1], c[0] - b[0])
val distance = sqrt((a[0] - b[0]).pow(2) + (a[1] - b[1]).pow(2))
```

## 🏗️ Architecture Differences

### Python: Sequential Processing
```
┌──────────────┐
│ Main Loop    │
│ while True:  │
└──────┬───────┘
       │
┌──────▼───────┐
│ Read Frame   │
└──────┬───────┘
       │
┌──────▼───────┐
│ Process      │
└──────┬───────┘
       │
┌──────▼───────┐
│ Draw UI      │
└──────┬───────┘
       │
┌──────▼───────┐
│ Display      │
└──────────────┘
```

### Kotlin Android: Reactive Architecture
```
┌────────────────────────┐
│ MainActivity           │
│ (Lifecycle Owner)      │
└───────┬────────────────┘
        │
    ┌───▼───┐  ┌──────────┐
    │Camera │  │ViewModel │
    │Thread │  │ (State)  │
    └───┬───┘  └────┬─────┘
        │           │
    ┌───▼───────────▼───┐
    │   StateFlow       │
    │   (Reactive)      │
    └───┬───────────────┘
        │
    ┌───▼───────┐
    │ Compose   │
    │ (Auto     │
    │  Update)  │
    └───────────┘
```

## 🚀 Performance Optimizations

### Python
- Single-threaded
- No memory constraints
- Desktop performance
- Unlimited resources

### Kotlin Android
- Multi-threaded (Coroutines)
- Strict memory limits (150MB)
- Mobile performance (30 FPS)
- Battery constraints

```kotlin
// Mobile optimizations
- Lower resolution: 480x640 (vs 1280x720)
- GPU delegate for MediaPipe
- Frame throttling (min 33ms)
- Auto-pause on background
- Efficient memory management
```

## 📱 Platform-Specific Features

### Android-Only Components

```kotlin
// 1. Permissions
requestPermissionLauncher.launch(Manifest.permission.CAMERA)

// 2. Lifecycle
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        // Only run when app visible
    }
}

// 3. Context
val context = LocalContext.current

// 4. Resources
getString(R.string.app_name)

// 5. Configuration Changes
android:configChanges="orientation|screenSize"
```

## 🔧 Build System

### Python
```bash
pip install mediapipe opencv-python numpy
python pushup_counter.py
```

### Kotlin Android
```kotlin
// build.gradle.kts
dependencies {
    implementation("com.google.mediapipe:tasks-vision:0.10.10")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}

// Build
./gradlew assembleDebug
```

## 📊 Key Metrics Comparison

| Metric | Python (Desktop) | Kotlin (Android) |
|--------|-----------------|------------------|
| Resolution | 1280x720 | 480x640 |
| FPS Target | No limit | 30 FPS |
| RAM Usage | Unlimited | <150MB |
| Thread Model | Single | Multi (Coroutines) |
| UI Updates | Manual | Reactive (StateFlow) |
| Lifecycle | None | Full lifecycle support |

## ✅ Migration Checklist

When converting from Python to Kotlin Android:

- [ ] Replace `cv2.VideoCapture` → CameraX
- [ ] Replace `mediapipe` → MediaPipe Android SDK
- [ ] Replace `numpy` → Kotlin math/FloatArray
- [ ] Replace `while True` → Lifecycle + Coroutines
- [ ] Replace `cv2.imshow` → Jetpack Compose
- [ ] Add permission handling
- [ ] Add lifecycle management
- [ ] Implement StateFlow for state
- [ ] Use background threads for processing
- [ ] Add proper error handling
- [ ] Optimize for mobile (resolution, FPS)
- [ ] Test on physical device

## 🎯 Summary

The Android version maintains **100% feature parity** with the Python version while adding:

✅ Proper Android architecture
✅ Lifecycle awareness
✅ Thread safety
✅ Reactive UI updates
✅ Permission handling
✅ Mobile performance optimizations
✅ Battery efficiency
✅ Memory management

All core logic (state machine, validation, angles, anti-cheat) is **identical** in functionality but implemented idiomatically in Kotlin for Android.

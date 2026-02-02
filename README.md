# PushupTracker - AI-Powered Pushup Counter

A premium Android application that uses MediaPipe AI to count pushups, detect form, and provide real-time feedback.

## Features

✅ **6 Pushup Type Detection**: Standard, Knee, Wide, Diamond, Decline, Incline  
✅ **Real-Time Form Feedback**: Instant coaching on body alignment and technique  
✅ **Anti-Cheat System**: Detects head-only movement, bouncing, and partial reps  
✅ **Performance Optimized**: 30 FPS processing, < 150MB memory usage  
✅ **Premium UI**: Material3 design with dark mode support  

## Setup Instructions

### 1. Download MediaPipe Model

The app requires the MediaPipe Pose Landmarker model file.

**Download:**
1. Go to: https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task
2. Download the `pose_landmarker_lite.task` file

**Add to Project:**
1. Create directory: `app/src/main/assets/`
2. Place `pose_landmarker_lite.task` in the assets folder

```
app/
└── src/
    └── main/
        └── assets/
            └── pose_landmarker_lite.task  ← Place model here
```

### 2. Build the App

```bash
# Open in Android Studio
# OR build from command line:

# Windows
./gradlew.bat assembleDebug

# Mac/Linux
./gradlew assembleDebug
```

### 3. Run on Device/Emulator

```bash
# Install on connected device
./gradlew.bat installDebug

# OR use Android Studio's Run button (Shift+F10)
```

## Requirements

- **Android Studio**: Arctic Fox or newer
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Camera Permission**: Required for pose detection
- **Device**: Physical device recommended (emulator may have camera issues)

## Project Structure

```
app/src/main/java/com/fitness/pushuptracker/
├── MainActivity.kt              # App entry point
├── detector/
│   ├── MediaPipePoseDetector.kt # MediaPipe integration
│   └── PushupCounter.kt         # Core counting logic
├── camera/
│   └── CameraProcessor.kt       # CameraX management
├── viewmodel/
│   └── WorkoutViewModel.kt      # State management
└── ui/
    ├── WorkoutScreen.kt         # Main UI
    └── theme/
        └── Theme.kt             # Material3 theme
```

## Testing

### Unit Tests
```bash
./gradlew.bat testDebugUnitTest
```

### Instrumented Tests (requires device/emulator)
```bash
./gradlew.bat connectedAndroidTest
```

**Test Coverage:**
- 21 Unit Tests (logic validation)
- 21 USP Feature Tests (type detection, anti-cheat)
- 21 Edge Case Tests (robustness)
- 13 Performance Tests (speed, memory)

## Troubleshooting

### Issue: "Model not found"
**Solution**: Ensure `pose_landmarker_lite.task` is in `app/src/main/assets/`

### Issue: "Camera permission denied"
**Solution**: Grant camera permission in app settings

### Issue: "Emulator crashes"
**Solution**: Use a physical device. MediaPipe + CameraX may not work well on emulators.

### Issue: "Build fails"
**Solution**: 
1. Sync Gradle: File → Sync Project with Gradle Files
2. Clean build: Build → Clean Project
3. Rebuild: Build → Rebuild Project

## Performance Targets

- **Frame Processing**: < 33ms (30 FPS)
- **Memory Usage**: < 150MB
- **Battery Impact**: Minimal (GPU-accelerated)
- **Rep Validation**: 0.5s - 3.0s timing window

## License

MIT License - See LICENSE file for details

## Credits

- **MediaPipe**: Google's ML framework for pose detection
- **CameraX**: Android's camera library
- **Jetpack Compose**: Modern Android UI toolkit

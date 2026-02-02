# Android Project Setup Checklist

Complete this checklist to ensure your pushup tracker Android app is properly configured.

## 📋 Pre-Setup Requirements

### System Requirements
- [ ] Android Studio Hedgehog (2023.1.1) or newer installed
- [ ] JDK 17 or newer installed
- [ ] Android SDK Platform 34 installed
- [ ] At least 4GB RAM available
- [ ] 2GB free disk space

### Device Requirements
- [ ] Physical Android device (API 24+)
- [ ] USB cable for debugging
- [ ] Developer mode enabled on device
- [ ] USB debugging enabled

⚠️ **Important**: Camera features don't work on emulators. You MUST use a physical device.

## 📁 Step 1: Project Creation

- [ ] Open Android Studio
- [ ] Create new project: File → New → New Project
- [ ] Select "Empty Activity"
- [ ] Configure project:
  - Name: `Pushup Tracker`
  - Package: `com.fitness.pushuptracker`
  - Save location: Choose directory
  - Language: `Kotlin`
  - Minimum SDK: `API 24 (Android 7.0)`
  - Build configuration language: `Kotlin DSL (build.gradle.kts)`
- [ ] Click "Finish"
- [ ] Wait for Gradle sync to complete

## 📂 Step 2: File Structure Setup

Create the following directory structure:

```
app/src/main/
├── kotlin/com/fitness/pushuptracker/
│   ├── MainActivity.kt
│   ├── detector/
│   │   ├── PushupCounter.kt
│   │   └── MediaPipePoseDetector.kt
│   ├── camera/
│   │   └── CameraProcessor.kt
│   ├── viewmodel/
│   │   └── WorkoutViewModel.kt
│   └── ui/
│       ├── WorkoutScreen.kt
│       └── theme/
│           └── Theme.kt (auto-generated)
├── res/
│   ├── values/
│   │   └── strings.xml
│   └── xml/
│       └── file_paths.xml
└── assets/
    └── pose_landmarker_lite.task
```

### Create Directories
- [ ] Right-click `app/src/main/kotlin/com/fitness/pushuptracker/`
- [ ] New → Package → `detector`
- [ ] New → Package → `camera`
- [ ] New → Package → `viewmodel`
- [ ] New → Package → `ui`
- [ ] Right-click `app/src/main/` → New → Folder → Assets Folder

## 📄 Step 3: Copy Files

### Core Logic Files
- [ ] Copy `PushupCounter.kt` to `app/src/main/kotlin/com/fitness/pushuptracker/detector/`
- [ ] Copy `MediaPipePoseDetector.kt` to `app/src/main/kotlin/com/fitness/pushuptracker/detector/`
- [ ] Copy `CameraProcessor.kt` to `app/src/main/kotlin/com/fitness/pushuptracker/camera/`
- [ ] Copy `WorkoutViewModel.kt` to `app/src/main/kotlin/com/fitness/pushuptracker/viewmodel/`
- [ ] Copy `WorkoutScreen.kt` to `app/src/main/kotlin/com/fitness/pushuptracker/ui/`
- [ ] Copy `MainActivity.kt` to `app/src/main/kotlin/com/fitness/pushuptracker/` (replace existing)

### Configuration Files
- [ ] Copy `build.gradle.kts` to `app/build.gradle.kts` (replace existing)
- [ ] Copy `AndroidManifest.xml` to `app/src/main/AndroidManifest.xml` (replace existing)

## 🤖 Step 4: Download MediaPipe Model

- [ ] Open browser and navigate to:
  ```
  https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/latest/pose_landmarker_lite.task
  ```
- [ ] Download the file (should be ~12MB)
- [ ] Copy `pose_landmarker_lite.task` to `app/src/main/assets/`
- [ ] Verify file is in correct location

## 🔧 Step 5: Configure Build Files

### app/build.gradle.kts
Verify these key sections exist:

```kotlin
android {
    namespace = "com.fitness.pushuptracker"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.fitness.pushuptracker"
        minSdk = 24
        targetSdk = 34
    }
    
    buildFeatures {
        compose = true
    }
}

dependencies {
    // CameraX
    implementation("androidx.camera:camera-camera2:1.3.1")
    
    // MediaPipe
    implementation("com.google.mediapipe:tasks-vision:0.10.10")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.material3:material3")
}
```

- [ ] Check all dependencies are present
- [ ] Verify version numbers match

### settings.gradle.kts
Ensure repositories are configured:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

- [ ] Verify repositories configuration

## 📱 Step 6: Sync and Build

- [ ] Click "Sync Project with Gradle Files" (elephant icon)
- [ ] Wait for sync to complete (may take 2-5 minutes)
- [ ] Check for errors in "Build" tab
- [ ] If errors occur, check:
  - [ ] Internet connection
  - [ ] Gradle version compatibility
  - [ ] JDK version (should be 17)

### Common Sync Issues

**Issue**: "Could not resolve dependencies"
- [ ] Check internet connection
- [ ] File → Invalidate Caches → Invalidate and Restart

**Issue**: "Unsupported class file version"
- [ ] File → Project Structure → SDK Location
- [ ] Set Gradle JDK to JDK 17

**Issue**: "Cannot resolve symbol"
- [ ] Build → Clean Project
- [ ] Build → Rebuild Project

## 📱 Step 7: Device Setup

### Enable Developer Mode
- [ ] On device: Settings → About Phone
- [ ] Tap "Build Number" 7 times
- [ ] Developer mode enabled message appears

### Enable USB Debugging
- [ ] Settings → System → Developer Options
- [ ] Enable "USB Debugging"
- [ ] Connect device via USB
- [ ] Authorize computer when prompted

### Verify Connection
- [ ] In Android Studio: Run → Select Device
- [ ] Your device should appear in list
- [ ] If not, check:
  - [ ] USB cable is data cable (not charge-only)
  - [ ] USB debugging is enabled
  - [ ] Driver installed (Windows only)

## 🚀 Step 8: First Run

- [ ] Click "Run" button (green triangle) or Shift+F10
- [ ] Select your physical device
- [ ] Wait for app to build and install (1-3 minutes first time)
- [ ] App should launch on device

### Expected Behavior on First Run
- [ ] Permission dialog appears
- [ ] Tap "Grant Permission" or "Allow"
- [ ] Camera preview appears
- [ ] UI shows "Position yourself in frame"
- [ ] No crashes or errors

## ✅ Step 9: Functionality Test

### Test Camera
- [ ] Camera preview is visible
- [ ] Preview is not upside down or mirrored incorrectly
- [ ] FPS counter shows 25-32 FPS

### Test Pose Detection
- [ ] Position yourself in frame
- [ ] Full body should be visible
- [ ] State changes from IDLE to SETUP
- [ ] Green skeleton overlay appears

### Test Pushup Counting
- [ ] Get into pushup position (arms extended)
- [ ] State should change to READY
- [ ] Perform a slow pushup
- [ ] Watch state cycle: DESCENDING → BOTTOM → ASCENDING → TOP → COUNTED
- [ ] Count increments by 1
- [ ] Form feedback appears

### Test Controls
- [ ] Tap "Start" button
- [ ] Perform pushups
- [ ] Tap "Pause" button
- [ ] Counting pauses
- [ ] Tap "Reset" button
- [ ] Count resets to 0

## 🐛 Step 10: Troubleshooting

### App crashes on startup
- [ ] Check logcat for errors (View → Tool Windows → Logcat)
- [ ] Verify `pose_landmarker_lite.task` is in assets/
- [ ] Check camera permission is granted

### Camera doesn't start
- [ ] Check permission dialog wasn't dismissed
- [ ] Settings → Apps → Pushup Tracker → Permissions → Camera (ON)
- [ ] Restart app

### Low FPS (<20)
- [ ] Check device is mid-range or better
- [ ] Close background apps
- [ ] Reduce resolution in CameraProcessor.kt

### Pose not detected
- [ ] Ensure good lighting
- [ ] Stand 4-6 feet from camera
- [ ] Full body must be visible
- [ ] Front-facing camera works best

### Reps not counting
- [ ] Ensure body is straight (no hip sag)
- [ ] Go to full depth (elbows ~90°)
- [ ] Come to full extension (arms straight)
- [ ] Move slowly (0.5-3 seconds per rep)

## 📊 Step 11: Performance Verification

Run the app and check metrics:

- [ ] FPS: 28-32 (acceptable: >25)
- [ ] RAM Usage: 120-150MB (Settings → Developer Options → Running Services)
- [ ] Battery drain: ~2% per minute (acceptable)
- [ ] No memory leaks (use Android Profiler)

## 🎯 Final Verification Checklist

Before considering setup complete:

- [ ] App builds without errors
- [ ] App runs on physical device
- [ ] Camera permission granted
- [ ] Camera preview working
- [ ] Pose detection working (skeleton visible)
- [ ] Pushups being counted
- [ ] Form feedback appearing
- [ ] All buttons working (Start, Pause, Reset)
- [ ] FPS ≥ 25
- [ ] No crashes during 5-minute test
- [ ] State machine transitions correctly
- [ ] Invalid reps are rejected

## 📝 Notes

### Important File Locations
```
Model file:     app/src/main/assets/pose_landmarker_lite.task
Main code:      app/src/main/kotlin/com/fitness/pushuptracker/
Manifest:       app/src/main/AndroidManifest.xml
Build config:   app/build.gradle.kts
```

### Build Variants
- **Debug**: For development, includes debugging symbols
- **Release**: For production, minified and optimized

### Useful Commands
```bash
# Clean build
./gradlew clean

# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install on device
./gradlew installDebug

# Uninstall from device
adb uninstall com.fitness.pushuptracker
```

## 🎉 Success Criteria

Setup is complete when:

✅ App installs and runs without crashes
✅ Camera shows live preview
✅ Skeleton overlay visible when in frame
✅ Pushups counted correctly
✅ Form feedback displayed
✅ Performance meets targets (≥25 FPS, <150MB RAM)
✅ All controls functional

---

**If all items are checked, your Android pushup counter is ready for use!** 🚀

For issues not covered here, check:
- README.md for detailed documentation
- PYTHON_TO_KOTLIN_GUIDE.md for architecture explanation
- Android Studio logcat for error messages

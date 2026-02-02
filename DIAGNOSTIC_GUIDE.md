# 🔧 PUSHUP TRACKER DIAGNOSTIC GUIDE

## 🚨 CRITICAL: Run Diagnostics First

Your app now has **TWO launcher icons**:
1. **Pushup Tracker** - The main app
2. **🔧 Pushup Tracker Diagnostic** - The diagnostic tool

## Step-by-Step Diagnostic Process

### 1. Install the App
```bash
./gradlew installDebug
```

### 2. Launch Diagnostic Activity
- Look for the app with "Diagnostic" in the launcher
- OR run: `adb shell am start -n com.fitness.pushuptracker/.DiagnosticActivity`

### 3. Run Tests
Click the buttons in order:
1. **Test Basic App Functionality** - Verifies app can run, access files, has permissions
2. **Test Camera** - Checks if camera hardware is accessible
3. **Test MediaPipe** - Verifies model file exists and MediaPipe can initialize
4. **Test Pushup Logic** - Tests angle calculation math

OR click **▶️ RUN ALL TESTS** to run everything at once.

### 4. Check Logs
While running tests, monitor logs:
```bash
# Clear old logs first
adb logcat -c

# Monitor all diagnostic logs
adb logcat -s "DIAGNOSTIC|DEBUG|CAMERA_TEST|MEDIAPIPE_TEST|PUSHUP_TEST"

# OR save to file
adb logcat -s "DIAGNOSTIC|DEBUG|CAMERA_TEST|MEDIAPIPE_TEST|PUSHUP_TEST" > diagnostic_logs.txt
```

## 📋 What Each Test Checks

### Test 1: Basic App Functionality
- ✅ App can start
- ✅ Can write to file system
- ✅ Camera permission granted
- ✅ Assets folder accessible
- ✅ Model file exists in assets

**Expected Output:**
```
✅ App onCreate called
✅ Can write files
✅ Camera permission granted
📁 Assets folder contains: pose_landmarker_lite.task
🔍 MediaPipe models found: [pose_landmarker_lite.task]
```

**If Failed:**
- Missing model file → Download from: https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task
- No camera permission → Grant in Settings
- Cannot write files → Check app permissions

### Test 2: Camera
- ✅ CameraProvider available
- ✅ Back camera exists
- ✅ Front camera exists
- ✅ Camera count

**Expected Output:**
```
✅ CameraProvider available
✅ Back camera available
✅ Front camera available
📷 Total cameras found: 2
```

**If Failed:**
- No CameraProvider → CameraX dependency issue
- No cameras → Testing on emulator without camera support
- Camera error → Check AndroidManifest camera permissions

### Test 3: MediaPipe
- ✅ Model file found
- ✅ Model size correct (~9.9 MB)
- ✅ MediaPipe initializes

**Expected Output:**
```
✅ Model file found: pose_landmarker_lite.task
📦 Model size: 10376192 bytes (9 MB)
✅ Model size looks correct (~9.9 MB expected)
✅ MediaPipe initialized successfully
```

**If Failed:**
- Model not found → File not in `app/src/main/assets/`
- Wrong size → Corrupted download, re-download
- Init failed → Check MediaPipe dependency version

### Test 4: Pushup Logic
- ✅ Angle calculation for straight arm (~180°)
- ✅ Angle calculation for bent arm (~90°)

**Expected Output:**
```
📐 Straight arm test: 180° (expected ~180°)
✅ Angle calculation works for straight arm
📐 Bent arm test: 90° (expected ~90°)
✅ Angle calculation works for bent arm
```

**If Failed:**
- Wrong angles → Math error in angle calculation
- Crash → Missing imports or syntax error

## 🔍 Common Issues & Fixes

### Issue: "Model file not found"
**Fix:**
1. Download model: https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task
2. Place in: `app/src/main/assets/pose_landmarker_lite.task`
3. Verify size: Should be exactly 10,376,192 bytes (9.89 MB)
4. Clean and rebuild: `./gradlew clean assembleDebug`

### Issue: "Camera permission NOT granted"
**Fix:**
1. Uninstall app
2. Reinstall: `./gradlew installDebug`
3. Grant permission when prompted
4. OR manually: Settings → Apps → Pushup Tracker → Permissions → Camera → Allow

### Issue: "CameraProvider not available"
**Fix:**
Check `build.gradle` has:
```gradle
implementation "androidx.camera:camera-core:1.3.1"
implementation "androidx.camera:camera-camera2:1.3.1"
implementation "androidx.camera:camera-lifecycle:1.3.1"
implementation "androidx.camera:camera-view:1.3.1"
```

### Issue: "MediaPipe init failed"
**Fix:**
Check `build.gradle` has:
```gradle
implementation "com.google.mediapipe:tasks-vision:0.10.14"
```

## 📊 Send Me These If Tests Fail

1. **Screenshot of diagnostic results** (from the app)
2. **Full logs:**
   ```bash
   adb logcat -d > full_logs.txt
   ```
3. **Assets folder contents:**
   ```bash
   ls -la app/src/main/assets/
   ```
4. **Build.gradle dependencies:**
   ```bash
   cat app/build.gradle | grep -A20 "dependencies"
   ```

## ✅ Next Steps After Diagnostics Pass

Once ALL tests show ✅:
1. Close diagnostic app
2. Launch main "Pushup Tracker" app
3. Grant camera permission
4. Position yourself in pushup plank position
5. Watch logs: `adb logcat -s "WorkoutViewModel"`
6. Look for: `💓 ALIVE | State: READY | Angles: L=170° R=172°`

## 🎯 Quick Test Commands

```bash
# Install app
./gradlew installDebug

# Launch diagnostic
adb shell am start -n com.fitness.pushuptracker/.DiagnosticActivity

# Watch logs
adb logcat -c && adb logcat -s "DIAGNOSTIC|DEBUG|CAMERA_TEST|MEDIAPIPE_TEST|PUSHUP_TEST"

# Save logs
adb logcat -d > diagnostic_output.txt
```

## 🚨 Emergency: Nothing Works?

Run this minimal test:
```bash
# Check if app installs
./gradlew installDebug

# Check if app launches
adb shell am start -n com.fitness.pushuptracker/.DiagnosticActivity

# Check for crashes
adb logcat -s "AndroidRuntime:E"
```

If app crashes on launch, send me the crash log from the last command.

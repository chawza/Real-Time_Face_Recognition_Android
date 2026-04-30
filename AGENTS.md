# AGENTS.md — Real-Time Face Recognition Android

## Project Overview

This is an **Android application** that performs **real-time face recognition** using on-device ML. It uses the **MobileFaceNet** TensorFlow Lite model to generate face embeddings and compares them via Euclidean distance against a saved set of registered faces. All inference happens offline; no re-training is required to add new faces.

- **Package**: `com.atharvakale.facerecognition`
- **Language**: Java
- **IDE**: Android Studio
- **Build system**: Gradle (Android Gradle Plugin 8.8.0)
- **Compile SDK**: 35
- **Min SDK**: 21
- **Target SDK**: 35
- **Java compatibility**: 17

## Tech Stack

| Component | Technology / Library |
|-----------|---------------------|
| UI Framework | Android SDK (AppCompat, ConstraintLayout, Material Components) |
| Camera | CameraX (core, camera2, lifecycle, view) |
| Face Detection | ML Kit Face Detection (com.google.mlkit:face-detection) |
| Inference | TensorFlow Lite (tflite, tflite-support, tflite-task-vision) |
| Model | MobileFaceNet (`mobile_face_net.tflite`) — 112×112 input, 192-d embedding output |
| Serialization | Gson (for saving/loading registered face embeddings to SharedPreferences) |
| Animation | Lottie (splash screen) |

## Project Structure

```
├── app/
│   ├── build.gradle                     # App-level dependencies and build config
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   └── mobile_face_net.tflite   # TFLite model (do not compress)
│       ├── java/com/atharvakale/facerecognition/
│       │   ├── MainActivity.java         # Core camera, detection, recognition, and UI logic
│       │   ├── splash_screen.java        # 2.5s splash screen with Lottie animation
│       │   └── SimilarityClassifier.java # Data class for face recognition results/embeddings
│       └── res/                        # Layouts, drawables, values, navigation, menu
├── build.gradle                         # Project-level plugin config
├── settings.gradle
├── gradle.properties
└── gradle/wrapper/
```

## Key Source Files

### `MainActivity.java`
- **Responsibility**: Entire face recognition pipeline and user interactions.
- **Pipeline**:
  1. Binds CameraX preview and `ImageAnalysis` (640×480, keep-only-latest backpressure).
  2. Converts `ImageProxy` → `InputImage` for ML Kit Face Detection.
  3. Extracts the first detected face bounding box, crops, rotates, and resizes to **112×112**.
  4. Normalizes pixels (`mean=128`, `std=128`) into a `ByteBuffer`.
  5. Runs inference via `Interpreter.runForMultipleInputsOutputs()` to get a **192-d float embedding**.
  6. Compares the embedding to all registered faces using **Euclidean distance**.
  7. Displays the nearest match if distance is below a configurable threshold (default `1.0f`), otherwise shows "Unknown".
- **Registered faces storage**: `HashMap<String, SimilarityClassifier.Recognition>` serialized to JSON with Gson and stored in `SharedPreferences` (key: `HashMap`).
- **Modes**:
  - **Recognize**: Live matching against saved faces.
  - **Add Face**: Captures current face embedding and prompts for a name to register.
- **Actions menu** (via AlertDialog): View list, Update/delete entries, Save/Load recognitions, Clear all, Import photo (beta), Hyperparameters (distance threshold), Developer mode (shows nearest + 2nd nearest distances).
- **Important fields**:
  - `inputSize = 112`
  - `OUTPUT_SIZE = 192`
  - `IMAGE_MEAN = 128.0f`, `IMAGE_STD = 128.0f`
  - `isModelQuantized = false`
  - `distance` threshold stored in SharedPreferences (`Distance`)

### `SimilarityClassifier.java`
- Simple data holder (`Recognition`) with `id`, `title`, `distance`, and an `extra` field used to store the `float[][]` embedding.

### `splash_screen.java`
- Launcher activity. Delays 2500ms then routes to `MainActivity`.

## Build & Run

1. Open the project in **Android Studio**.
2. Sync Gradle.
3. Connect an Android device or start an emulator.
4. Run `app`.

> **Note**: The app requires the `CAMERA` permission. It is requested at runtime on first launch.

### From Terminal

```bash
# macOS/Linux
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

APK output path: `app/build/outputs/apk/debug/app-debug.apk`

## Model Details

- **File**: `app/src/main/assets/mobile_face_net.tflite`
- **Architecture**: MobileFaceNet
- **Input**: `1 × 112 × 112 × 3` float32 (RGB normalized with mean=128, std=128)
- **Output**: `1 × 192` float32 face embedding vector
- **Compression**: Gradle `aaptOptions { noCompress "tflite" }` prevents APK compression so the model can be memory-mapped.

## Agent Notes / Conventions

- **Do not reformat or modernize the entire codebase** unless explicitly requested. Keep the existing Java style and XML naming.
- **Camera lifecycle**: `ProcessCameraProvider.bindToLifecycle()` is called in `cameraBind()`. Switching cameras (`camera_switch`) unbinds all and re-binds.
- **ImageProxy lifecycle**: `imageProxy.close()` is called in `onComplete()` of the ML Kit task. This is critical; omitting it will freeze the camera feed.
- **Bitmap recycling**: Several utility methods (`getCropBitmapByCPU`, `rotateBitmap`, `getResizedBitmap`) recycle the source Bitmap after creating a new one. Ensure any changes preserve this to avoid memory leaks.
- **SharedPreferences format**: Registered faces are stored as a JSON string representation of `HashMap<String, Recognition>`. When loading, `Double` values inside the `ArrayList` embeddings are explicitly cast back to `float` (see `readFromSP()`).
- **Import Photo flow** (`loadphoto()` / `onActivityResult()`): Uses `Intent.ACTION_GET_CONTENT` to pick an image, runs ML Kit detection on it, and then calls `addFace()` to register. It currently does not auto-orient imported photos (known limitation in README).
- **Threading**: ML Kit analysis runs on a single-thread executor. UI updates are made directly from success/failure listeners (they run on the main thread via ML Kit’s default behavior).
- **Front camera handling**: `flipX = true` when using the front camera so the preview/mirror behavior is consistent.
- **Tests**: Only placeholder `ExampleUnitTest` and `ExampleInstrumentedTest` exist. There is no face-recognition-specific test suite.

## Common Pitfalls to Avoid

1. **Do not compress `.tflite` in `build.gradle`**: The `noCompress` rule is required.
2. **Do not change `inputSize` or `OUTPUT_SIZE`** unless you also replace the model with one that has matching tensor shapes.
3. **Do not remove `imageProxy.close()`** in the ML Kit completion listener.
4. **SharedPreferences key names**: `HashMap` (for registered faces) and `Distance` (for threshold) are hardcoded in multiple places. Keep them in sync if refactoring.

## Dependencies (Key Versions)

- Android Gradle Plugin: `8.8.0`
- Gradle: `8.10.2`
- CameraX: `1.4.1`
- ML Kit Face Detection: `16.1.7`
- TensorFlow Lite: `2.16.1`
- TensorFlow Lite Support: `0.4.4`
- Gson: `2.11.0`
- Lottie: `6.6.2`
- AppCompat: `1.7.0`
- Material: `1.12.0`

## License

Apache 2.0 (per TensorFlow authors attribution in `SimilarityClassifier.java`).

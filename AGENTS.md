# AGENTS.md — Real-Time Face Recognition Android

## Project Overview

This is an **Android application** that performs **real-time face recognition** using on-device ML. It uses the **MobileFaceNet** TensorFlow Lite model to generate face embeddings and compares them via Euclidean distance against a saved set of registered faces. All inference happens offline; no re-training is required to add new faces.

- **Package**: `com.atharvakale.facerecognition`
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (ViewModel + Repository)
- **DI**: Hilt
- **Async**: Kotlin Coroutines + Flow
- **IDE**: Android Studio
- **Build system**: Gradle (Android Gradle Plugin 8.8.0)
- **Compile SDK**: 35
- **Min SDK**: 24
- **Target SDK**: 35
- **JVM target**: 17

## Tech Stack

| Component | Technology / Library |
|-----------|---------------------|
| UI | Jetpack Compose (Material 3, Compose Navigation) |
| Camera | CameraX (core, camera2, lifecycle, view) |
| Face Detection | ML Kit Face Detection (com.google.mlkit:face-detection) |
| Inference | TensorFlow Lite (tflite, tflite-support, tflite-task-vision) |
| Model | MobileFaceNet (`mobile_face_net.tflite`) — 112×112 input, 192-d embedding output |
| Persistence | Room (face embeddings), DataStore Preferences (settings) |
| Serialization | Gson (Room TypeConverter) |
| DI | Hilt (Dagger) |
| Async | Kotlin Coroutines + Flow |
| Navigation | Navigation Compose |
| Animation | Lottie Compose (splash screen) |

## Project Structure

```
├── app/
│   ├── build.gradle                     # App-level dependencies and build config
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   └── mobile_face_net.tflite   # TFLite model (do not compress)
│       ├── kotlin/com/atharvakale/facerecognition/
│       │   ├── App.kt                    # @HiltAndroidApp Application class
│       │   ├── MainActivity.kt           # Single Activity, setContent with Compose
│       │   ├── data/
│       │   │   ├── db/
│       │   │   │   ├── AppDatabase.kt    # Room database
│       │   │   │   ├── FaceEmbeddingDao.kt
│       │   │   │   ├── FaceEmbeddingEntity.kt
│       │   │   │   └── FaceEmbeddingTypeConverter.kt
│       │   │   ├── datastore/
│       │   │   │   └── SettingsRepository.kt  # DataStore (threshold, dev mode)
│       │   │   └── FaceRepository.kt     # Room DAO wrapper + SP migration
│       │   ├── ml/
│       │   │   ├── FaceEmbeddingExtractor.kt  # TFLite interpreter wrapper
│       │   │   ├── FacePreprocessor.kt         # Bitmap crop/rotate/scale/YUV utils
│       │   │   ├── FaceVerifier.kt             # Euclidean distance matching
│       │   │   └── FaceDetectionAnalyzer.kt    # ImageAnalysis.Analyzer + ML Kit
│       │   ├── ui/
│       │   │   ├── navigation/NavGraph.kt
│       │   │   ├── theme/ (Theme.kt, Color.kt, Type.kt)
│       │   │   ├── screens/ (SplashScreen.kt, MainScreen.kt, RealtimeScreen.kt)
│       │   │   └── components/ (CameraPreview.kt, FacePreviewCard.kt, MetricsPanel.kt, Dialogs.kt)
│       │   ├── viewmodel/
│       │   │   ├── MainViewModel.kt
│       │   │   └── RealtimeViewModel.kt
│       │   └── di/
│       │       ├── DatabaseModule.kt
│       │       ├── DataStoreModule.kt
│       │       └── InferenceModule.kt
│       └── res/
│           ├── drawable/  (icons, backgrounds)
│           ├── raw/       (Lottie animation JSON)
│           └── values/    (strings, colors, themes)
├── build.gradle                         # Project-level plugin config
├── settings.gradle
├── gradle.properties
└── gradle/wrapper/
```

## Key Source Files

### `data/` — Data Layer
- **`FaceEmbeddingEntity`**: Room entity with `name` (PK) and `embedding: List<Float>` (192-d).
- **`FaceEmbeddingDao`**: DAO with Flow-based `getAll()`, plus suspend `insert`, `delete`, `deleteAll`.
- **`FaceRepository`**: Wraps DAO, exposes `Flow<List<FaceEmbeddingEntity>>`. Contains `migrateFromSharedPrefs()` for one-time data migration from legacy SharedPreferences format.
- **`SettingsRepository`**: Wraps DataStore, exposes `distanceThreshold: Flow<Float>` and `developerMode: Flow<Boolean>`.

### `ml/` — ML Logic
- **`FaceEmbeddingExtractor`**: Loads `mobile_face_net.tflite`, normalizes pixels (mean=128, std=128), runs inference, returns `FloatArray` (192-d).
- **`FacePreprocessor`**: Pure utility functions — `cropFace()`, `rotateBitmap()`, `scaleToInputSize()`, `yuvToBitmap()`. Handles bitmap recycling internally.
- **`FaceVerifier`**: Computes Euclidean distance between embeddings. `findNearest()` returns best match, `findNearestTwo()` returns top-2.
- **`FaceDetectionAnalyzer`**: Implements `ImageAnalysis.Analyzer`. Uses ML Kit to detect faces, preprocesses them, extracts embeddings, emits `AnalysisResult` via callback.

### `viewmodel/` — ViewModels
- **`MainViewModel`**: Manages `MainUiState` (mode, recognized name, distance, face preview, registered faces, developer mode, threshold, camera lens). Handles face detection results, add/delete/clear faces, camera switching, threshold updates.
- **`RealtimeViewModel`**: Manages `RealtimeUiState` (matched name, distance, confidence %, inference time, FPS, DB count). Tracks FPS via frame counting.

### `ui/` — Compose UI
- **`SplashScreen`**: Lottie animation with 2.5s delay, then navigates to Main.
- **`MainScreen`**: Camera preview (via `AndroidView` wrapping CameraX `PreviewView`), face preview card, control buttons, actions dialog, add/delete/hyperparameter dialogs.
- **`RealtimeScreen`**: Full-screen camera with overlaid metrics panel (matched name, distance, confidence, inference time, FPS, DB count).
- **`CameraPreview`**: Reusable composable that binds CameraX preview + ImageAnalysis to lifecycle.

### `App.kt` — Application Class
- `@HiltAndroidApp`. Initializes TFLite model on startup. Runs one-time SharedPreferences → Room migration on `Dispatchers.IO`.

### `MainActivity.kt`
- Single `@AndroidEntryPoint` Activity. Calls `setContent` with Compose theme + navigation graph.

## Build & Run

1. Open the project in **Android Studio**.
2. Sync Gradle.
3. Connect an Android device or start an emulator (min API 24).
4. Run `app`.

> **Note**: The app requires the `CAMERA` permission. It is requested at runtime on first launch.

### From Terminal

```bash
./gradlew assembleDebug
```

APK output path: `app/build/outputs/apk/debug/app-debug.apk`

## Model Details

- **File**: `app/src/main/assets/mobile_face_net.tflite`
- **Architecture**: MobileFaceNet
- **Input**: `1 × 112 × 112 × 3` float32 (RGB normalized with mean=128, std=128)
- **Output**: `1 × 192` float32 face embedding vector
- **Compression**: Gradle `aaptOptions { noCompress "tflite" }` prevents APK compression so the model can be memory-mapped.

## Agent Notes / Conventions

- **Language**: All source code is Kotlin. There are no Java files.
- **Camera lifecycle**: CameraX is bound via `ProcessCameraProvider.bindToLifecycle()` in `CameraPreview` composable. Camera switching unbinds all and re-binds.
- **Camera selector constants**: Use `CameraSelector.LENS_FACING_BACK` and `CameraSelector.LENS_FACING_FRONT` (not deprecated `android.hardware.Camera.CameraInfo` constants).
- **ImageProxy lifecycle**: `imageProxy.close()` is called in `onComplete` of the ML Kit task inside `FaceDetectionAnalyzer`. This is critical; omitting it will freeze the camera feed.
- **Bitmap recycling**: `FacePreprocessor` methods (`cropFace`, `rotateBitmap`, `scaleToInputSize`) recycle the source Bitmap after creating a new one. Ensure any changes preserve this to avoid memory leaks.
- **Data migration**: On first launch after upgrade, `FaceRecognitionApp` migrates face embeddings from legacy SharedPreferences (JSON format with `Double` arrays) to Room. The migration flag is stored in SharedPreferences key `room_migrated`.
- **State management**: UI state is exposed via `StateFlow<UiState>` from ViewModels. Collected in Compose via `collectAsState()`.
- **Threading**: ML Kit analysis runs on a single-thread executor (via CameraX `ImageAnalysis.setAnalyzer()`). ViewModel operations use `viewModelScope.launch` with Coroutines.
- **Front camera handling**: `flipX = true` when using `CameraSelector.LENS_FACING_FRONT` so the preview/mirror behavior is consistent.
- **DI**: Hilt provides all singletons. `FaceRepository`, `SettingsRepository`, `FaceEmbeddingExtractor`, `FaceVerifier`, and `FaceDetectionAnalyzer` are `@Singleton` with `@Inject` constructors. ViewModels are `@HiltViewModel`.

## Common Pitfalls to Avoid

1. **Do not compress `.tflite` in `build.gradle`**: The `noCompress` rule is required.
2. **Do not change `INPUT_SIZE` (112) or `OUTPUT_SIZE` (192)** unless you also replace the model with one that has matching tensor shapes.
3. **Do not remove `imageProxy.close()`** in the ML Kit completion listener (`FaceDetectionAnalyzer`).
4. **Room migration**: If changing the Room schema (e.g. adding columns), add a migration or use `fallbackToDestructiveMigration()`. Do not change `version = 1` without a migration strategy.
5. **DataStore vs SharedPreferences**: Settings now use DataStore, not SharedPreferences. The only remaining SharedPreferences usage is the one-time migration flag (`room_migrated`).
6. **Compose recomposition**: Bitmap state in `MainUiState.facePreview` triggers recomposition. For performance-sensitive paths, consider `remember` with keys.

## Dependencies (Key Versions)

- Android Gradle Plugin: `8.8.0`
- Kotlin: `2.0.21`
- KSP: `2.0.21-1.0.28`
- Gradle: `8.10.2`
- Compose BOM: `2025.01.01`
- Hilt: `2.52`
- CameraX: `1.4.1`
- ML Kit Face Detection: `16.1.7`
- TensorFlow Lite: `2.16.1`
- Room: `2.6.1`
- DataStore: `1.1.1`
- Gson: `2.11.0`
- Lottie Compose: `6.6.2`
- Coroutines: `1.9.0`
- Navigation Compose: `2.8.5`
- Material Icons Extended (Compose)
- AppCompat: `1.7.0`

## License

Apache 2.0 (per TensorFlow authors attribution).

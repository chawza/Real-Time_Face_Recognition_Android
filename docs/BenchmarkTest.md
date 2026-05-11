## Running the Face Alignment Benchmark Test

The `FaceAlignmentBenchmarkTest` compares two face preprocessing pipelines (aligned vs. crop-and-scale) by computing embeddings and cosine similarity matrices. It runs on a physical device or emulator and requires you to supply your own face images.

### Why Local Images?

Test images are **not committed** to the repository (privacy and repo size). The test loads them from device storage at runtime and skips automatically if none are found.

### Step 1: Prepare Your Images

Gather 2–4 face photos (JPEG or PNG). For meaningful results, include:
- At least two photos of the **same person** (e.g., `Nabeel.jpg` and `TestSample.jpg`) to measure same-person similarity.
- At least one photo of a **different person** to measure cross-person distance.

### Step 2: Push Images to the Device

```bash
# Find your app's external files directory
adb shell mkdir -p /sdcard/Android/data/com.atharvakale.facerecognition/files/benchmark_images

# Push each image
adb push Nabeel.jpg /sdcard/Android/data/com.atharvakale.facerecognition/files/benchmark_images/
adb push TestSample.jpg /sdcard/Android/data/com.atharvakale.facerecognition/files/benchmark_images/
```

> **Note:** If you're using an emulator, the path may differ. You can verify the correct path by checking `context.getExternalFilesDir(null)` in the test log output.

### Step 3: Run the Test

```bash
./gradlew connectedAndroidTest -c app/src/androidTest/java/com/atharvakale/facerecognition/FaceAlignmentBenchmarkTest.kt
```

Or from Android Studio: right-click the test class → **Run**.

### Step 4: Read the Results

The test outputs to **Logcat** under the tag `FaceBenchmark`. Filter with:

```bash
adb logcat -s FaceBenchmark
```

You'll see:
- Per-image detection and embedding status
- **Cosine similarity matrix** for the aligned pipeline
- **Cosine similarity matrix** for the old crop-and-scale pipeline

Higher same-person similarity (>0.9) and lower cross-person similarity indicate better face alignment.

### Troubleshooting

| Issue | Cause | Fix |
|-------|-------|-----|
| "SKIPPED: No test images found" | Images not pushed to device | Re-run the `adb push` commands |
| "NO FACE DETECTED" for an image | ML Kit couldn't find a face | Use a clearer, frontal face photo |
| Test fails to launch | App not installed | Run `./gradlew installDebug` first |

### Cleaning Up

To remove test images from the device:

```bash
adb shell rm -rf /sdcard/Android/data/com.atharvakale.facerecognition/files/benchmark_images
```
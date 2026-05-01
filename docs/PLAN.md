# Face Recognition ML Pipeline — Plan

## Completed Fixes (2026-05-01)

### 1. Fixed bounding-box / rotation coordinate mismatch
**Root cause of ~45-51% accuracy on devices with non-zero sensor rotation.** In `FaceDetectionAnalyzer`, the image was rotated *before* cropping with ML Kit's bounding box, but ML Kit returns coordinates in the un-rotated image space. When sensor rotation was 90°/270°, the crop targeted the wrong region — extracting background pixels instead of the face.

**Fix**: Crop from the un-rotated bitmap first, then rotate the cropped face.

**Files**: `FaceDetectionAnalyzer.kt:59-63`

### 2. Removed SharedPreferences → Room migration
Legacy migration code that converted old JSON-encoded face embeddings from SharedPreferences into Room entities has been removed. All storage is now Room-only.

**Files**: `FaceRepository.kt`, `FaceRecognitionApp.kt`

### 3. Unified FaceDetector via DI
The codebase had two `FaceDetector` instances — one with landmarks (created in `FaceDetectionAnalyzer.init`) and one without (provided by `InferenceModule`). This caused subtle bounding-box differences between camera and gallery enrollment paths.

**Fix**: `InferenceModule` now provides a single `FaceDetector` with `LANDMARK_MODE_ALL`. `FaceDetectionAnalyzer` accepts it via constructor injection instead of instantiating its own. Both recognition and gallery enrollment paths use the same detector.

**Files**: `InferenceModule.kt`, `FaceDetectionAnalyzer.kt`

### 4. Replaced Gson serialization with lossless binary encoding
Embeddings were serialized to/from JSON via Gson, causing float precision loss on every Room read/write cycle. 192 dimensions × subtle precision shifts accumulated to significant similarity degradation.

**Fix**: `FaceEmbeddingTypeConverter` now stores embeddings as raw IEEE 754 little-endian bytes. Zero precision loss. Added `fallbackToDestructiveMigration()` since the column type changed from TEXT to BLOB.

**Files**: `FaceEmbeddingTypeConverter.kt`, `DatabaseModule.kt`

### 5. Fixed aspect-ratio distortion in `scaleToInputSize`
The previous `scaleToInputSize` used independent X/Y scale factors (`112/width`, `112/height`), stretching non-square face crops. This distorted facial geometry, confusing a model trained on aspect-ratio-correct inputs.

**Fix**: Use uniform scaling with letterboxing (white padding) to center the face on a 112×112 canvas without distortion.

**Files**: `FacePreprocessor.kt:58-75`

---

## How to Verify the Fixes

1. **Clear all existing face data** — old embeddings (stored with Gson precision loss) are invalid. Use "Clear All" in Database List, or the destructive migration will wipe them automatically.
2. **Re-enroll faces** using the camera (not gallery) for the most controlled test.
3. **Run a 1:1 verification** of the same person. Confidence should now be **85-95%+**.
4. **Test both cameras** — front camera and back camera — to validate the rotation fix.

---

## Planned Improvements

### 6. Multi-Frame Enrollment
**Problem**: A single camera frame is captured to enroll a face. MobileFaceNet embeddings have intra-person variance (pose, expression, lighting).

**Solution**: Capture N frames (3–5) per enrollment, compute the embedding for each, and store the **element-wise mean** as the enrolled embedding. UI shows progress indicator.

**Expected gain**: +10–15% confidence for same-person comparisons.

### 7. Landmark-Based Face Alignment
**Problem**: ML Kit provides face landmarks (eyes, nose, mouth), but the pipeline does raw bounding-box cropping. Without similarity-transform alignment (eyes → fixed positions), the model sees misaligned faces.

**Solution**: Use ML Kit landmark coordinates to compute a similarity transform that aligns eyes to (38, 52) and (74, 52) on the 112×112 input, as done in the original InsightFace pipeline.

### 8. Embedding Quality Gate
Reject enrollment frames where face is too small, too dark, or blurry. Show user feedback.

### 9. FP16 Quantized TFLite Model
Quantize `mobile_face_net.tflite` to FP16: no accuracy loss, ~2x faster inference.

### 10. Threshold Tuning UI
In-app threshold calibration: capture same/different person pairs, plot similarity distributions, suggest optimal threshold.

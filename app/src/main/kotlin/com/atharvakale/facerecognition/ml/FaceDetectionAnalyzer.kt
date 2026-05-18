package com.atharvakale.facerecognition.ml

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AnalysisResult(
    val faceBitmap: Bitmap,
    val embedding: FloatArray,
    val boundingBox: RectF,
    val imageWidth: Int,
    val imageHeight: Int,
    val detectionTimeMs: Long = 0,
    val preprocessingTimeMs: Long = 0,
    val embeddingTimeMs: Long = 0
) {
    override fun equals(other: Any?): Boolean = false
    override fun hashCode(): Int = embedding.contentHashCode()
}

@Singleton
class FaceDetectionAnalyzer @Inject constructor(
    private val embeddingExtractor: FaceEmbeddingExtractor,
    private val detector: FaceDetector
) {

    private val _latestResult = MutableStateFlow<AnalysisResult?>(null)

    @OptIn(ExperimentalGetImage::class)
    fun analyze(imageProxy: ImageProxy, flipX: Boolean, onResult: (AnalysisResult?) -> Unit) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            onResult(null)
            return
        }

        val frameBmp = imageProxy.toBitmap()
        val inputImage = InputImage.fromBitmap(frameBmp, 0)
        val detectionStart = System.nanoTime()

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                val detectionTimeMs = (System.nanoTime() - detectionStart) / 1_000_000

                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val preprocessStart = System.nanoTime()
                    val boundingBox = RectF(face.boundingBox)

                    val imageWidth = frameBmp.width; val imageHeight = frameBmp.height

                    val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
                    val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

                    val scaled: Bitmap = if (leftEye != null && rightEye != null) {
                        // uses ML kit eyes data to align captured face. this is the idea case for highest accuracy
                        FacePreprocessor.alignFace(frameBmp, leftEye, rightEye).also {
                            frameBmp.recycle()
                        }
                    } else {
                        // fallback without alignment. just pass the face from the captured bounding box
                        // accuracy should be not as great as eyes alignment
                        // TODO: compare the accuracy difference
                        val expandedBox = FacePreprocessor.expandBoundingBox(boundingBox, frameBmp.width, frameBmp.height)
                        val croppedFace = FacePreprocessor.cropFace(frameBmp, expandedBox)
                        FacePreprocessor.scaleToInputSize(croppedFace)
                    }

                    val preprocessingTimeMs = (System.nanoTime() - preprocessStart) / 1_000_000

                    val embeddingStart = System.nanoTime()
                    val inputBuffer = FacePreprocessor.toNormalizedRgbBuffer(scaled)
                    val embedding = embeddingExtractor.getEmbedding(inputBuffer)
                    val embeddingTimeMs = (System.nanoTime() - embeddingStart) / 1_000_000
                    val result = AnalysisResult(
                        scaled, embedding, boundingBox, imageWidth, imageHeight,
                        detectionTimeMs, preprocessingTimeMs, embeddingTimeMs
                    )
                    _latestResult.value = result
                    onResult(result)
                } else {
                    frameBmp.recycle()
                    onResult(null)
                }
            }
            .addOnFailureListener {
                frameBmp.recycle()
                onResult(null)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
package com.atharvakale.facerecognition.ml

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val latestResult: StateFlow<AnalysisResult?> = _latestResult.asStateFlow()

    @OptIn(ExperimentalGetImage::class)
    fun analyze(imageProxy: ImageProxy, flipX: Boolean, onResult: (AnalysisResult?) -> Unit) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            onResult(null)
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
        val detectionStart = System.nanoTime()

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                val detectionTimeMs = (System.nanoTime() - detectionStart) / 1_000_000

                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val preprocessStart = System.nanoTime()
                    val frameBmp = imageProxy.toBitmap()
                    val boundingBox = RectF(face.boundingBox)
                    val expandedBox = FacePreprocessor.expandBoundingBox(boundingBox, frameBmp.width, frameBmp.height)
                    val croppedFace = FacePreprocessor.cropFace(frameBmp, expandedBox)
                    val rotatedFace = FacePreprocessor.rotateBitmap(croppedFace, rotation, false, false)
                    val finalFace = if (flipX) {
                        FacePreprocessor.rotateBitmap(rotatedFace, 0, true, false)
                    } else {
                        rotatedFace
                    }
                    val rotatedWidth = if (rotation == 90 || rotation == 270) mediaImage.height else mediaImage.width
                    val rotatedHeight = if (rotation == 90 || rotation == 270) mediaImage.width else mediaImage.height
                    val scaled = FacePreprocessor.scaleToInputSize(finalFace)
                    val preprocessingTimeMs = (System.nanoTime() - preprocessStart) / 1_000_000

                    val embeddingStart = System.nanoTime()
                    val inputBuffer = FacePreprocessor.toNormalizedRgbBuffer(scaled)
                    val embedding = embeddingExtractor.getEmbedding(inputBuffer)
                    val embeddingTimeMs = (System.nanoTime() - embeddingStart) / 1_000_000

                    val result = AnalysisResult(
                        scaled, embedding, boundingBox, rotatedWidth, rotatedHeight,
                        detectionTimeMs, preprocessingTimeMs, embeddingTimeMs
                    )
                    _latestResult.value = result
                    onResult(result)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener {
                onResult(null)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
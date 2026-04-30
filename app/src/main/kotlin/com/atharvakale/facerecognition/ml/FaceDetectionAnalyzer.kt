package com.atharvakale.facerecognition.ml

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AnalysisResult(
    val faceBitmap: Bitmap,
    val embedding: FloatArray,
    val boundingBox: RectF
) {
    override fun equals(other: Any?): Boolean = false
    override fun hashCode(): Int = embedding.contentHashCode()
}

@Singleton
class FaceDetectionAnalyzer @Inject constructor(
    private val embeddingExtractor: FaceEmbeddingExtractor
) {
    private val detector: FaceDetector

    private val _latestResult = MutableStateFlow<AnalysisResult?>(null)
    val latestResult: StateFlow<AnalysisResult?> = _latestResult.asStateFlow()

    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
        detector = FaceDetection.getClient(options)
    }

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

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val frameBmp = FacePreprocessor.yuvToBitmap(mediaImage)
                    val rotatedBmp = FacePreprocessor.rotateBitmap(frameBmp, rotation, false, false)
                    val boundingBox = RectF(face.boundingBox)
                    val croppedFace = FacePreprocessor.cropFace(rotatedBmp, boundingBox)
                    val finalFace = if (flipX) {
                        FacePreprocessor.rotateBitmap(croppedFace, 0, true, false)
                    } else {
                        croppedFace
                    }
                    val scaled = FacePreprocessor.scaleToInputSize(finalFace)
                    val embedding = embeddingExtractor.getEmbedding(scaled)
                    val result = AnalysisResult(scaled, embedding, boundingBox)
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

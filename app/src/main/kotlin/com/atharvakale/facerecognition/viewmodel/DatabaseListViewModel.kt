package com.atharvakale.facerecognition.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atharvakale.facerecognition.data.FaceRepository
import com.atharvakale.facerecognition.data.model.RegisteredFace
import com.atharvakale.facerecognition.data.datastore.SettingsRepository
import com.atharvakale.facerecognition.ml.FaceDetectionAnalyzer
import com.atharvakale.facerecognition.ml.FaceEmbeddingExtractor
import com.atharvakale.facerecognition.ml.FacePreprocessor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceLandmark
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DatabaseUiState(
    val faces: List<RegisteredFace> = emptyList(),
    val distanceThreshold: Float = 0.3f,
    val galleryReady: Boolean = false,
    val galleryError: String? = null
)

@HiltViewModel
class DatabaseListViewModel @Inject constructor(
    private val faceRepository: FaceRepository,
    private val settingsRepository: SettingsRepository,
    private val embeddingExtractor: FaceEmbeddingExtractor,
    val faceDetectionAnalyzer: FaceDetectionAnalyzer,
    private val faceDetector: FaceDetector,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var pendingEmbedding: FloatArray? = null

    private val _galleryReady = MutableStateFlow(false)
    private val _galleryError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DatabaseUiState> = combine(
        faceRepository.getRegisteredFaces(),
        settingsRepository.distanceThreshold,
        _galleryReady,
        _galleryError
    ) { faces, threshold, ready, error ->
        DatabaseUiState(
            faces = faces,
            distanceThreshold = threshold,
            galleryReady = ready,
            galleryError = error
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DatabaseUiState())

    fun processGalleryImage(uri: Uri) {
        viewModelScope.launch {
            _galleryReady.value = false
            _galleryError.value = null
            try {
                val embedding = withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (bitmap == null) return@withContext null

                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    val faces: List<com.google.mlkit.vision.face.Face> = faceDetector.process(inputImage).await()
                    if (faces.isEmpty()) {
                        bitmap.recycle()
                        return@withContext null
                    }

                    val face = faces[0]
                    val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
                    val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

                    val scaled: Bitmap = if (leftEye != null && rightEye != null) {
                        FacePreprocessor.alignFace(bitmap, leftEye, rightEye).also {
                            bitmap.recycle()
                        }
                    } else {
                        val boundingBox = RectF(face.boundingBox)
                        val expandedBox = FacePreprocessor.expandBoundingBox(boundingBox, bitmap.width, bitmap.height)
                        val cropped = FacePreprocessor.cropFace(bitmap, expandedBox)
                        FacePreprocessor.scaleToInputSize(cropped)
                    }
                    val inputBuffer = FacePreprocessor.toNormalizedRgbBuffer(scaled)
                    scaled.recycle()
                    embeddingExtractor.getEmbedding(inputBuffer)
                }

                if (embedding != null) {
                    pendingEmbedding = embedding
                    _galleryReady.value = true
                } else {
                    _galleryError.value = "No face detected in image"
                }
            } catch (e: Exception) {
                _galleryError.value = "Failed to process image: ${e.message}"
            }
        }
    }

    fun onCameraFaceReady(embedding: FloatArray) {
        pendingEmbedding = embedding
    }

    fun addFace(name: String) {
        val embedding = pendingEmbedding ?: return
        viewModelScope.launch {
            faceRepository.registerFace(name, embedding)
            pendingEmbedding = null
            _galleryReady.value = false
            _galleryError.value = null
        }
    }

    fun dismissGalleryError() {
        _galleryError.value = null
    }

    fun dismissGalleryReady() {
        _galleryReady.value = false
        pendingEmbedding = null
    }

    fun deleteFace(name: String) {
        viewModelScope.launch {
            faceRepository.deleteFace(name)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            faceRepository.clearAll()
        }
    }

    fun updateThreshold(value: Float) {
        viewModelScope.launch {
            settingsRepository.updateDistanceThreshold(value)
        }
    }
}

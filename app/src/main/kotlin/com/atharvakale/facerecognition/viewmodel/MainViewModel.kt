package com.atharvakale.facerecognition.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atharvakale.facerecognition.data.FaceRepository
import com.atharvakale.facerecognition.data.datastore.SettingsRepository
import com.atharvakale.facerecognition.ml.AnalysisResult
import com.atharvakale.facerecognition.ml.FaceDetectionAnalyzer
import com.atharvakale.facerecognition.ml.FaceEmbeddingExtractor
import com.atharvakale.facerecognition.ml.FacePreprocessor
import com.atharvakale.facerecognition.ml.FaceVerifier
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
import androidx.camera.core.CameraSelector
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

enum class ScreenMode { RECOGNIZE, ADD_FACE }

data class MainUiState(
    val mode: ScreenMode = ScreenMode.RECOGNIZE,
    val recognizedName: String = "Add Face",
    val distance: Float = Float.MAX_VALUE,
    val secondNearestName: String = "",
    val secondNearestDistance: Float = Float.MAX_VALUE,
    val registeredFaceNames: List<String> = emptyList(),
    val developerMode: Boolean = false,
    val distanceThreshold: Float = 1.0f,
    val isAnalyzing: Boolean = true,
    val currentEmbedding: FloatArray? = null,
    val cameraLensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val flipX: Boolean = false,
    val galleryReady: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val faceRepository: FaceRepository,
    private val settingsRepository: SettingsRepository,
    private val faceVerifier: FaceVerifier,
    private val embeddingExtractor: FaceEmbeddingExtractor,
    val faceDetectionAnalyzer: FaceDetectionAnalyzer,
    private val faceDetector: FaceDetector,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var pendingEmbedding: FloatArray? = null

    init {
        viewModelScope.launch {
            combine(
                faceRepository.getRegisteredFaces(),
                settingsRepository.distanceThreshold,
                settingsRepository.developerMode
            ) { faces, threshold, devMode ->
                _uiState.value = _uiState.value.copy(
                    registeredFaceNames = faces.map { it.name },
                    distanceThreshold = threshold,
                    developerMode = devMode,
                    currentEmbedding = null
                )
                Triple(faces, threshold, devMode)
            }.stateIn(viewModelScope, SharingStarted.Eagerly, Triple(emptyList(), 1.0f, false))
        }
    }

    fun onFaceDetected(result: AnalysisResult?) {
        if (_uiState.value.mode != ScreenMode.RECOGNIZE) {
            if (result != null) {
                pendingEmbedding = result.embedding
            }
            return
        }

        if (result == null) {
            if (_uiState.value.registeredFaceNames.isEmpty()) {
                _uiState.value = _uiState.value.copy(recognizedName = "Add Face")
            } else {
                _uiState.value = _uiState.value.copy(recognizedName = "No Face Detected!")
            }
            return
        }

        viewModelScope.launch {
            val registered = faceRepository.getAllOnce()
            if (registered.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    recognizedName = "Add Face"
                )
                return@launch
            }

            val nearest = faceVerifier.findNearest(result.embedding, registered)
            if (nearest != null) {
                val state = _uiState.value
                val displayName = if (nearest.distance < state.distanceThreshold) {
                    nearest.name
                } else {
                    "Unknown"
                }

                if (state.developerMode) {
                    val nearestTwo = faceVerifier.findNearestTwo(result.embedding, registered)
                    _uiState.value = state.copy(
                        recognizedName = displayName,
                        distance = nearest.distance,
                        secondNearestName = nearestTwo.getOrNull(1)?.name ?: "",
                        secondNearestDistance = nearestTwo.getOrNull(1)?.distance ?: Float.MAX_VALUE
                    )
                } else {
                    _uiState.value = state.copy(
                        recognizedName = displayName,
                        distance = nearest.distance
                    )
                }
            }
        }
    }

    fun processGalleryImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(galleryReady = false)
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
                    val boundingBox = RectF(face.boundingBox)
                    val cropped = FacePreprocessor.cropFace(bitmap, boundingBox)
                    val scaled = FacePreprocessor.scaleToInputSize(cropped)
                    embeddingExtractor.getEmbedding(scaled)
                }

                if (embedding != null) {
                    pendingEmbedding = embedding
                    _uiState.value = _uiState.value.copy(
                        mode = ScreenMode.ADD_FACE,
                        galleryReady = true
                    )
                }
            } catch (_: Exception) {
            }
        }
    }

    fun addFace(name: String) {
        val embedding = pendingEmbedding ?: return
        viewModelScope.launch {
            faceRepository.registerFace(name, embedding)
            pendingEmbedding = null
            _uiState.value = _uiState.value.copy(
                mode = ScreenMode.RECOGNIZE,
                isAnalyzing = true,
                galleryReady = false
            )
        }
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

    fun toggleDeveloperMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.toggleDeveloperMode(enabled)
        }
    }

    fun setMode(mode: ScreenMode) {
        _uiState.value = _uiState.value.copy(
            mode = mode,
            isAnalyzing = mode == ScreenMode.RECOGNIZE,
            galleryReady = false
        )
    }

    fun switchCamera() {
        val current = _uiState.value.cameraLensFacing
        val newFacing = if (current == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _uiState.value = _uiState.value.copy(
            cameraLensFacing = newFacing,
            flipX = newFacing == CameraSelector.LENS_FACING_FRONT
        )
    }

    fun saveFaces() {
    }

    fun loadFaces() {
    }
}

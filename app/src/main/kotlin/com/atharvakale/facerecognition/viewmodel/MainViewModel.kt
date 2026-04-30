package com.atharvakale.facerecognition.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atharvakale.facerecognition.data.FaceRepository
import com.atharvakale.facerecognition.data.datastore.SettingsRepository
import com.atharvakale.facerecognition.ml.AnalysisResult
import com.atharvakale.facerecognition.ml.FaceVerifier
import androidx.camera.core.CameraSelector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ScreenMode { RECOGNIZE, ADD_FACE }

data class MainUiState(
    val mode: ScreenMode = ScreenMode.RECOGNIZE,
    val recognizedName: String = "Add Face",
    val distance: Float = Float.MAX_VALUE,
    val secondNearestName: String = "",
    val secondNearestDistance: Float = Float.MAX_VALUE,
    val facePreview: Bitmap? = null,
    val registeredFaceNames: List<String> = emptyList(),
    val developerMode: Boolean = false,
    val distanceThreshold: Float = 1.0f,
    val isAnalyzing: Boolean = true,
    val currentEmbedding: FloatArray? = null,
    val cameraLensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val flipX: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val faceRepository: FaceRepository,
    private val settingsRepository: SettingsRepository,
    private val faceVerifier: FaceVerifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var pendingEmbedding: FloatArray? = null
    private var pendingBitmap: Bitmap? = null

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
                pendingBitmap = result.faceBitmap
                _uiState.value = _uiState.value.copy(
                    facePreview = result.faceBitmap
                )
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
                    recognizedName = "Add Face",
                    facePreview = result.faceBitmap
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
                        secondNearestDistance = nearestTwo.getOrNull(1)?.distance ?: Float.MAX_VALUE,
                        facePreview = result.faceBitmap
                    )
                } else {
                    _uiState.value = state.copy(
                        recognizedName = displayName,
                        distance = nearest.distance,
                        facePreview = result.faceBitmap
                    )
                }
            }
        }
    }

    fun addFace(name: String) {
        val embedding = pendingEmbedding ?: return
        viewModelScope.launch {
            faceRepository.registerFace(name, embedding)
            pendingEmbedding = null
            pendingBitmap = null
            _uiState.value = _uiState.value.copy(
                mode = ScreenMode.RECOGNIZE,
                isAnalyzing = true
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
            isAnalyzing = mode == ScreenMode.RECOGNIZE
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
        // Room auto-persists; this is a no-op kept for API compatibility
    }

    fun loadFaces() {
        // Room auto-loads via Flow; this is a no-op kept for API compatibility
    }
}

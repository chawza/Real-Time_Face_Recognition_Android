package com.atharvakale.facerecognition.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atharvakale.facerecognition.data.FaceRepository
import com.atharvakale.facerecognition.data.datastore.SettingsRepository
import com.atharvakale.facerecognition.ml.AnalysisResult
import com.atharvakale.facerecognition.ml.FaceDetectionAnalyzer
import com.atharvakale.facerecognition.ml.FaceVerifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RealtimeUiState(
    val matchedName: String = "Initializing...",
    val distance: Float = Float.MAX_VALUE,
    val confidence: Float = 0f,
    val inferenceTimeMs: Long = 0,
    val fps: Float = 0f,
    val dbFaceCount: Int = 0,
    val statusText: String = "Initializing..."
)

@HiltViewModel
class RealtimeViewModel @Inject constructor(
    private val faceRepository: FaceRepository,
    private val settingsRepository: SettingsRepository,
    private val faceVerifier: FaceVerifier,
    val faceDetectionAnalyzer: FaceDetectionAnalyzer
) : ViewModel() {

    private val _uiState = MutableStateFlow(RealtimeUiState())
    val uiState: StateFlow<RealtimeUiState> = _uiState.asStateFlow()

    private var frameCount = 0
    private var fpsUpdateTime = System.currentTimeMillis()
    private var currentFps = 0f

    init {
        viewModelScope.launch {
            val faces = faceRepository.getAllOnce()
            _uiState.value = _uiState.value.copy(dbFaceCount = faces.size)
        }
    }

    fun onFaceAnalyzed(result: AnalysisResult?, inferenceTimeMs: Long) {
        updateFps()

        if (result == null) {
            _uiState.value = _uiState.value.copy(
                matchedName = "No Face Detected",
                statusText = "Scanning...",
                distance = Float.MAX_VALUE,
                confidence = 0f
            )
            return
        }

        viewModelScope.launch {
            val registered = faceRepository.getAllOnce()
            val threshold = settingsRepository.distanceThreshold.first()

            if (registered.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    matchedName = "No DB",
                    statusText = "Face detected",
                    inferenceTimeMs = inferenceTimeMs,
                    fps = currentFps
                )
                return@launch
            }

            val nearest = faceVerifier.findNearest(result.embedding, registered)
            if (nearest != null) {
                val name = if (nearest.distance < threshold) nearest.name else "Unknown"
                val confidence = (1f - nearest.distance).coerceIn(0f, 1f) * 100f
                _uiState.value = _uiState.value.copy(
                    matchedName = name,
                    distance = nearest.distance,
                    confidence = confidence,
                    inferenceTimeMs = inferenceTimeMs,
                    fps = currentFps,
                    statusText = "Face detected"
                )
            }
        }
    }

    private fun updateFps() {
        frameCount++
        val now = System.currentTimeMillis()
        if (now - fpsUpdateTime >= 1000) {
            currentFps = frameCount * 1000f / (now - fpsUpdateTime)
            frameCount = 0
            fpsUpdateTime = now
        }
    }
}

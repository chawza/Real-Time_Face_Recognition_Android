package com.atharvakale.facerecognition.viewmodel

import android.graphics.RectF
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

data class RecognitionUiState(
    val matchedName: String = "Initializing...",
    val distance: Float = Float.MAX_VALUE,
    val confidence: Float = 0f,
    val detectionTimeMs: Long = 0,
    val preprocessingTimeMs: Long = 0,
    val embeddingTimeMs: Long = 0,
    val similarityTimeMs: Long = 0,
    val fps: Float = 0f,
    val dbFaceCount: Int = 0,
    val statusText: String = "Initializing...",
    val boundingBox: RectF? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0
)

@HiltViewModel
class RecognitionViewModel @Inject constructor(
    private val faceRepository: FaceRepository,
    private val settingsRepository: SettingsRepository,
    private val faceVerifier: FaceVerifier,
    val faceDetectionAnalyzer: FaceDetectionAnalyzer
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecognitionUiState())
    val uiState: StateFlow<RecognitionUiState> = _uiState.asStateFlow()

    private var frameCount = 0
    private var fpsUpdateTime = System.currentTimeMillis()
    private var currentFps = 0f

    init {
        viewModelScope.launch {
            val faces = faceRepository.getAllOnce()
            _uiState.value = _uiState.value.copy(dbFaceCount = faces.size)
        }
    }

    fun onFaceAnalyzed(result: AnalysisResult?) {
        updateFps()

        if (result == null) {
            _uiState.value = _uiState.value.copy(
                matchedName = "No Face Detected",
                statusText = "Scanning...",
                confidence = 0f,
                boundingBox = null
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
                    detectionTimeMs = result.detectionTimeMs,
                    preprocessingTimeMs = result.preprocessingTimeMs,
                    embeddingTimeMs = result.embeddingTimeMs,
                    fps = currentFps
                )
                return@launch
            }

            val similarityStart = System.nanoTime()
            val nearest = faceVerifier.findNearest(result.embedding, registered)
            val similarityTimeMs = (System.nanoTime() - similarityStart) / 1_000_000

            if (nearest != null) {
                val name = if (nearest.similarity >= threshold) nearest.name else "Unknown"
                val confidence = (nearest.similarity * 100f).coerceIn(0f, 100f)
                _uiState.value = _uiState.value.copy(
                    matchedName = name,
                    distance = 1f - nearest.similarity,
                    confidence = confidence,
                    detectionTimeMs = result.detectionTimeMs,
                    preprocessingTimeMs = result.preprocessingTimeMs,
                    embeddingTimeMs = result.embeddingTimeMs,
                    similarityTimeMs = similarityTimeMs,
                    fps = currentFps,
                    statusText = "Face detected",
                    dbFaceCount = registered.size,
                    boundingBox = result.boundingBox,
                    imageWidth = result.imageWidth,
                    imageHeight = result.imageHeight
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
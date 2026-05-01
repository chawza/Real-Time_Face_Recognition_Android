package com.atharvakale.facerecognition.viewmodel

import android.graphics.RectF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atharvakale.facerecognition.data.FaceRepository
import com.atharvakale.facerecognition.data.model.RegisteredFace
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

enum class VerificationPhase { SELECT, VERIFYING }

data class VerificationUiState(
    val phase: VerificationPhase = VerificationPhase.SELECT,
    val registeredFaces: List<RegisteredFace> = emptyList(),
    val selectedFace: RegisteredFace? = null,
    val distance: Float = Float.MAX_VALUE,
    val isMatch: Boolean = false,
    val confidence: Float = 0f,
    val detectionTimeMs: Long = 0,
    val preprocessingTimeMs: Long = 0,
    val embeddingTimeMs: Long = 0,
    val similarityTimeMs: Long = 0,
    val fps: Float = 0f,
    val statusText: String = "Select a face",
    val boundingBox: RectF? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0
)

@HiltViewModel
class VerificationViewModel @Inject constructor(
    private val faceRepository: FaceRepository,
    private val settingsRepository: SettingsRepository,
    private val faceVerifier: FaceVerifier,
    val faceDetectionAnalyzer: FaceDetectionAnalyzer
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerificationUiState())
    val uiState: StateFlow<VerificationUiState> = _uiState.asStateFlow()

    private var frameCount = 0
    private var fpsUpdateTime = System.currentTimeMillis()
    private var currentFps = 0f

    init {
        viewModelScope.launch {
            faceRepository.getRegisteredFaces().collect { faces ->
                _uiState.value = _uiState.value.copy(registeredFaces = faces)
            }
        }
    }

    fun selectFace(face: RegisteredFace) {
        _uiState.value = _uiState.value.copy(
            selectedFace = face,
            phase = VerificationPhase.VERIFYING,
            statusText = "Verifying against \"${face.name}\""
        )
    }

    fun backToSelect() {
        _uiState.value = _uiState.value.copy(
            phase = VerificationPhase.SELECT,
            selectedFace = null,
            distance = Float.MAX_VALUE,
            isMatch = false,
            confidence = 0f,
            statusText = "Select a face"
        )
    }

    fun onFaceAnalyzed(result: AnalysisResult?) {
        updateFps()

        val selected = _uiState.value.selectedFace
        if (result == null || selected == null) {
            _uiState.value = _uiState.value.copy(
                distance = Float.MAX_VALUE,
                isMatch = false,
                confidence = 0f,
                statusText = "No face detected",
                boundingBox = null
            )
            return
        }

        viewModelScope.launch {
            val threshold = settingsRepository.distanceThreshold.first()

            val similarityStart = System.nanoTime()
            val similarity = faceVerifier.cosineSimilarity(
                result.embedding,
                selected.embedding.toFloatArray()
            )
            val similarityTimeMs = (System.nanoTime() - similarityStart) / 1_000_000

            val isMatch = similarity >= threshold
            val confidence = (similarity * 100f).coerceIn(0f, 100f)
            _uiState.value = _uiState.value.copy(
                distance = 1f - similarity,
                isMatch = isMatch,
                confidence = confidence,
                detectionTimeMs = result.detectionTimeMs,
                preprocessingTimeMs = result.preprocessingTimeMs,
                embeddingTimeMs = result.embeddingTimeMs,
                similarityTimeMs = similarityTimeMs,
                fps = currentFps,
                statusText = if (isMatch) "Match!" else "No match",
                boundingBox = result.boundingBox,
                imageWidth = result.imageWidth,
                imageHeight = result.imageHeight
            )
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

package com.atharvakale.facerecognition.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atharvakale.facerecognition.ml.FaceEmbeddingExtractor
import com.atharvakale.facerecognition.ml.FacePreprocessor
import com.atharvakale.facerecognition.ml.FaceVerifier
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.BufferedInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.inject.Inject

data class BenchmarkCellResult(
    val similarity: Float,
    val elapsedMs: Long
)

data class BenchmarkUiState(
    val isLoading: Boolean = false,
    val progress: String = "",
    val labels: List<String> = emptyList(),
    val matrix: List<List<BenchmarkCellResult?>> = emptyList(),
    val csvContent: String? = null,
    val deviceInfo: String = BenchmarkViewModel.getDeviceInfo(),
    val error: String? = null,
    val failedImages: List<String> = emptyList()
)

@HiltViewModel
class BenchmarkViewModel @Inject constructor(
    private val embeddingExtractor: FaceEmbeddingExtractor,
    private val faceDetector: FaceDetector,
    private val faceVerifier: FaceVerifier,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        fun getDeviceInfo(): String {
            val manufacturer = Build.MANUFACTURER.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            val model = Build.MODEL
            val androidVersion = Build.VERSION.RELEASE
            val sdk = Build.VERSION.SDK_INT
            return "$manufacturer $model, Android $androidVersion (API $sdk)"
        }
    }

    private val _uiState = MutableStateFlow(BenchmarkUiState())
    val uiState: StateFlow<BenchmarkUiState> = _uiState.asStateFlow()

    fun runBenchmark(zipUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            embeddingExtractor.initialize(context)
            _uiState.value = BenchmarkUiState(
                isLoading = true,
                deviceInfo = getDeviceInfo()
            )

            try {
                val imageExtensions = setOf("jpg", "jpeg", "png", "bmp", "webp")
                val embeddings = mutableListOf<Pair<String, FloatArray>>()
                val failedImages = mutableListOf<String>()

                context.contentResolver.openInputStream(zipUri)?.use { stream ->
                    ZipInputStream(BufferedInputStream(stream)).use { zipStream ->
                        var entry = zipStream.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory) {
                                val fileName = entry.name.substringAfterLast('/')
                                if (fileName.startsWith("._") || entry.name.contains("__MACOSX")) {
                                    zipStream.closeEntry()
                                    entry = zipStream.nextEntry
                                    continue
                                }
                                val extension = fileName.substringAfterLast('.', "").lowercase()
                                val label = fileName.substringBeforeLast('.')

                                if (extension in imageExtensions) {
                                    _uiState.value = _uiState.value.copy(
                                        progress = "Extracting face: $label"
                                    )
                                    try {
                                        val bytes = zipStream.readBytes()
                                        val bitmap = decodeSampledBitmap(bytes)
                                        if (bitmap != null) {
                                            val embedding = extractEmbedding(bitmap)
                                            if (embedding != null) {
                                                embeddings.add(label to embedding)
                                            } else {
                                                failedImages.add("$label (no face detected)")
                                            }
                                        } else {
                                            failedImages.add("$label (decode failed)")
                                        }
                                    } catch (e: Exception) {
                                        failedImages.add("$label (${e.message})")
                                    }
                                }
                            }
                            zipStream.closeEntry()
                            entry = zipStream.nextEntry
                        }
                    }
                }

                if (embeddings.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No valid face images found in ZIP.\nFailed: ${failedImages.joinToString()}"
                    )
                    return@launch
                }

                val labels = embeddings.map { it.first }
                val n = embeddings.size
                val matrix = MutableList(n) { mutableListOf<BenchmarkCellResult?>() }

                for (i in 0 until n) {
                    for (j in 0 until n) {
                        _uiState.value = _uiState.value.copy(
                            progress = "Matching ${labels[i]} x ${labels[j]} (${i * n + j + 1}/${n * n})"
                        )
                        val start = System.nanoTime()
                        val sim = faceVerifier.cosineSimilarity(
                            embeddings[i].second,
                            embeddings[j].second
                        )
                        val elapsedMs = (System.nanoTime() - start) / 1_000_000
                        matrix[i].add(BenchmarkCellResult(sim, elapsedMs))
                    }
                }

                val csv = buildCsv(labels, matrix, failedImages)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    progress = "",
                    labels = labels,
                    matrix = matrix.map { it.toList() },
                    csvContent = csv,
                    failedImages = failedImages
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Benchmark failed: ${e.message}"
                )
            }
        }
    }

    private fun decodeSampledBitmap(bytes: ByteArray): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

        options.inSampleSize = calculateInSampleSize(options, 1024, 1024)
        options.inJustDecodeBounds = false

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private suspend fun extractEmbedding(bitmap: Bitmap): FloatArray? {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val faces = try {
            faceDetector.process(inputImage).await()
        } catch (e: Exception) {
            bitmap.recycle()
            return null
        }

        if (faces.isEmpty()) {
            bitmap.recycle()
            return null
        }

        val face = faces[0]
        val boundingBox = FacePreprocessor.expandBoundingBox(
            RectF(face.boundingBox),
            bitmap.width,
            bitmap.height
        )

        var faceBitmap = FacePreprocessor.cropFace(bitmap, boundingBox)
        faceBitmap = FacePreprocessor.scaleToInputSize(faceBitmap)
        val buffer = FacePreprocessor.toNormalizedRgbBuffer(faceBitmap)
        faceBitmap.recycle()
        return embeddingExtractor.getEmbedding(buffer)
    }

    private fun buildCsv(
        labels: List<String>,
        matrix: List<List<BenchmarkCellResult?>>,
        failedImages: List<String>
    ): String {
        val sb = StringBuilder()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())

        sb.appendLine("Device Info,${getDeviceInfo()}")
        sb.appendLine("Date,$timestamp")
        sb.appendLine("Model,MobileFaceNet (112x112 -> 192-d)")
        sb.appendLine()

        sb.append("Similarity (%)")
        labels.forEach { sb.append(",$it") }
        sb.appendLine()
        matrix.forEachIndexed { i, row ->
            sb.append(labels[i])
            row.forEach { cell ->
                if (cell != null) {
                    sb.append(",${String.format(Locale.US, "%.2f", cell.similarity * 100)}")
                } else {
                    sb.append(",N/A")
                }
            }
            sb.appendLine()
        }

        sb.appendLine()
        sb.append("Time (ms)")
        labels.forEach { sb.append(",$it") }
        sb.appendLine()
        matrix.forEachIndexed { i, row ->
            sb.append(labels[i])
            row.forEach { cell ->
                if (cell != null) {
                    sb.append(",${cell.elapsedMs}")
                } else {
                    sb.append(",N/A")
                }
            }
            sb.appendLine()
        }

        if (failedImages.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Failed Images")
            failedImages.forEach { sb.appendLine(it) }
        }

        return sb.toString()
    }
}

package com.atharvakale.facerecognition

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
class FaceAlignmentBenchmarkTest {

    companion object {
        private const val TAG = "FaceBenchmark"
        private const val INPUT_SIZE = 112
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 128.0f
        private const val OUTPUT_SIZE = 192
        private const val REF_LEFT_X = 38.2946f
        private const val REF_LEFT_Y = 51.6963f
        private const val REF_RIGHT_X = 73.5318f
        private const val REF_RIGHT_Y = 51.5014f
    }

    private fun loadTestImagesDir(): File {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        return File(context.getExternalFilesDir(null), "benchmark_images")
    }

    private fun loadTestImage(file: File): Bitmap {
        val fullBitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw IllegalArgumentException("Cannot decode image: ${file.name}")
        val maxDim = 800
        val scale = minOf(maxDim.toFloat() / fullBitmap.width, maxDim.toFloat() / fullBitmap.height, 1f)
        if (scale >= 1f) return fullBitmap
        val w = (fullBitmap.width * scale).toInt()
        val h = (fullBitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(fullBitmap, w, h, true)
    }

    private fun loadModelBuffer(): MappedByteBuffer {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val fileDescriptor = context.assets.openFd("mobile_face_net.tflite")
        val inputStream = java.io.FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            java.nio.channels.FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    private fun runInference(interpreter: org.tensorflow.lite.Interpreter, inputBuffer: ByteBuffer): FloatArray {
        val inputArray = arrayOf(inputBuffer)
        val embeddings = arrayOf(FloatArray(OUTPUT_SIZE))
        val outputMap = mapOf(0 to embeddings)
        interpreter.runForMultipleInputsOutputs(inputArray, outputMap)
        val result = embeddings[0]
        return l2Normalize(result)
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var normSq = 0f
        for (v in vector) normSq += v * v
        val norm = sqrt(normSq)
        if (norm < 1e-10f) return vector
        return FloatArray(vector.size) { i -> vector[i] / norm }
    }

    private fun toNormalizedRgbBuffer(bitmap: Bitmap): ByteBuffer {
        val imgData = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        imgData.order(ByteOrder.nativeOrder())
        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val pixelValue = intValues[i * INPUT_SIZE + j]
                val r = (pixelValue shr 16) and 0xFF
                val g = (pixelValue shr 8) and 0xFF
                val b = pixelValue and 0xFF
                imgData.putFloat((r - IMAGE_MEAN) / IMAGE_STD)
                imgData.putFloat((g - IMAGE_MEAN) / IMAGE_STD)
                imgData.putFloat((b - IMAGE_MEAN) / IMAGE_STD)
            }
        }
        imgData.rewind()
        return imgData
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denominator = sqrt(normA) * sqrt(normB)
        if (denominator < 1e-10f) return 0f
        return (dot / denominator).coerceIn(-1f, 1f)
    }

    private fun alignFace(source: Bitmap, leftEye: PointF, rightEye: PointF): Bitmap {
        val srcLeft: PointF
        val srcRight: PointF
        if (leftEye.x <= rightEye.x) {
            srcLeft = leftEye; srcRight = rightEye
        } else {
            srcLeft = rightEye; srcRight = leftEye
        }

        val srcCx = (srcLeft.x + srcRight.x) / 2f
        val srcCy = (srcLeft.y + srcRight.y) / 2f
        val srcDx = srcRight.x - srcLeft.x
        val srcDy = srcRight.y - srcLeft.y
        val srcDist = sqrt(srcDx * srcDx + srcDy * srcDy)
        val srcAngle = atan2(srcDy, srcDx)

        val tgtDx = REF_RIGHT_X - REF_LEFT_X
        val tgtDy = REF_RIGHT_Y - REF_LEFT_Y
        val tgtDist = sqrt(tgtDx * tgtDx + tgtDy * tgtDy)
        val tgtCx = (REF_LEFT_X + REF_RIGHT_X) / 2f
        val tgtCy = (REF_LEFT_Y + REF_RIGHT_Y) / 2f
        val tgtAngle = atan2(tgtDy, tgtDx)

        val scale = tgtDist / srcDist
        val rotRad = tgtAngle - srcAngle
        val cosR = cos(rotRad) * scale
        val sinR = sin(rotRad) * scale

        val a = cosR; val b = -sinR; val c = sinR; val d = cosR
        val tx = tgtCx - a * srcCx - b * srcCy
        val ty = tgtCy - c * srcCx - d * srcCy

        val matrix = Matrix()
        matrix.setValues(floatArrayOf(a, b, tx, c, d, ty, 0f, 0f, 1f))

        val result = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.rgb(128, 128, 128))
        canvas.drawBitmap(source, matrix, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
        return result
    }

    private fun cropAndScaleOld(source: Bitmap, boundingBox: RectF): Bitmap {
        val width = boundingBox.width().toInt().coerceAtLeast(1)
        val height = boundingBox.height().toInt().coerceAtLeast(1)
        val cropped = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cropped)
        val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
        val matrix = Matrix()
        matrix.postTranslate(-boundingBox.left, -boundingBox.top)
        canvas.drawBitmap(source, matrix, paint)
        return Bitmap.createScaledBitmap(cropped, INPUT_SIZE, INPUT_SIZE, true)
    }

    data class EmbeddingResult(
        val name: String,
        val embedding: FloatArray,
        val pipeline: String
    )

    @Test
    fun benchmarkAlignedVsOldPipeline() {
        val imagesDir = loadTestImagesDir()
        val imageFiles = imagesDir.listFiles()
            ?.filter { it.extension.lowercase() in listOf("jpg", "jpeg", "png") }
            ?.sortedBy { it.name }
            ?: emptyList()

        if (imageFiles.isEmpty()) {
            Log.d(TAG, "================================================================")
            Log.d(TAG, "SKIPPED: No test images found in ${imagesDir.absolutePath}")
            Log.d(TAG, "To run this benchmark, push face images to the device:")
            Log.d(TAG, "  adb shell mkdir -p ${imagesDir.absolutePath}")
            Log.d(TAG, "  adb push <image>.jpg ${imagesDir.absolutePath}/")
            Log.d(TAG, "================================================================")
            return
        }

        val interpreter = org.tensorflow.lite.Interpreter(loadModelBuffer())

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()
        val detector = FaceDetection.getClient(options)

        val alignedResults = mutableListOf<EmbeddingResult>()
        val oldResults = mutableListOf<EmbeddingResult>()

        Log.d(TAG, "================================================================")
        Log.d(TAG, "FACE ALIGNMENT BENCHMARK (Android on-device)")
        Log.d(TAG, "================================================================")

        for (file in imageFiles) {
            val shortName = file.nameWithoutExtension
            val bitmap = loadTestImage(file)
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            val faces = com.google.android.gms.tasks.Tasks.await(detector.process(inputImage))
            if (faces.isEmpty()) {
                Log.d(TAG, "  $shortName: NO FACE DETECTED")
                continue
            }

            val face = faces[0]
            val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
            val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position

            if (leftEye != null && rightEye != null) {
                val aligned = alignFace(bitmap, leftEye, rightEye)
                val alignedBuffer = toNormalizedRgbBuffer(aligned)
                val alignedEmb = runInference(interpreter, alignedBuffer)
                alignedResults.add(EmbeddingResult(shortName, alignedEmb, "ALIGNED"))
                Log.d(TAG, "  $shortName (aligned): OK  norm=${String.format("%.6f", sqrt(alignedEmb.fold(0f) { a, v -> a + v * v }))}")
            }

            val bitmap2 = loadTestImage(file)
            val inputImage2 = InputImage.fromBitmap(bitmap2, 0)
            val faces2 = com.google.android.gms.tasks.Tasks.await(detector.process(inputImage2))
            if (faces2.isNotEmpty()) {
                val bbox = RectF(faces2[0].boundingBox)
                val margin = 0.3f
                val dx = bbox.width() * margin / 2f
                val dy = bbox.height() * margin / 2f
                val expanded = RectF(
                    (bbox.left - dx).coerceAtLeast(0f),
                    (bbox.top - dy).coerceAtLeast(0f),
                    (bbox.right + dx).coerceAtMost(bitmap2.width.toFloat()),
                    (bbox.bottom + dy).coerceAtMost(bitmap2.height.toFloat())
                )
                val oldFace = cropAndScaleOld(bitmap2, expanded)
                val oldBuffer = toNormalizedRgbBuffer(oldFace)
                val oldEmb = runInference(interpreter, oldBuffer)
                oldResults.add(EmbeddingResult(shortName, oldEmb, "OLD"))
                Log.d(TAG, "  $shortName (old):     OK")
            }
        }

        Log.d(TAG, "")
        Log.d(TAG, "================================================================")
        Log.d(TAG, "COSINE SIMILARITY MATRIX — ALIGNED PIPELINE")
        Log.d(TAG, "================================================================")
        printSimilarityMatrix(alignedResults)

        Log.d(TAG, "")
        Log.d(TAG, "================================================================")
        Log.d(TAG, "COSINE SIMILARITY MATRIX — OLD CROP+SCALE PIPELINE")
        Log.d(TAG, "================================================================")
        printSimilarityMatrix(oldResults)

        interpreter.close()
    }

    private fun printSimilarityMatrix(results: List<EmbeddingResult>) {
        if (results.size < 2) {
            Log.d(TAG, "  Not enough results for matrix")
            return
        }

        val header = results.joinToString("") { String.format("%12s", it.name.take(10)) }
        Log.d(TAG, "  ${"".padEnd(12)}$header")

        for (a in results) {
            val row = StringBuilder(String.format("  %-12s", a.name.take(10)))
            for (b in results) {
                val sim = cosineSimilarity(a.embedding, b.embedding)
                row.append(String.format("%12.4f", sim))
            }
            Log.d(TAG, row.toString())
        }
    }
}
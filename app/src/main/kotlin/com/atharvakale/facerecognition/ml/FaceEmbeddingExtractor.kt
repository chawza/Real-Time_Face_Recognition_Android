package com.atharvakale.facerecognition.ml

import android.content.Context
import android.content.res.AssetFileDescriptor
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class FaceEmbeddingExtractor @Inject constructor() {

    companion object {
        private const val MODEL_FILE = "mobile_face_net.tflite"
        private const val OUTPUT_SIZE = 192
    }

    private var interpreter: Interpreter? = null

    fun initialize(context: Context) {
        if (interpreter != null) return
        interpreter = Interpreter(loadModelFile(context, MODEL_FILE))
    }

    fun getEmbedding(inputBuffer: java.nio.ByteBuffer): FloatArray {
        val inputArray = arrayOf(inputBuffer)
        val embeddings = arrayOf(FloatArray(OUTPUT_SIZE))
        val outputMap = mapOf(0 to embeddings)

        interpreter?.runForMultipleInputsOutputs(inputArray, outputMap)

        val result = embeddings[0]
        return l2Normalize(result)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var normSq = 0f
        for (v in vector) normSq += v * v
        val norm = sqrt(normSq)
        if (norm < 1e-10f) return vector
        return FloatArray(vector.size) { i -> vector[i] / norm }
    }

    @Throws(IOException::class)
    private fun loadModelFile(context: Context, modelFile: String): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(modelFile)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset: Long = fileDescriptor.startOffset
        val declaredLength: Long = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}
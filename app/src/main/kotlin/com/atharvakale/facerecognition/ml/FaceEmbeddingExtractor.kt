package com.atharvakale.facerecognition.ml

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceEmbeddingExtractor @Inject constructor() {

    companion object {
        private const val MODEL_FILE = "mobile_face_net.tflite"
        private const val INPUT_SIZE = 112
        private const val OUTPUT_SIZE = 192
        private const val IMAGE_MEAN = 128.0f
        private const val IMAGE_STD = 128.0f
    }

    private var interpreter: Interpreter? = null

    fun initialize(context: Context) {
        if (interpreter != null) return
        interpreter = Interpreter(loadModelFile(context, MODEL_FILE))
    }

    fun getEmbedding(bitmap: Bitmap): FloatArray {
        val imgData = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        imgData.order(ByteOrder.nativeOrder())

        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        imgData.rewind()

for (i in 0 until INPUT_SIZE) {
                for (j in 0 until INPUT_SIZE) {
                    val pixelValue = intValues[i * INPUT_SIZE + j]
                    imgData.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData.putFloat((((pixelValue shr 8) and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData.putFloat((((pixelValue shr 16) and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                }
            }

        val inputArray = arrayOf(imgData)
        val embeddings = arrayOf(FloatArray(OUTPUT_SIZE))
        val outputMap = mapOf(0 to embeddings)

        interpreter?.runForMultipleInputsOutputs(inputArray, outputMap)

        return embeddings[0]
    }

    fun close() {
        interpreter?.close()
        interpreter = null
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
package com.atharvakale.facerecognition.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import java.nio.ByteBuffer
import java.nio.ByteOrder

object FacePreprocessor {

    private const val INPUT_SIZE = 112
    private const val IMAGE_MEAN = 127.5f
    private const val IMAGE_STD = 128.0f

    fun cropFace(source: Bitmap, boundingBox: RectF): Bitmap {
        val width = boundingBox.width().toInt().coerceAtLeast(1)
        val height = boundingBox.height().toInt().coerceAtLeast(1)
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        paint.color = Color.WHITE
        canvas.drawRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), paint)
        val matrix = Matrix()
        matrix.postTranslate(-boundingBox.left, -boundingBox.top)
        canvas.drawBitmap(source, matrix, paint)
        if (!source.isRecycled) source.recycle()
        return resultBitmap
    }

    fun expandBoundingBox(box: RectF, imageWidth: Int, imageHeight: Int, margin: Float = 0.3f): RectF {
        val width = box.width()
        val height = box.height()
        val dx = width * margin / 2f
        val dy = height * margin / 2f
        return RectF(
            (box.left - dx).coerceAtLeast(0f),
            (box.top - dy).coerceAtLeast(0f),
            (box.right + dx).coerceAtMost(imageWidth.toFloat()),
            (box.bottom + dy).coerceAtMost(imageHeight.toFloat())
        )
    }

    fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int, flipX: Boolean, flipY: Boolean): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        matrix.postScale(if (flipX) -1.0f else 1.0f, if (flipY) -1.0f else 1.0f)
        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
        if (rotatedBitmap != bitmap) bitmap.recycle()
        return rotatedBitmap
    }

    fun scaleToInputSize(bitmap: Bitmap, size: Int = 112): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scaleWidth = size.toFloat() / width
        val scaleHeight = size.toFloat() / height
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        val resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
        bitmap.recycle()
        return resizedBitmap
    }

    fun toNormalizedRgbBuffer(bitmap: Bitmap): ByteBuffer {
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
}
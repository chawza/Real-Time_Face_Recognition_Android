package com.atharvakale.facerecognition.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.YuvImage
import android.media.Image
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.abs

object FacePreprocessor {

    fun cropFace(source: Bitmap, boundingBox: RectF): Bitmap {
        val expanded = expandBoundingBox(boundingBox, source.width, source.height)
        val width = expanded.width().toInt().coerceAtLeast(1)
        val height = expanded.height().toInt().coerceAtLeast(1)
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        paint.color = Color.WHITE
        canvas.drawRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), paint)
        val matrix = Matrix()
        matrix.postTranslate(-expanded.left, -expanded.top)
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

    fun yuvToBitmap(image: Image): Bitmap {
        val nv21 = yuv420ToNv21(image)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(
            Rect(0, 0, yuvImage.width, yuvImage.height), 100, out
        )
        val imageBytes = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun yuv420ToNv21(image: Image): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 4
        val nv21 = ByteArray(ySize + uvSize * 2)

        val yBuffer: ByteBuffer = image.planes[0].buffer
        val uBuffer: ByteBuffer = image.planes[1].buffer
        val vBuffer: ByteBuffer = image.planes[2].buffer

        var rowStride = image.planes[0].rowStride
        var pos = 0

        if (rowStride == width) {
            yBuffer.get(nv21, 0, ySize)
            pos += ySize
        } else {
            var yBufferPos = -rowStride.toLong()
            while (pos < ySize) {
                yBufferPos += rowStride
                yBuffer.position(yBufferPos.toInt())
                yBuffer.get(nv21, pos, width)
                pos += width
            }
        }

        rowStride = image.planes[2].rowStride
        val pixelStride = image.planes[2].pixelStride

        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val vuPos = col * pixelStride + row * rowStride
                nv21[pos++] = vBuffer.get(vuPos)
                nv21[pos++] = uBuffer.get(vuPos)
            }
        }

        return nv21
    }
}
package com.atharvakale.facerecognition.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object FacePreprocessor {

    private const val INPUT_SIZE = 112
    private const val IMAGE_MEAN = 127.5f
    private const val IMAGE_STD = 128.0f
    private const val REF_LEFT_X = 38.2946f
    private const val REF_LEFT_Y = 51.6963f
    private const val REF_RIGHT_X = 73.5318f
    private const val REF_RIGHT_Y = 51.5014f

    fun alignFace(source: Bitmap, leftEye: PointF, rightEye: PointF): Bitmap {
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
        canvas.drawBitmap(source, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        return result
    }

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
package com.atharvakale.facerecognition.ui.components

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun FaceBoundingBoxOverlay(
    boundingBox: RectF?,
    imageWidth: Int,
    imageHeight: Int,
    flipX: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00E676)
) {
    if (boundingBox == null || imageWidth == 0 || imageHeight == 0) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val viewWidth = size.width
        val viewHeight = size.height

        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        val scale = maxOf(scaleX, scaleY)

        val offsetX = (viewWidth - imageWidth * scale) / 2f
        val offsetY = (viewHeight - imageHeight * scale) / 2f

        val left = if (flipX) {
            offsetX + (imageWidth - boundingBox.right) * scale
        } else {
            offsetX + boundingBox.left * scale
        }
        val top = offsetY + boundingBox.top * scale
        val right = if (flipX) {
            offsetX + (imageWidth - boundingBox.left) * scale
        } else {
            offsetX + boundingBox.right * scale
        }
        val bottom = offsetY + boundingBox.bottom * scale

        val strokeWidth = 3.dp.toPx()
        val cornerLen = 24.dp.toPx()
        val cornerRadius = 8.dp.toPx()

        drawRoundRect(
            color = color,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            alpha = 0.6f,
            style = Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f))
            )
        )

        val white = Color.White
        val sw = strokeWidth * 1.5f

        drawLine(white, Offset(left, top), Offset(left + cornerLen, top), sw)
        drawLine(white, Offset(left, top), Offset(left, top + cornerLen), sw)

        drawLine(white, Offset(right, top), Offset(right - cornerLen, top), sw)
        drawLine(white, Offset(right, top), Offset(right, top + cornerLen), sw)

        drawLine(white, Offset(left, bottom), Offset(left + cornerLen, bottom), sw)
        drawLine(white, Offset(left, bottom), Offset(left, bottom - cornerLen), sw)

        drawLine(white, Offset(right, bottom), Offset(right - cornerLen, bottom), sw)
        drawLine(white, Offset(right, bottom), Offset(right, bottom - cornerLen), sw)
    }
}

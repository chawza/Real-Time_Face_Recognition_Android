package com.atharvakale.facerecognition.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.atharvakale.facerecognition.ui.theme.MatchGreen
import com.atharvakale.facerecognition.ui.theme.PanelBackground
import com.atharvakale.facerecognition.ui.theme.Teal200
import com.atharvakale.facerecognition.ui.theme.UnknownRed

@Composable
fun MetricsPanel(
    matchedName: String,
    distance: Float,
    confidence: Float,
    inferenceTimeMs: Long,
    fps: Float,
    dbFaceCount: Int,
    statusText: String,
    faceThumbnail: android.graphics.Bitmap? = null,
    modifier: Modifier = Modifier
) {
    val nameColor = if (matchedName == "Unknown" || matchedName == "No DB" || matchedName == "No Face Detected") {
        UnknownRed
    } else {
        MatchGreen
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(PanelBackground)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (faceThumbnail != null) {
                androidx.compose.foundation.Image(
                    bitmap = faceThumbnail.asImageBitmap(),
                    contentDescription = "Detected face",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column {
                Text(
                    text = matchedName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Confidence: ${"%.1f".format(confidence)}%",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Teal200
                )
                Text(
                    text = "Inference: $inferenceTimeMs ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "FPS: ${"%.1f".format(fps)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                    Text(
                        text = "DB: $dbFaceCount faces",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}
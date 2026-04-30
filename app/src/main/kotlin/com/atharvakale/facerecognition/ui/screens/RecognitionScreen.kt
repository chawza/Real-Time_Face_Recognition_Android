package com.atharvakale.facerecognition.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.atharvakale.facerecognition.ui.components.CameraPreview
import com.atharvakale.facerecognition.ui.components.FaceBoundingBoxOverlay
import com.atharvakale.facerecognition.ui.components.MetricsPanel
import com.atharvakale.facerecognition.viewmodel.RecognitionViewModel

@Composable
fun RecognitionScreen(
    onBack: () -> Unit,
    viewModel: RecognitionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_FRONT) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreview(
                lensFacing = lensFacing,
                flipX = lensFacing == CameraSelector.LENS_FACING_FRONT,
                analyzer = viewModel.faceDetectionAnalyzer,
                onResult = { result -> viewModel.onFaceAnalyzed(result) },
                modifier = Modifier.fillMaxSize()
            )

            FaceBoundingBoxOverlay(
                boundingBox = uiState.boundingBox,
                imageWidth = uiState.imageWidth,
                imageHeight = uiState.imageHeight,
                flipX = lensFacing == CameraSelector.LENS_FACING_FRONT,
                modifier = Modifier.fillMaxSize()
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        IconButton(
            onClick = {
                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                    CameraSelector.LENS_FACING_BACK
                else CameraSelector.LENS_FACING_FRONT
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(
                Icons.Default.Cameraswitch,
                contentDescription = "Switch camera",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "Recognition Mode",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        )

            MetricsPanel(
                matchedName = uiState.matchedName,
                confidence = uiState.confidence,
                detectionTimeMs = uiState.detectionTimeMs,
                preprocessingTimeMs = uiState.preprocessingTimeMs,
                embeddingTimeMs = uiState.embeddingTimeMs,
                similarityTimeMs = uiState.similarityTimeMs,
                fps = uiState.fps,
                dbFaceCount = uiState.dbFaceCount,
                statusText = uiState.statusText,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
    }
}

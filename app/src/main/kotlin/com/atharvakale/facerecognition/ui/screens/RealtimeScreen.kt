package com.atharvakale.facerecognition.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.atharvakale.facerecognition.ui.components.CameraPreview
import com.atharvakale.facerecognition.ui.components.MetricsPanel
import com.atharvakale.facerecognition.viewmodel.RealtimeViewModel
import androidx.camera.core.CameraSelector

@Composable
fun RealtimeScreen(
    viewModel: RealtimeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreview(
                lensFacing = CameraSelector.LENS_FACING_FRONT,
                onAnalysisReady = { },
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            text = uiState.statusText,
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.TopStart)
        )

        MetricsPanel(
            matchedName = uiState.matchedName,
            distance = uiState.distance,
            confidence = uiState.confidence,
            inferenceTimeMs = uiState.inferenceTimeMs,
            fps = uiState.fps,
            dbFaceCount = uiState.dbFaceCount,
            statusText = uiState.statusText,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

package com.atharvakale.facerecognition.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.atharvakale.facerecognition.ui.theme.MatchGreen
import com.atharvakale.facerecognition.ui.theme.UnknownRed
import com.atharvakale.facerecognition.viewmodel.VerificationPhase
import com.atharvakale.facerecognition.viewmodel.VerificationViewModel

@Composable
fun VerificationScreen(
    onBack: () -> Unit,
    viewModel: VerificationViewModel = hiltViewModel()
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

    when (uiState.phase) {
        VerificationPhase.SELECT -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text("Select Face to Verify", style = MaterialTheme.typography.headlineMedium)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.registeredFaces.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No faces in database", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.registeredFaces, key = { it.name }) { face ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectFace(face) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 16.dp)
                                ) {
                                    Text(
                                        text = face.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Tap to verify",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        VerificationPhase.VERIFYING -> {
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
                    onClick = { viewModel.backToSelect() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
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
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                val targetName = uiState.selectedFace?.name ?: ""
                Text(
                    text = "Verifying: $targetName",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )

                MetricsPanel(
                    matchedName = if (uiState.isMatch) "MATCH" else "NO MATCH",
                    confidence = uiState.confidence,
                    detectionTimeMs = uiState.detectionTimeMs,
                    preprocessingTimeMs = uiState.preprocessingTimeMs,
                    embeddingTimeMs = uiState.embeddingTimeMs,
                    similarityTimeMs = uiState.similarityTimeMs,
                    fps = uiState.fps,
                    dbFaceCount = 1,
                    statusText = uiState.statusText,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

package com.atharvakale.facerecognition.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.atharvakale.facerecognition.ml.FaceDetectionAnalyzer
import com.atharvakale.facerecognition.ui.components.ActionsDialog
import com.atharvakale.facerecognition.ui.components.AddFaceDialog
import com.atharvakale.facerecognition.ui.components.CameraPreview
import com.atharvakale.facerecognition.ui.components.DeleteFacesDialog
import com.atharvakale.facerecognition.ui.components.FaceListDialog
import com.atharvakale.facerecognition.ui.components.FacePreviewCard
import com.atharvakale.facerecognition.ui.components.HyperparameterDialog
import com.atharvakale.facerecognition.viewmodel.MainViewModel
import com.atharvakale.facerecognition.viewmodel.ScreenMode

@Composable
fun MainScreen(
    onNavigateToRealtime: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showActionsDialog by remember { mutableStateOf(false) }
    var showAddFaceDialog by remember { mutableStateOf(false) }
    var showFaceListDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHyperparameterDialog by remember { mutableStateOf(false) }

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

    val analyzer = remember { mutableStateOf<FaceDetectionAnalyzer?>(null) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    CameraPreview(
                        lensFacing = uiState.cameraLensFacing,
                        onAnalysisReady = { imageAnalyzer ->
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    IconButton(
                        onClick = { viewModel.switchCamera() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch camera",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (uiState.mode == ScreenMode.RECOGNIZE) {
                            viewModel.setMode(ScreenMode.ADD_FACE)
                        } else {
                            viewModel.setMode(ScreenMode.RECOGNIZE)
                        }
                    }
                ) {
                    Text(if (uiState.mode == ScreenMode.RECOGNIZE) "Add Face" else "Recognize")
                }

                if (uiState.mode == ScreenMode.ADD_FACE) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { showAddFaceDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add face")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { /* gallery */ }) {
                        Icon(Icons.Default.Collections, contentDescription = "Gallery")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FacePreviewCard(
                bitmap = uiState.facePreview,
                name = uiState.recognizedName,
                mode = uiState.mode,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = { showActionsDialog = true }) {
                Text("ACTIONS")
            }
        }
    }

    if (showActionsDialog) {
        ActionsDialog(
            onAction = { index ->
                showActionsDialog = false
                when (index) {
                    0 -> showFaceListDialog = true
                    1 -> showDeleteDialog = true
                    2 -> viewModel.saveFaces()
                    3 -> viewModel.loadFaces()
                    4 -> viewModel.clearAll()
                    5 -> { /* gallery import */ }
                    6 -> showHyperparameterDialog = true
                    7 -> viewModel.toggleDeveloperMode(!uiState.developerMode)
                    8 -> onNavigateToRealtime()
                }
            },
            onDismiss = { showActionsDialog = false }
        )
    }

    if (showAddFaceDialog) {
        AddFaceDialog(
            onConfirm = { name ->
                viewModel.addFace(name)
                showAddFaceDialog = false
            },
            onDismiss = { showAddFaceDialog = false }
        )
    }

    if (showFaceListDialog) {
        FaceListDialog(
            faces = uiState.registeredFaceNames,
            onDismiss = { showFaceListDialog = false }
        )
    }

    if (showDeleteDialog) {
        DeleteFacesDialog(
            faces = uiState.registeredFaceNames,
            onConfirm = { names ->
                names.forEach { viewModel.deleteFace(it) }
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showHyperparameterDialog) {
        HyperparameterDialog(
            currentValue = uiState.distanceThreshold,
            onUpdate = { viewModel.updateThreshold(it) },
            onDismiss = { showHyperparameterDialog = false }
        )
    }
}

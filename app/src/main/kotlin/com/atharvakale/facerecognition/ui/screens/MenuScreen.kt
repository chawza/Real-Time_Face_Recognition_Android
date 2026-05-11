package com.atharvakale.facerecognition.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.atharvakale.facerecognition.ui.components.HyperparameterDialog
import com.atharvakale.facerecognition.viewmodel.SettingsViewModel

@Composable
fun MenuScreen(
    onNavigateToDatabase: () -> Unit,
    onNavigateToRecognition: () -> Unit,
    onNavigateToVerification: () -> Unit,
    onNavigateToBenchmark: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showHyperparameterDialog by remember { mutableStateOf(false) }
    val currentThreshold by viewModel.distanceThreshold.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Face Recognition",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNavigateToDatabase,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(
                Icons.Default.List,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text("  Database List", style = MaterialTheme.typography.titleMedium)
        }

        Button(
            onClick = onNavigateToRecognition,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(
                Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text("  Face Recognition", style = MaterialTheme.typography.titleMedium)
        }

        Button(
            onClick = onNavigateToVerification,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(
                Icons.Default.VerifiedUser,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text("  Face Verification", style = MaterialTheme.typography.titleMedium)
        }

        Button(
            onClick = onNavigateToBenchmark,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(
                Icons.Default.Assessment,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text("  ZIP Benchmark", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = { showHyperparameterDialog = true }) {
            Text("Settings")
        }
    }

    if (showHyperparameterDialog) {
        HyperparameterDialog(
            currentValue = currentThreshold,
            onUpdate = { viewModel.updateThreshold(it) },
            onDismiss = { showHyperparameterDialog = false }
        )
    }
}
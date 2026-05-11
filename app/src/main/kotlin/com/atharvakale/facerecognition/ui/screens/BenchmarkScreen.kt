package com.atharvakale.facerecognition.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.atharvakale.facerecognition.viewmodel.BenchmarkCellResult
import com.atharvakale.facerecognition.viewmodel.BenchmarkViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(
    onBack: () -> Unit,
    viewModel: BenchmarkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val horizontalScrollState = rememberScrollState()

    val zipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.runBenchmark(it) }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                os.write(uiState.csvContent!!.toByteArray(Charsets.UTF_8))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ZIP Benchmark") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Device: ${uiState.deviceInfo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Button(
                onClick = {
                    zipLauncher.launch(
                        arrayOf("application/zip", "application/x-zip-compressed")
                    )
                },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.FolderZip,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select ZIP File", style = MaterialTheme.typography.titleMedium)
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = uiState.progress,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (uiState.labels.isNotEmpty() && uiState.matrix.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Cross-Matching Results (${uiState.labels.size} images)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        ResultsTable(
                            labels = uiState.labels,
                            matrix = uiState.matrix,
                            horizontalScrollState = horizontalScrollState
                        )
                    }
                }

                if (uiState.failedImages.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = "Failed: ${uiState.failedImages.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        val timestamp = SimpleDateFormat(
                            "yyyyMMdd_HHmmss", Locale.US
                        ).format(Date())
                        csvLauncher.launch("benchmark_$timestamp.csv")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download CSV", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun ResultsTable(
    labels: List<String>,
    matrix: List<List<BenchmarkCellResult?>>,
    horizontalScrollState: androidx.compose.foundation.ScrollState
) {
    val cellWidth = 85.dp
    val cellHeight = 56.dp
    val labelWidth = 76.dp
    val headerHeight = 52.dp
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val headerBg = MaterialTheme.colorScheme.surfaceVariant
    val diagonalBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)

    Column(
        modifier = Modifier.horizontalScroll(horizontalScrollState)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .width(labelWidth)
                    .height(headerHeight)
                    .border(1.dp, borderColor)
                    .background(headerBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            labels.forEach { label ->
                Box(
                    modifier = Modifier
                        .width(cellWidth)
                        .height(headerHeight)
                        .border(1.dp, borderColor)
                        .background(headerBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }

        matrix.forEachIndexed { i, row ->
            Row {
                Box(
                    modifier = Modifier
                        .width(labelWidth)
                        .height(cellHeight)
                        .border(1.dp, borderColor)
                        .background(headerBg),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = labels[i],
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }

                row.forEachIndexed { j, cell ->
                    val isDiagonal = i == j
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .height(cellHeight)
                            .border(1.dp, borderColor)
                            .background(if (isDiagonal) diagonalBg else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (cell != null) {
                            val pct = cell.similarity * 100f
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format("%.1f%%", pct),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        pct >= 80f -> Color(0xFF2E7D32)
                                        pct >= 50f -> Color(0xFFF57F17)
                                        else -> Color(0xFFC62828)
                                    }
                                )
                                Text(
                                    text = "${cell.elapsedMs}ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text(
                                "N/A",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

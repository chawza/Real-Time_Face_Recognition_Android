package com.atharvakale.facerecognition.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun HyperparameterDialog(
    currentValue: Float,
    onUpdate: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(currentValue.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Similarity Threshold") },
        text = {
            Column {
                Text("Minimum similarity to consider a face match (0.0 - 1.0):")
                Text("")
                Text("Higher = stricter matching")
                Text("  0.8+: Very strict")
                Text("  0.3: Balanced (default)")
                Text("  0.1: Lenient (more false matches)")
                Text("")
                Text("Threshold Value:")
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                value.toFloatOrNull()?.let { onUpdate(it) }
            }) { Text("Update") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
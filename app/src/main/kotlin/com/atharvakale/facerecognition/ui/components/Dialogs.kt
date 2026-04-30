package com.atharvakale.facerecognition.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AddFaceDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                enabled = name.isNotBlank()
            ) { Text("ADD") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ActionsDialog(
    onAction: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val actions = listOf(
        "View Recognition List",
        "Update Recognition List",
        "Save Recognitions",
        "Load Recognitions",
        "Clear All Recognitions",
        "Import Photo",
        "Hyperparameters",
        "Developer Mode",
        "Real-Time Metrics View"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Action") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                actions.forEachIndexed { index, action ->
                    TextButton(
                        onClick = { onAction(index) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(action, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun FaceListDialog(
    faces: List<String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { if (faces.isEmpty()) Text("No Faces Added!!") else Text("Recognitions") },
        text = {
            if (faces.isEmpty()) {
                Text("No registered faces found.")
            } else {
                Column {
                    faces.forEach { name ->
                        Text(name, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

@Composable
fun DeleteFacesDialog(
    faces: List<String>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val checkedStates = remember { faces.map { mutableStateOf(false) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Recognition to delete") },
        text = {
            if (faces.isEmpty()) {
                Text("No faces to delete.")
            } else {
                Column {
                    faces.forEachIndexed { index, name ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = checkedStates[index].value,
                                onCheckedChange = { checkedStates[index].value = it }
                            )
                            Text(name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val toDelete = faces.filterIndexed { index, _ -> checkedStates[index].value }
                onConfirm(toDelete)
            }) { Text("Delete Selected") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun HyperparameterDialog(
    currentValue: Float,
    onUpdate: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(currentValue.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Euclidean Distance") },
        text = {
            Column {
                Text("0.00 -> Perfect Match")
                Text("1.00 -> Default")
                Text("Turn On Developer Mode to find optimum value")
                Text("\nCurrent Value:")
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

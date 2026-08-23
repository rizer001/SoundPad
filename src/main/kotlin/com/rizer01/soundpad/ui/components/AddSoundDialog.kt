package com.rizer01.soundpad.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rizer01.soundpad.model.SoundCategory
import com.rizer01.soundpad.model.SoundFile

@Composable
fun AddSoundDialog(
    categories: List<SoundCategory>,
    onDismiss: () -> Unit,
    onAdd: (List<SoundFile>) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.id ?: "default") }
    var volume by remember { mutableStateOf(1.0f) }
    var loop by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "🎵 Add Sounds",
                    style = MaterialTheme.typography.headlineSmall
                )

                HorizontalDivider()

                // Drag & drop zone
                Surface(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("📂", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "Drag & drop audio files here",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "MP3, WAV, OGG, FLAC, M4A, AAC, WMA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Category selector
                Text("Category", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.take(4).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.id,
                            onClick = { selectedCategory = cat.id },
                            label = { Text(cat.name) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Volume
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔊", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${(volume * 100).toInt()}%")
                }

                // Loop toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔁 Loop", modifier = Modifier.weight(1f))
                    Switch(checked = loop, onCheckedChange = { loop = it })
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            // TODO: Open file picker and create SoundFiles
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Files")
                    }
                }
            }
        }
    }
}

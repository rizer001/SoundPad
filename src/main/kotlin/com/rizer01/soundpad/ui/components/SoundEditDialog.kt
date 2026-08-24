package com.rizer01.soundpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rizer01.soundpad.audio.AudioPlayer
import com.rizer01.soundpad.hotkey.HotkeyManager
import com.rizer01.soundpad.model.SoundFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val AccentCyan = Color(0xFF00D4FF)

/**
 * Dialog to edit a sound's volume, keybind, and hotkey toggle.
 */
@Composable
fun SoundEditDialog(
    sound: SoundFile,
    audioPlayer: AudioPlayer,
    hotkeyManager: HotkeyManager,
    onDismiss: () -> Unit,
    onSave: (SoundFile) -> Unit,
    onDelete: () -> Unit
) {
    var volume by remember { mutableFloatStateOf(sound.volume) }
    var currentHotkey by remember { mutableStateOf(sound.hotkey) }
    var hotkeyEnabled by remember { mutableStateOf(sound.hotkey != null) }
    var isCapturing by remember { mutableStateOf(false) }
    var capturingMessage by remember { mutableStateOf("") }
    var previewPlaying by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Edit, null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Edit: ${sound.name}", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.width(380.dp)
            ) {
                // ── Volume ──
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.VolumeUp, null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Volume", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            "${(volume * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentCyan,
                            activeTrackColor = AccentCyan,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Quick volume buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(25, 50, 75, 100).forEach { pct ->
                            OutlinedButton(
                                onClick = { volume = pct / 100f },
                                modifier = Modifier.weight(1f).height(32.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("${pct}%", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                // ── Keybind ──
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Keyboard, null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Hotkey", style = MaterialTheme.typography.bodyMedium)
                        }
                        // Enable/disable toggle
                        Switch(
                            checked = hotkeyEnabled,
                            onCheckedChange = {
                                hotkeyEnabled = it
                                if (!it) currentHotkey = null
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentCyan,
                                checkedTrackColor = AccentCyan.copy(alpha = 0.3f)
                            )
                        )
                    }

                    if (hotkeyEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Current keybind display
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = if (currentHotkey != null)
                                    AccentCyan.copy(alpha = 0.1f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        if (isCapturing) Icons.Filled.HourglassBottom else Icons.Filled.Keyboard,
                                        null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isCapturing) AccentCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        when {
                                            isCapturing -> capturingMessage
                                            currentHotkey != null -> currentHotkey!!
                                            else -> "No keybind set"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (currentHotkey != null) AccentCyan
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Bind / Clear buttons
                            if (isCapturing) {
                                FilledTonalButton(
                                    onClick = { isCapturing = false },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Icon(Icons.Filled.Close, null, Modifier.size(16.dp))
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = {
                                        isCapturing = true
                                        capturingMessage = "Press any key..."
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Filled.Link, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (currentHotkey != null) "Rebind" else "Bind")
                                }

                                if (currentHotkey != null) {
                                    IconButton(
                                        onClick = {
                                            currentHotkey = null
                                            hotkeyManager.unbind(sound.id)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Clear, "Remove keybind",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        // Key capture coroutine
                        if (isCapturing) {
                            LaunchedEffect(Unit) {
                                val key = withContext(Dispatchers.IO) {
                                    hotkeyManager.captureNextKey(5000)
                                }
                                isCapturing = false
                                if (key != null) {
                                    currentHotkey = key
                                    hotkeyManager.bind(key, sound.id)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                // ── Preview ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (previewPlaying) {
                                audioPlayer.stop(sound.id)
                                previewPlaying = false
                            } else {
                                audioPlayer.play(sound, volume = volume)
                                previewPlaying = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            if (previewPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            null, Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (previewPlaying) "Stop Preview" else "Preview Sound")
                    }

                    // Delete button
                    OutlinedButton(
                        onClick = {
                            audioPlayer.stop(sound.id)
                            onDelete()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                audioPlayer.stop(sound.id)
                onSave(
                    sound.copy(
                        volume = volume,
                        hotkey = if (hotkeyEnabled) currentHotkey else null
                    )
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                audioPlayer.stop(sound.id)
                onDismiss()
            }) {
                Text("Cancel")
            }
        }
    )
}

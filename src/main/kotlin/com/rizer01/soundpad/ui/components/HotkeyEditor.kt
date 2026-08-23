package com.rizer01.soundpad.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rizer01.soundpad.hotkey.HotkeyManager

@Composable
fun HotkeyEditor(
    soundName: String,
    currentHotkey: String?,
    hotkeyManager: HotkeyManager,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var isListening by remember { mutableStateOf(false) }
    var capturedKey by remember { mutableStateOf(currentHotkey) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "⌨️ Assign Hotkey",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    "for \"$soundName\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Current hotkey display
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = capturedKey ?: "No hotkey assigned",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        color = if (capturedKey != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Error message
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onDismiss() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    if (currentHotkey != null) {
                        Button(
                            onClick = {
                                capturedKey = null
                                error = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Remove")
                        }
                    }

                    Button(
                        onClick = {
                            capturedKey?.let { key ->
                                if (!hotkeyManager.isBound(key)) {
                                    onSave(key)
                                    onDismiss()
                                } else {
                                    error = "This hotkey is already assigned"
                                }
                            }
                        },
                        enabled = capturedKey != null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save")
                    }
                }

                // Info
                Text(
                    "Press any key combination to assign.\n" +
                    "Works even when app is minimized.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

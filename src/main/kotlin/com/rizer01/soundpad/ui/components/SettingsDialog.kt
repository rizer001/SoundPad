package com.rizer01.soundpad.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rizer01.soundpad.model.AppSettings
import com.rizer01.soundpad.ui.theme.isDarkTheme

private val AccentCyan = Color(0xFF00D4FF)

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onSave: (AppSettings) -> Unit
) {
    var darkTheme by remember { mutableStateOf(isDarkTheme.value) }
    var hotkeysEnabled by remember { mutableStateOf(true) }
    var minimizeToTray by remember { mutableStateOf(true) }
    var autoStart by remember { mutableStateOf(false) }
    var virtualCable by remember { mutableStateOf(false) }

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
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .then(Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = AccentCyan
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                HorizontalDivider()

                // Theme
                SettingsToggle(
                    title = "Dark Theme",
                    description = "Use dark color scheme",
                    icon = if (darkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    checked = darkTheme,
                    onCheckedChange = { darkTheme = it }
                )

                // Hotkeys
                SettingsToggle(
                    title = "Global Hotkeys",
                    description = "Enable keyboard shortcuts for sounds",
                    icon = Icons.Filled.Keyboard,
                    checked = hotkeysEnabled,
                    onCheckedChange = { hotkeysEnabled = it }
                )

                // Minimize to tray
                SettingsToggle(
                    title = "Minimize to Tray",
                    description = "Keep app running in system tray when closed",
                    icon = Icons.Filled.WebAsset,
                    checked = minimizeToTray,
                    onCheckedChange = { minimizeToTray = it }
                )

                // Auto start
                SettingsToggle(
                    title = "Auto Start",
                    description = "Start with system",
                    icon = Icons.Filled.Rocket,
                    checked = autoStart,
                    onCheckedChange = { autoStart = it }
                )

                // Virtual Cable
                SettingsToggle(
                    title = "Virtual Audio Cable",
                    description = "Route audio to VB-Cable (Discord/OBS)",
                    icon = Icons.Filled.Mic,
                    checked = virtualCable,
                    onCheckedChange = { virtualCable = it }
                )

                HorizontalDivider()

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            isDarkTheme.value = darkTheme
                            onSave(
                                AppSettings(
                                    darkTheme = darkTheme,
                                    hotkeysEnabled = hotkeysEnabled,
                                    minimizeToTray = minimizeToTray,
                                    autoStart = autoStart,
                                    virtualCableEnabled = virtualCable
                                )
                            )
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon in styled container (like the website)
        Box(
            modifier = Modifier
                .size(40.dp)
                .then(Modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = AccentCyan
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AccentCyan,
                checkedTrackColor = AccentCyan.copy(alpha = 0.3f)
            )
        )
    }
}

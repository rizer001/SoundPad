package com.rizer01.soundpad.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentSettings: AppSettings,
    availableDevices: List<String> = listOf("default"),
    currentDevice: String = "default",
    onDismiss: () -> Unit,
    onSave: (AppSettings, String) -> Unit
) {
    var darkTheme by remember { mutableStateOf(currentSettings.darkTheme) }
    var hotkeysEnabled by remember { mutableStateOf(currentSettings.hotkeysEnabled) }
    var minimizeToTray by remember { mutableStateOf(currentSettings.minimizeToTray) }
    var autoStart by remember { mutableStateOf(currentSettings.autoStart) }
    var virtualCable by remember { mutableStateOf(currentSettings.virtualCableEnabled) }
    var selectedDevice by remember { mutableStateOf(currentDevice) }
    var deviceExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .padding(24.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Settings, null, Modifier.size(24.dp), tint = AccentCyan)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Settings", style = MaterialTheme.typography.headlineSmall)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Audio Output Device
                    Text("Audio Output", style = MaterialTheme.typography.titleSmall, color = AccentCyan)
                    ExposedDropdownMenuBox(
                        expanded = deviceExpanded,
                        onExpandedChange = { deviceExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedDevice.replace("Primary Sound Driver", "System Default"),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Output Device") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deviceExpanded) },
                            leadingIcon = {
                                Icon(Icons.Filled.Speaker, null, tint = AccentCyan)
                            },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = deviceExpanded,
                            onDismissRequest = { deviceExpanded = false }
                        ) {
                            availableDevices.forEach { device ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            device.replace("Primary Sound Driver", "System Default"),
                                            color = if (device == selectedDevice) AccentCyan
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        selectedDevice = device
                                        deviceExpanded = false
                                    },
                                    leadingIcon = {
                                        if (device == selectedDevice) {
                                            Icon(Icons.Filled.Check, null, tint = AccentCyan, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Toggles
                    SettingsToggle("Dark Theme", "Use dark color scheme",
                        if (darkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                        darkTheme) { darkTheme = it }

                    SettingsToggle("Global Hotkeys", "Enable keyboard shortcuts for sounds",
                        Icons.Filled.Keyboard, hotkeysEnabled) { hotkeysEnabled = it }

                    SettingsToggle("Minimize to Tray", "Keep running in system tray",
                        Icons.Filled.WebAsset, minimizeToTray) { minimizeToTray = it }

                    SettingsToggle("Auto Start", "Start with system",
                        Icons.Filled.Rocket, autoStart) { autoStart = it }

                    SettingsToggle("Virtual Audio Cable", "Route to VB-Cable (Discord/OBS)",
                        Icons.Filled.Mic, virtualCable) { virtualCable = it }
                }

                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                // Buttons — ALWAYS visible at the bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            isDarkTheme.value = darkTheme
                            // Preserve ALL existing fields (masterVolume, gridColumns, etc.)
                            onSave(
                                currentSettings.copy(
                                    darkTheme = darkTheme,
                                    outputDevice = selectedDevice,
                                    hotkeysEnabled = hotkeysEnabled,
                                    minimizeToTray = minimizeToTray,
                                    autoStart = autoStart,
                                    virtualCableEnabled = virtualCable
                                ),
                                selectedDevice
                            )
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Check, null, Modifier.size(16.dp))
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
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(20.dp), tint = AccentCyan)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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

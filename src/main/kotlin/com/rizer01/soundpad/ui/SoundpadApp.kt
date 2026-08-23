package com.rizer01.soundpad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rizer01.soundpad.audio.AudioPlayer
import com.rizer01.soundpad.hotkey.HotkeyManager
import com.rizer01.soundpad.model.NowPlaying
import com.rizer01.soundpad.model.SoundFile
import com.rizer01.soundpad.onGlobalFilesDropped
import com.rizer01.soundpad.store.PresetStore
import com.rizer01.soundpad.store.SettingsStore
import com.rizer01.soundpad.store.SoundStore
import com.rizer01.soundpad.ui.components.*
import com.rizer01.soundpad.ui.theme.isDarkTheme
import kotlinx.coroutines.delay
import java.io.File

private val AccentCyan = Color(0xFF00D4FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundpadApp() {
    val soundStore = remember { SoundStore() }
    val presetStore = remember { PresetStore() }
    val settingsStore = remember { SettingsStore() }
    val audioPlayer = remember { AudioPlayer() }
    val hotkeyManager = remember { HotkeyManager() }

    val categories by soundStore.categories.collectAsState()
    val filteredSounds by soundStore.filteredSounds.collectAsState()
    val selectedCategory by soundStore.selectedCategory.collectAsState()
    val searchQuery by soundStore.searchQuery.collectAsState()
    val masterVolume = remember { mutableStateOf(settingsStore.settings.masterVolume) }
    val nowPlaying = remember { mutableStateOf<List<NowPlaying>>(emptyList()) }
    val outputDevice = remember { mutableStateOf(settingsStore.settings.outputDevice) }

    var showSettings by remember { mutableStateOf(false) }

    // Helper to add files
    val addFiles: (List<File>) -> Unit = remember(soundStore, selectedCategory) {
        { files ->
            val categoryId = selectedCategory ?: "default"
            val soundFiles = files.map { it.toSoundFile(categoryId) }
            soundStore.addSounds(soundFiles)
        }
    }

    // Register as global drag & drop handler
    LaunchedEffect(Unit) {
        onGlobalFilesDropped = { files -> addFiles(files) }
    }
    DisposableEffect(Unit) {
        onDispose { onGlobalFilesDropped = null }
    }

    // Initialize hotkey manager
    LaunchedEffect(Unit) {
        hotkeyManager.init { soundId ->
            soundStore.getSound(soundId)?.let { sound ->
                audioPlayer.play(sound, masterVolume.value)
            }
        }
    }

    // Track now playing
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            val playing = audioPlayer.getNowPlaying().mapNotNull { (id, state) ->
                soundStore.getSound(id)?.let { sound ->
                    NowPlaying(sound = sound, state = state)
                }
            }
            nowPlaying.value = playing
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.dispose()
            hotkeyManager.dispose()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                AccentCyan.copy(alpha = 0.12f),
                                MaterialTheme.shapes.small
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.GraphicEq,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = AccentCyan
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Soundpad",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            },
            actions = {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { soundStore.setSearchQuery(it) },
                    modifier = Modifier.width(300.dp)
                )

                Spacer(Modifier.width(8.dp))

                // File picker button
                FilledTonalButton(
                    onClick = {
                        val files = openAudioFileChooser()
                        if (files.isNotEmpty()) {
                            addFiles(files)
                        }
                    },
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Filled.FolderOpen,
                        contentDescription = "Add files",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Add Files", style = MaterialTheme.typography.labelMedium)
                }

                Spacer(Modifier.width(4.dp))

                IconButton(onClick = { isDarkTheme.value = !isDarkTheme.value }) {
                    Icon(
                        imageVector = if (isDarkTheme.value) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = "Toggle theme",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = { showSettings = true }) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Main content
        Row(modifier = Modifier.fillMaxSize()) {
            Sidebar(
                categories = categories,
                selectedCategory = selectedCategory,
                soundCounts = categories.associateWith { cat ->
                    if (cat.id == "default") soundStore.getSoundCount()
                    else soundStore.getSoundsForCategory(cat.id).size
                },
                onCategoryClick = { soundStore.selectCategory(it) },
                onAddCategory = { /* TODO */ },
                modifier = Modifier.width(220.dp).fillMaxHeight()
            )

            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.outline
            )

            SoundGrid(
                sounds = filteredSounds,
                audioPlayer = audioPlayer,
                masterVolume = masterVolume.value,
                hotkeyManager = hotkeyManager,
                onSoundAdded = { soundStore.addSound(it) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        StatusBar(
            nowPlaying = nowPlaying.value,
            masterVolume = masterVolume.value,
            onMasterVolumeChange = { masterVolume.value = it },
            outputDevice = outputDevice.value,
            onStopAll = { audioPlayer.stopAll() },
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false },
            onSave = { settings ->
                settingsStore.update { settings }
            }
        )
    }
}

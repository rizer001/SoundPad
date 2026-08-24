package com.rizer01.soundpad.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rizer01.soundpad.audio.AudioPlayer
import com.rizer01.soundpad.db.DatabaseManager
import com.rizer01.soundpad.hotkey.HotkeyManager
import com.rizer01.soundpad.model.NowPlaying
import com.rizer01.soundpad.model.SoundCategory
import com.rizer01.soundpad.model.SoundFile
import com.rizer01.soundpad.onGlobalFilesDropped
import com.rizer01.soundpad.store.SettingsStore
import com.rizer01.soundpad.store.SoundStore
import com.rizer01.soundpad.ui.components.*
import com.rizer01.soundpad.ui.theme.isDarkTheme
import kotlinx.coroutines.delay
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private val AccentCyan = Color(0xFF00D4FF)

/** Available category icons */
private data class CategoryIconOption(val name: String, val icon: ImageVector)

private val CATEGORY_ICONS = listOf(
    CategoryIconOption("folder", Icons.Filled.Folder),
    CategoryIconOption("emoji", Icons.Filled.EmojiEmotions),
    CategoryIconOption("notifications", Icons.Filled.Notifications),
    CategoryIconOption("music", Icons.Filled.MusicNote),
    CategoryIconOption("star", Icons.Filled.Star)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundpadApp(db: DatabaseManager) {
    val soundStore = remember { SoundStore(db) }
    val settingsStore = remember { SettingsStore(db) }
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
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteCategoryConfirm by remember { mutableStateOf<String?>(null) }
    var editingSound by remember { mutableStateOf<SoundFile?>(null) }
    var deletingSound by remember { mutableStateOf<SoundFile?>(null) }
    val availableDevices = remember { mutableStateOf(audioPlayer.getOutputDevices()) }

    // Add files helper — copies files to portable sounds/ dir
    val addFiles: (List<File>) -> Unit = remember(soundStore, selectedCategory) {
        { files ->
            val categoryId = selectedCategory ?: "default"
            soundStore.addSounds(files.map { file ->
                // Copy to portable directory
                val portablePath = db.copySoundToPortable(file)
                SoundFile(
                    id = "${file.nameWithoutExtension}_${System.currentTimeMillis()}",
                    name = file.nameWithoutExtension,
                    filePath = portablePath,
                    categoryId = categoryId,
                    hotkey = null,
                    volume = 1.0f,
                    loop = false
                )
            })
        }
    }

    // Register global DnD callback
    LaunchedEffect(Unit) {
        onGlobalFilesDropped = { files -> addFiles(files) }
    }
    DisposableEffect(Unit) {
        onDispose { onGlobalFilesDropped = null }
    }

    // Initialize hotkeys
    LaunchedEffect(Unit) {
        hotkeyManager.init { soundId ->
            soundStore.getSound(soundId)?.let { sound ->
                audioPlayer.play(sound, masterVolume.value)
            }
        }
    }

    // Track playback
    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            nowPlaying.value = audioPlayer.getNowPlaying().mapNotNull { (id, state) ->
                soundStore.getSound(id)?.let { NowPlaying(it, state) }
            }
        }
    }

    // Update output device when changed
    LaunchedEffect(outputDevice.value) {
        audioPlayer.setOutputDevice(outputDevice.value)
    }

    // Auto-save on app close
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.dispose()
            hotkeyManager.dispose()
            soundStore.saveAllToDb()
            // Save current runtime state to settings
            settingsStore.update {
                copy(
                    masterVolume = masterVolume.value,
                    outputDevice = outputDevice.value,
                    darkTheme = isDarkTheme.value
                )
            }
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
                            .background(AccentCyan.copy(alpha = 0.12f), MaterialTheme.shapes.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.GraphicEq, null, Modifier.size(20.dp), tint = AccentCyan)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Soundpad", style = MaterialTheme.typography.headlineSmall)
                }
            },
            actions = {
                DropZone(
                    onFilesDropped = { addFiles(it) },
                    onBrowseClick = {
                        val files = openAudioFileChooser()
                        if (files.isNotEmpty()) addFiles(files)
                    },
                    modifier = Modifier.padding(end = 8.dp)
                )

                Spacer(Modifier.width(8.dp))

                SearchBar(
                    query = searchQuery,
                    onQueryChange = { soundStore.setSearchQuery(it) },
                    modifier = Modifier.width(220.dp)
                )

                IconButton(onClick = {
                    isDarkTheme.value = !isDarkTheme.value
                    settingsStore.update { copy(darkTheme = isDarkTheme.value) }
                }) {
                    Icon(
                        if (isDarkTheme.value) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        "Theme", tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Filled.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
                onAddCategory = { showAddCategoryDialog = true },
                onDeleteCategory = { showDeleteCategoryConfirm = it },
                modifier = Modifier.width(220.dp).fillMaxHeight()
            )

            VerticalDivider(modifier = Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outline)

            SoundGrid(
                sounds = filteredSounds,
                audioPlayer = audioPlayer,
                masterVolume = masterVolume.value,
                hotkeyManager = hotkeyManager,
                onSoundAdded = { soundStore.addSound(it) },
                onEditSound = { editingSound = it },
                onDeleteSound = { deletingSound = it },
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }

        StatusBar(
            nowPlaying = nowPlaying.value,
            masterVolume = masterVolume.value,
            onMasterVolumeChange = {
                masterVolume.value = it
                settingsStore.update { copy(masterVolume = it) }
            },
            outputDevice = outputDevice.value,
            onStopAll = { audioPlayer.stopAll() },
            modifier = Modifier.fillMaxWidth()
        )
    }

    // ── Dialogs ──

    if (showSettings) {
        SettingsDialog(
            currentSettings = settingsStore.settings,
            availableDevices = availableDevices.value,
            currentDevice = outputDevice.value,
            onDismiss = { showSettings = false },
            onSave = { settings, device ->
                settingsStore.update { settings }
                outputDevice.value = device
                audioPlayer.setOutputDevice(device)
            }
        )
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, iconName, iconPath ->
                soundStore.addCategory(
                    SoundCategory(
                        id = name.lowercase().replace(" ", "_"),
                        name = name,
                        icon = iconName,
                        customIconPath = iconPath
                    )
                )
                showAddCategoryDialog = false
            }
        )
    }

    showDeleteCategoryConfirm?.let { catId ->
        val catName = categories.find { it.id == catId }?.name ?: catId
        AlertDialog(
            onDismissRequest = { showDeleteCategoryConfirm = null },
            title = { Text("Delete Category") },
            text = { Text("Delete \"$catName\"? Sounds will be moved to All Sounds.") },
            confirmButton = {
                TextButton(onClick = {
                    soundStore.removeCategory(catId)
                    showDeleteCategoryConfirm = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCategoryConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sound edit dialog
    editingSound?.let { sound ->
        SoundEditDialog(
            sound = sound,
            audioPlayer = audioPlayer,
            hotkeyManager = hotkeyManager,
            onDismiss = { editingSound = null },
            onSave = { updated ->
                soundStore.updateSound(updated)
                if (updated.hotkey != null) {
                    hotkeyManager.bind(updated.hotkey, updated.id)
                } else {
                    hotkeyManager.unbind(updated.id)
                }
                editingSound = null
            },
            onDelete = {
                audioPlayer.stop(sound.id)
                hotkeyManager.unbind(sound.id)
                soundStore.removeSound(sound.id)
                editingSound = null
            }
        )
    }

    // Sound delete confirmation
    deletingSound?.let { sound ->
        AlertDialog(
            onDismissRequest = { deletingSound = null },
            title = { Text("Delete Sound") },
            text = { Text("Delete \"${sound.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    audioPlayer.stop(sound.id)
                    hotkeyManager.unbind(sound.id)
                    soundStore.removeSound(sound.id)
                    deletingSound = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSound = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Category creation dialog with icon picker (5 icons + custom image placeholder)
 */
@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, iconName: String, iconPath: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("folder") }
    var customImagePath by remember { mutableStateOf<String?>(null) }

    // File chooser for custom icon image
    val chooseImage = {
        val fc = JFileChooser()
        fc.dialogTitle = "Select Category Icon"
        fc.fileFilter = FileNameExtensionFilter("Images", "png", "jpg", "jpeg", "gif", "svg", "ico")
        fc.isAcceptAllFileFilterUsed = false
        val result = fc.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            customImagePath = fc.selectedFile.absolutePath
            selectedIcon = "custom"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.width(380.dp)
            ) {
                // Category name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Category name...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Icon picker — 5 built-in icons
                Text("Icon", style = MaterialTheme.typography.titleSmall, color = AccentCyan)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CATEGORY_ICONS.forEach { option ->
                        val isSelected = selectedIcon == option.name
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) AccentCyan.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .then(
                                    if (isSelected) Modifier.border(2.dp, AccentCyan, CircleShape)
                                    else Modifier
                                )
                                .clickable {
                                    selectedIcon = option.name
                                    customImagePath = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                option.icon,
                                contentDescription = option.name,
                                modifier = Modifier.size(22.dp),
                                tint = if (isSelected) AccentCyan else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Custom icon — drop zone (same layout as main DropZone)
                Text("Custom icon", style = MaterialTheme.typography.titleSmall, color = AccentCyan)

                val isCustomSelected = selectedIcon == "custom"
                var iconDragOver by remember { mutableStateOf(false) }
                val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "svg", "ico")

                val iconDropPanel = remember {
                    object : javax.swing.JPanel() {
                        init {
                            isOpaque = false
                            background = java.awt.Color(0, 0, 0, 0)
                            dropTarget = java.awt.dnd.DropTarget(this, java.awt.dnd.DnDConstants.ACTION_COPY,
                                object : java.awt.dnd.DropTargetListener {
                                    override fun dragEnter(dtde: java.awt.dnd.DropTargetDragEvent) {
                                        dtde.acceptDrag(java.awt.dnd.DnDConstants.ACTION_COPY)
                                        javax.swing.SwingUtilities.invokeLater { iconDragOver = true }
                                    }
                                    override fun dragOver(dtde: java.awt.dnd.DropTargetDragEvent) {
                                        dtde.acceptDrag(java.awt.dnd.DnDConstants.ACTION_COPY)
                                    }
                                    override fun dragExit(dtde: java.awt.dnd.DropTargetEvent) {
                                        javax.swing.SwingUtilities.invokeLater { iconDragOver = false }
                                    }
                                    override fun dropActionChanged(dtde: java.awt.dnd.DropTargetDragEvent) {}
                                    override fun drop(dtde: java.awt.dnd.DropTargetDropEvent) {
                                        try {
                                            dtde.acceptDrop(java.awt.dnd.DnDConstants.ACTION_COPY)
                                            val transferable = dtde.transferable
                                            if (transferable.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor)) {
                                                @Suppress("UNCHECKED_CAST")
                                                val files = transferable.getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor) as? List<File>
                                                if (!files.isNullOrEmpty()) {
                                                    val imageFile = files.firstOrNull { it.exists() && it.extension.lowercase() in IMAGE_EXTENSIONS }
                                                    if (imageFile != null) {
                                                        javax.swing.SwingUtilities.invokeLater {
                                                            iconDragOver = false
                                                            customImagePath = imageFile.absolutePath
                                                            selectedIcon = "custom"
                                                        }
                                                    }
                                                }
                                            }
                                            dtde.dropComplete(true)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            dtde.dropComplete(false)
                                        }
                                    }
                                })
                        }
                    }
                }

                // Row: [drop square] [text] [browse button] — same layout as main DropZone
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Drop square (48x48, same as main menu)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(
                                width = 2.dp,
                                color = if (iconDragOver) AccentCyan else if (isCustomSelected) AccentCyan.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .background(
                                if (iconDragOver) AccentCyan.copy(alpha = 0.1f)
                                else Color.White.copy(alpha = 0.05f)
                            )
                    ) {
                        // AWT DropTarget panel
                        androidx.compose.ui.awt.SwingPanel(
                            factory = { iconDropPanel },
                            modifier = Modifier.fillMaxSize()
                        )
                        // Icon overlay
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                if (customImagePath != null) Icons.Filled.Image else Icons.Filled.CloudUpload,
                                contentDescription = "Drop image here",
                                modifier = Modifier.size(22.dp),
                                tint = if (iconDragOver) AccentCyan else if (customImagePath != null) AccentCyan else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Text label
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = if (iconDragOver) "Release to set icon"
                                   else if (customImagePath != null) File(customImagePath!!).name
                                   else "Drop image here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (iconDragOver) AccentCyan else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = if (customImagePath != null) "Custom icon selected"
                                   else "PNG, JPG, SVG, GIF",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (customImagePath != null) AccentCyan.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Browse button (folder icon, same as main menu)
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { chooseImage() },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.FolderOpen,
                                contentDescription = "Browse image",
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selectedIcon, customImagePath) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

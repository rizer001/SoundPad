package com.rizer01.soundpad.ui.components

import com.rizer01.soundpad.model.SoundFile
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.UUID
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private val SUPPORTED_EXTENSIONS = setOf("mp3", "wav", "ogg", "flac", "m4a", "aac", "wma")

/**
 * Opens a native file picker dialog and returns selected audio files.
 */
fun openAudioFileChooser(): List<File> {
    return try {
        val chooser = JFileChooser().apply {
            dialogTitle = "Select Audio Files"
            fileSelectionMode = JFileChooser.FILES_ONLY
            isMultiSelectionEnabled = true
            fileFilter = FileNameExtensionFilter(
                "Audio Files (*.mp3, *.wav, *.ogg, *.flac, *.m4a, *.aac, *.wma)",
                "mp3", "wav", "ogg", "flac", "m4a", "aac", "wma"
            )
        }

        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFiles?.filter { file ->
                file.exists() && file.extension.lowercase() in SUPPORTED_EXTENSIONS
            }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        // Fallback to AWT FileDialog
        try {
            val dialog = FileDialog(null as Frame?, "Select Audio Files", FileDialog.LOAD)
            dialog.isMultipleMode = true
            dialog.isVisible = true

            dialog.files?.filter { file ->
                file.exists() && file.extension.lowercase() in SUPPORTED_EXTENSIONS
            }?.toList() ?: emptyList()
        } catch (e2: Exception) {
            emptyList()
        }
    }
}

/**
 * Convert a File to a SoundFile
 */
fun File.toSoundFile(categoryId: String = "default"): SoundFile {
    return SoundFile(
        id = UUID.randomUUID().toString(),
        name = this.nameWithoutExtension,
        filePath = this.absolutePath,
        categoryId = categoryId
    )
}

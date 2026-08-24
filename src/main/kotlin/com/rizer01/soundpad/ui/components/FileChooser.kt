package com.rizer01.soundpad.ui.components

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Opens a native file chooser dialog for selecting audio files.
 * Returns a list of selected files (empty if cancelled).
 */
fun openAudioFileChooser(): List<File> {
    val chooser = JFileChooser().apply {
        dialogTitle = "Select Audio Files"
        fileSelectionMode = JFileChooser.FILES_ONLY
        isMultiSelectionEnabled = true
        fileFilter = FileNameExtensionFilter(
            "Audio Files (MP3, WAV, OGG, FLAC, M4A, AAC, WMA)",
            "mp3", "wav", "ogg", "flac", "m4a", "aac", "wma"
        )
    }

    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFiles.toList()
    } else {
        emptyList()
    }
}

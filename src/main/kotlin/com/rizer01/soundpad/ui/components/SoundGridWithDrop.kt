package com.rizer01.soundpad.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.rizer01.soundpad.audio.AudioPlayer
import com.rizer01.soundpad.hotkey.HotkeyManager
import com.rizer01.soundpad.model.SoundFile

/**
 * Wraps SoundGrid. Drag & drop is handled at the window level in Main.kt.
 */
@Composable
fun SoundGridWithDrop(
    sounds: List<SoundFile>,
    audioPlayer: AudioPlayer,
    masterVolume: Float,
    hotkeyManager: HotkeyManager,
    onFilesDropped: (List<java.io.File>) -> Unit,
    onEditSound: (SoundFile) -> Unit = {},
    onDeleteSound: (SoundFile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    SoundGrid(
        sounds = sounds,
        audioPlayer = audioPlayer,
        masterVolume = masterVolume,
        hotkeyManager = hotkeyManager,
        onSoundAdded = {},
        onEditSound = onEditSound,
        onDeleteSound = onDeleteSound,
        modifier = modifier
    )
}

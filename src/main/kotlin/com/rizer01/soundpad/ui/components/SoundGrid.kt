package com.rizer01.soundpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rizer01.soundpad.audio.AudioPlayer
import com.rizer01.soundpad.hotkey.HotkeyManager
import com.rizer01.soundpad.model.SoundFile

private val AccentCyan = Color(0xFF00D4FF)

@Composable
fun SoundGrid(
    sounds: List<SoundFile>,
    audioPlayer: AudioPlayer,
    masterVolume: Float,
    hotkeyManager: HotkeyManager,
    onSoundAdded: (SoundFile) -> Unit,
    modifier: Modifier = Modifier
) {
    if (sounds.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon in styled container (like the website)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .then(
                            Modifier.padding(0.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.AudioFile,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = AccentCyan.copy(alpha = 0.5f)
                    )
                }
                Text(
                    "No sounds yet",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Drag & drop audio files here\nor click + to add sounds",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sounds, key = { it.id }) { sound ->
                SoundCard(
                    sound = sound,
                    audioPlayer = audioPlayer,
                    masterVolume = masterVolume,
                    hotkey = hotkeyManager.getHotkey(sound.id)
                )
            }
        }
    }
}

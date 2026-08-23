package com.rizer01.soundpad.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rizer01.soundpad.audio.AudioPlayer
import com.rizer01.soundpad.model.PlaybackState
import com.rizer01.soundpad.model.SoundFile
import com.rizer01.soundpad.ui.theme.StatusLooping
import com.rizer01.soundpad.ui.theme.StatusPlaying
import com.rizer01.soundpad.ui.theme.StatusStopped

private val AccentCyan = Color(0xFF00D4FF)
private val AccentPurple = Color(0xFF7C3AED)

@Composable
fun SoundCard(
    sound: SoundFile,
    audioPlayer: AudioPlayer,
    masterVolume: Float,
    hotkey: String?,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(sound.id) {
        while (true) {
            kotlinx.coroutines.delay(100)
            isPlaying = audioPlayer.getState(sound.id) == PlaybackState.PLAYING
        }
    }

    val backgroundColor by animateColorAsState(
        if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "bg"
    )

    val borderColor by animateColorAsState(
        if (isPlaying) AccentCyan.copy(alpha = 0.3f)
        else Color.Transparent,
        label = "border"
    )

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                if (isPlaying) {
                    audioPlayer.stop(sound.id)
                } else {
                    audioPlayer.play(sound, volume = masterVolume * sound.volume, loop = sound.loop)
                }
            },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        border = if (isPlaying) CardDefaults.outlinedCardBorder() else null
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // Hotkey badge (top-left)
            hotkey?.let { key ->
                Surface(
                    modifier = Modifier.align(Alignment.TopStart),
                    shape = RoundedCornerShape(6.dp),
                    color = AccentCyan.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = key,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan
                    )
                }
            }

            // Loop indicator (top-right)
            if (sound.loop) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    shape = RoundedCornerShape(6.dp),
                    color = AccentPurple.copy(alpha = 0.15f)
                ) {
                    Icon(
                        Icons.Filled.Repeat,
                        contentDescription = "Looping",
                        modifier = Modifier
                            .padding(4.dp)
                            .size(14.dp),
                        tint = AccentPurple
                    )
                }
            }

            // Center content
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play/Stop icon in styled container (like the website)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isPlaying)
                                AccentCyan.copy(alpha = 0.12f)
                            else
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Stop" else "Play",
                        modifier = Modifier.size(32.dp),
                        tint = if (isPlaying) AccentCyan else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Sound name
                Text(
                    text = sound.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Duration
                if (sound.duration > 0) {
                    Text(
                        text = formatDuration(sound.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Playing indicator (bottom)
            if (isPlaying) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AccentCyan
                )
            }

            // Format badge (bottom-left)
            Surface(
                modifier = Modifier.align(Alignment.BottomStart),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = sound.getExtension(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

private fun formatDuration(seconds: Float): String {
    val mins = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    return "%d:%02d".format(mins, secs)
}

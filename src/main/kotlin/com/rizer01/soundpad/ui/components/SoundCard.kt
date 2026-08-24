package com.rizer01.soundpad.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rizer01.soundpad.audio.AudioPlayer
import com.rizer01.soundpad.model.PlaybackState
import com.rizer01.soundpad.model.SoundFile
import kotlinx.coroutines.delay

private val AccentCyan = Color(0xFF00D4FF)
private val AccentPurple = Color(0xFF7C3AED)

@Composable
fun SoundCard(
    sound: SoundFile,
    audioPlayer: AudioPlayer,
    masterVolume: Float,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var elapsedTime by remember { mutableFloatStateOf(0f) }
    var totalDuration by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(sound.id) {
        while (true) {
            delay(100)
            val state = audioPlayer.getState(sound.id)
            isPlaying = state == PlaybackState.PLAYING
            if (isPlaying) {
                progress = audioPlayer.getProgress(sound.id)
                elapsedTime = audioPlayer.getElapsedTime(sound.id)
                totalDuration = audioPlayer.getDuration(sound.id)
            } else {
                progress = 0f
                elapsedTime = 0f
            }
        }
    }

    val backgroundColor by animateColorAsState(
        if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surfaceVariant,
        label = "bg"
    )

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        border = if (isPlaying) CardDefaults.outlinedCardBorder() else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── Action buttons (top corners, tiny circles) ──
            // Delete (top-left)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Delete sound",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }

            // Edit (top-right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(AccentCyan.copy(alpha = 0.15f))
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Edit sound",
                    modifier = Modifier.size(12.dp),
                    tint = AccentCyan
                )
            }

            // ── Center content (play button + name) ──
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Play/Stop icon — smaller background (40dp, CircleShape)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPlaying)
                                AccentCyan.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                        .clickable {
                            if (isPlaying) {
                                audioPlayer.stop(sound.id)
                            } else {
                                audioPlayer.play(sound, volume = masterVolume * sound.volume, loop = sound.loop)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Stop" else "Play",
                        modifier = Modifier.size(22.dp),
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
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Time display
                val displayDuration = if (totalDuration > 0) totalDuration else sound.duration
                if (isPlaying && displayDuration > 0) {
                    Text(
                        text = "${formatTime(elapsedTime)} / ${formatTime(displayDuration)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan
                    )
                } else if (displayDuration > 0) {
                    Text(
                        text = formatTime(displayDuration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Progress bar (bottom) ──
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = if (isPlaying) progress else 0f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(AccentCyan)
                )
            }

            // Format badge (bottom-left)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 16.dp),
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

            // Keybind badge (bottom-right) — opposite of format badge
            sound.hotkey?.let { key ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 8.dp, bottom = 16.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = AccentCyan.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentCyan,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // Loop indicator (below edit button, right)
            if (sound.loop) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp, top = 34.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = AccentPurple.copy(alpha = 0.15f)
                ) {
                    Icon(
                        Icons.Filled.Repeat,
                        contentDescription = "Looping",
                        modifier = Modifier.padding(3.dp).size(12.dp),
                        tint = AccentPurple
                    )
                }
            }
        }
    }
}

private fun formatTime(seconds: Float): String {
    val mins = (seconds / 60).toInt()
    val secs = (seconds % 60).toInt()
    return "%d:%02d".format(mins, secs)
}

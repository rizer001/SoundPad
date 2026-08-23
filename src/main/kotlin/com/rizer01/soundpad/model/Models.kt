package com.rizer01.soundpad.model

import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID

/**
 * Represents a single sound file in the soundpad
 */
@Serializable
data class SoundFile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val filePath: String,
    val duration: Float = 0f,       // seconds
    val volume: Float = 1.0f,       // 0.0 - 1.0
    val hotkey: String? = null,     // e.g., "F1", "Ctrl+Shift+1"
    val categoryId: String = "default",
    val loop: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getFile(): File = File(filePath)
    fun exists(): Boolean = getFile().exists()
    fun getExtension(): String = filePath.substringAfterLast('.').uppercase()
}

/**
 * Category for organizing sounds
 */
@Serializable
data class SoundCategory(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String = "folder",    // Material icon name
    val color: Long = 0xFF6750A4,   // Material purple
    val order: Int = 0
)

/**
 * A preset is a saved collection of sounds with their settings
 */
@Serializable
data class Preset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val sounds: List<SoundFile> = emptyList(),
    val categories: List<SoundCategory> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Application settings
 */
@Serializable
data class AppSettings(
    val outputDevice: String = "default",   // "default" or device name
    val masterVolume: Float = 0.8f,
    val darkTheme: Boolean = true,
    val minimizeToTray: Boolean = true,
    val autoStart: Boolean = false,
    val hotkeysEnabled: Boolean = true,
    val virtualCableEnabled: Boolean = false,
    val gridColumns: Int = 4,
    val lastPresetId: String? = null
)

/**
 * Playback state for a sound
 */
enum class PlaybackState {
    STOPPED, PLAYING, PAUSED
}

/**
 * Represents a currently playing sound
 */
data class NowPlaying(
    val sound: SoundFile,
    val state: PlaybackState,
    val progress: Float = 0f    // 0.0 - 1.0
)

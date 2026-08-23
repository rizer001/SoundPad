package com.rizer01.soundpad.audio

import com.rizer01.soundpad.model.PlaybackState
import com.rizer01.soundpad.model.SoundFile
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.io.File
import javax.sound.sampled.*

private val logger = KotlinLogging.logger {}

/**
 * Core audio player engine.
 * Uses javax.sound.sampled with SPI plugins for MP3/OGG support.
 */
class AudioPlayer {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeClips = mutableMapOf<String, ActiveClip>()

    data class ActiveClip(
        val sound: SoundFile,
        var state: PlaybackState,
        var volume: Float,
        var thread: Job? = null
    )

    /**
     * Play a sound file
     */
    fun play(sound: SoundFile, volume: Float = sound.volume, loop: Boolean = sound.loop) {
        stop(sound.id)

        val file = File(sound.filePath)
        if (!file.exists()) {
            logger.warn { "Sound file not found: ${sound.filePath}" }
            return
        }

        val job = scope.launch {
            try {
                playAudio(file, sound.id, volume, loop)
            } catch (e: Exception) {
                logger.error(e) { "Error playing sound: ${sound.name}" }
                activeClips.remove(sound.id)
            }
        }

        activeClips[sound.id] = ActiveClip(
            sound = sound,
            state = PlaybackState.PLAYING,
            volume = volume,
            thread = job
        )
    }

    /**
     * Play audio using javax.sound with automatic format detection.
     * Supports WAV, AIFF, MP3 (via jlayer), OGG (via jorbis).
     */
    private suspend fun playAudio(
        file: File,
        soundId: String,
        volume: Float,
        loop: Boolean
    ) {
        val audioInputStream = AudioSystem.getAudioInputStream(file)
        val baseFormat = audioInputStream.format

        // Convert to PCM for playback
        val decodedFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            baseFormat.sampleRate,
            16,
            baseFormat.channels,
            baseFormat.channels * 2,
            baseFormat.sampleRate,
            false
        )
        val decodedStream = AudioSystem.getAudioInputStream(decodedFormat, audioInputStream)

        val info = DataLine.Info(Clip::class.java, decodedFormat)
        val clip = AudioSystem.getLine(info) as Clip
        clip.open(decodedStream)

        // Set volume via gain control
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            val gainControl = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val gain = if (volume <= 0f) gainControl.minimum
            else (20 * kotlin.math.log10(volume.toDouble())).toFloat()
                .coerceIn(gainControl.minimum, gainControl.maximum)
            gainControl.value = gain
        }

        activeClips[soundId]?.state = PlaybackState.PLAYING

        if (loop) {
            clip.loop(Clip.LOOP_CONTINUOUSLY)
        } else {
            clip.start()
        }

        // Wait for playback to finish
        while (clip.isActive) {
            delay(100)
        }

        clip.close()
        activeClips.remove(soundId)
    }

    /**
     * Stop a specific sound
     */
    fun stop(soundId: String) {
        activeClips[soundId]?.thread?.cancel()
        activeClips[soundId]?.state = PlaybackState.STOPPED
        activeClips.remove(soundId)
    }

    /**
     * Stop all sounds
     */
    fun stopAll() {
        activeClips.keys.toList().forEach { stop(it) }
    }

    /**
     * Set volume for a playing sound
     */
    fun setVolume(soundId: String, volume: Float) {
        activeClips[soundId]?.let {
            it.volume = volume
        }
    }

    /**
     * Get current playback state
     */
    fun getState(soundId: String): PlaybackState {
        return activeClips[soundId]?.state ?: PlaybackState.STOPPED
    }

    /**
     * Get all currently playing sounds
     */
    fun getNowPlaying(): List<Pair<String, PlaybackState>> {
        return activeClips.map { it.key to it.value.state }
    }

    /**
     * Get available audio output devices
     */
    fun getOutputDevices(): List<String> {
        val devices = mutableListOf("default")
        for (mixerInfo in AudioSystem.getMixerInfo()) {
            val mixer = AudioSystem.getMixer(mixerInfo)
            val sourceLines = mixer.sourceLineInfo
            for (lineInfo in sourceLines) {
                if (SourceDataLine::class.java.isAssignableFrom(lineInfo.lineClass) ||
                    Clip::class.java.isAssignableFrom(lineInfo.lineClass)
                ) {
                    devices.add(mixerInfo.name)
                    break
                }
            }
        }
        return devices
    }

    /**
     * Cleanup
     */
    fun dispose() {
        stopAll()
        scope.cancel()
    }
}

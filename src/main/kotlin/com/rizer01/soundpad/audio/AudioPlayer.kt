package com.rizer01.soundpad.audio

import com.rizer01.soundpad.model.PlaybackState
import com.rizer01.soundpad.model.SoundFile
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext
import mu.KotlinLogging
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.sound.sampled.*

private val logger = KotlinLogging.logger {}

class AudioPlayer {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activePlayers = ConcurrentHashMap<String, ActivePlayer>()
    private var outputDeviceName: String = "default"

    data class ActivePlayer(
        val sound: SoundFile,
        var state: PlaybackState,
        var volume: Float,
        var thread: Job? = null,
        var line: SourceDataLine? = null
    )

    fun setOutputDevice(name: String) {
        outputDeviceName = name
    }

    fun play(sound: SoundFile, volume: Float = sound.volume, loop: Boolean = sound.loop) {
        stop(sound.id)

        val file = File(sound.filePath)
        if (!file.exists()) {
            logger.warn { "Sound file not found: ${sound.filePath}" }
            return
        }

        val job = scope.launch {
            try {
                playWithSourceDataLine(file, sound.id, volume, loop)
            } catch (e: CancellationException) {
                logger.debug { "Playback cancelled for: ${sound.name}" }
            } catch (e: Exception) {
                logger.error(e) { "Error playing sound: ${sound.name}" }
            } finally {
                activePlayers.remove(sound.id)
            }
        }

        activePlayers[sound.id] = ActivePlayer(
            sound = sound,
            state = PlaybackState.PLAYING,
            volume = volume,
            thread = job
        )
    }

    private suspend fun playWithSourceDataLine(
        file: File,
        soundId: String,
        volume: Float,
        loop: Boolean
    ) {
        val audioInputStream = AudioSystem.getAudioInputStream(file)
        val baseFormat = audioInputStream.format

        val pcmFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            baseFormat.sampleRate,
            16,
            baseFormat.channels,
            baseFormat.channels * 2,
            baseFormat.sampleRate,
            false
        )
        val pcmStream = AudioSystem.getAudioInputStream(pcmFormat, audioInputStream)

        // Read all bytes into memory for reliable looping
        val audioBytes = pcmStream.readBytes()
        pcmStream.close()
        audioInputStream.close()

        val lineInfo = DataLine.Info(SourceDataLine::class.java, pcmFormat)
        val line = findDeviceLine(pcmFormat) ?: AudioSystem.getLine(lineInfo) as SourceDataLine

        line.open(pcmFormat)
        line.start()
        activePlayers[soundId]?.line = line

        // Apply volume
        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            val gainControl = line.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
            val gain = if (volume <= 0f) gainControl.minimum
            else (20 * kotlin.math.log10(volume.toDouble())).toFloat()
                .coerceIn(gainControl.minimum, gainControl.maximum)
            gainControl.value = gain
        }

        val job = coroutineContext[Job]!!

        do {
            var offset = 0
            while (offset < audioBytes.size && job.isActive) {
                val chunkSize = minOf(4096, audioBytes.size - offset)
                line.write(audioBytes, offset, chunkSize)
                offset += chunkSize
            }

            if (loop && job.isActive) {
                delay(50)
            }
        } while (loop && job.isActive)

        line.drain()
        line.close()
    }

    private fun findDeviceLine(format: AudioFormat): SourceDataLine? {
        try {
            for (mixerInfo in AudioSystem.getMixerInfo()) {
                if (mixerInfo.name.contains(outputDeviceName, ignoreCase = true)) {
                    val mixer = AudioSystem.getMixer(mixerInfo)
                    val lineInfo = DataLine.Info(SourceDataLine::class.java, format)
                    if (mixer.isLineSupported(lineInfo)) {
                        return mixer.getLine(lineInfo) as SourceDataLine
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn(e) { "Could not find device: $outputDeviceName" }
        }
        return null
    }

    fun stop(soundId: String) {
        val player = activePlayers.remove(soundId) ?: return
        player.thread?.cancel()
        player.state = PlaybackState.STOPPED
        try {
            player.line?.let { line ->
                if (line.isOpen) {
                    line.drain()
                    line.close()
                }
            }
        } catch (e: Exception) {
            logger.debug { "Error closing line: ${e.message}" }
        }
    }

    fun stopAll() {
        activePlayers.keys.toList().forEach { stop(it) }
    }

    fun getState(soundId: String): PlaybackState {
        return activePlayers[soundId]?.state ?: PlaybackState.STOPPED
    }

    fun getNowPlaying(): List<Pair<String, PlaybackState>> {
        return activePlayers.map { it.key to it.value.state }
    }

    fun getOutputDevices(): List<String> {
        val devices = mutableListOf("default")
        for (mixerInfo in AudioSystem.getMixerInfo()) {
            val mixer = AudioSystem.getMixer(mixerInfo)
            for (lineInfo in mixer.sourceLineInfo) {
                if (SourceDataLine::class.java.isAssignableFrom(lineInfo.lineClass)) {
                    devices.add(mixerInfo.name)
                    break
                }
            }
        }
        return devices
    }

    fun dispose() {
        stopAll()
        scope.cancel()
    }
}

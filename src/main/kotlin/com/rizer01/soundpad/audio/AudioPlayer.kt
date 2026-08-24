package com.rizer01.soundpad.audio

import com.rizer01.soundpad.model.PlaybackState
import com.rizer01.soundpad.model.SoundFile
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.sound.sampled.*
import kotlin.coroutines.coroutineContext

private val logger = KotlinLogging.logger {}

class AudioPlayer {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activePlayers = ConcurrentHashMap<String, ActivePlayer>()
    private var outputDeviceName: String = "default"

    data class ActivePlayer(
        val sound: SoundFile,
        @Volatile var state: PlaybackState,
        var volume: Float,
        var thread: Job? = null,
        var line: SourceDataLine? = null,
        var totalBytes: Int = 0,
        var bytesWritten: Int = 0,
        var startTimeMs: Long = 0L,
        var durationMs: Long = 0L
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

        // Pre-calculate duration from PCM data (not file bytes!)
        val durationMs = try {
            val ais = AudioSystem.getAudioInputStream(file)
            val baseFormat = ais.format
            val pcmFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.sampleRate, 16, baseFormat.channels,
                baseFormat.channels * 2, baseFormat.sampleRate, false
            )
            val pcmStream = AudioSystem.getAudioInputStream(pcmFormat, ais)
            val pcmBytes = pcmStream.readAllBytes().size.toLong()
            pcmStream.close()
            ais.close()
            val frameSize = pcmFormat.frameSize.toLong()
            val frameRate = pcmFormat.frameRate.toLong()
            if (frameSize > 0 && frameRate > 0) (pcmBytes / frameSize * 1000 / frameRate) else 0L
        } catch (e: Exception) {
            logger.warn(e) { "Could not calculate duration, using fallback" }
            0L
        }

        activePlayers[sound.id] = ActivePlayer(
            sound = sound,
            state = PlaybackState.PLAYING,
            volume = volume,
            thread = job,
            startTimeMs = System.currentTimeMillis(),
            durationMs = durationMs
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

        var audioBytes = pcmStream.readAllBytes()
        pcmStream.close()
        audioInputStream.close()

        // Apply volume to PCM samples directly (16-bit signed LE)
        audioBytes = applyVolume(audioBytes, volume)

        activePlayers[soundId]?.totalBytes = audioBytes.size

        val lineInfo = DataLine.Info(SourceDataLine::class.java, pcmFormat)
        val line = findDeviceLine(pcmFormat) ?: AudioSystem.getLine(lineInfo) as SourceDataLine

        line.open(pcmFormat)
        line.start()
        activePlayers[soundId]?.line = line

        // Don't use MASTER_GAIN — volume is already baked into samples

        val job = coroutineContext[Job]!!

        do {
            var offset = 0
            while (offset < audioBytes.size && job.isActive) {
                val chunkSize = minOf(4096, audioBytes.size - offset)
                line.write(audioBytes, offset, chunkSize)
                offset += chunkSize
                activePlayers[soundId]?.bytesWritten = offset
            }

            if (loop && job.isActive) {
                activePlayers[soundId]?.bytesWritten = 0
                delay(50)
            }
        } while (loop && job.isActive)

        line.drain()
        line.close()
    }

    /**
     * Apply volume scaling to 16-bit signed PCM samples.
     * Each sample is 2 bytes (little-endian): low byte + high byte.
     */
    private fun applyVolume(data: ByteArray, volume: Float): ByteArray {
        if (volume >= 0.99f) return data // no scaling needed
        val result = ByteArray(data.size)
        var i = 0
        while (i < data.size - 1) {
            val sample = (data[i].toInt() and 0xFF) or (data[i + 1].toInt() shl 8)
            val scaled = (sample.toShort() * volume).toInt().coerceIn(-32768, 32767)
            result[i] = (scaled and 0xFF).toByte()
            result[i + 1] = ((scaled shr 8) and 0xFF).toByte()
            i += 2
        }
        return result
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

    /** Get playback progress 0.0 - 1.0 for a sound (time-based) */
    fun getProgress(soundId: String): Float {
        val player = activePlayers[soundId] ?: return 0f
        if (player.durationMs <= 0) return 0f
        val elapsed = System.currentTimeMillis() - player.startTimeMs
        return (elapsed.toFloat() / player.durationMs).coerceIn(0f, 1f)
    }

    /** Get elapsed playback time in seconds */
    fun getElapsedTime(soundId: String): Float {
        val player = activePlayers[soundId] ?: return 0f
        return ((System.currentTimeMillis() - player.startTimeMs) / 1000f)
    }

    /** Get total duration in seconds */
    fun getDuration(soundId: String): Float {
        val player = activePlayers[soundId] ?: return 0f
        return player.durationMs / 1000f
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

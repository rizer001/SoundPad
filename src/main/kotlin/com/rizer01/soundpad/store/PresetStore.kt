package com.rizer01.soundpad.store

import com.rizer01.soundpad.model.Preset
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

/**
 * Manages preset persistence (save/load/delete).
 * Presets are stored as JSON files in the app's data directory.
 */
class PresetStore {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val presetsDir: File by lazy {
        val dir = Paths.get(System.getProperty("user.home"), ".soundpad", "presets").toFile()
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    /**
     * Get all saved presets
     */
    fun getAllPresets(): List<Preset> {
        return presetsDir.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    json.decodeFromString<Preset>(file.readText())
                } catch (e: Exception) {
                    logger.error(e) { "Failed to load preset: ${file.name}" }
                    null
                }
            }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    /**
     * Save a preset
     */
    fun savePreset(preset: Preset): Boolean {
        return try {
            val file = File(presetsDir, "${sanitizeFileName(preset.name)}.json")
            file.writeText(json.encodeToString(preset))
            logger.info { "Saved preset: ${preset.name}" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to save preset: ${preset.name}" }
            false
        }
    }

    /**
     * Load a preset by name
     */
    fun loadPreset(name: String): Preset? {
        val file = File(presetsDir, "${sanitizeFileName(name)}.json")
        return if (file.exists()) {
            try {
                json.decodeFromString<Preset>(file.readText())
            } catch (e: Exception) {
                logger.error(e) { "Failed to load preset: $name" }
                null
            }
        } else null
    }

    /**
     * Delete a preset
     */
    fun deletePreset(name: String): Boolean {
        val file = File(presetsDir, "${sanitizeFileName(name)}.json")
        return if (file.exists()) {
            file.delete()
        } else false
    }

    /**
     * Export preset to a specific file
     */
    fun exportToFile(preset: Preset, targetFile: File): Boolean {
        return try {
            targetFile.writeText(json.encodeToString(preset))
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to export preset to: ${targetFile.absolutePath}" }
            false
        }
    }

    /**
     * Import preset from a file
     */
    fun importFromFile(sourceFile: File): Preset? {
        return try {
            json.decodeFromString<Preset>(sourceFile.readText())
        } catch (e: Exception) {
            logger.error(e) { "Failed to import preset from: ${sourceFile.absolutePath}" }
            null
        }
    }

    /**
     * Get the presets directory path
     */
    fun getPresetsDirectory(): String = presetsDir.absolutePath

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
            .replace(" ", "_")
            .lowercase()
    }
}

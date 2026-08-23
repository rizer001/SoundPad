package com.rizer01.soundpad.store

import com.rizer01.soundpad.model.AppSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.File
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

/**
 * Manages app settings persistence.
 * Settings are stored as a JSON file in the user's home directory.
 */
class SettingsStore {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val settingsFile: File by lazy {
        val dir = Paths.get(System.getProperty("user.home"), ".soundpad").toFile()
        if (!dir.exists()) dir.mkdirs()
        File(dir, "settings.json")
    }

    private var _settings = AppSettings()
    val settings: AppSettings get() = _settings

    /**
     * Load settings from disk, or use defaults
     */
    fun load(): AppSettings {
        _settings = if (settingsFile.exists()) {
            try {
                json.decodeFromString<AppSettings>(settingsFile.readText())
            } catch (e: Exception) {
                logger.error(e) { "Failed to load settings, using defaults" }
                AppSettings()
            }
        } else {
            AppSettings()
        }
        return _settings
    }

    /**
     * Save current settings to disk
     */
    fun save() {
        try {
            settingsFile.writeText(json.encodeToString(_settings))
            logger.debug { "Settings saved" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to save settings" }
        }
    }

    /**
     * Update settings and save
     */
    fun update(block: AppSettings.() -> AppSettings) {
        _settings = _settings.block()
        save()
    }

    /**
     * Reset to defaults
     */
    fun reset() {
        _settings = AppSettings()
        save()
    }
}

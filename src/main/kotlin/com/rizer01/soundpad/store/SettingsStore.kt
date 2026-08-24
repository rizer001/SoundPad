package com.rizer01.soundpad.store

import com.rizer01.soundpad.db.DatabaseManager
import com.rizer01.soundpad.model.AppSettings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Manages app settings persistence via SQLite.
 * Settings are stored as key-value pairs in the settings table.
 */
class SettingsStore(private val db: DatabaseManager) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var _settings = AppSettings()
    val settings: AppSettings get() = _settings

    init {
        load()
    }

    /** Load settings from SQLite */
    private fun load() {
        val raw = db.loadSetting("app_settings")
        _settings = if (raw != null) {
            try {
                json.decodeFromString<AppSettings>(raw)
            } catch (e: Exception) {
                logger.error(e) { "Failed to parse settings, using defaults" }
                AppSettings()
            }
        } else {
            // First run — save defaults
            AppSettings().also { save(it) }
        }
    }

    /** Save settings to SQLite */
    private fun save(settings: AppSettings) {
        try {
            db.saveSetting("app_settings", json.encodeToString(settings))
            logger.debug { "Settings saved to database" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to save settings" }
        }
    }

    /** Update settings and persist */
    fun update(block: AppSettings.() -> AppSettings) {
        _settings = _settings.block()
        save(_settings)
    }

    /** Reset to defaults */
    fun reset() {
        _settings = AppSettings()
        save(_settings)
    }
}

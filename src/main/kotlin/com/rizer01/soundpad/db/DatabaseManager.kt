package com.rizer01.soundpad.db

import com.rizer01.soundpad.model.AppSettings
import com.rizer01.soundpad.model.SoundCategory
import com.rizer01.soundpad.model.SoundFile
import mu.KotlinLogging
import java.io.File
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet

private val logger = KotlinLogging.logger {}

/**
 * Portable SQLite database manager.
 *
 * All data (sounds, categories, settings) is stored in a single `soundpad.db` file
 * located in a `data/` subdirectory next to the JAR (or project root in dev mode).
 *
 * Audio files dropped into the app are copied to a `sounds/` folder next to the JAR
 * so the entire installation is self-contained.
 */
class DatabaseManager {

    /** Root directory: next to JAR in release, or project root in dev */
    val appDir: File by lazy { resolveAppDir() }

    /** Database file: <appDir>/data/soundpad.db */
    val dbFile: File by lazy { File(appDir, "data/soundpad.db") }

    /** Sounds storage: <appDir>/sounds/ */
    val soundsDir: File by lazy { File(appDir, "sounds") }

    private var connection: Connection? = null

    // ── Lifecycle ──

    fun init() {
        appDir.mkdirs()
        dbFile.parentFile?.mkdirs()
        soundsDir.mkdirs()

        Class.forName("org.sqlite.JDBC")
        connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        connection!!.autoCommit = false

        createTables()
        logger.info { "Database initialized at ${dbFile.absolutePath}" }
    }

    fun close() {
        try {
            connection?.commit()
            connection?.close()
        } catch (e: Exception) {
            logger.error(e) { "Error closing database" }
        }
    }

    // ── Schema ──

    private fun createTables() {
        val stmt = connection!!.createStatement()

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS sounds (
                id          TEXT PRIMARY KEY,
                name        TEXT NOT NULL,
                filePath    TEXT NOT NULL,
                volume      REAL DEFAULT 1.0,
                hotkey      TEXT,
                hotkeyEnabled INTEGER DEFAULT 0,
                categoryId  TEXT DEFAULT 'default',
                loop        INTEGER DEFAULT 0,
                duration    REAL DEFAULT 0.0,
                createdAt   INTEGER NOT NULL
            )
        """.trimIndent())

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS categories (
                id       TEXT PRIMARY KEY,
                name     TEXT NOT NULL,
                icon     TEXT DEFAULT 'folder',
                color    INTEGER DEFAULT ${0xFF6750A4L},
                sortKey  INTEGER DEFAULT 0,
                customIconPath TEXT
            )
        """.trimIndent())

        // Migration: add customIconPath column if missing (for existing DBs)
        try {
            stmt.executeUpdate("ALTER TABLE categories ADD COLUMN customIconPath TEXT")
        } catch (_: Exception) { /* column already exists */ }

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS settings (
                key   TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
        """.trimIndent())

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS presets (
                id          TEXT PRIMARY KEY,
                name        TEXT NOT NULL,
                description TEXT DEFAULT '',
                createdAt   INTEGER NOT NULL,
                updatedAt   INTEGER NOT NULL
            )
        """.trimIndent())

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS preset_sounds (
                presetId TEXT NOT NULL,
                soundId  TEXT NOT NULL,
                sortKey  INTEGER DEFAULT 0,
                FOREIGN KEY (presetId) REFERENCES presets(id),
                PRIMARY KEY (presetId, soundId)
            )
        """.trimIndent())

        connection!!.commit()
        stmt.close()
    }

    // ── Sounds CRUD ──

    fun insertSound(sound: SoundFile) {
        val ps = connection!!.prepareStatement("""
            INSERT OR REPLACE INTO sounds (id, name, filePath, volume, hotkey, hotkeyEnabled, categoryId, loop, duration, createdAt)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent())
        ps.setString(1, sound.id)
        ps.setString(2, sound.name)
        ps.setString(3, sound.filePath)
        ps.setFloat(4, sound.volume)
        ps.setString(5, sound.hotkey)
        ps.setInt(6, if (sound.hotkey != null) 1 else 0)
        ps.setString(7, sound.categoryId)
        ps.setInt(8, if (sound.loop) 1 else 0)
        ps.setFloat(9, sound.duration)
        ps.setLong(10, sound.createdAt)
        ps.executeUpdate()
        ps.close()
        connection!!.commit()
    }

    fun deleteSound(soundId: String) {
        val ps = connection!!.prepareStatement("DELETE FROM sounds WHERE id = ?")
        ps.setString(1, soundId)
        ps.executeUpdate()
        ps.close()
        connection!!.commit()
    }

    fun loadAllSounds(): List<SoundFile> {
        val stmt = connection!!.createStatement()
        val rs = stmt.executeQuery("SELECT * FROM sounds ORDER BY createdAt")
        val list = mutableListOf<SoundFile>()
        while (rs.next()) {
            list.add(rs.toSoundFile())
        }
        rs.close()
        stmt.close()
        return list
    }

    fun updateSound(sound: SoundFile) = insertSound(sound) // INSERT OR REPLACE

    fun clearAllSounds() {
        connection!!.createStatement().executeUpdate("DELETE FROM sounds")
        connection!!.commit()
    }

    // ── Categories CRUD ──

    fun insertCategory(cat: SoundCategory) {
        val ps = connection!!.prepareStatement("""
            INSERT OR REPLACE INTO categories (id, name, icon, color, sortKey, customIconPath)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent())
        ps.setString(1, cat.id)
        ps.setString(2, cat.name)
        ps.setString(3, cat.icon)
        ps.setLong(4, cat.color)
        ps.setInt(5, cat.order)
        ps.setString(6, cat.customIconPath)
        ps.executeUpdate()
        ps.close()
        connection!!.commit()
    }

    fun deleteCategory(categoryId: String) {
        val ps = connection!!.prepareStatement("DELETE FROM categories WHERE id = ?")
        ps.setString(1, categoryId)
        ps.executeUpdate()
        ps.close()
        connection!!.commit()
    }

    fun loadAllCategories(): List<SoundCategory> {
        val stmt = connection!!.createStatement()
        val rs = stmt.executeQuery("SELECT * FROM categories ORDER BY sortKey")
        val list = mutableListOf<SoundCategory>()
        while (rs.next()) {
            list.add(SoundCategory(
                id = rs.getString("id"),
                name = rs.getString("name"),
                icon = rs.getString("icon"),
                color = rs.getLong("color"),
                order = rs.getInt("sortKey"),
                customIconPath = try { rs.getString("customIconPath") } catch (_: Exception) { null }
            ))
        }
        rs.close()
        stmt.close()
        return list
    }

    // ── Settings ──

    fun saveSetting(key: String, value: String) {
        val ps = connection!!.prepareStatement("""
            INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)
        """.trimIndent())
        ps.setString(1, key)
        ps.setString(2, value)
        ps.executeUpdate()
        ps.close()
        connection!!.commit()
    }

    fun loadSetting(key: String): String? {
        val ps = connection!!.prepareStatement("SELECT value FROM settings WHERE key = ?")
        ps.setString(1, key)
        val rs = ps.executeQuery()
        val value = if (rs.next()) rs.getString("value") else null
        rs.close()
        ps.close()
        return value
    }

    fun loadAllSettings(): Map<String, String> {
        val stmt = connection!!.createStatement()
        val rs = stmt.executeQuery("SELECT key, value FROM settings")
        val map = mutableMapOf<String, String>()
        while (rs.next()) {
            map[rs.getString("key")] = rs.getString("value")
        }
        rs.close()
        stmt.close()
        return map
    }

    fun clearAllSettings() {
        connection!!.createStatement().executeUpdate("DELETE FROM settings")
        connection!!.commit()
    }

    // ── Sound file management (copy to portable dir) ──

    /**
     * Copy an audio file into the portable sounds/ directory.
     * Returns the new absolute path.
     */
    fun copySoundToPortable(sourceFile: File): String {
        val target = File(soundsDir, "${System.currentTimeMillis()}_${sourceFile.name}")
        sourceFile.copyTo(target, overwrite = true)
        return target.absolutePath
    }

    // ── Default categories seed ──

    fun seedDefaultCategoriesIfEmpty() {
        if (loadAllCategories().isEmpty()) {
            val defaults = listOf(
                SoundCategory(id = "default", name = "All Sounds", icon = "folder", order = 0),
                SoundCategory(id = "memes", name = "Memes", icon = "mood", order = 1),
                SoundCategory(id = "alerts", name = "Alerts", icon = "notifications", order = 2),
                SoundCategory(id = "music", name = "Music", icon = "music_note", order = 3),
                SoundCategory(id = "voice", name = "Voice", icon = "record_voice_over", order = 4),
                SoundCategory(id = "sfx", name = "Sound Effects", icon = "hearing", order = 5),
                SoundCategory(id = "custom", name = "Custom", icon = "star", order = 6)
            )
            defaults.forEach { insertCategory(it) }
            logger.info { "Seeded ${defaults.size} default categories" }
        }
    }

    // ── Helpers ──

    private fun ResultSet.toSoundFile() = SoundFile(
        id = getString("id"),
        name = getString("name"),
        filePath = getString("filePath"),
        volume = getFloat("volume"),
        hotkey = getString("hotkey"),
        categoryId = getString("categoryId"),
        loop = getInt("loop") == 1,
        duration = getFloat("duration"),
        createdAt = getLong("createdAt")
    )

    private fun resolveAppDir(): File {
        // Try to find the JAR location
        val jarLocation = try {
            val codeSource = DatabaseManager::class.java.protectionDomain?.codeSource
            if (codeSource != null) {
                File(codeSource.location.toURI()).parentFile
            } else null
        } catch (e: Exception) { null }

        if (jarLocation != null && jarLocation.exists()) {
            // Running from JAR — use directory next to JAR
            return jarLocation
        }

        // Dev mode — use project directory
        return File(System.getProperty("user.dir"))
    }
}

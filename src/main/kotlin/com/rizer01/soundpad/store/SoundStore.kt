package com.rizer01.soundpad.store

import com.rizer01.soundpad.model.Preset
import com.rizer01.soundpad.model.SoundCategory
import com.rizer01.soundpad.model.SoundFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

/**
 * Manages the in-memory state of the sound library.
 * Handles categories, sounds, search, and selection.
 */
class SoundStore {

    private val _categories = MutableStateFlow(defaultCategories())
    val categories: StateFlow<List<SoundCategory>> = _categories.asStateFlow()

    private val _sounds = MutableStateFlow<List<SoundFile>>(emptyList())
    val sounds: StateFlow<List<SoundFile>> = _sounds.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredSounds = MutableStateFlow<List<SoundFile>>(emptyList())
    val filteredSounds: StateFlow<List<SoundFile>> = _filteredSounds.asStateFlow()

    init {
        updateFilteredSounds()
    }

    /**
     * Add a sound file to the library
     */
    fun addSound(sound: SoundFile) {
        _sounds.update { current -> current + sound }
        updateFilteredSounds()
    }

    /**
     * Add multiple sound files
     */
    fun addSounds(sounds: List<SoundFile>) {
        _sounds.update { current -> current + sounds }
        updateFilteredSounds()
    }

    /**
     * Remove a sound by ID
     */
    fun removeSound(soundId: String) {
        _sounds.update { current -> current.filter { it.id != soundId } }
        updateFilteredSounds()
    }

    /**
     * Update a sound
     */
    fun updateSound(updated: SoundFile) {
        _sounds.update { current ->
            current.map { if (it.id == updated.id) updated else it }
        }
        updateFilteredSounds()
    }

    /**
     * Get a sound by ID
     */
    fun getSound(soundId: String): SoundFile? {
        return _sounds.value.find { it.id == soundId }
    }

    /**
     * Add a category
     */
    fun addCategory(category: SoundCategory) {
        _categories.update { current -> current + category }
    }

    /**
     * Remove a category
     */
    fun removeCategory(categoryId: String) {
        _categories.update { current -> current.filter { it.id != categoryId } }
        // Move sounds from deleted category to default
        _sounds.update { current ->
            current.map {
                if (it.categoryId == categoryId) it.copy(categoryId = "default") else it
            }
        }
        updateFilteredSounds()
    }

    /**
     * Select a category for filtering
     */
    fun selectCategory(categoryId: String?) {
        _selectedCategory.value = categoryId
        updateFilteredSounds()
    }

    /**
     * Update search query
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        updateFilteredSounds()
    }

    /**
     * Update filtered sounds based on category and search
     */
    private fun updateFilteredSounds() {
        val allSounds = _sounds.value
        val categoryId = _selectedCategory.value
        val query = _searchQuery.value.lowercase().trim()

        val filtered = allSounds.filter { sound ->
            val matchesCategory = categoryId == null || sound.categoryId == categoryId
            val matchesSearch = query.isEmpty() ||
                    sound.name.lowercase().contains(query) ||
                    sound.filePath.lowercase().contains(query)
            matchesCategory && matchesSearch
        }

        _filteredSounds.value = filtered
    }

    /**
     * Load sounds from a preset
     */
    fun loadPreset(preset: Preset) {
        _sounds.value = preset.sounds
        _categories.value = preset.categories.ifEmpty { defaultCategories() }
        _selectedCategory.value = null
        _searchQuery.value = ""
        updateFilteredSounds()
    }

    /**
     * Export current state as a preset
     */
    fun exportPreset(name: String, description: String = ""): Preset {
        return Preset(
            name = name,
            description = description,
            sounds = _sounds.value,
            categories = _categories.value
        )
    }

    /**
     * Get total sound count
     */
    fun getSoundCount(): Int = _sounds.value.size

    /**
     * Get sounds for a specific category
     */
    fun getSoundsForCategory(categoryId: String): List<SoundFile> {
        return _sounds.value.filter { it.categoryId == categoryId }
    }

    private fun defaultCategories(): List<SoundCategory> {
        return listOf(
            SoundCategory(id = "default", name = "All Sounds", icon = "folder", order = 0),
            SoundCategory(id = "memes", name = "Memes", icon = "mood", order = 1),
            SoundCategory(id = "alerts", name = "Alerts", icon = "notifications", order = 2),
            SoundCategory(id = "music", name = "Music", icon = "music_note", order = 3),
            SoundCategory(id = "voice", name = "Voice", icon = "record_voice_over", order = 4),
            SoundCategory(id = "sfx", name = "Sound Effects", icon = "hearing", order = 5),
            SoundCategory(id = "custom", name = "Custom", icon = "star", order = 6)
        )
    }
}

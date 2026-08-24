package com.rizer01.soundpad.hotkey

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.NativeHookException
import com.github.kwhat.jnativehook.NativeInputEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

/**
 * Manages global hotkeys that work even when the app is not focused.
 * Uses JNativeHook for cross-platform global shortcuts.
 */
class HotkeyManager {

    private val bindings = ConcurrentHashMap<String, String>()   // key combo → sound ID
    private val reverseBindings = ConcurrentHashMap<String, String>()  // sound ID → key combo
    private var callback: ((String) -> Unit)? = null
    private var isRegistered = false

    // Key capture state
    @Volatile
    private var capturing = false
    @Volatile
    private var capturedKey: String? = null
    private var captureLatch: CountDownLatch? = null

    /**
     * Initialize the global hotkey listener
     */
    fun init(onHotkeyTriggered: (soundId: String) -> Unit) {
        callback = onHotkeyTriggered

        try {
            GlobalScreen.registerNativeHook()
            GlobalScreen.addNativeKeyListener(object : NativeKeyListener {
                override fun nativeKeyPressed(e: NativeKeyEvent) {
                    val combo = keyEventToString(e)

                    // If capturing, record the key and stop
                    if (capturing) {
                        capturedKey = combo
                        captureLatch?.countDown()
                        return
                    }

                    bindings[combo]?.let { soundId ->
                        logger.debug { "Hotkey triggered: $combo → $soundId" }
                        callback?.invoke(soundId)
                    }
                }

                override fun nativeKeyTyped(e: NativeKeyEvent) {}
                override fun nativeKeyReleased(e: NativeKeyEvent) {}
            })
            isRegistered = true
            logger.info { "Global hotkey listener registered" }
        } catch (e: NativeHookException) {
            logger.error(e) { "Failed to register global hotkey listener" }
        }
    }

    /**
     * Capture the next key press. Returns the key combo string, or null if timed out.
     * Blocks for up to [timeoutMs] milliseconds.
     */
    fun captureNextKey(timeoutMs: Long = 5000): String? {
        if (!isRegistered) return null
        capturing = true
        capturedKey = null
        captureLatch = CountDownLatch(1)

        val result = try {
            captureLatch?.await(timeoutMs, TimeUnit.MILLISECONDS)
            capturedKey
        } catch (e: InterruptedException) {
            null
        } finally {
            capturing = false
            captureLatch = null
        }
        return result
    }

    /**
     * Bind a hotkey to a sound
     */
    fun bind(hotkey: String, soundId: String) {
        reverseBindings[soundId]?.let { oldKey ->
            bindings.remove(oldKey)
        }
        bindings[hotkey] = soundId
        reverseBindings[soundId] = hotkey
        logger.debug { "Bound hotkey: $hotkey → $soundId" }
    }

    /**
     * Unbind a hotkey
     */
    fun unbind(soundId: String) {
        reverseBindings[soundId]?.let { key ->
            bindings.remove(key)
        }
        reverseBindings.remove(soundId)
    }

    /**
     * Unbind all hotkeys
     */
    fun unbindAll() {
        bindings.clear()
        reverseBindings.clear()
    }

    /**
     * Get the hotkey for a sound
     */
    fun getHotkey(soundId: String): String? = reverseBindings[soundId]

    /**
     * Check if a hotkey is already bound
     */
    fun isBound(hotkey: String): Boolean = bindings.containsKey(hotkey)

    /**
     * Convert a NativeKeyEvent to a readable string
     */
    private fun keyEventToString(e: NativeKeyEvent): String {
        val parts = mutableListOf<String>()

        // Modifier masks from NativeInputEvent
        val mods = e.modifiers
        if (mods and NativeInputEvent.ALT_MASK != 0) parts.add("Alt")
        if (mods and NativeInputEvent.CTRL_MASK != 0) parts.add("Ctrl")
        if (mods and NativeInputEvent.SHIFT_MASK != 0) parts.add("Shift")
        if (mods and NativeInputEvent.META_MASK != 0) parts.add("Meta")

        val keyName = NativeKeyEvent.getKeyText(e.keyCode)
        if (keyName.isNotEmpty() && keyName != "Unknown") {
            parts.add(keyName)
        }

        return parts.joinToString("+")
    }

    /**
     * Cleanup
     */
    fun dispose() {
        if (isRegistered) {
            try {
                GlobalScreen.unregisterNativeHook()
            } catch (e: NativeHookException) {
                logger.error(e) { "Failed to unregister global hotkey listener" }
            }
        }
        bindings.clear()
        reverseBindings.clear()
    }
}

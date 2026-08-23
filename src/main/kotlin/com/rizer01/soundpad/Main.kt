package com.rizer01.soundpad

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.rizer01.soundpad.ui.SoundpadApp
import com.rizer01.soundpad.ui.theme.SoundpadTheme
import java.awt.Dimension
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.*
import java.io.File

// Global callback for drag & drop files — set by SoundpadApp
var onGlobalFilesDropped: ((List<File>) -> Unit)? = null

fun main() = application {
    val windowState = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(1200.dp, 800.dp)
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Soundpad",
        state = windowState,
    ) {
        window.minimumSize = Dimension(900, 600)

        // Register AWT DropTarget on the JFrame for drag & drop
        LaunchedEffect(Unit) {
            val frame = window
            frame.dropTarget = object : DropTarget() {
                override fun drop(dtde: DropTargetDropEvent) {
                    try {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY)
                        val transferable = dtde.transferable
                        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            @Suppress("UNCHECKED_CAST")
                            val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                            if (!files.isNullOrEmpty()) {
                                val audioExtensions = setOf("mp3", "wav", "ogg", "flac", "m4a", "aac", "wma")
                                val audioFiles = files.filter { f ->
                                    f.exists() && f.extension.lowercase() in audioExtensions
                                }
                                if (audioFiles.isNotEmpty()) {
                                    onGlobalFilesDropped?.invoke(audioFiles)
                                }
                            }
                        }
                        dtde.dropComplete(true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        dtde.dropComplete(false)
                    }
                }
            }
        }

        SoundpadTheme {
            SoundpadApp()
        }
    }
}

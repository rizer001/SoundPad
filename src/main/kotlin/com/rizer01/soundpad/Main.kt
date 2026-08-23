package com.rizer01.soundpad

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
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

var onGlobalFilesDropped: ((List<File>) -> Unit)? = null

private fun createDropTarget(): DropTarget {
    return object : DropTarget() {
        override fun drop(dtde: DropTargetDropEvent) {
            try {
                dtde.acceptDrop(DnDConstants.ACTION_COPY)
                val transferable = dtde.transferable
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    @Suppress("UNCHECKED_CAST")
                    val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                    if (!files.isNullOrEmpty()) {
                        val exts = setOf("mp3", "wav", "ogg", "flac", "m4a", "aac", "wma")
                        val audioFiles = files.filter { it.exists() && it.extension.lowercase() in exts }
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

        override fun dragEnter(dtde: DropTargetDragEvent) {
            // placeholder
        }
    }
}

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

        LaunchedEffect(Unit) {
            val frame = window
            frame.dropTarget = createDropTarget()
            try {
                frame.glassPane?.dropTarget = createDropTarget()
                frame.glassPane?.isVisible = false
            } catch (_: Exception) {}
        }

        SoundpadTheme {
            SoundpadApp()
        }
    }
}

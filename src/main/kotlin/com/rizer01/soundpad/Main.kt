package com.rizer01.soundpad

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.rizer01.soundpad.db.DatabaseManager
import com.rizer01.soundpad.ui.SoundpadApp
import com.rizer01.soundpad.ui.theme.SoundpadTheme
import java.awt.Dimension
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.*
import java.io.File
import javax.swing.JFrame

/** Global callback: files dropped from OS file explorer onto the window */
var onGlobalFilesDropped: ((List<File>) -> Unit)? = null

/** Global database manager — initialized once at startup */
val dbManager = DatabaseManager()

private val AudioExtensions = setOf("mp3", "wav", "ogg", "flac", "m4a", "aac", "wma")

private fun installDropTarget(frame: JFrame) {
    val listener = object : DropTargetListener {
        override fun dragEnter(dtde: DropTargetDragEvent) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY)
        }

        override fun dragOver(dtde: DropTargetDragEvent) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY)
        }

        override fun dragExit(dtde: DropTargetEvent) {}

        override fun dropActionChanged(dtde: DropTargetDragEvent) {}

        override fun drop(dtde: DropTargetDropEvent) {
            try {
                dtde.acceptDrop(DnDConstants.ACTION_COPY)
                val transferable = dtde.transferable
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    @Suppress("UNCHECKED_CAST")
                    val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                    if (!files.isNullOrEmpty()) {
                        val audioFiles = files.filter { it.exists() && it.extension.lowercase() in AudioExtensions }
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

    // Set DropTarget on contentPane — this is the actual rendering surface
    frame.contentPane.dropTarget = DropTarget(frame.contentPane, DnDConstants.ACTION_COPY, listener)
    // Also set on rootPane as fallback
    frame.rootPane.dropTarget = DropTarget(frame.rootPane, DnDConstants.ACTION_COPY, listener)
}

fun main() = application {
    // Initialize database
    dbManager.init()

    // Shutdown hook — save everything on exit
    Runtime.getRuntime().addShutdownHook(Thread {
        println("[Soundpad] Shutdown hook: saving state...")
        dbManager.close()
        println("[Soundpad] Database closed. Goodbye!")
    })

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
        // Install AWT DropTarget on JFrame after it's available
        LaunchedEffect(Unit) {
            installDropTarget(window as JFrame)
        }
        SoundpadTheme {
            SoundpadApp(db = dbManager)
        }
    }
}

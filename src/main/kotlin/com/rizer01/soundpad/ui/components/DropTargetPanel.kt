package com.rizer01.soundpad.ui.components

import com.rizer01.soundpad.onGlobalFilesDropped
import java.awt.Color
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.*
import javax.swing.JPanel

private val AudioExtensions = setOf("mp3", "wav", "ogg", "flac", "m4a", "aac", "wma")

/**
 * A transparent JPanel that intercepts file drag-and-drop events from the OS file explorer.
 */
class DropTargetPanel : JPanel() {

    var onDragEnter: () -> Unit = {}
    var onDragExit: () -> Unit = {}

    private val dropListener = object : DropTargetListener {
        override fun dragEnter(dtde: DropTargetDragEvent) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY)
            onDragEnter()
        }

        override fun dragOver(dtde: DropTargetDragEvent) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY)
        }

        override fun dragExit(dtde: DropTargetEvent) {
            onDragExit()
        }

        override fun dropActionChanged(dtde: DropTargetDragEvent) {}

        override fun drop(dtde: DropTargetDropEvent) {
            try {
                dtde.acceptDrop(DnDConstants.ACTION_COPY)
                val transferable = dtde.transferable
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    @Suppress("UNCHECKED_CAST")
                    val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<java.io.File>
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
            } finally {
                onDragExit()
            }
        }
    }

    init {
        isOpaque = false
        background = Color(0, 0, 0, 0)
        isFocusable = false
        dropTarget = DropTarget(this, DnDConstants.ACTION_COPY, dropListener)
    }
}

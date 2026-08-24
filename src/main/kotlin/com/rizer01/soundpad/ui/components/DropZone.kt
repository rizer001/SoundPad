package com.rizer01.soundpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.awt.SwingPanel
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.*
import java.io.File
import javax.swing.JPanel
import javax.swing.SwingUtilities

private val AUDIO_EXTENSIONS = setOf("mp3", "wav", "ogg", "flac", "m4a", "aac", "wma")
private val AccentCyan = Color(0xFF00D4FF)

/**
 * Drop zone: white-bordered square + label + browse button.
 */
@Composable
fun DropZone(
    onFilesDropped: (List<File>) -> Unit,
    onBrowseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragOver by remember { mutableStateOf(false) }

    val dropPanel = remember {
        object : JPanel() {
            init {
                isOpaque = false
                background = Color(0, 0, 0, 0).awtColor()

                dropTarget = DropTarget(this, DnDConstants.ACTION_COPY, object : DropTargetListener {
                    override fun dragEnter(dtde: DropTargetDragEvent) {
                        dtde.acceptDrag(DnDConstants.ACTION_COPY)
                        SwingUtilities.invokeLater { isDragOver = true }
                    }
                    override fun dragOver(dtde: DropTargetDragEvent) {
                        dtde.acceptDrag(DnDConstants.ACTION_COPY)
                    }
                    override fun dragExit(dtde: DropTargetEvent) {
                        SwingUtilities.invokeLater { isDragOver = false }
                    }
                    override fun dropActionChanged(dtde: DropTargetDragEvent) {}
                    override fun drop(dtde: DropTargetDropEvent) {
                        try {
                            dtde.acceptDrop(DnDConstants.ACTION_COPY)
                            val transferable = dtde.transferable
                            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                                @Suppress("UNCHECKED_CAST")
                                val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                                if (!files.isNullOrEmpty()) {
                                    val audioFiles = files.filter { it.exists() && it.extension.lowercase() in AUDIO_EXTENSIONS }
                                    if (audioFiles.isNotEmpty()) {
                                        SwingUtilities.invokeLater {
                                            isDragOver = false
                                            onFilesDropped(audioFiles)
                                        }
                                    }
                                }
                            }
                            dtde.dropComplete(true)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            dtde.dropComplete(false)
                        }
                    }
                })
            }
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // White bordered drop square with AWT DropTarget
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = 2.dp,
                    color = if (isDragOver) AccentCyan else Color.White.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp)
                )
                .background(
                    if (isDragOver) AccentCyan.copy(alpha = 0.1f)
                    else Color.White.copy(alpha = 0.05f)
                )
        ) {
            // AWT DropTarget panel
            SwingPanel(
                factory = { dropPanel },
                modifier = Modifier.fillMaxSize()
            )
            // Icon overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.CloudUpload,
                    contentDescription = "Drop files here",
                    modifier = Modifier.size(22.dp),
                    tint = if (isDragOver) AccentCyan else Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Label text
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = if (isDragOver) "Release to add" else "Drop audio files here",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isDragOver) AccentCyan else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "MP3, WAV, OGG, FLAC",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Browse button (folder icon) — opens file chooser
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onBrowseClick),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.FolderOpen,
                    contentDescription = "Browse files",
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Convert Compose Color to AWT Color */
private fun Color.awtColor(): java.awt.Color {
    return java.awt.Color(
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
        (alpha * 255).toInt()
    )
}

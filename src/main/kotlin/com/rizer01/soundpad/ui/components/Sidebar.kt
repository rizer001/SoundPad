package com.rizer01.soundpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rizer01.soundpad.model.SoundCategory

private val AccentCyan = Color(0xFF00D4FF)
private val AccentPurple = Color(0xFF7C3AED)

@Composable
fun Sidebar(
    categories: List<SoundCategory>,
    selectedCategory: String?,
    soundCounts: Map<SoundCategory, Int>,
    onCategoryClick: (String?) -> Unit,
    onAddCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Header
        Text(
            "Categories",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Spacer(Modifier.height(4.dp))

        // "All Sounds" category
        CategoryItem(
            name = "All Sounds",
            icon = Icons.Filled.Folder,
            count = soundCounts.values.sum(),
            isSelected = selectedCategory == null,
            onClick = { onCategoryClick(null) }
        )

        Spacer(Modifier.height(8.dp))

        // Category list
        categories.filter { it.id != "default" }.forEach { category ->
            CategoryItem(
                name = category.name,
                icon = getCategoryIcon(category.name),
                count = soundCounts[category] ?: 0,
                isSelected = selectedCategory == category.id,
                onClick = { onCategoryClick(category.id) }
            )
        }

        Spacer(Modifier.weight(1f))

        // Add category button
        OutlinedButton(
            onClick = onAddCategory,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Add Category")
        }

        Spacer(Modifier.height(8.dp))

        // Stats
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.AudioFile,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "${soundCounts.values.sum()} sounds",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryItem(
    name: String,
    icon: ImageVector,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    else
        MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Icon in styled container (like the website)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) AccentCyan else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (count > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "$count",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) AccentCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private fun getCategoryIcon(name: String): ImageVector {
    return when (name.lowercase()) {
        "memes" -> Icons.Filled.EmojiEmotions
        "alerts" -> Icons.Filled.Notifications
        "music" -> Icons.Filled.MusicNote
        "voice" -> Icons.Filled.RecordVoiceOver
        "sound effects", "sfx" -> Icons.AutoMirrored.Filled.VolumeUp
        "custom" -> Icons.Filled.Star
        else -> Icons.Filled.Folder
    }
}

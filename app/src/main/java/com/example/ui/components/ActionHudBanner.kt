package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LyraBorderGlow
import com.example.ui.theme.LyraCyan
import com.example.ui.theme.LyraSuccess
import com.example.ui.theme.LyraSurfaceCard
import com.example.ui.theme.LyraViolet
import com.example.ui.theme.LyraWarning

@Composable
fun ActionHudBanner(
    isVisionActive: Boolean,
    isAccessibilityActive: Boolean,
    memoryCount: Int,
    onVisionClick: () -> Unit,
    onAccessibilityClick: () -> Unit,
    onMemoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(LyraSurfaceCard.copy(alpha = 0.7f))
            .border(1.dp, LyraBorderGlow, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("action_hud_banner"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HudStatusChip(
            icon = Icons.Default.CameraAlt,
            label = if (isVisionActive) "Vision On" else "Vision Off",
            isActive = isVisionActive,
            activeColor = LyraCyan,
            onClick = onVisionClick,
            testTag = "hud_vision_chip"
        )

        HudStatusChip(
            icon = Icons.Default.TouchApp,
            label = if (isAccessibilityActive) "Control Ready" else "No Service",
            isActive = isAccessibilityActive,
            activeColor = LyraSuccess,
            onClick = onAccessibilityClick,
            testTag = "hud_accessibility_chip"
        )

        HudStatusChip(
            icon = Icons.Default.Psychology,
            label = "$memoryCount Memories",
            isActive = memoryCount > 0,
            activeColor = LyraViolet,
            onClick = onMemoryClick,
            testTag = "hud_memory_chip"
        )
    }
}

@Composable
private fun HudStatusChip(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor else LyraWarning.copy(alpha = 0.6f))
        )
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

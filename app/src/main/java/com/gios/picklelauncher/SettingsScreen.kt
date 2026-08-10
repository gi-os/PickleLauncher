package com.gios.picklelauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF1A1A2E)
private val FocusRing = Color(0xFFFFC107)
private val RowBg = Color(0x22FFFFFF)
private val RowFocusedBg = Color(0x44444444)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFAAAAAA)

/**
 * Settings screen — an internal "app" you can place in any grid slot.
 * All key handling is done by MainActivity; this composable is purely visual.
 *
 * Items:
 * 0. Wallpaper Color — cycle through preset colors
 * 1. Wallpaper Photo — pick a photo from storage
 * 2. Clear Photo — remove photo (only shown when a photo is set)
 * 3. Done — close settings
 */
@Composable
fun SettingsScreen(state: LauncherState) {
    var focusIndex by remember { mutableIntStateOf(0) }

    // Build the visible items list.
    val hasPhoto = state.wallpaperPhotoUri != null
    val items = mutableListOf("Wallpaper Color", "Wallpaper Photo")
    if (hasPhoto) items.add("Clear Photo")
    items.add("Done")

    // Sync focus from state (MainActivity drives D-pad).
    focusIndex = state.pickerFocusIndex.coerceIn(0, items.lastIndex)

    Column(
        modifier = Modifier.fillMaxSize().background(Bg),
    ) {
        // Header.
        Box(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Settings",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // Current wallpaper preview.
        Box(
            modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(state.currentWallpaperColor())),
                )
                Text(
                    text = if (hasPhoto) "Photo wallpaper" else "Color wallpaper",
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
            }
        }

        // Settings list.
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items.forEachIndexed { index, label ->
                val focused = focusIndex == index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (focused) RowFocusedBg else RowBg)
                        .then(
                            if (focused) Modifier.border(2.dp, FocusRing, RoundedCornerShape(6.dp))
                            else Modifier
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Softkey bar.
        SoftkeyBar6(
            f1Label = "Back",
            f2Label = "",
            f3Label = "",
            f4Label = "Done",
            centerLabel = "Select",
        )
    }
}

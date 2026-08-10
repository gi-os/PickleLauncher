package com.gios.picklelauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FocusRing = Color(0xFFFFC107)
private val SlotBg = Color(0x22FFFFFF)
private val SlotEmpty = Color(0x11FFFFFF)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFAAAAAA)
private val SoftkeyBar = Color(0xFF2A2A2A)
private val SoftkeySelectBg = Color(0xFF444444)
private val EditRing = Color(0xFF4CAF50)

@Composable
fun HomeScreen(state: LauncherState, showGrid: Boolean) {
    val bgColor = Color(state.currentWallpaperColor())
    val ctx = LocalContext.current
    val photoBitmap = remember(state.wallpaperPhotoUri) {
        if (state.wallpaperPhotoUri != null)
            loadPhotoBitmap(ctx, state.wallpaperPhotoUri!!)?.asImageBitmap()
        else null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Photo wallpaper background, if set.
        if (photoBitmap != null) {
            Image(
                bitmap = photoBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(bgColor))
        }

        Column(modifier = Modifier.fillMaxSize()) {
            ClockArea(state)

            if (showGrid) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                ) {
                    AppGrid(state)
                }
                // Page indicator.
                Text(
                    text = "Page ${state.currentPage + 1}/${state.pageCount}",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            SoftkeyBar5(
                f1Label = if (state.editMode) "Clear" else "Edit",
                f2Label = if (state.editMode) "Done" else "Menu",
                centerLabel = if (showGrid) "Open" else "Menu",
                f3Label = "Wall",
                f4Label = if (showGrid) "Back" else "Exit",
            )
        }
    }
}

@Composable
private fun ClockArea(state: LauncherState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 12.dp, end = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.timeText,
            color = TextPrimary,
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = state.dateText,
            color = TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AppGrid(state: LauncherState) {
    val context = LocalContext.current
    val pageSlots = state.getPageSlots()
    val ringColor = if (state.editMode) EditRing else FocusRing

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val cellWidth = maxWidth / GRID_COLS
        val cellHeight = maxHeight / GRID_ROWS

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            for (row in 0 until GRID_ROWS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (col in 0 until GRID_COLS) {
                        val slot = row * GRID_COLS + col
                        val entry = pageSlots[slot]
                        val focused = state.focusIndex == slot
                        AppCell(
                            context = context,
                            entry = entry,
                            label = SLOT_LABELS[slot],
                            focused = focused,
                            editMode = state.editMode,
                            ringColor = ringColor,
                            cellWidth = cellWidth,
                            cellHeight = cellHeight,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppCell(
    context: Context,
    entry: AppEntry?,
    label: String,
    focused: Boolean,
    editMode: Boolean,
    ringColor: Color,
    cellWidth: androidx.compose.ui.unit.Dp,
    cellHeight: androidx.compose.ui.unit.Dp,
) {
    // No background behind the icon — transparent, so the wallpaper shows through.
    val bg = if (entry != null) SlotBg else SlotEmpty

    Box(
        modifier = Modifier
            .width(cellWidth)
            .height(cellHeight)
            .padding(3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .then(
                if (focused) Modifier.border(2.dp, ringColor, RoundedCornerShape(8.dp))
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(4.dp),
        ) {
            if (entry != null) {
                // App icon — loaded from the system, rendered as ImageBitmap.
                // No background behind it, just the raw icon on the wallpaper.
                var iconBitmap by remember(entry.packageName, entry.activityName) {
                    mutableStateOf<ImageBitmap?>(null)
                }
                LaunchedEffect(entry.packageName, entry.activityName) {
                    val drawable = state_getAppIcon(context, entry.packageName, entry.activityName)
                    if (drawable != null) {
                        iconBitmap = drawableToImageBitmap(drawable)
                    }
                }
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap!!,
                        contentDescription = entry.label,
                        modifier = Modifier.size(40.dp),
                    )
                }
                // App name underneath.
                Text(
                    text = entry.label,
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                )
            } else if (editMode) {
                Text(
                    text = "+",
                    color = TextSecondary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Number label — mirrors the physical key.
            Text(
                text = label,
                color = if (focused) ringColor else TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 2.dp),
            )
        }
    }
}

// Helper to call state.getAppIcon without passing state into AppCell.
private fun state_getAppIcon(context: Context, pkg: String, act: String): Drawable? {
    return try {
        val pm = context.packageManager
        val info = pm.getActivityInfo(android.content.ComponentName(pkg, act), 0)
        info.loadIcon(pm)
    } catch (e: Exception) { null }
}

/**
 * 5-button softkey bar matching the physical layout:
 * F1 (left) | F2 (left-center) | Center | F3 (right-center) | F4 (right)
 */
@Composable
private fun SoftkeyBar5(
    f1Label: String,
    f2Label: String,
    centerLabel: String,
    f3Label: String,
    f4Label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(SoftkeyBar),
    ) {
        // F1
        SoftkeySection(f1Label, Modifier.weight(1f), false)
        SoftkeyDivider()
        // F2
        SoftkeySection(f2Label, Modifier.weight(1f), false)
        SoftkeyDivider()
        // Center
        SoftkeySection(centerLabel, Modifier.weight(1.2f), true)
        SoftkeyDivider()
        // F3
        SoftkeySection(f3Label, Modifier.weight(1f), false)
        SoftkeyDivider()
        // F4
        SoftkeySection(f4Label, Modifier.weight(1f), false)
    }
}

@Composable
private fun SoftkeySection(label: String, modifier: Modifier, isCenter: Boolean) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .then(if (isCenter) Modifier.background(SoftkeySelectBg) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SoftkeyDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(Color(0x33FFFFFF)),
    )
}

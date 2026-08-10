package com.gios.picklelauncher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFAAAAAA)
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
                    // Animated page transition — slides left/right.
                    AnimatedContent(
                        targetState = state.currentPage,
                        transitionSpec = {
                            if (state.pageSlideDir >= 0) {
                                // Forward: new page slides in from right, old slides out left.
                                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                            } else {
                                // Backward: new page slides in from left, old slides out right.
                                slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                            }
                        },
                        label = "page",
                    ) {
                        AppGrid(state)
                    }
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

            // Physical layout: F1 top-left, F2 bottom-left, F3 top-right, F4 bottom-right
            // On the actual KY-42C hardware, F2(bottom-left)=Apps, F3(top-right)=Wall
            SoftkeyBar6(
                f1Label = if (state.editMode) "Clear" else "Edit",
                f2Label = if (state.editMode) "Done" else "Apps",
                f3Label = if (showGrid) "Wall" else "Menu",
                f4Label = if (showGrid) "Back" else "Exit",
                centerLabel = if (showGrid) "Open" else "Menu",
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
    // No background box — transparent so the wallpaper shows through.
    // Only the focus ring appears when a cell is selected.
    Box(
        modifier = Modifier
            .width(cellWidth)
            .height(cellHeight)
            .padding(4.dp)
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
                // App icon — larger now, no background behind it.
                var iconBitmap by remember(entry.packageName, entry.activityName) {
                    mutableStateOf<ImageBitmap?>(null)
                }
                LaunchedEffect(entry.packageName, entry.activityName) {
                    val drawable = loadAppIcon(context, entry.packageName, entry.activityName)
                    if (drawable != null) {
                        iconBitmap = drawableToImageBitmap(drawable)
                    }
                }
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap!!,
                        contentDescription = entry.label,
                        modifier = Modifier.size(56.dp),
                    )
                }
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

private fun loadAppIcon(context: Context, pkg: String, act: String): Drawable? {
    return try {
        val pm = context.packageManager
        if (pkg == AppEntry.SETTINGS_PKG) {
            // Draw a simple gear-like icon for settings.
            null // Falls back to text label, which is fine.
        } else {
            val info = pm.getActivityInfo(android.content.ComponentName(pkg, act), 0)
            info.loadIcon(pm)
        }
    } catch (e: Exception) { null }
}

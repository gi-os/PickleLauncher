package com.gios.picklelauncher

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Colors matching the Kyocera stock dark theme.
private val BgDefault = Color(0xFF1A1A2E)
private val FocusRing = Color(0xFFFFC107) // amber, same as PickleSolitaire focus
private val SlotBg = Color(0x22FFFFFF) // semi-transparent white for occupied slots
private val SlotEmpty = Color(0x11FFFFFF) // barely visible for empty slots
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFAAAAAA)
private val SoftkeyBar = Color(0xFF2A2A2A)
private val SoftkeySelectBg = Color(0xFF444444)
private val EditRing = Color(0xFF4CAF50) // green in edit mode

/**
 * The home screen. Two modes:
 * - Normal: clock at top, 4×3 app grid, softkey bar at bottom. Pressing a
 *   number key launches the app in that slot.
 * - Edit: same grid but each slot shows a green ring; pressing a slot opens
 *   the app picker to assign a new app to it.
 *
 * The clock-only screen before entering the grid is the Kyocera standby
 * screen — time/date at top, status bar, notification area. Pressing Menu
 * (z key / center) enters the grid; pressing Back returns to the clock.
 * We implement this as a simple mode toggle so the clock is always visible
 * above the grid.
 */
@Composable
fun HomeScreen(
    state: LauncherState,
    showGrid: Boolean,
) {
    val bgColor = Color(state.currentWallpaperColor())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
        // Clock / standby area at the top — always visible.
        ClockArea(state)

        if (showGrid) {
            // The 4×3 app grid fills the remaining space.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 4.dp),
            ) {
                AppGrid(state)
            }
        } else {
            // Just the clock — fill the rest with empty space.
            Spacer(modifier = Modifier.weight(1f))
        }

        // Softkey bar at the bottom — matches PickleSolitaire's layout pattern.
        SoftkeyBar(
            leftLabel = if (state.editMode) "Clear" else "Menu",
            centerLabel = if (showGrid) "Open" else "Menu",
            rightLabel = if (state.editMode) "Done" else "Back",
        )
    }
}

@Composable
private fun ClockArea(state: LauncherState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Time — large, centered, the focal point of the standby screen.
        Text(
            text = state.timeText,
            color = TextPrimary,
            fontSize = 42.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        // Date underneath, smaller and muted.
        Text(
            text = state.dateText,
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AppGrid(state: LauncherState) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
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
                        AppCell(
                            entry = state.slots[slot],
                            label = SLOT_LABELS[slot],
                            focused = state.focusIndex == slot,
                            editMode = state.editMode,
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
    entry: AppEntry?,
    label: String,
    focused: Boolean,
    editMode: Boolean,
    cellWidth: androidx.compose.ui.unit.Dp,
    cellHeight: androidx.compose.ui.unit.Dp,
) {
    val ringColor = if (editMode) EditRing else FocusRing
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
                // App name, centered, truncated.
                Text(
                    text = entry.label,
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (editMode) {
                // Empty slot in edit mode shows a "+" hint.
                Text(
                    text = "+",
                    color = TextSecondary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Number label at the bottom-left corner of the cell — mirrors
            // the physical key that launches it, exactly like the stock grid.
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

/**
 * The bottom softkey bar. Same Row-of-3 layout as PickleSolitaire to avoid
 * the overlap issue with absolutely-positioned corners on the narrow bar.
 *
 * F1 = left, F2 = right-top, F3 = right-bottom, F4 = bottom-right on this
 * device. We use left/center/right as a simple 3-section bar.
 */
@Composable
private fun SoftkeyBar(
    leftLabel: String,
    centerLabel: String,
    rightLabel: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(SoftkeyBar),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            SoftkeyLabel(leftLabel)
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0x33FFFFFF)),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(SoftkeySelectBg),
            contentAlignment = Alignment.Center,
        ) {
            SoftkeyLabel(centerLabel)
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color(0x33FFFFFF)),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            SoftkeyLabel(rightLabel)
        }
    }
}

@Composable
private fun SoftkeyLabel(label: String) {
    Text(
        text = label,
        color = TextPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
}

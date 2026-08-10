package com.gios.picklelauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged

private val Bg = Color(0xFF1A1A2E)
private val FocusRing = Color(0xFFFFC107)
private val RowBg = Color(0x22FFFFFF)
private val RowFocusedBg = Color(0x44444444)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFAAAAAA)
private val SoftkeyBar = Color(0xFF2A2A2A)
private val SoftkeySelectBg = Color(0xFF444444)

/**
 * Shared list UI for both the app picker (assigning to a slot) and the
 * app drawer (browsing all apps to launch). D-pad up/down moves one app
 * at a time, the list auto-scrolls to keep the focused item visible.
 */
@Composable
fun AppListScreen(
    state: LauncherState,
    isDrawer: Boolean,
) {
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        apps = state.getInstalledApps()
    }

    // Auto-scroll to keep focused item visible.
    LaunchedEffect(state.pickerFocusIndex, apps.size) {
        if (apps.isNotEmpty()) {
            val idx = state.pickerFocusIndex.coerceIn(0, apps.size - 1)
            listState.animateScrollToItem(idx)
        }
    }

    val title = if (isDrawer) "Apps" else "Slot ${SLOT_LABELS[state.editingSlot]} — Select App"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (apps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No apps found", color = TextSecondary, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(apps) { index, app ->
                        val focused = state.pickerFocusIndex == index
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (focused) RowFocusedBg else RowBg)
                                .then(
                                    if (focused) Modifier.border(
                                        2.dp, FocusRing, RoundedCornerShape(6.dp)
                                    ) else Modifier
                                )
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = app.label,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // Softkey bar — same 2×3 layout as home.
        SoftkeyBar6(
            f1Label = if (isDrawer) "Back" else "Clear",
            f2Label = "",
            f3Label = "",
            f4Label = "Back",
            centerLabel = "Select",
        )
    }
}

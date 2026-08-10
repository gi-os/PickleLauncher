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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFAAAAAA)
private val SoftkeyBar = Color(0xFF2A2A2A)

/**
 * App picker — a scrollable list of all launchable apps. D-pad up/down
 * moves through the list, center/OK selects, Back cancels. All navigation
 * is handled by MainActivity's dispatchKeyEvent; this composable is purely
 * visual.
 */
@Composable
fun AppPickerScreen(state: LauncherState) {
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var focusIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        apps = state.getInstalledApps()
    }

    // Sync focus from the state (MainActivity sets state.focusIndex on D-pad).
    focusIndex = state.focusIndex.coerceIn(0, (apps.size - 1).coerceAtLeast(0))

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
                text = "Select App — Slot ${SLOT_LABELS[state.editingSlot]}",
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
                    Text(
                        text = "No apps found",
                        color = TextSecondary,
                        fontSize = 14.sp,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(apps) { app ->
                        val index = apps.indexOf(app)
                        val focused = focusIndex == index
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(RowBg)
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(SoftkeyBar),
        ) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Clear", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0x33FFFFFF)))
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF444444)),
                contentAlignment = Alignment.Center,
            ) {
                Text("Select", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color(0x33FFFFFF)))
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Back", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

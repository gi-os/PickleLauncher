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

private val SoftkeyBar = Color(0xFF2A2A2A)
private val SoftkeySelectBg = Color(0xFF444444)
private val SoftkeyDivider = Color(0x33FFFFFF)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFAAAAAA)

/**
 * The softkey bar — 2 rows × 3 columns. The center column is one tall
 * Select button spanning both rows. The four corner cells are the
 * option buttons, matching the physical layout confirmed on the KY-42C
 * via KeyProbe (same as PickleSolitaire):
 *
 *   F1     | SELECT | F3
 *   F2     | SELECT | F4
 *
 * F1 top-left, F2 bottom-left, F3 top-right, F4 bottom-right.
 */
@Composable
fun SoftkeyBar6(
    f1Label: String,
    f2Label: String,
    f3Label: String,
    f4Label: String,
    centerLabel: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(SoftkeyBar),
    ) {
        // Left column: F1 (top) and F2 (bottom).
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            SoftkeyCell(f1Label, Modifier.weight(1f).fillMaxWidth())
            SoftkeyDividerLine(horizontal = true)
            SoftkeyCell(f2Label, Modifier.weight(1f).fillMaxWidth())
        }

        SoftkeyDividerLine(horizontal = false)

        // Center column: one tall Select button spanning both rows.
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .background(SoftkeySelectBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = centerLabel,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        SoftkeyDividerLine(horizontal = false)

        // Right column: F3 (top) and F4 (bottom).
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
            SoftkeyCell(f3Label, Modifier.weight(1f).fillMaxWidth())
            SoftkeyDividerLine(horizontal = true)
            SoftkeyCell(f4Label, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun SoftkeyCell(label: String, modifier: Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SoftkeyDividerLine(horizontal: Boolean) {
    Box(
        modifier = if (horizontal) {
            Modifier.fillMaxWidth().height(1.dp).background(SoftkeyDivider)
        } else {
            Modifier.fillMaxHeight().width(1.dp).background(SoftkeyDivider)
        },
    )
}

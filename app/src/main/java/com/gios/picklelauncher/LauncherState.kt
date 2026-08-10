package com.gios.picklelauncher

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.text.format.DateFormat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Calendar

/**
 * 12 grid slots, matching the keypad: keys 1-9, then *, 0, # in Kyocera's
 * order. The number labels on screen mirror the physical key that launches
 * them — exactly like the stock "standby & menu" app grid.
 *
 *   Slot  0 → key 1     Slot  4 → key 5     Slot  8 → key *
 *   Slot  1 → key 2     Slot  5 → key 6     Slot  9 → key 0
 *   Slot  2 → key 3     Slot  6 → key 7     Slot 10 → key #
 *   Slot  3 → key 4     Slot  7 → key 8     Slot 11 → (unused on stock)
 *
 * Wait — the stock Kyocera grid is 4 rows × 3 columns, read left-to-right,
 * top-to-bottom. Keys 1-9 map to the first 9 cells (3×3), then *, 0, # for
 * the last row. So:
 *
 *   Row 1: 1  2  3
 *   Row 2: 4  5  6
 *   Row 3: 7  8  9
 *   Row 4: *  0  #
 */
const val GRID_ROWS = 4
const val GRID_COLS = 3
const val NUM_SLOTS = GRID_ROWS * GRID_COLS // 12

/** The keypad label for each slot index (matches the physical key). */
val SLOT_LABELS = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")

/**
 * Maps an Android keycode (KEYCODE_1..KEYCODE_9, KEYCODE_0, KEYCODE_STAR,
 * KEYCODE_POUND) to the slot index it should activate. Returns -1 if the
 * key isn't a launcher shortcut.
 */
fun keyCodeToSlot(keyCode: Int): Int = when (keyCode) {
    android.view.KeyEvent.KEYCODE_1 -> 0
    android.view.KeyEvent.KEYCODE_2 -> 1
    android.view.KeyEvent.KEYCODE_3 -> 2
    android.view.KeyEvent.KEYCODE_4 -> 3
    android.view.KeyEvent.KEYCODE_5 -> 4
    android.view.KeyEvent.KEYCODE_6 -> 5
    android.view.KeyEvent.KEYCODE_7 -> 6
    android.view.KeyEvent.KEYCODE_8 -> 7
    android.view.KeyEvent.KEYCODE_9 -> 8
    android.view.KeyEvent.KEYCODE_STAR -> 9
    android.view.KeyEvent.KEYCODE_0 -> 10
    android.view.KeyEvent.KEYCODE_POUND -> 11
    else -> -1
}

private const val PREFS = "pickle_launcher"
private const val KEY_SLOTS = "slots"
private const val KEY_WALLPAPER = "wallpaper_color"
private const val SEP = "|"

data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
) {
    fun flatten(): String = "$packageName$SEP$activityName$SEP$label"
    companion object {
        fun unflatten(s: String): AppEntry? {
            val parts = s.split(SEP)
            if (parts.size < 3) return null
            return AppEntry(parts[0], parts[1], parts.drop(2).joinToString(SEP))
        }
    }
}

class LauncherState(private val context: Context) {

    /** The 12 grid slots. null = empty. */
    var slots: Array<AppEntry?> by mutableStateOf(loadSlots())
        private set

    /** Which slot the D-pad cursor is on (0..11). -1 = no focus. */
    var focusIndex: Int by mutableIntStateOf(0)

    /** True when in edit mode (long-press entered it, Back exits). */
    var editMode: Boolean by mutableStateOf(false)
        private set

    /** Which slot is currently being assigned ( picker is open for it). */
    var editingSlot: Int by mutableIntStateOf(-1)
        private set

    /** The app picker is showing when this is >= 0. */
    val pickerOpen: Boolean get() = editingSlot >= 0

    /** Current time string, updated by the activity. */
    var timeText: String by mutableStateOf(formatTime())
        private set

    /** Current date string. */
    var dateText: String by mutableStateOf(formatDate())
        private set

    /** Wallpaper color index (0 = default black, 1-N = preset colors). */
    var wallpaperColorIndex: Int by mutableIntStateOf(loadWallpaperColor())
        private set

    /** Available wallpaper colors. */
    val wallpaperColors = listOf(
        0xFF1A1A2E.toInt(), // midnight blue (default)
        0xFF0B3D0B.toInt(), // forest green
        0xFF1A1A1A.toInt(), // charcoal
        0xFF1B1B3A.toInt(), // deep indigo
        0xFF2D1B00.toInt(), // dark amber
        0xFF1A0D2E.toInt(), // dark purple
    )

    /** Returns all launchable apps on the device, sorted by name. */
    fun getInstalledApps(): List<AppEntry> {
        val pm = context.packageManager
        val main = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val results: List<ResolveInfo> = pm.queryIntentActivities(main, 0)
        return results.map { ri ->
            AppEntry(
                packageName = ri.activityInfo.packageName,
                activityName = ri.activityInfo.name,
                label = ri.loadLabel(pm).toString(),
            )
        }.sortedBy { it.label.lowercase() }
    }

    /** Launches the app in the given slot, if any. */
    fun launchApp(slot: Int) {
        val entry = slots.getOrNull(slot) ?: return
        runCatching {
            val intent = android.content.Intent().apply {
                setClassName(entry.packageName, entry.activityName)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /** Sets the slot to the given app and saves. */
    fun setSlot(slot: Int, entry: AppEntry?) {
        val newSlots = slots.copyOf()
        newSlots[slot] = entry
        slots = newSlots
        saveSlots()
    }

    /** Clears a slot (removes the app). */
    fun clearSlot(slot: Int) {
        setSlot(slot, null)
    }

    fun enterEditMode() {
        editMode = true
    }

    fun exitEditMode() {
        editMode = false
        editingSlot = -1
    }

    fun openPicker(slot: Int) {
        editingSlot = slot
    }

    fun closePicker() {
        editingSlot = -1
    }

    /** Called when the user selects an app in the picker. */
    fun assignApp(entry: AppEntry) {
        if (editingSlot >= 0) {
            setSlot(editingSlot, entry)
        }
        editingSlot = -1
    }

    fun cycleWallpaper() {
        wallpaperColorIndex = (wallpaperColorIndex + 1) % wallpaperColors.size
        saveWallpaperColor()
    }

    fun currentWallpaperColor(): Int = wallpaperColors[wallpaperColorIndex]

    fun updateClock() {
        timeText = formatTime()
        dateText = formatDate()
    }

    fun moveFocus(dx: Int, dy: Int) {
        val col = focusIndex % GRID_COLS
        val row = focusIndex / GRID_COLS
        val newCol = (col + dx).coerceIn(0, GRID_COLS - 1)
        val newRow = (row + dy).coerceIn(0, GRID_ROWS - 1)
        focusIndex = newRow * GRID_COLS + newCol
    }

    // --- Persistence ---

    private fun loadSlots(): Array<AppEntry?> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SLOTS, null) ?: return arrayOfNulls(NUM_SLOTS)
        val parts = raw.split("\n")
        return Array(NUM_SLOTS) { i ->
            if (i < parts.size) AppEntry.unflatten(parts[i]) else null
        }
    }

    private fun saveSlots() {
        val raw = slots.joinToString("\n") { it?.flatten() ?: "" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_SLOTS, raw).apply()
    }

    private fun loadWallpaperColor(): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_WALLPAPER, 0)

    private fun saveWallpaperColor() {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_WALLPAPER, wallpaperColorIndex).apply()
    }

    private fun formatTime(): String {
        val cal = Calendar.getInstance()
        val is24 = DateFormat.is24HourFormat(context)
        val h = if (is24) cal.get(Calendar.HOUR_OF_DAY) else {
            var h12 = cal.get(Calendar.HOUR)
            if (h12 == 0) h12 = 12
            h12
        }
        val m = cal.get(Calendar.MINUTE)
        return String.format("%02d:%02d", h, m)
    }

    private fun formatDate(): String {
        val cal = Calendar.getInstance()
        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        return "${days[cal.get(Calendar.DAY_OF_WEEK) - 1]} ${months[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.DAY_OF_MONTH)}"
    }
}

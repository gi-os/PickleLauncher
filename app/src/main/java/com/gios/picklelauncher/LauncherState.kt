package com.gios.picklelauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.format.DateFormat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Calendar

const val GRID_ROWS = 3
const val GRID_COLS = 3
const val SLOTS_PER_PAGE = GRID_ROWS * GRID_COLS // 9
const val MAX_PAGES = 5

val SLOT_LABELS = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9")

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
    else -> -1
}

enum class ScreenMode { HOME, PICKER, DRAWER }

private const val PREFS = "pickle_launcher"
private const val KEY_SLOTS = "slots"
private const val KEY_WALLPAPER_COLOR = "wallpaper_color"
private const val KEY_WALLPAPER_PHOTO = "wallpaper_photo"
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
            if (parts.size < 3 || parts[0].isEmpty()) return null
            return AppEntry(parts[0], parts[1], parts.drop(2).joinToString(SEP))
        }
    }
}

class LauncherState(private val context: Context) {

    var slots: List<AppEntry?> by mutableStateOf(loadSlots())
        private set

    var currentPage: Int by mutableIntStateOf(0)

    var focusIndex: Int by mutableIntStateOf(0)

    var pickerFocusIndex: Int by mutableIntStateOf(0)

    var editMode: Boolean by mutableStateOf(false)
        private set

    var editingSlot: Int by mutableIntStateOf(-1)
        private set

    var screenMode: ScreenMode by mutableStateOf(ScreenMode.HOME)

    var timeText: String by mutableStateOf(formatTime())
        private set

    var dateText: String by mutableStateOf(formatDate())
        private set

    var wallpaperColorIndex: Int by mutableIntStateOf(loadWallpaperColor())
        private set

    var wallpaperPhotoUri: String? by mutableStateOf(loadWallpaperPhoto())
        private set

    val wallpaperColors = listOf(
        0xFF1A1A2E.toInt(), 0xFF0B3D0B.toInt(), 0xFF1A1A1A.toInt(),
        0xFF1B1B3A.toInt(), 0xFF2D1B00.toInt(), 0xFF1A0D2E.toInt(),
    )

    private val iconCache = mutableMapOf<String, Drawable>()

    val pageCount: Int get() = ((slots.size + SLOTS_PER_PAGE - 1) / SLOTS_PER_PAGE).coerceAtLeast(1)

    fun getPageSlots(): Array<AppEntry?> {
        val start = currentPage * SLOTS_PER_PAGE
        return Array(SLOTS_PER_PAGE) { i -> slots.getOrNull(start + i) }
    }

    fun getAppIcon(packageName: String, activityName: String): Drawable? {
        val key = "$packageName/$activityName"
        iconCache[key]?.let { return it }
        return try {
            val pm = context.packageManager
            val info = pm.getActivityInfo(ComponentName(packageName, activityName), 0)
            info.loadIcon(pm).also { iconCache[key] = it }
        } catch (e: Exception) { null }
    }

    fun getInstalledApps(): List<AppEntry> {
        val pm = context.packageManager
        val main = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val results: List<ResolveInfo> = pm.queryIntentActivities(main, 0)
        return results.map { ri ->
            AppEntry(
                packageName = ri.activityInfo.packageName,
                activityName = ri.activityInfo.name,
                label = ri.loadLabel(pm).toString(),
            )
        }.sortedBy { it.label.lowercase() }
    }

    fun launchApp(slot: Int) {
        val pageSlot = getPageSlots()[slot] ?: return
        runCatching {
            val intent = Intent().apply {
                setClassName(pageSlot.packageName, pageSlot.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun launchAppDirect(entry: AppEntry) {
        runCatching {
            val intent = Intent().apply {
                setClassName(entry.packageName, entry.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun setSlot(slotInPage: Int, entry: AppEntry?) {
        val index = currentPage * SLOTS_PER_PAGE + slotInPage
        val newSlots = slots.toMutableList()
        while (newSlots.size <= index) newSlots.add(null)
        newSlots[index] = entry
        slots = newSlots
        saveSlots()
    }

    fun clearSlot(slotInPage: Int) {
        setSlot(slotInPage, null)
    }

    fun nextPage() {
        if (currentPage < MAX_PAGES - 1) {
            val needed = (currentPage + 2) * SLOTS_PER_PAGE
            if (slots.size < needed) {
                val newSlots = slots.toMutableList()
                while (newSlots.size < needed) newSlots.add(null)
                slots = newSlots
            }
            currentPage++
            focusIndex = 0
        }
    }

    fun prevPage() {
        if (currentPage > 0) {
            currentPage--
            focusIndex = 0
        }
    }

    fun enterEditMode() { editMode = true }
    fun exitEditMode() { editMode = false; editingSlot = -1 }

    fun openPicker(slotInPage: Int) {
        editingSlot = slotInPage
        pickerFocusIndex = 0
        screenMode = ScreenMode.PICKER
    }

    fun closePicker() {
        editingSlot = -1
        screenMode = ScreenMode.HOME
    }

    fun assignApp(entry: AppEntry) {
        if (editingSlot >= 0) setSlot(editingSlot, entry)
        editingSlot = -1
        screenMode = ScreenMode.HOME
    }

    fun openDrawer() {
        pickerFocusIndex = 0
        screenMode = ScreenMode.DRAWER
    }

    fun closeDrawer() {
        screenMode = ScreenMode.HOME
    }

    fun cycleWallpaperColor() {
        wallpaperColorIndex = (wallpaperColorIndex + 1) % wallpaperColors.size
        saveWallpaperColor()
    }

    fun setWallpaperPhoto(uriString: String) {
        wallpaperPhotoUri = uriString
        saveWallpaperPhoto()
    }

    fun clearWallpaperPhoto() {
        wallpaperPhotoUri = null
        saveWallpaperPhoto()
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

    fun movePickerFocus(dy: Int) {
        pickerFocusIndex = (pickerFocusIndex + dy).coerceAtLeast(0)
    }

    fun setPickerFocus(value: Int) {
        pickerFocusIndex = value
    }

    // --- Persistence ---

    private fun loadSlots(): List<AppEntry?> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SLOTS, null) ?: return MutableList(SLOTS_PER_PAGE) { null }
        val parts = raw.split("\n")
        return parts.map { AppEntry.unflatten(it) }
    }

    private fun saveSlots() {
        val raw = slots.joinToString("\n") { it?.flatten() ?: "" }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_SLOTS, raw).apply()
    }

    private fun loadWallpaperColor(): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_WALLPAPER_COLOR, 0)

    private fun saveWallpaperColor() {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_WALLPAPER_COLOR, wallpaperColorIndex).apply()
    }

    private fun loadWallpaperPhoto(): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_WALLPAPER_PHOTO, null)

    private fun saveWallpaperPhoto() {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (wallpaperPhotoUri != null) prefs.putString(KEY_WALLPAPER_PHOTO, wallpaperPhotoUri)
        else prefs.remove(KEY_WALLPAPER_PHOTO)
        prefs.apply()
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

// --- Utility functions ---

fun drawableToImageBitmap(drawable: Drawable): ImageBitmap {
    val width = drawable.intrinsicWidth.coerceAtLeast(48)
    val height = drawable.intrinsicHeight.coerceAtLeast(48)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(canvas)
    return bitmap.asImageBitmap()
}

fun loadPhotoBitmap(context: Context, uriString: String): Bitmap? {
    return try {
        val uri = Uri.parse(uriString)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > 960) sampleSize *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, opts)
        }
    } catch (e: Exception) { null }
}

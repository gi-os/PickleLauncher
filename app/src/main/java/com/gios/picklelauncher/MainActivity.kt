package com.gios.picklelauncher

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {

    private lateinit var state: LauncherState
    private val handler = Handler(Looper.getMainLooper())
    private var clockRunnable: Runnable? = null
    private var showGrid by mutableStateOf(false)

    // For picking a wallpaper photo from the gallery.
    private val pickPhoto = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Persist permission so we can read it after restart.
            try {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // Some providers don't support persistable — store the URI anyway,
                // it may work on re-open if the permission is re-granted.
            }
            state.setWallpaperPhoto(uri.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = LauncherState(applicationContext)
        hideSystemBars()

        setContent {
            MaterialTheme {
                val s = remember { state }
                when (s.screenMode) {
                    ScreenMode.PICKER -> AppListScreen(state = s, isDrawer = false)
                    ScreenMode.DRAWER -> AppListScreen(state = s, isDrawer = true)
                    ScreenMode.HOME -> HomeScreen(state = s, showGrid = showGrid)
                }
            }
        }

        clockRunnable = object : Runnable {
            override fun run() {
                state.updateClock()
                handler.postDelayed(this, 30_000)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        state.updateClock()
        handler.post(clockRunnable!!)
        showGrid = false
        state.screenMode = ScreenMode.HOME
        state.exitEditMode()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(clockRunnable!!)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event)
        val s = state

        // Route to the appropriate handler based on current screen.
        return when (s.screenMode) {
            ScreenMode.PICKER -> handleListKey(event, isDrawer = false)
            ScreenMode.DRAWER -> handleListKey(event, isDrawer = true)
            ScreenMode.HOME -> handleHomeKey(event)
        }
    }

    private fun handleHomeKey(event: KeyEvent): Boolean {
        val s = state

        return when (event.keyCode) {
            // --- D-pad navigation in the grid ---
            KeyEvent.KEYCODE_DPAD_LEFT -> { s.moveFocus(-1, 0); true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { s.moveFocus(1, 0); true }
            KeyEvent.KEYCODE_DPAD_UP -> { s.moveFocus(0, -1); true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { s.moveFocus(0, 1); true }

            // Center / Enter / F3: open focused app, or enter edit, or show grid
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_F3 -> {
                when {
                    !showGrid -> { showGrid = true; true }
                    s.editMode -> { s.openPicker(s.focusIndex); true }
                    else -> { handleActivate(s.focusIndex); true }
                }
            }

            // --- Number keys 1-9: launch the app in that slot ---
            KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3,
            KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6,
            KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9 -> {
                val slot = keyCodeToSlot(event.keyCode)
                if (!showGrid) {
                    showGrid = true
                    s.focusIndex = slot
                    true
                } else if (s.editMode) {
                    s.openPicker(slot)
                    true
                } else {
                    s.focusIndex = slot
                    handleActivate(slot)
                    true
                }
            }

            // --- * = previous page ---
            KeyEvent.KEYCODE_STAR -> {
                if (showGrid) { s.prevPage(); true } else { showGrid = true; true }
            }

            // --- # = next page ---
            KeyEvent.KEYCODE_POUND -> {
                if (showGrid) { s.nextPage(); true } else { showGrid = true; true }
            }

            // --- 0 = app drawer ---
            KeyEvent.KEYCODE_0 -> {
                if (showGrid) { s.openDrawer(); true } else { showGrid = true; s.openDrawer(); true }
            }

            // --- Softkeys ---
            // F1: Edit mode toggle (or clear slot in edit mode)
            KeyEvent.KEYCODE_F1 -> {
                when {
                    !showGrid -> { showGrid = true; true }
                    s.editMode -> { s.clearSlot(s.focusIndex); true }
                    else -> { s.enterEditMode(); true }
                }
            }

            // F2: Menu / toggle edit mode
            KeyEvent.KEYCODE_F2 -> {
                if (showGrid) {
                    if (s.editMode) s.exitEditMode() else s.enterEditMode()
                }
                true
            }

            // F3 already handled above (same as center).

            // F4: Wallpaper — cycle color, or if long-pressed, pick photo.
            // We handle short press here; long press via onKeyLongPress.
            KeyEvent.KEYCODE_F4 -> {
                s.cycleWallpaperColor()
                true
            }

            // --- Back ---
            KeyEvent.KEYCODE_BACK -> {
                when {
                    s.editMode -> { s.exitEditMode(); true }
                    showGrid -> { showGrid = false; true }
                    else -> false
                }
            }

            else -> super.dispatchKeyEvent(event)
        }
    }

    private fun handleListKey(event: KeyEvent, isDrawer: Boolean): Boolean {
        val s = state
        return when (event.keyCode) {
            // D-pad up/down: move one app at a time (no skipping).
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (s.pickerFocusIndex > 0) s.setPickerFocus(s.pickerFocusIndex - 1)
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                s.setPickerFocus(s.pickerFocusIndex + 1)
                true
            }

            // Center / Enter / F3: select the focused app.
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_F3 -> {
                val apps = s.getInstalledApps()
                val idx = s.pickerFocusIndex.coerceIn(0, apps.size - 1)
                if (apps.isNotEmpty()) {
                    if (isDrawer) {
                        s.launchAppDirect(apps[idx])
                        s.closeDrawer()
                    } else {
                        s.assignApp(apps[idx])
                    }
                }
                true
            }

            // F1: Clear (picker only) or Back (drawer).
            KeyEvent.KEYCODE_F1 -> {
                if (isDrawer) { s.closeDrawer(); true }
                else {
                    if (s.editingSlot >= 0) s.clearSlot(s.editingSlot)
                    s.closePicker()
                    true
                }
            }

            // F2: cancel/back.
            KeyEvent.KEYCODE_F2 -> {
                if (isDrawer) s.closeDrawer() else s.closePicker()
                true
            }

            // Back: close the list.
            KeyEvent.KEYCODE_BACK -> {
                if (isDrawer) s.closeDrawer() else s.closePicker()
                true
            }

            else -> false
        }
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (showGrid && state.screenMode == ScreenMode.HOME) {
                    state.enterEditMode()
                    return true
                }
            }
            // Long-press F4: pick a photo wallpaper.
            KeyEvent.KEYCODE_F4 -> {
                pickPhoto.launch("image/*")
                return true
            }
        }
        return super.onKeyLongPress(keyCode, event)
    }

    private fun handleActivate(slot: Int) {
        val pageSlots = state.getPageSlots()
        val entry = pageSlots.getOrNull(slot)
        if (entry != null) {
            state.launchApp(slot)
        } else if (!state.editMode) {
            state.enterEditMode()
            state.openPicker(slot)
        }
    }
}

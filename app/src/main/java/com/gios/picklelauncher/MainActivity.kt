package com.gios.picklelauncher

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var state: LauncherState
    private val handler = Handler(Looper.getMainLooper())
    private var clockRunnable: Runnable? = null

    /** True when the app grid is showing (vs just the clock). */
    private var showGrid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = LauncherState(applicationContext)
        hideSystemBars()

        setContent {
            MaterialTheme {
                val s = remember { state }
                if (s.pickerOpen) {
                    AppPickerScreen(state = s)
                } else {
                    HomeScreen(
                        state = s,
                        showGrid = showGrid,
                    )
                }
            }
        }

        // Clock tick — updates every 30 seconds (good enough for minutes).
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
        // When returning to the launcher (from an app), show the clock first.
        showGrid = false
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

        // In the app picker, route keys there.
        if (state.pickerOpen) {
            return handlePickerKey(event)
        }

        val s = state
        return when (event.keyCode) {
            // --- D-pad navigation ---
            KeyEvent.KEYCODE_DPAD_LEFT -> { s.moveFocus(-1, 0); true }
            KeyEvent.KEYCODE_DPAD_RIGHT -> { s.moveFocus(1, 0); true }
            KeyEvent.KEYCODE_DPAD_UP -> { s.moveFocus(0, -1); true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { s.moveFocus(0, 1); true }

            // Center / Enter / F3: activate the focused slot
            // (or toggle grid on/off from clock view)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_F3 -> {
                if (!showGrid) {
                    showGrid = true
                } else if (s.editMode) {
                    s.openPicker(s.focusIndex)
                } else {
                    handleActivate(s.focusIndex)
                }
                true
            }

            // --- Number keys: launch the app in that slot directly ---
            // Keys 1-9, *, 0, # map to slots 0-11.
            // This is the killer feature — press a number, app opens.
            // No need to navigate the D-pad first.
            KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3,
            KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6,
            KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9,
            KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_STAR, KeyEvent.KEYCODE_POUND -> {
                if (!showGrid) {
                    // From the clock screen, a number key opens the grid
                    // and focuses that slot.
                    showGrid = true
                    s.focusIndex = keyCodeToSlot(event.keyCode)
                    true
                } else if (s.editMode) {
                    // In edit mode, number key opens the picker for that slot.
                    s.openPicker(keyCodeToSlot(event.keyCode))
                    true
                } else {
                    // Normal mode: launch the app directly.
                    val slot = keyCodeToSlot(event.keyCode)
                    s.focusIndex = slot
                    handleActivate(slot)
                    true
                }
            }

            // --- Softkeys ---
            // F1: Menu — toggle the grid from clock view.
            //     In edit mode, F1 clears the focused slot.
            KeyEvent.KEYCODE_F1 -> {
                when {
                    s.editMode -> { s.clearSlot(s.focusIndex); true }
                    !showGrid -> { showGrid = true; true }
                    else -> { showGrid = false; true }
                }
            }

            // F2: toggle edit mode (long-press equivalent — we also support
            // actual long-press on center key below).
            KeyEvent.KEYCODE_F2 -> {
                if (showGrid) {
                    if (s.editMode) s.exitEditMode() else s.enterEditMode()
                }
                true
            }

            // F4: wallpaper cycle
            KeyEvent.KEYCODE_F4 -> {
                s.cycleWallpaper()
                true
            }

            // --- Back ---
            // From grid → clock. From clock → default behavior (exit).
            // In edit mode → exit edit mode first.
            KeyEvent.KEYCODE_BACK -> {
                when {
                    s.editMode -> { s.exitEditMode(); true }
                    showGrid -> { showGrid = false; true }
                    else -> false // let Android handle it (go home / exit)
                }
            }

            else -> super.dispatchKeyEvent(event)
        }
    }

    /**
     * Handles long-press on the center key to enter edit mode — the
     * "press and hold to select" the user asked for.
     */
    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            if (showGrid && !state.pickerOpen) {
                state.enterEditMode()
                return true
            }
        }
        return super.onKeyLongPress(keyCode, event)
    }

    private fun handleActivate(slot: Int) {
        val entry = state.slots.getOrNull(slot)
        if (entry != null) {
            state.launchApp(slot)
        } else if (!state.editMode) {
            // Empty slot, not in edit mode — enter edit mode to assign it.
            state.enterEditMode()
            state.openPicker(slot)
        }
    }

    private fun handlePickerKey(event: KeyEvent): Boolean {
        val s = state
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> { s.moveFocus(0, -1); true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { s.moveFocus(0, 1); true }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_F3 -> {
                // Select the focused app from the list.
                val apps = s.getInstalledApps()
                val idx = s.focusIndex.coerceIn(0, apps.size - 1)
                if (apps.isNotEmpty()) {
                    s.assignApp(apps[idx])
                }
                true
            }
            // F1 in picker = Clear (remove the app from the slot).
            KeyEvent.KEYCODE_F1 -> {
                val slot = s.editingSlot
                if (slot >= 0) s.clearSlot(slot)
                s.closePicker()
                true
            }
            // Back = cancel picker.
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_F2 -> {
                s.closePicker()
                true
            }
            else -> false
        }
    }
}

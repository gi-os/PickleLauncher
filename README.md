# Pickle Launcher

A custom home screen launcher for the Kyocera DIGNO KY-42C garaho — an Android 10 flip phone with no touchscreen, just a D-pad, numeric keypad, and four softkeys.

Built to replace the stock "Standby & Menu" launcher with the same familiar layout: a 4×3 grid of app shortcuts, each linked to a physical number key (1-9, *, 0, #). Press a number, the app opens.

## Features

- **Clock-first standby screen** — time and date at the top, just like the stock home screen. Press Menu or any number key to reveal the app grid.
- **4×3 grid with number shortcuts** — 12 slots matching the keypad. Press `3` to launch the app in slot 3, instantly.
- **Long-press to customize** — hold the center key (or press F2) to enter edit mode. Select any slot to assign, change, or clear the app.
- **Customizable wallpaper** — press F4 to cycle through preset background colors.
- **D-pad navigation** — full D-pad support for browsing the grid without touching a number key.
- **No touchscreen required** — every action works from the keypad and D-pad.

## Key Map

| Key | Action |
|-----|--------|
| 1-9, *, 0, # | Launch the app in that grid slot |
| D-pad | Move focus through the grid |
| Center / Enter / F3 | Activate focused slot, or toggle grid |
| F1 (Menu) | Toggle grid on/off from clock |
| F2 | Toggle edit mode |
| F4 | Cycle wallpaper |
| Back | Grid → Clock → Exit |
| Long-press Center | Enter edit mode |

## Build

Requires JDK 17 and Android SDK with API 34:

```
brew install --cask temurin@17
brew install --cask android-commandlinetools
```

Set `sdk.dir` in `local.properties`:
```
sdk.dir=/opt/homebrew/share/android-commandlinetools
```

Build:
```
./gradlew assembleDebug
```

Install via adb:
```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

After installing, press the Home key and select "Launcher" as your home app.

## Device

- Kyocera DIGNO KY-42C (Gratina)
- Android 10 / API 29
- 32-bit only (armeabi-v7a)
- No touchscreen — D-pad + numeric keypad + 4 softkeys (F1-F4)
- Touch Cruiser must be turned off (long-press the physical "III" key)

## Tech

- Kotlin + Jetpack Compose
- No external dependencies beyond AndroidX/Compose
- Material 3 theming
- SharedPreferences for slot persistence
- Same immersive mode pattern as PickleSolitaire to hide the OEM softkey bar

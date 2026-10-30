# Vezel Drive Launcher

Native Kotlin + Android Views launcher APK for Honda Vezel / AC8227L-class head units.

**Sprint 2:** vehicle personalization — six paint WebP placeholders, SharedPreferences persistence, responsive dashboard stage. This app is **not** registered as the default HOME screen.

| | |
|---|---|
| applicationId | `com.vezel.launcher` |
| minSdk | 27 |
| ABIs | `armeabi-v7a`, `arm64-v8a` |
| UI target | 1280×720 landscape |

## Build

Windows:

```bat
gradlew.bat :app:assembleDebug
```

macOS / Linux:

```bash
./gradlew :app:assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

Regenerate paint placeholders (optional):

```bat
python tools\generate_vezel_paint_placeholders.py
```

## USB install

```bat
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Launch from the **app drawer**. Do not set as default home this milestone.

## Vehicle paint

Settings → six swatches. Choice is stored in SharedPreferences and restored on the dashboard after restart. Asset notes: [docs/vehicle-assets.md](docs/vehicle-assets.md).

## Rollback

Uninstall the app:

```bat
adb uninstall com.vezel.launcher
```

Or use the device’s application settings. Because there is no `HOME` / `DEFAULT` intent filter, uninstall fully restores the previous launcher behavior.

## Logging

On start, logcat tag `VezelLauncher` prints API level, supported ABIs, package name, and heap memory.

```bat
adb logcat -s VezelLauncher
```

Record device results in [docs/compatibility-log.md](docs/compatibility-log.md).

## Out of scope (this milestone)

HOME intent filter, weather, MediaSession binding, GPS live speed, OEM package binding, Room, Flutter, MCU/camera.

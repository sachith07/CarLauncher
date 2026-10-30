# Vezel Drive Launcher

Native Kotlin + Android Views launcher APK for Honda Vezel / AC8227L-class head units.

**Sprint 0–1:** buildable dashboard scaffold. This app is **not** registered as the default HOME screen.

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

## USB install

```bat
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Launch from the **app drawer**. Do not set as default home this milestone.

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

HOME intent filter, weather, MediaSession binding, GPS speed, WebP vehicle assets, Room, Flutter, MCU/camera.

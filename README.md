# Vezel Drive Launcher

Native Kotlin + Android Views launcher APK for Honda Vezel / AC8227L-class head units.

**Dashboard GUI:** mockup-aligned dark landscape home — greeting/clock/theme pill, SPEED · NOW PLAYING · WEATHER cards, silver vehicle hero with orange horizon, 8-item dock. Live clock, GPS, media, weather, OEM launches; `drawer` / `homePilot` flavors kept.

| | |
|---|---|
| applicationId | `com.vezel.launcher` |
| minSdk | 27 |
| ABIs | `armeabi-v7a`, `arm64-v8a` |
| UI target | 1280×720 landscape |
| version | `0.7.0-dashboard-gui` (versionCode 7) |

## Build

Windows (default **drawer** flavor — not HOME):

```bat
gradlew.bat :app:assembleDrawerDebug
```

HOME-capable pilot (use only after recovery tests):

```bat
gradlew.bat :app:assembleHomePilotDebug
```

Release (debug keystore if `keystore.properties` is absent — see [docs/release-signing.md](docs/release-signing.md)):

```bat
gradlew.bat :app:assembleDrawerRelease
```

Debug APK example: `app/build/outputs/apk/drawer/debug/app-drawer-debug.apk`

## USB install

```bat
adb install -r app\build\outputs\apk\drawer\debug\app-drawer-debug.apk
```

Launch from the **app drawer** unless you intentionally installed `homePilot` and verified [docs/home-fallback.md](docs/home-fallback.md).

## Permissions

- `ACCESS_FINE_LOCATION` — GPS speed (m/s × 3.6) and weather location; denied → `--` / offline placeholders.
- `INTERNET` — Open-Meteo HTTPS weather (cached; never blocks startup).
- Notification listener (optional) — media session metadata; otherwise placeholder + deep-link to media package.

## OEM apps & camera

Settings → OEM apps: prefer factory radio / maps / phone / media packages. Dock uses those prefs, then known candidates, then a system picker/toast.

**Reverse camera:** keep the stock MCU/firmware camera path intact. This launcher does not bind or replace the rear-camera surface. After camera exit, Android should resume the previous foreground activity — verify on device after warm wake.

## Vehicle paint

Settings → six swatches. Asset notes: [docs/vehicle-assets.md](docs/vehicle-assets.md).

## Rollback

See [docs/home-fallback.md](docs/home-fallback.md). Uninstall:

```bat
adb uninstall com.vezel.launcher
```

## Docs

- [Dashboard GUI mockup](docs/dashboard-gui-mockup.png)
- [Acceptance checklist](docs/acceptance-checklist.md)
- [HOME fallback](docs/home-fallback.md)
- [Release signing](docs/release-signing.md)
- [Endurance](docs/endurance.md)
- [Compatibility log](docs/compatibility-log.md)

## Logging

```bat
adb logcat -s VezelLauncher
```

## Out of scope

MCU flashing, removing factory apps, claiming licensed Honda car renders, reverse-camera takeover, custom radio DSP.

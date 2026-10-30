# Endurance & memory notes

Target hardware: AC8227L-class, Mali-450 MP, API 27, 1280×720.

## Expectations (DEV-001)

- Prefer &lt; ~150 MiB launcher PSS as an **aspirational** benchmark, not a hard gate.
- Smooth ~30 fps idle dashboard subject to device measurement.
- Offline-first: no blocking network on cold start (weather fetches after resume).

## Mitigations in this build

- Vehicle WebP decode on a background executor + LRU cache; dark stage while loading.
- Clock via `TIME_TICK` (no 1 Hz UI churn).
- GPS updates ≥1 s; speed treated stale after ~8 s without a fix.
- Weather HTTPS off the UI thread; SharedPreferences cache with timestamp degrade.
- Media session callbacks only when notification listener is enabled.
- Window transitions use short fades; **Reduce animations** (default on) skips paint cross-fade.
- `reduceAnimations` preference persists.

## Soak procedure (device)

1. Install drawer debug APK; grant location if testing speed/weather.
2. Leave dashboard visible 30–60 minutes with intermittent map/audio switches.
3. `adb shell dumpsys meminfo com.vezel.launcher` at start and end; note PSS.
4. `adb logcat -s VezelLauncher` for crashes / media / weather warnings.
5. Engage reverse camera (stock MCU); confirm resume without ANR.
6. Force-stop and reopen; confirm paint/theme/OEM prefs restore.

Record results in [compatibility-log.md](compatibility-log.md).

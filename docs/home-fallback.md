# HOME pilot & safe recovery

## Flavors

| Flavor | APK task | HOME intent | When to use |
|--------|----------|-------------|-------------|
| `drawer` (default) | `assembleDrawerDebug` / `assembleDrawerRelease` | No — MAIN/LAUNCHER only | Everyday testing; **preferred until recovery is proven** |
| `homePilot` | `assembleHomePilotDebug` / `assembleHomePilotRelease` | Adds MAIN + HOME + DEFAULT | Gated pilot only |

This app **never** programmatically sets itself as the default home. Android may prompt when a HOME-capable APK is installed; choose **Just once** until rollback is verified.

## Before setting default home

1. Install **drawer** build first; confirm Radio / Maps / Phone / camera / uninstall path.
2. Install **homePilot** only when ready; if prompted for home app, use Just once.
3. Confirm Settings → **Home app settings** and **System settings** buttons open.
4. Confirm stock reverse camera still overlays and returns to this (or stock) UI.

## Rollback / reset

1. **Clear defaults** — Settings → Apps → Vezel Drive → Open by default → Clear defaults (wording varies by OEM).
2. **Home picker** — use in-app **Home app settings**, or Android Settings → Apps → Default apps → Home app → choose stock launcher.
3. **Uninstall** — `adb uninstall com.vezel.launcher` or head-unit application manager. With drawer flavor, uninstall alone restores prior behavior.

## Crash recovery

- `VezelApp` logs uncaught exceptions to logcat tag `VezelLauncher`.
- Paint, theme, OEM packages, and weather cache live in SharedPreferences and survive process death.
- `MainActivity` uses `singleTask` so HOME relaunches reuse one task when the pilot flavor is active.
- There is **no** auto-restart loop after a crash; reopen from drawer or HOME as configured.

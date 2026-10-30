# Compatibility log

Device findings for Vezel Drive Launcher (AC8227L / related head units).

Fill in rows after each install or smoke test.

| Date | Device / MCU | Android API | ABI | Flavor | Install result | Notes |
|------|--------------|-------------|-----|--------|----------------|-------|
| | | | | | | |

## Checklist (per device)

- [ ] APK installs via USB (`adb install -r`)
- [ ] Drawer flavor appears as MAIN/LAUNCHER only
- [ ] Startup logcat shows API, ABI, package, memory, flavor (`VezelLauncher`)
- [ ] Dashboard: clock/date, speed gauge, media + weather tiles, vehicle stage, dock
- [ ] Location permission: speed `--` when denied; km/h (incl. 0) with fix
- [ ] Dock Radio / Maps / Phone open preferred or fallback without crash
- [ ] Apps grid + Refresh; Back returns to dashboard
- [ ] Settings: paint, OEM spinners, theme, reduce animations, home/system recovery
- [ ] Weather online or timestamped cache / offline placeholder
- [ ] Stock reverse camera still works; resume after exit
- [ ] Uninstall / clear-defaults restores stock home (see home-fallback.md)

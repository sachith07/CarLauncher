# Compatibility log

Device findings for Vezel Drive Launcher (AC8227L / related head units).

Fill in rows after each install or smoke test. Do **not** set this app as default HOME during Sprint 0–1.

| Date | Device / MCU | Android API | ABI | Install result | Notes |
|------|--------------|-------------|-----|----------------|-------|
| | | | | | |

## Checklist (per device)

- [ ] APK installs via USB (`adb install -r`)
- [ ] Appears in app drawer (MAIN/LAUNCHER only)
- [ ] Startup logcat shows API, ABI, package, memory (`VezelLauncher` tag)
- [ ] Dashboard opens at landscape layout
- [ ] Dock Radio / Maps / Phone fallback toast or picker without crash
- [ ] Apps grid lists launcher activities; Back returns to dashboard
- [ ] Settings shows about + “not default home” note
- [ ] Uninstall restores prior state (rollback)

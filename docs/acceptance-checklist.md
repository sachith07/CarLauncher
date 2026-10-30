# Acceptance checklist — Sprints 3–6

Use while parked. Do **not** set default HOME until recovery steps below pass.

## Build / install

- [ ] `gradlew.bat :app:assembleDrawerDebug` succeeds
- [ ] Optional: `gradlew.bat :app:assembleHomePilotDebug` (HOME-capable APK)
- [ ] Optional: `gradlew.bat :app:assembleDrawerRelease` (debug keystore OK without `keystore.properties`)
- [ ] USB/sideload install; app appears in drawer (`com.vezel.launcher`)

## Sprint 3 — Live widgets

- [ ] Clock/date update after wake and across midnight / timezone change
- [ ] GPS speed: permission prompt; with fix shows km/h including `0`; denied/stale shows `--` / unavailable
- [ ] Apps grid lists launchable activities; Refresh works; Back returns to dashboard
- [ ] Media tile: placeholder without notification access; with access shows session or idle; tap opens player

## Sprint 4 — OEM compatibility

- [ ] Settings → OEM apps: Radio / Maps / Phone / Media spinners persist
- [ ] Dock Radio / Maps / Phone open preferred or fallback packages without crash
- [ ] Stock reverse camera (MCU) still works; launcher resumes after camera exit (device test)
- [ ] Warm wake: dashboard re-binds clock / speed / media

## Sprint 5 — Launcher pilot

- [ ] **drawer** flavor: no HOME filter; uninstall restores prior launcher
- [ ] **homePilot** flavor only: HOME+DEFAULT present; choose “Just once” first
- [ ] Settings → Home app settings / System settings open
- [ ] Documented rollback works (clear defaults or uninstall) — see [home-fallback.md](home-fallback.md)
- [ ] After process kill, color/theme/OEM prefs restore

## Sprint 6 — Polish

- [ ] Weather: HTTPS fetch or timestamped cache / offline placeholder (no startup block)
- [ ] Theme dark / day / auto persists; reduce-animations preference works
- [ ] ~30–60 min soak: no crash loop / obvious unbounded growth (see [endurance.md](endurance.md))
- [ ] Version name `0.6.0-sprints3-6` on About screen

## Explicit non-goals

- No MCU flashing, no factory app removal, no licensed Honda renders claimed.

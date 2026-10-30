# Vehicle paint assets

## Status (Sprint 2)

**Placeholder** original silhouettes — not licensed Honda photography or OEM CAD renders.

| File | Paint key | Notes |
|---|---|---|
| `app/src/main/res/drawable-nodpi/vezel_blue_gray.webp` | `blue_gray` | Default |
| `app/src/main/res/drawable-nodpi/vezel_white.webp` | `white` | |
| `app/src/main/res/drawable-nodpi/vezel_red.webp` | `red` | |
| `app/src/main/res/drawable-nodpi/vezel_black.webp` | `black` | |
| `app/src/main/res/drawable-nodpi/vezel_silver.webp` | `silver` | |
| `app/src/main/res/drawable-nodpi/vezel_blue.webp` | `blue` | |

Each WebP is **720×360** RGBA with alpha, labeled “PLACEHOLDER” in-image, sized for the dashboard stage (not full-screen 1280×720 layers).

## License

Original procedural art generated for this project (`tools/generate_vezel_paint_placeholders.py`). Free to use within this repository. Replace with approved, accurately matched Vezel model renders when licensing is confirmed (see PRD risk: model year / asset rights).

## Runtime

- `PaintColorRepository` / `PaintColor` map keys → drawable resources.
- `VehicleAssetRepository` decodes off the UI thread into an `LruCache`, preloads all six, and never clears the previous bitmap until the next decode completes (no white flash).
- Preference key `vehicle_color` in `LauncherPreferences` (migrates Sprint 1 `slot_*` keys).

# TT Client — NeoForge 26.2

Client-side utility mod with a sharp ClickGUI, HUD, and working helpers only.

**Version 1.9.0**

## What changed in 1.9.0
- **Right Shift was a blank smear** because ClickGUI read `TTClient.modules`, which was never assigned. It now uses `TTClientClient.modules`.
- Empty Combat / stub categories are skipped so you only see panels that have modules.
- Dim overlay is optional (`ClickGUI → Dim`). Vanilla menu blur stays disabled.
- New working modules: **AutoTool**, **SpeedHud**, **WorldClock**.
- Removed empty stubs from the shipped module list.

## How to use
1. Java 25 + NeoForge 26.2
2. `./gradlew build` → `build/libs/ttclient-1.9.0.jar`
3. Put the jar in `mods/`
4. In-game: **Right Shift** opens ClickGUI
5. Left click toggle · Right click settings · Middle click bind · Type to search

Config: `config/ttclient/config.properties`

## License
MIT

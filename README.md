# TT Client — NeoForge 26.2

Client-side utility mod with a ClickGUI, HUD, movement/player helpers, and FPS-focused options.

**Version 1.2.0**

## What changed in 1.2.0
- **Fixed Right Shift blur**: ClickGUI no longer uses vanilla Screen menu blur. Overlay is a flat dim only.
- Removed unused ClickGUI Particles setting. Blur setting is now darker overlay, default off.
- Module list matches real source files. Phantom registrations that could not compile were removed.
- New modules: **AutoRespawn**, **CoordCopy**.
- **AntiAFK** jumps on an interval.
- **Sprint** uses movement keys instead of fragile input-vector fields.
- Config saves when the ClickGUI closes.

## How to use
1. Java 25 + NeoForge 26.2
2. `./gradlew build` → `build/libs/ttclient-1.2.0.jar`
3. Put the jar in `mods/`
4. In-game: **Right Shift** opens ClickGUI

Config: `config/ttclient/config.properties`

## License
MIT

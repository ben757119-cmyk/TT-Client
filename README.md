# TT Client — NeoForge 26.2

Client-side utility mod with a sharp ClickGUI, HUD, and working helpers only.

**Version 1.10.0**

## What changed in 1.10.0
- **Right Shift blur:** Dim overlay is **off by default**. Vanilla menu blur stays disabled (`isInGameUi`, empty `extractBlurredBackground`, no `super.extractRenderState`). Enable `ClickGUI → Dim` if you want a solid shade.
- New working modules: **Compass**, **BreakProgress**, **DurabilityAlert**, **HungerInfo**.
- Empty Combat category removed from the enum.
- HUD already shows nether coords, biome, ping, armor.

## How to use
1. Java 25 + NeoForge 26.2
2. `./gradlew build` → `build/libs/ttclient-1.10.0.jar`
3. Put the jar in `mods/`
4. In-game: **Right Shift** opens ClickGUI
5. Left click toggle · Right click settings · Middle click bind · Type to search

Config: `config/ttclient/config.properties`

## License
MIT

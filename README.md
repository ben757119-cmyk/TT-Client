# TT Client — NeoForge 26.2

Client-side utility mod with a sharp ClickGUI, HUD, movement/player helpers.

**Version 1.8.0**

## What changed in 1.8.0
- Right Shift ClickGUI: no vanilla menu blur (`isInGameUi`, empty `extractBlurredBackground`, no `super.extractRenderState`). Blur toggle removed; optional **Dim** overlay only.
- HUD potions actually draw. Search + binds unchanged.
- New working modules: **LightLevel**, **TargetInfo**, **ToggleSneak**, **SessionTimer**.
- **FPSBoost** only does what it can without mixins: unfocused FPS cap.
- Removed stub modules that did nothing and broke the compile (ESP/XRay/NoRender/KillAura/Reach/Fly/Nuker/Scaffold/Timer and other empty registrations).
- Fixed `ModuleManager.onRender2D` and `DeathCoords`/`AutoJump` calling a nonexistent `mc()`.

## How to use
1. Java 25 + NeoForge 26.2
2. `./gradlew build` → `build/libs/ttclient-1.8.0.jar`
3. Put the jar in `mods/`
4. In-game: **Right Shift** opens ClickGUI
5. Left click toggle · Right click settings · Middle click bind · Type to search

Config: `config/ttclient/config.properties`

## License
MIT

# TT Client — NeoForge 26.2

Client-side utility mod with a ClickGUI, HUD, movement/player helpers, and FPS-focused options.

**Version 1.1.0**

## What changed in 1.1.0
- Modules initialize on the **client only**. Dedicated servers can load the jar without touching `Minecraft.getInstance()`.
- Config now saves when you close the ClickGUI, toggle a bind, or disconnect.
- ClickGUI **search**: type while the GUI is open, Backspace to clear characters.
- HUD: nether/overworld converted coords, biome name, ping.
- New modules: **Keystrokes**, **AutoFish**, **Waypoints** (death point), **ChatTimestamps**.
- **AutoEat** actually uses the held food item when hunger is low.
- Removed gimmick stubs that did nothing useful: ChinaHat, PortalGodMode, Spammer, DiscordRPC, Announcer, FakePlayer.

## How to use
1. Java 25 + NeoForge 26.2
2. `./gradlew build` → `build/libs/ttclient-1.1.0.jar`
3. Put the jar in `mods/`
4. In-game: **Right Shift** opens ClickGUI
5. Left click toggle · Right click settings · Middle click bind · Type to search

## Default binds
- Right Shift — ClickGUI
- C — Zoom (hold), if left at default

Config: `config/ttclient/config.properties`

## License
MIT

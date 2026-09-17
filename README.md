# TT Client — NeoForge 26.2

Client-side utility mod with a ClickGUI, HUD, movement/player helpers, and FPS-focused options.

**Version 1.7.0**

## What changed in 1.7.0
- **Right Shift is sharp again**: ClickGUI no longer calls `super.extractRenderState` (that reapplied vanilla 26.2 menu blur). `extractBlurredBackground` stays a no-op. Overlay is a flat dim only.
- ClickGUI search is back (type while the GUI is open).
- New working modules: **AutoJump**, **DeathCoords** (chat + clipboard).
- Removed stub / empty modules that did nothing: ChinaHat, PopChams, Trajectories, Radar, Search, NoFog, FakePlayer, LiquidPlace, AutoTorch, NewChunks, StashFinder, AutoCrystal, Surround, Offhand, Hitboxes, MiddleClickFriend, NoRotate, ChatSuffix, Spammer, DiscordRPC, PortalGodMode, Announcer, NameProtect, AutoPotion, Freecam.

## How to use
1. Java 25 + NeoForge 26.2
2. `./gradlew build` → `build/libs/ttclient-1.7.0.jar`
3. Put the jar in `mods/`
4. In-game: **Right Shift** opens ClickGUI
5. Left click toggle · Right click settings · Middle click bind · Type to search

## Default binds
- Right Shift — ClickGUI

Config: `config/ttclient/config.properties`

## License
MIT

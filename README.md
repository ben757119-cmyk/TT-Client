# TT Client — NeoForge 26.2

Client-side utility mod with a sharp ClickGUI, HUD, and working helpers only.

**Version 1.11.0**

## What changed in 1.11.0
- **Right Shift smear fixed.** Override extractBackground, extractBlurredBackground, extractTransparentBackground, and extractMenuBackground. No blurBeforeThisStratum. SolidDim off by default. Previous build drew the vanilla fillGradient sheet even with blur off.
- Accent color setting tints panels.
- Unused ClickGUI Style modes removed.
- New modules: **DayCounter**, **WeatherHud**, **MemoryInfo**.

## How to use
1. Java 25 + NeoForge 26.2
2. `./gradlew build` → `build/libs/ttclient-1.11.0.jar`
3. Put the jar in `mods/`
4. In-game: **Right Shift** opens ClickGUI

Config: `config/ttclient/config.properties`

## License
MIT

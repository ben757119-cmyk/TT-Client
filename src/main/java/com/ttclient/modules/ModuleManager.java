package com.ttclient.modules;

import com.ttclient.modules.client.*;
import com.ttclient.modules.combat.*;
import com.ttclient.modules.misc.*;
import com.ttclient.modules.movement.*;
import com.ttclient.modules.player.*;
import com.ttclient.modules.render.*;
import com.ttclient.modules.world.*;
import com.ttclient.modules.basefind.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public void init() {
        register(new ClickGUIModule());
        register(new HUD());
        register(new ArrayListMod());
        register(new Notifications());
        register(new Keystrokes());
        register(new CustomMainMenu());
        register(new StreamerMode());
        register(new DiscordRPC());

        register(new KillAura());
        register(new AimAssist());
        register(new TriggerBot());
        register(new AutoClicker());
        register(new Criticals());
        register(new Reach());
        register(new Hitboxes());
        register(new WTap());
        register(new AutoCrystal());
        register(new AutoTotem());
        register(new Offhand());
        register(new AutoTrap());
        register(new Surround());
        register(new HoleFill());
        register(new AutoXP());
        register(new AutoArmorSwap());
        register(new AnchorAura());
        register(new OneClickAnchors());
        register(new Rage());
        register(new Shredder());
        register(new Destroyer());
        register(new MaceHack());
        register(new MaceAura());
        register(new MaceSwap());
        register(new WindChargeAssist());
        register(new SpearKiller());

        register(new Sprint());
        register(new AutoWalk());
        register(new AutoJump());
        register(new ToggleSneak());
        register(new Fly());
        register(new WarpNearest());
        register(new AntiFlightKick());
        register(new Speed());
        register(new Velocity());
        register(new Step());
        register(new Strafe());
        register(new Jesus());
        register(new NoSlow());
        register(new NoClip());
        register(new Spider());
        register(new LongJump());
        register(new AirJump());
        register(new Parkour());
        register(new SafeWalk());
        register(new InventoryMove());
        register(new BoatFly());
        register(new ElytraFly());

        register(new AutoEat());
        register(new AutoFish());
        register(new AutoRespawn());
        register(new AutoTool());
        register(new Freecam());
        register(new AutoMace());
        register(new com.ttclient.modules.player.AutoTotem());
        register(new AutoArmor());
        register(new AutoGapple());
        register(new AutoPotion());
        register(new AutoMine());
        register(new AutoSprintReset());
        register(new FastBreak());
        register(new FastPlace());
        register(new NoBreakDelay());
        register(new NoFall());
        register(new ChestStealer());
        register(new ChestRelay());
        register(new InventoryCleaner());
        register(new MiddleClickPearl());
        register(new AttributeSwapper());
        register(new AntiHunger());
        register(new DurabilityAlert());
        register(new HungerInfo());

        register(new Fullbright());
        register(new Zoom());
        register(new LightLevel());
        register(new TargetInfo());
        register(new SpeedHud());
        register(new Compass());
        register(new BreakProgress());
        register(new DayCounter());
        register(new WeatherHud());
        register(new ESP());
        register(new Tracers());
        register(new NameTags());
        register(new StorageESP());
        register(new BlockESP());
        register(new HoleESP());
        register(new Search());
        register(new ChinaHat());
        register(new Breadcrumbs());
        register(new BlockHighlight());
        register(new Crosshair());
        register(new CustomSky());
        register(new Ambience());
        register(new Brightness());

        register(new AntiAFK());
        register(new ChatTimestamps());
        register(new CoordCopy());
        register(new DeathCoords());
        register(new FPSBoost());
        register(new MemoryInfo());
        register(new SessionTimer());
        register(new WorldClock());
        register(new AutoChatGame());
        register(new AutoGG());
        register(new AutoReconnect());
        register(new ChatSuffix());
        register(new Announcer());

        register(new Waypoints());
        register(new Nuker());
        register(new Scaffold());
        register(new AutoFarm());
        register(new AutoTorch());
        register(new Timer());

        register(new BaseScanner());
        register(new BasefindHits());
        register(new BeaconFinder());
        register(new ChestStashFinder());
        register(new CropFarmFinder());
        register(new PortalFinder());
        register(new SpawnFinder());
        register(new StrongholdFinder());
    }

    private void register(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getByCategory(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module m : modules) {
            if (clazz.isInstance(m)) return (T) m;
        }
        return null;
    }

    public Module getByName(String name) {
        if (name == null) return null;
        String n = name.toLowerCase(Locale.ROOT);
        for (Module m : modules) {
            if (m.getName().equalsIgnoreCase(name) || m.getName().toLowerCase(Locale.ROOT).replace(" ", "").equals(n)) {
                return m;
            }
        }
        return null;
    }

    public void onTick() {
        for (Module m : modules) {
            if (m.isEnabled()) {
                try { m.onTick(); } catch (Throwable ignored) {}
            }
        }
    }

    public void onRender2D(GuiGraphicsExtractor graphics, float partialTick) {
        for (Module m : modules) {
            if (m.isEnabled()) {
                try { m.onRender2D(graphics, partialTick); } catch (Throwable ignored) {}
            }
        }
    }

    /** Disable modules that opted into DisableOnDeath. */
    public void disableModulesOnDeath() {
        for (Module m : modules) {
            if (m.isEnabled() && m.shouldDisableOnDeath()) {
                try { m.setEnabled(false); } catch (Throwable ignored) {}
            }
        }
    }

    /** Disable modules that opted into DisableOnServer (leave / change server). */
    public void disableModulesOnServerChange() {
        for (Module m : modules) {
            if (m.isEnabled() && m.shouldDisableOnServerChange()) {
                try { m.setEnabled(false); } catch (Throwable ignored) {}
            }
        }
    }
}

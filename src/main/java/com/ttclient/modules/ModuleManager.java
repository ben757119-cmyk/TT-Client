package com.ttclient.modules;

import com.ttclient.modules.basefind.BaseScanner;
import com.ttclient.modules.basefind.BeaconFinder;
import com.ttclient.modules.basefind.ChestStashFinder;
import com.ttclient.modules.basefind.CropFarmFinder;
import com.ttclient.modules.basefind.PortalRoomFinder;
import com.ttclient.modules.client.ArrayListMod;
import com.ttclient.modules.client.ClickGUIModule;
import com.ttclient.modules.client.CustomMainMenu;
import com.ttclient.modules.client.HUD;
import com.ttclient.modules.client.Keystrokes;
import com.ttclient.modules.client.Notifications;
import com.ttclient.modules.client.StreamerMode;
import com.ttclient.modules.client.TargetHUD;
import com.ttclient.modules.combat.*;
import com.ttclient.modules.misc.*;
import com.ttclient.modules.movement.*;
import com.ttclient.modules.player.*;
import com.ttclient.modules.render.*;
import com.ttclient.modules.world.Nuker;
import com.ttclient.modules.world.Scaffold;
import com.ttclient.modules.world.Timer;
import com.ttclient.modules.world.Waypoints;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public void init() {
        // Client
        register(new ClickGUIModule());
        register(new HUD());
        register(new ArrayListMod());
        register(new Notifications());
        register(new Keystrokes());
        register(new TargetHUD());
        register(new CustomMainMenu());
        register(new StreamerMode());

        // Combat
        register(new KillAura());
        register(new AimAssist());
        register(new AutoClicker());
        register(new AutoCrystal());
        register(new AutoTotem());
        register(new AutoTrap());
        register(new AutoXP());
        register(new Criticals());
        register(new Destroyer());
        register(new Hitboxes());
        register(new HoleFill());
        register(new Offhand());
        register(new Reach());
        register(new Surround());
        register(new TriggerBot());
        register(new WTap());
        register(new AutoArmorSwap());
        register(new AnchorAura());
        register(new OneClickAnchors());
        register(new Rage());
        register(new MaceHack());
        register(new MaceAura());
        register(new MaceSwap());
        register(new WindChargeAssist());
        register(new SpearKiller());
        register(new Shredder());

        // Movement
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

        // Player
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

        // Render
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

        // Misc
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

        // World
        register(new Waypoints());
        register(new Nuker());
        register(new Scaffold());
        register(new AutoFarm());
        register(new AutoTorch());
        register(new Timer());

        // Basefind
        register(new BaseScanner());
        register(new BeaconFinder());
        register(new ChestStashFinder());
        register(new CropFarmFinder());
        register(new PortalRoomFinder());
    }

    private void register(Module m) {
        modules.add(m);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> getByCategory(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).collect(Collectors.toList());
    }

    public List<Module> search(String query) {
        if (query == null || query.isBlank()) return List.copyOf(modules);
        String q = query.toLowerCase(Locale.ROOT);
        return modules.stream()
                .filter(m -> m.getName().toLowerCase(Locale.ROOT).contains(q)
                        || m.getDescription().toLowerCase(Locale.ROOT).contains(q)
                        || m.getCategory().name.toLowerCase(Locale.ROOT).contains(q))
                .collect(Collectors.toList());
    }

    public Module getModule(String name) {
        return modules.stream().filter(m -> m.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public <T extends Module> T getModule(Class<T> clazz) {
        return modules.stream().filter(clazz::isInstance).map(clazz::cast).findFirst().orElse(null);
    }

    public void onTick() {
        for (Module m : modules) {
            if (m.isEnabled()) {
                try { m.onTick(); } catch (Throwable ignored) {}
            }
        }
    }

    public void onRender2D(GuiGraphicsExtractor graphics, float partialTick) {
        if (StreamerMode.shouldHideLocalUi()) return;
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

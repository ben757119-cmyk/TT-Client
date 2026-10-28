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
        register(new PackTweaks());
        register(new ToolStyles());
        register(new Music());
        register(new ClickGUIModule());
        register(new SeizureMode());
        register(new StreamerMode());
        register(new HUD());
        register(new Notifications());
        register(new Keystrokes());
        register(new ArrayListMod());

        register(new Destroyer());
        register(new KillAura());
        register(new AimAssist());
        register(new TriggerBot());
        register(new AutoClicker());
        register(new Criticals());
        register(new Reach());
        register(new Rage());
        register(new Shredder());
        register(new Hitboxes());
        register(new AutoCrystal());
        register(new Surround());
        register(new HoleFill());
        register(new AutoTrap());
        register(new AnchorAura());
        register(new OneClickAnchors());
        register(new Offhand());
        register(new AutoXP());
        register(new AutoArmorSwap());
        register(new WTap());

        register(new MaceHack());
        register(new MaceAura());
        register(new MaceSwap());
        register(new WindChargeAssist());
        register(new SpearKiller());
        register(new ImStorkingIt());

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
        register(new EntitySpeed());
        register(new BoatFly());

        register(new AutoTool());
        register(new AutoEat());
        register(new AutoFish());
        register(new AutoRespawn());
        register(new AutoArmor());
        register(new AutoTotem());
        register(new ChestStealer());
        register(new InventoryCleaner());
        register(new FastPlace());
        register(new FastBreak());
        register(new NoBreakDelay());
        register(new InstantRebreak());
        register(new NoFall());
        register(new Freecam());
        register(new MiddleClickPearl());
        register(new AutoGapple());
        register(new AutoPotion());
        register(new AutoMace());
        register(new AutoMine());
        register(new AutoSprintReset());
        register(new AttributeSwapper());
        register(new ChestRelay());
        register(new DurabilityAlert());
        register(new HungerInfo());

        register(new Fullbright());
        register(new Zoom());
        register(new XRay());
        register(new ESP());
        register(new StorageESP());
        register(new HoleESP());
        register(new NameTags());
        register(new Tracers());
        register(new ChinaHat());
        register(new BlockESP());
        register(new LightLevel());
        register(new TargetInfo());
        register(new SpeedHud());
        register(new Compass());
        register(new BreakProgress());
        register(new DayCounter());
        register(new WeatherHud());
        register(new NoRender());
        register(new ViewModel());

        register(new Nuker());
        register(new Scaffold());
        register(new Waypoints());
        register(new Timer());
        register(new FakePlayer());

        register(new AntiAFK());
        register(new ChatTimestamps());
        register(new CoordCopy());
        register(new DeathCoords());
        register(new SessionTimer());
        register(new FPSBoost());
        register(new MemoryInfo());
        register(new WorldClock());
        register(new AutoReconnect());
        register(new DiscordRPC());

        register(new BaseScanner());
        register(new SpawnerFinder());
        register(new ChestStashFinder());
        register(new PortalFinder());
        register(new BeaconFinder());
        register(new HopperFinder());
        register(new ShulkerFinder());
        register(new GeodeFinder());
        register(new LecternHallFinder());
        register(new KelpFinder());
    }

    private void register(Module module) { modules.add(module); }
    public List<Module> getModules() { return modules; }
    public List<Module> getModulesByCategory(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).collect(Collectors.toList());
    }
    public List<Module> getByCategory(Category category) {
        return getModulesByCategory(category);
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
        for (Module m : modules) if (m.isEnabled()) m.onTick();
    }
    public void onRender2D(GuiGraphicsExtractor graphics, float partialTick) {
        for (Module m : modules) if (m.isEnabled()) {
            try { m.onRender2D(graphics, partialTick); } catch (Throwable ignored) {}
        }
    }
}

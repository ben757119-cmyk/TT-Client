package com.ttclient.modules;

import com.ttclient.modules.client.*;
import com.ttclient.modules.combat.*;
import com.ttclient.modules.misc.*;
import com.ttclient.modules.movement.*;
import com.ttclient.modules.player.*;
import com.ttclient.modules.render.*;
import com.ttclient.modules.world.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Registers every module class shipped in the mod.
 * Note: combat.AutoTotem is a legacy stub — only player.AutoTotem is registered.
 */
public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public void init() {
        // Client
        register(new ClickGUIModule());
        register(new HUD());
        register(new Notifications());
        register(new Keystrokes());
        register(new ArrayListMod());

        // Combat
        register(new KillAura());
        register(new AimAssist());
        register(new TriggerBot());
        register(new AutoClicker());
        register(new Criticals());
        register(new Reach());
        register(new Hitboxes());
        register(new AutoCrystal());
        register(new Surround());
        register(new Offhand());
        register(new AutoArmorSwap());
        register(new WTap());

        // Movement
        register(new Sprint());
        register(new AutoWalk());
        register(new AutoJump());
        register(new ToggleSneak());
        register(new Fly());
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
        register(new AutoTotem()); // player package
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
        register(new HoleESP());
        register(new Search());
        register(new NoFog());
        register(new NoRender());
        register(new Crosshair());
        register(new Breadcrumbs());
        register(new BlockHighlight());
        register(new ChinaHat());
        register(new CustomSky());
        register(new Radar());
        register(new Trajectories());
        register(new ViewModel());
        register(new PopChams());
        register(new Brightness());

        // World
        register(new Waypoints());
        register(new TrialChamberFinder());
        register(new StashFinder());
        register(new Scaffold());
        register(new Nuker());
        register(new Timer());
        register(new XRay());
        register(new AutoFarm());
        register(new AutoTorch());
        register(new FakePlayer());
        register(new LiquidPlace());
        register(new NewChunks());

        // Misc
        register(new FPSBoost());
        register(new MemoryInfo());
        register(new WorldClock());
        register(new AntiAFK());
        register(new ChatTimestamps());
        register(new AutoChatGame());
        register(new CoordCopy());
        register(new DeathCoords());
        register(new SessionTimer());
        register(new AutoGG());
        register(new Announcer());
        register(new Spammer());
        register(new ChatSuffix());
        register(new NameProtect());
        register(new DiscordRPC());
        register(new AutoReconnect());
        register(new MiddleClickFriend());
        register(new NoRotate());
        register(new PortalGodMode());
        register(new SoundVolume());
        register(new Ambience());
        register(new CustomMainMenu());
    }

    private void register(Module module) { modules.add(module); }
    public List<Module> getModules() { return modules; }
    public List<Module> getModulesByCategory(Category category) {
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
        for (Module m : modules) if (m.isEnabled()) m.onTick();
    }
    public void onRender2D(GuiGraphicsExtractor graphics, float partialTick) {
        for (Module m : modules) if (m.isEnabled()) m.onRender2D(graphics, partialTick);
    }
}

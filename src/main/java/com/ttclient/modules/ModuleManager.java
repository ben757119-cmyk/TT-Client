package com.ttclient.modules;

import com.ttclient.modules.client.*;
import com.ttclient.modules.combat.*;
import com.ttclient.modules.misc.*;
import com.ttclient.modules.movement.*;
import com.ttclient.modules.player.*;
import com.ttclient.modules.render.*;
import com.ttclient.modules.world.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public void init() {
        register(new ClickGUIModule());
        register(new HUD());
        register(new CustomMainMenu());
        register(new Notifications());
        register(new Keystrokes());

        register(new Fullbright());
        register(new Zoom());
        register(new NameTags());
        register(new Tracers());
        register(new ESP());
        register(new StorageESP());
        register(new HoleESP());
        register(new ViewModel());
        register(new NoRender());
        register(new Ambience());
        register(new CustomSky());
        register(new Breadcrumbs());
        register(new BlockHighlight());
        register(new PopChams());
        register(new Search());

        register(new Sprint());
        register(new AutoWalk());
        register(new InventoryMove());
        register(new NoSlow());
        register(new Jesus());
        register(new Step());
        register(new Speed());
        register(new Velocity());
        register(new Fly());
        register(new NoClip());
        register(new ElytraFly());
        register(new LongJump());

        register(new AutoTool());
        register(new AutoArmor());
        register(new FastPlace());
        register(new FastBreak());
        register(new NoFall());
        register(new AutoEat());
        register(new InventoryCleaner());
        register(new MiddleClickPearl());
        register(new AutoFish());
        register(new ChestStealer());
        register(new AutoGapple());
        register(new Freecam());

        register(new XRay());
        register(new Nuker());
        register(new Scaffold());
        register(new Timer());
        register(new Waypoints());
        register(new AutoFarm());
        register(new LiquidPlace());

        register(new KillAura());
        register(new Criticals());
        register(new AutoClicker());
        register(new AimAssist());
        register(new Reach());
        register(new AutoTotem());
        register(new AutoCrystal());
        register(new Surround());
        register(new Offhand());

        register(new FPSBoost());
        register(new AntiAFK());
        register(new AutoReconnect());
        register(new MiddleClickFriend());
        register(new NoRotate());
        register(new AutoRespawn());
        register(new ChatSuffix());
        register(new ChatTimestamps());
    }

    private void register(Module module) {
        modules.add(module);
    }

    public List<Module> getModules() {
        return modules;
    }

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
        for (Module m : modules) {
            if (m.isEnabled()) m.onTick();
        }
    }

    public void onRender2D(net.minecraft.client.gui.GuiGraphics graphics, float partialTick) {
        for (Module m : modules) {
            if (m.isEnabled()) m.onRender2D(graphics, partialTick);
        }
    }
}

package com.ttclient.modules;

import com.ttclient.modules.client.*;
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
        register(new LightLevel());
        register(new TargetInfo());

        register(new Sprint());
        register(new AutoWalk());
        register(new AutoJump());
        register(new ToggleSneak());

        register(new AutoEat());
        register(new AutoFish());
        register(new AutoRespawn());

        register(new Waypoints());

        register(new FPSBoost());
        register(new AntiAFK());
        register(new ChatTimestamps());
        register(new CoordCopy());
        register(new DeathCoords());
        register(new SessionTimer());
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
    public void onRender2D(net.minecraft.client.gui.GuiGraphics graphics, float partialTick) {
        for (Module m : modules) if (m.isEnabled()) m.onRender2D(graphics, partialTick);
    }
}

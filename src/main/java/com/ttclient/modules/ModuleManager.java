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
        // Modules are registered by the full client ModuleManager on main;
        // this file on older trees only — keep getByCategory alias + render path.
    }

    private void register(Module module) { modules.add(module); }
    public List<Module> getModules() { return modules; }
    public List<Module> getModulesByCategory(Category category) {
        return modules.stream().filter(m -> m.getCategory() == category).collect(Collectors.toList());
    }

    /** Alias used by ClickGUI layouts. */
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
    public void onRender2D(net.minecraft.client.gui.GuiGraphicsExtractor graphics, float partialTick) {
        for (Module m : modules) if (m.isEnabled()) {
            try { m.onRender2D(graphics, partialTick); } catch (Throwable ignored) {}
        }
    }
}

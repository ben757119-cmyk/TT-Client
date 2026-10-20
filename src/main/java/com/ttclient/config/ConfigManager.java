package com.ttclient.config;

import com.ttclient.TTClient;
import com.ttclient.client.TTClientClient;
import com.ttclient.modules.Module;
import com.ttclient.modules.render.BlockESP;
import com.ttclient.modules.world.XRay;
import com.ttclient.settings.*;

import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigManager {
    private final Path configDir;
    private final Path configFile;

    /** Shared layout: category display name -> [x, y, open(1/0)] */
    public static final Map<String, double[]> PANEL_LAYOUT = new HashMap<>();

    public ConfigManager() {
        configDir = Path.of("config", "ttclient");
        configFile = configDir.resolve("config.properties");
        try { Files.createDirectories(configDir); }
        catch (IOException e) { TTClient.LOGGER.error("Failed to create config dir", e); }
    }

    public void save() {
        if (TTClientClient.modules == null) return;
        Properties props = new Properties();
        for (Module mod : TTClientClient.modules.getModules()) {
            String prefix = mod.getName().replace(" ", "_") + ".";
            props.setProperty(prefix + "enabled", String.valueOf(mod.isEnabled()));
            props.setProperty(prefix + "key", String.valueOf(mod.getKeyBind()));
            for (Setting<?> s : mod.getSettings()) {
                props.setProperty(prefix + s.getName().replace(" ", "_"), String.valueOf(s.get()));
            }
            if (mod instanceof BlockESP be) props.setProperty(prefix + "blocks", be.saveBlocks());
            if (mod instanceof XRay xr) props.setProperty(prefix + "blocks", xr.saveBlocks());
        }
        // ClickGUI panel layout (x, y, open)
        for (Map.Entry<String, double[]> e : PANEL_LAYOUT.entrySet()) {
            String k = e.getKey();
            double[] v = e.getValue();
            if (v == null || v.length < 2) continue;
            props.setProperty("gui.panel." + k + ".x", String.valueOf(v[0]));
            props.setProperty("gui.panel." + k + ".y", String.valueOf(v[1]));
            if (v.length >= 3) props.setProperty("gui.panel." + k + ".open", String.valueOf(v[2] >= 0.5));
        }
        try (OutputStream out = Files.newOutputStream(configFile)) {
            props.store(out, "TT Client Config");
        } catch (IOException e) {
            TTClient.LOGGER.error("Failed to save config", e);
        }
    }

    public void load() {
        if (!Files.exists(configFile) || TTClientClient.modules == null) return;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configFile)) { props.load(in); }
        catch (IOException e) { TTClient.LOGGER.error("Failed to load config", e); return; }
        for (Module mod : TTClientClient.modules.getModules()) {
            String prefix = mod.getName().replace(" ", "_") + ".";
            String enabled = props.getProperty(prefix + "enabled");
            if (enabled != null) mod.setEnabled(Boolean.parseBoolean(enabled));
            String key = props.getProperty(prefix + "key");
            if (key != null) try { mod.setKeyBind(Integer.parseInt(key)); } catch (Exception ignored) {}
            for (Setting<?> s : mod.getSettings()) {
                String val = props.getProperty(prefix + s.getName().replace(" ", "_"));
                if (val == null) continue;
                try {
                    if (s instanceof BoolSetting bs) bs.set(Boolean.parseBoolean(val));
                    else if (s instanceof NumberSetting ns) ns.set(Double.parseDouble(val));
                    else if (s instanceof ModeSetting ms) ms.set(val);
                    else if (s instanceof ColorSetting cs) cs.set(Integer.parseInt(val));
                } catch (Exception ignored) {}
            }
            if (mod instanceof BlockESP be) {
                String blocks = props.getProperty(prefix + "blocks");
                if (blocks != null) be.loadBlocks(blocks);
            }
            if (mod instanceof XRay xr) {
                String blocks = props.getProperty(prefix + "blocks");
                if (blocks != null) xr.loadBlocks(blocks);
            }
        }
        // Load ClickGUI panel layout into static map
        PANEL_LAYOUT.clear();
        for (String name : props.stringPropertyNames()) {
            if (!name.startsWith("gui.panel.") || !name.endsWith(".x")) continue;
            String cat = name.substring("gui.panel.".length(), name.length() - 2);
            try {
                double px = Double.parseDouble(props.getProperty("gui.panel." + cat + ".x", "0"));
                double py = Double.parseDouble(props.getProperty("gui.panel." + cat + ".y", "0"));
                boolean open = Boolean.parseBoolean(props.getProperty("gui.panel." + cat + ".open", "true"));
                PANEL_LAYOUT.put(cat, new double[]{px, py, open ? 1.0 : 0.0});
            } catch (Exception ignored) {}
        }
    }

    public void savePanelLayout(Map<String, double[]> layout) {
        PANEL_LAYOUT.clear();
        if (layout != null) PANEL_LAYOUT.putAll(layout);
        save();
    }
}

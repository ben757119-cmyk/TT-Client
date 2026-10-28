package com.ttclient.config;

import com.ttclient.TTClient;
import com.ttclient.client.TTClientClient;
import com.ttclient.modules.Module;
import com.ttclient.modules.client.ClickGUIModule;
import com.ttclient.settings.*;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class ConfigManager {
    /** Classic/Meteor panel layout: category name -> {x, y, open(0/1)}. */
    public static final java.util.Map<String, double[]> PANEL_LAYOUT = new java.util.concurrent.ConcurrentHashMap<>();

    private final Path configDir;
    private final Path configFile;

    public ConfigManager() {
        configDir = Path.of("config", "ttclient");
        configFile = configDir.resolve("config.properties");
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            TTClient.LOGGER.error("Failed to create config dir", e);
        }
    }

    public void save() {
        if (TTClientClient.modules == null) return;
        Properties props = new Properties();
        for (Module mod : TTClientClient.modules.getModules()) {
            String prefix = mod.getName().replace(" ", "_") + ".";
            // ClickGUI is a keybind opener — never persist enabled=true (causes auto-popup)
            boolean en = mod.isEnabled();
            if (mod instanceof ClickGUIModule) en = false;
            props.setProperty(prefix + "enabled", String.valueOf(en));
            props.setProperty(prefix + "key", String.valueOf(mod.getKeyBind()));
            for (Setting<?> s : mod.getSettings()) {
                props.setProperty(prefix + s.getName().replace(" ", "_"), String.valueOf(s.get()));
            }
        }
        for (var e : PANEL_LAYOUT.entrySet()) {
            double[] v = e.getValue();
            if (v != null && v.length >= 3) {
                props.setProperty("gui.panel." + e.getKey() + ".x", String.valueOf(v[0]));
                props.setProperty("gui.panel." + e.getKey() + ".y", String.valueOf(v[1]));
                props.setProperty("gui.panel." + e.getKey() + ".open", String.valueOf(v[2]));
            }
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
        try (InputStream in = Files.newInputStream(configFile)) {
            props.load(in);
        } catch (IOException e) {
            TTClient.LOGGER.error("Failed to load config", e);
            return;
        }
        for (Module mod : TTClientClient.modules.getModules()) {
            String prefix = mod.getName().replace(" ", "_") + ".";
            // Never restore ClickGUI as enabled — it opens the screen in onEnable
            if (!(mod instanceof ClickGUIModule)) {
                String enabled = props.getProperty(prefix + "enabled");
                if (enabled != null) mod.setEnabled(Boolean.parseBoolean(enabled));
            } else {
                mod.setEnabled(false);
            }
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
        }
        for (String key : props.stringPropertyNames()) {
            if (!key.startsWith("gui.panel.") || !key.endsWith(".x")) continue;
            String cat = key.substring("gui.panel.".length(), key.length() - 2);
            try {
                double x = Double.parseDouble(props.getProperty("gui.panel." + cat + ".x", "20"));
                double y = Double.parseDouble(props.getProperty("gui.panel." + cat + ".y", "30"));
                double open = Double.parseDouble(props.getProperty("gui.panel." + cat + ".open", "1"));
                PANEL_LAYOUT.put(cat, new double[]{x, y, open});
            } catch (Exception ignored) {}
        }
    }
}

package com.ttclient.config;

import com.ttclient.TTClient;
import com.ttclient.modules.Module;
import com.ttclient.settings.*;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class ConfigManager {
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
        if (TTClient.modules == null) return;
        Properties props = new Properties();
        for (Module mod : TTClient.modules.getModules()) {
            String prefix = mod.getName().replace(" ", "_") + ".";
            props.setProperty(prefix + "enabled", String.valueOf(mod.isEnabled()));
            props.setProperty(prefix + "key", String.valueOf(mod.getKeyBind()));
            for (Setting<?> s : mod.getSettings()) {
                props.setProperty(prefix + s.getName().replace(" ", "_"), String.valueOf(s.get()));
            }
        }
        try (OutputStream out = Files.newOutputStream(configFile)) {
            props.store(out, "TT Client Config");
        } catch (IOException e) {
            TTClient.LOGGER.error("Failed to save config", e);
        }
    }

    public void load() {
        if (!Files.exists(configFile) || TTClient.modules == null) return;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configFile)) {
            props.load(in);
        } catch (IOException e) {
            TTClient.LOGGER.error("Failed to load config", e);
            return;
        }
        for (Module mod : TTClient.modules.getModules()) {
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
        }
    }
}

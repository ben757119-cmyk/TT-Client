package com.ttclient.settings;

import java.util.Arrays;
import java.util.List;

public class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String description, String defaultValue, String... modes) {
        super(name, description, defaultValue);
        this.modes = Arrays.asList(modes);
    }

    public List<String> getModes() { return modes; }

    public void cycle() {
        int idx = modes.indexOf(get());
        set(modes.get((idx + 1) % modes.size()));
    }
}

package com.ttclient.settings;

public class ColorSetting extends Setting<Integer> {
    public ColorSetting(String name, String description, int defaultValue) {
        super(name, description, defaultValue);
    }

    public int getRed() { return (get() >> 16) & 0xFF; }
    public int getGreen() { return (get() >> 8) & 0xFF; }
    public int getBlue() { return get() & 0xFF; }
    public int getAlpha() { return (get() >> 24) & 0xFF; }
}

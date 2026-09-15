package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.NumberSetting;

public class Ambience extends Module {
    public final ColorSetting fogColor = addSetting(new ColorSetting("Fog Color", "Custom fog color", 0xFF1A1A2E));
    public final NumberSetting fogDistance = addSetting(new NumberSetting("Fog Distance", "Fog start distance", 0.5, 0.0, 1.0, 0.05));
    public final ColorSetting skyColor = addSetting(new ColorSetting("Sky Color", "Custom sky color", 0xFF0F0F23));

    public Ambience() {
        super("Ambience", "Custom world atmosphere", Category.RENDER);
    }
}

package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.NumberSetting;

public class ChinaHat extends Module {
    public final ColorSetting color = addSetting(new ColorSetting("Color", "Hat color", 0xAA00FFAA));
    public final NumberSetting radius = addSetting(new NumberSetting("Radius", "Hat radius", 0.6, 0.3, 1.5, 0.05));
    public final NumberSetting height = addSetting(new NumberSetting("Height", "Hat height", 0.3, 0.1, 0.8, 0.05));

    public ChinaHat() {
        super("ChinaHat", "Render a stylish hat above your head", Category.RENDER);
    }
}

package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.NumberSetting;

public class Breadcrumbs extends Module {
    public final ColorSetting color = addSetting(new ColorSetting("Color", "Trail color", 0xFF00FFAA));
    public final NumberSetting length = addSetting(new NumberSetting("Length", "Trail length", 50, 10, 200, 5));
    public final NumberSetting width = addSetting(new NumberSetting("Width", "Line width", 1.5, 0.5, 4.0, 0.5));

    public Breadcrumbs() {
        super("Breadcrumbs", "Leave a trail behind you", Category.RENDER);
    }
}

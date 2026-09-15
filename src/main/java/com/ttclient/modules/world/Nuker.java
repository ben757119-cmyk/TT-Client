package com.ttclient.modules.world;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;
import com.ttclient.settings.ModeSetting;

public class Nuker extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting("Mode", "Nuker mode", "Normal", "Normal", "Flatten", "Selective"));
    public final NumberSetting range = addSetting(new NumberSetting("Range", "Break range", 4.5, 1.0, 6.0, 0.5));

    public Nuker() {
        super("Nuker", "Automatically break blocks around you", Category.WORLD);
    }
}

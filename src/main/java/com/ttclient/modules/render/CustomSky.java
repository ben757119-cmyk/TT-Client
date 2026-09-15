package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ModeSetting;

public class CustomSky extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting("Mode", "Sky mode", "End", "End", "Nether", "Custom", "None"));
    public final BoolSetting stars = addSetting(new BoolSetting("Stars", "Extra stars", true));
    public final BoolSetting timeCycle = addSetting(new BoolSetting("Time Cycle", "Custom time", false));

    public CustomSky() {
        super("CustomSky", "Change the sky rendering", Category.RENDER);
    }
}

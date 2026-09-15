package com.ttclient.modules.world;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;

public class Timer extends Module {
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Game speed multiplier", 1.0, 0.1, 5.0, 0.1));

    public Timer() {
        super("Timer", "Change client game speed", Category.WORLD);
    }
}

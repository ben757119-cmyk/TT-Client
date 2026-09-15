package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;

public class Step extends Module {
    public final NumberSetting height = addSetting(new NumberSetting("Height", "Step height", 1.0, 0.6, 2.5, 0.1));

    public Step() {
        super("Step", "Step up blocks", Category.MOVEMENT);
    }
}

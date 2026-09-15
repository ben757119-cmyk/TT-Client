package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.ModeSetting;
import com.ttclient.settings.NumberSetting;

public class Speed extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting("Mode", "Speed mode", "Strafe", "Strafe", "Vanilla", "BHop"));
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Speed multiplier", 1.5, 1.0, 3.0, 0.1));

    public Speed() {
        super("Speed", "Move faster", Category.MOVEMENT);
    }
}

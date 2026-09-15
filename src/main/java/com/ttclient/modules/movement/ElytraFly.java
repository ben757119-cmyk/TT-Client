package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.ModeSetting;
import com.ttclient.settings.NumberSetting;

public class ElytraFly extends Module {
    public final ModeSetting mode = addSetting(new ModeSetting("Mode", "Elytra fly mode", "Control", "Control", "Boost", "Packet"));
    public final NumberSetting speed = addSetting(new NumberSetting("Speed", "Fly speed", 1.8, 0.5, 5.0, 0.1));

    public ElytraFly() {
        super("ElytraFly", "Enhanced elytra flight", Category.MOVEMENT);
    }
}

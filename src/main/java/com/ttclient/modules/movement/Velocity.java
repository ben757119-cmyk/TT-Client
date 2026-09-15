package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.NumberSetting;

public class Velocity extends Module {
    public final NumberSetting horizontal = addSetting(new NumberSetting("Horizontal", "Horizontal knockback %", 0.0, 0.0, 100.0, 5.0));
    public final NumberSetting vertical = addSetting(new NumberSetting("Vertical", "Vertical knockback %", 0.0, 0.0, 100.0, 5.0));

    public Velocity() {
        super("Velocity", "Modify knockback taken", Category.MOVEMENT);
    }
}

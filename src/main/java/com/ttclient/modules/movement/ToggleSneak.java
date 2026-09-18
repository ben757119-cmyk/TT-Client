package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;

public class ToggleSneak extends Module {
    public ToggleSneak() {
        super("ToggleSneak", "Hold sneak for you while enabled", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null) return;
        mc.options.keyShift.setDown(true);
    }

    @Override
    public void onDisable() {
        if (mc.options != null) {
            mc.options.keyShift.setDown(false);
        }
    }
}

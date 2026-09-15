package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;

public class AutoWalk extends Module {
    public AutoWalk() {
        super("AutoWalk", "Automatically walk forward", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player != null) {
            mc.options.keyUp.setDown(true);
        }
    }

    @Override
    public void onDisable() {
        if (mc.options != null) mc.options.keyUp.setDown(false);
    }
}

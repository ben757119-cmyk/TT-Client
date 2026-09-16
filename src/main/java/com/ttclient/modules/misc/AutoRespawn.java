package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;

public class AutoRespawn extends Module {
    public AutoRespawn() {
        super("AutoRespawn", "Skip the death screen and respawn immediately", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.player.isDeadOrDying()) {
            mc.player.respawn();
        }
    }
}

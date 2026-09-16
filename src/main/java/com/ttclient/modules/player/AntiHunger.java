package com.ttclient.modules.player;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;

public class AntiHunger extends Module {
    public AntiHunger() {
        super("AntiHunger", "Stop sprinting when standing still to save hunger", Category.PLAYER);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
        boolean moving = mc.options.keyUp.isDown() || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown();
        if (!moving && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }
    }
}

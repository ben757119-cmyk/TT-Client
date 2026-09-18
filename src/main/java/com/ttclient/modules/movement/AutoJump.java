package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.player.LocalPlayer;

public class AutoJump extends Module {
    public AutoJump() {
        super("AutoJump", "Jump automatically while moving on the ground", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null) return;
        LocalPlayer p = mc.player;
        if (!p.onGround()) return;
        boolean moving = mc.options.keyUp.isDown()
                || mc.options.keyDown.isDown()
                || mc.options.keyLeft.isDown()
                || mc.options.keyRight.isDown();
        if (moving) {
            p.jumpFromGround();
        }
    }
}

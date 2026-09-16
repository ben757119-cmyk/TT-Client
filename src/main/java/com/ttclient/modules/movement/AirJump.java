package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;

public class AirJump extends Module {
    private boolean wasJump;

    public AirJump() {
        super("AirJump", "Jump again while in the air", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.options == null) return;
        boolean jump = mc.options.keyJump.isDown();
        if (jump && !wasJump && !mc.player.onGround()) {
            mc.player.jumpFromGround();
            mc.player.setOnGround(true);
        }
        wasJump = jump;
    }
}

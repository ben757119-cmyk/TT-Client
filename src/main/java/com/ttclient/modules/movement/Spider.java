package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class Spider extends Module {
    public Spider() {
        super("Spider", "Climb walls", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        LocalPlayer p = mc.player;
        if (p.horizontalCollision) {
            p.setDeltaMovement(p.getDeltaMovement().x, 0.2, p.getDeltaMovement().z);
        }
    }
}

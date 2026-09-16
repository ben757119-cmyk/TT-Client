package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public class Jesus extends Module {
    public Jesus() {
        super("Jesus", "Walk on water and lava", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        LocalPlayer p = mc.player;
        if (p.isInWater() || p.isInLava()) {
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, Math.max(v.y, 0.1), v.z);
            p.setOnGround(true);
            p.fallDistance = 0;
        }
    }
}

package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;

public class Parkour extends Module {
    public Parkour() {
        super("Parkour", "Auto-jump at block edges", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.options == null) return;
        LocalPlayer p = mc.player;
        if (!p.onGround() || !mc.options.keyUp.isDown()) return;
        AABB box = p.getBoundingBox();
        AABB ahead = box.move(p.getDeltaMovement().x, -0.5, p.getDeltaMovement().z);
        if (mc.level.noCollision(p, ahead.inflate(-0.05, 0, -0.05))) {
            p.jumpFromGround();
        }
    }
}

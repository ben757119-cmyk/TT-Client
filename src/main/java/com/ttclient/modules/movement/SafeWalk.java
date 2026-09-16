package com.ttclient.modules.movement;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;

public class SafeWalk extends Module {
    public SafeWalk() {
        super("SafeWalk", "Sneak at block edges to avoid falling", Category.MOVEMENT);
    }

    @Override
    public void onTick() {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null || mc.options == null) return;
        LocalPlayer p = mc.player;
        if (!p.onGround()) return;
        AABB box = p.getBoundingBox();
        AABB below = box.move(0, -0.5, 0).inflate(-0.05, 0, -0.05);
        if (mc.level.noCollision(p, below)) {
            mc.options.keyShift.setDown(true);
        }
    }
}

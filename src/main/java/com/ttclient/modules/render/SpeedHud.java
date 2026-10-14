package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

public class SpeedHud extends Module {
    public SpeedHud() {
        super("SpeedHud", "Show horizontal speed", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphics graphics, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.player == null) return;
        Vec3 v = mc.player.getDeltaMovement();
        double speed = Math.sqrt(v.x * v.x + v.z * v.z) * 20.0;
        graphics.drawString(mc.font, String.format("Speed %.2f", speed), 4, 80, 0xFFE8EAED, false);
    }
}

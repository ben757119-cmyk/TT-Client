package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

public class SpeedHud extends Module {
    public SpeedHud() {
        super("SpeedHud", "Show horizontal speed", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.player == null) return;
        Vec3 v = minecraft.player.getDeltaMovement();
        double speed = Math.sqrt(v.x * v.x + v.z * v.z) * 20.0;
        graphics.text(minecraft.font, String.format("Speed %.2f", speed), 4, 80, 0xFFE8EAED, false);
    }
}

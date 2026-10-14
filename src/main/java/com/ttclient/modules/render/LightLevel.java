package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

public class LightLevel extends Module {
    public LightLevel() {
        super("LightLevel", "Show light level at feet", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphics graphics, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null) return;
        BlockPos pos = mc.player.blockPosition();
        int light = mc.level.getMaxLocalRawBrightness(pos);
        graphics.drawString(mc.font, "Light " + light, 4, 140, 0xFFE8EAED, false);
    }
}

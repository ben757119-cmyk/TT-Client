package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;

public class LightLevel extends Module {
    public LightLevel() {
        super("LightLevel", "Show light level at feet", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return;
        BlockPos pos = minecraft.player.blockPosition();
        int light = minecraft.level.getMaxLocalRawBrightness(pos);
        graphics.text(minecraft.font, "Light " + light, 4, 140, 0xFFE8EAED, false);
    }
}

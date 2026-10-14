package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class BreakProgress extends Module {
    public BreakProgress() {
        super("BreakProgress", "Show block break progress", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return;
        try {
            var f = minecraft.gameMode.getClass().getDeclaredField("destroyProgress");
            f.setAccessible(true);
            float p = f.getFloat(minecraft.gameMode);
            if (p > 0) {
                graphics.text(minecraft.font, String.format("Break %.0f%%", p * 100f), 4, 104, 0xFFE8EAED, false);
            }
        } catch (Throwable ignored) {}
    }
}

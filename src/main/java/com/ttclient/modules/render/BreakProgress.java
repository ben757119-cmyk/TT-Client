package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class BreakProgress extends Module {
    public BreakProgress() {
        super("BreakProgress", "Show block break progress", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphics graphics, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.gameMode == null) return;
        // Best-effort; destroy progress field names vary by mapping
        try {
            var f = mc.gameMode.getClass().getDeclaredField("destroyProgress");
            f.setAccessible(true);
            float p = f.getFloat(mc.gameMode);
            if (p > 0) {
                graphics.drawString(mc.font, String.format("Break %.0f%%", p * 100f), 4, 104, 0xFFE8EAED, false);
            }
        } catch (Throwable ignored) {}
    }
}

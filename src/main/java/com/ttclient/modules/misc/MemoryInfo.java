package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.gui.GuiGraphics;

public class MemoryInfo extends Module {
    public MemoryInfo() {
        super("MemoryInfo", "JVM used / max heap on the HUD", Category.MISC);
        setEnabled(false);
    }

    @Override
    public void onRender2D(GuiGraphics g, float partialTick) {
        if (mc.options.hideGui) return;
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long max = rt.maxMemory() / (1024 * 1024);
        String text = used + " / " + max + " MB";
        int w = mc.getWindow().getGuiScaledWidth();
        g.drawString(mc.font, text, w - mc.font.width(text) - 4, 42, 0xFF8B95A8, true);
    }
}

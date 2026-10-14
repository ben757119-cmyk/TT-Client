package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class MemoryInfo extends Module {
    public MemoryInfo() {
        super("MemoryInfo", "JVM used / max heap on the HUD", Category.MISC);
        setEnabled(false);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        if (mc().player == null) return;
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        long max = rt.maxMemory() / (1024 * 1024);
        String text = "Mem " + used + "/" + max + "MB";
        g.text(mc().font, text, 4, 176, 0xFF8B95A8, false);
    }
}

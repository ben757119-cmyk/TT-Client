package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.gui.GuiGraphics;

public class SessionTimer extends Module {
    private long startMs;

    public SessionTimer() {
        super("SessionTimer", "Show how long this session has been running", Category.MISC);
        setEnabled(true);
    }

    @Override
    public void onEnable() {
        startMs = System.currentTimeMillis();
    }

    @Override
    public void onRender2D(GuiGraphics g, float partialTick) {
        if (mc.player == null || mc.options.hideGui) return;
        if (startMs == 0) startMs = System.currentTimeMillis();
        long sec = Math.max(0, (System.currentTimeMillis() - startMs) / 1000);
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        String text = h > 0
                ? String.format("Session §f%d:%02d:%02d", h, m, s)
                : String.format("Session §f%d:%02d", m, s);
        int w = mc.font.width(text);
        g.drawString(mc.font, text, mc.getWindow().getGuiScaledWidth() - w - 4, mc.getWindow().getGuiScaledHeight() - 24, 0xFF00E8A0, false);
    }
}

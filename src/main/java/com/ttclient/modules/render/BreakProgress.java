package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;

public class BreakProgress extends Module {
    public BreakProgress() {
        super("BreakProgress", "Shows block-break percent while mining", Category.RENDER);
        setEnabled(true);
    }

    @Override
    public void onRender2D(GuiGraphics g, float partialTick) {
        if (mc.player == null || mc.options.hideGui) return;
        MultiPlayerGameMode gm = mc.gameMode;
        if (gm == null) return;
        float dest = gm.getDestroyStage();
        if (dest < 0) return;

        int pct = Math.min(100, (int) ((dest + 1) * 10));
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int barW = 80;
        int x = w / 2 - barW / 2;
        int y = h / 2 + 12;
        g.fill(x - 1, y - 1, x + barW + 1, y + 7, 0xAA000000);
        g.fill(x, y, x + (barW * pct / 100), y + 6, 0xFF00E8A0);
        String label = pct + "%";
        g.drawString(mc.font, label, w / 2 - mc.font.width(label) / 2, y - 11, 0xFF00E8A0, false);
    }
}

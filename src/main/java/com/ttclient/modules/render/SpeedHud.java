package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.gui.GuiGraphics;

public class SpeedHud extends Module {
    public SpeedHud() {
        super("SpeedHud", "Show horizontal speed in blocks per second", Category.RENDER);
        setEnabled(true);
    }

    @Override
    public void onRender2D(GuiGraphics g, float partialTick) {
        if (mc.player == null || mc.options.hideGui) return;
        double dx = mc.player.getX() - mc.player.xo;
        double dz = mc.player.getZ() - mc.player.zo;
        double bps = Math.sqrt(dx * dx + dz * dz) * 20.0;
        String text = String.format("Speed §f%.2f b/s", bps);
        int x = 4;
        int y = mc.getWindow().getGuiScaledHeight() - 14;
        g.drawString(mc.font, text, x + 1, y + 1, 0x000000, false);
        g.drawString(mc.font, text, x, y, 0xFF00E8A0, false);
    }
}

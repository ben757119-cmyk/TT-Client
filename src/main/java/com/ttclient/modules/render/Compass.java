package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public class Compass extends Module {
    public Compass() {
        super("Compass", "Cardinal direction strip at the top of the screen", Category.RENDER);
        setEnabled(true);
    }

    @Override
    public void onRender2D(GuiGraphics g, float partialTick) {
        var mc = mc();
        if (mc.player == null || mc.options.hideGui) return;
        int w = mc.getWindow().getGuiScaledWidth();
        int cx = w / 2;
        float yaw = Mth.wrapDegrees(mc.player.getYRot());
        String[] marks = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        float[] angles = {180, 225, -90, -45, 0, 45, 90, 135};

        g.fill(cx - 70, 2, cx + 70, 16, 0x88000000);
        g.drawString(mc.font, "^", cx - 3, 3, 0xFF00E8A0, false);

        for (int i = 0; i < marks.length; i++) {
            float diff = Mth.wrapDegrees(angles[i] - yaw);
            int x = cx + (int) (diff * 0.7f);
            if (x < cx - 66 || x > cx + 66) continue;
            int col = marks[i].length() == 1 ? 0xFFFFFFFF : 0xFF8B95A8;
            g.drawString(mc.font, marks[i], x - mc.font.width(marks[i]) / 2, 5, col, false);
        }
    }
}

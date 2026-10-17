package com.ttclient.util;

import com.ttclient.render.EspDraw;
import com.ttclient.render.EspProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;

import java.util.List;

/** Shared ESP hit list for basefind modules. */
public final class BasefindHits {
    public static final class Hit {
        public final BlockPos pos;
        public final String label;
        public final int color;

        public Hit(BlockPos pos, String label, int color) {
            this.pos = pos.immutable();
            this.label = label;
            this.color = color | 0xFF000000;
        }
    }

    private BasefindHits() {}

    public static void render(GuiGraphicsExtractor g, List<Hit> hits, boolean tracers) {
        if (hits == null || hits.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null || mc.getWindow() == null) return;

        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int cy = mc.getWindow().getGuiScaledHeight() / 2;

        for (Hit h : hits) {
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            int visible = 0;
            for (int i = 0; i < 8; i++) {
                double bx = h.pos.getX() + ((i & 1) == 0 ? 0.05 : 0.95);
                double by = h.pos.getY() + ((i & 2) == 0 ? 0.05 : 0.95);
                double bz = h.pos.getZ() + ((i & 4) == 0 ? 0.05 : 0.95);
                double[] p = new double[2];
                if (!EspProjection.worldToScreen(bx, by, bz, p)) continue;
                minX = Math.min(minX, p[0]);
                minY = Math.min(minY, p[1]);
                maxX = Math.max(maxX, p[0]);
                maxY = Math.max(maxY, p[1]);
                visible++;
            }
            if (visible < 2) continue;
            if (maxX - minX < 4) {
                double c = (minX + maxX) * 0.5;
                minX = c - 2;
                maxX = c + 2;
            }
            if (maxY - minY < 4) {
                double c = (minY + maxY) * 0.5;
                minY = c - 2;
                maxY = c + 2;
            }
            EspDraw.box(g, minX, minY, maxX, maxY, h.color, 1);
            EspDraw.label(g, mc.font, h.label, (minX + maxX) * 0.5, minY - 11, h.color);
            if (tracers) {
                EspDraw.line(g, cx, cy, (minX + maxX) * 0.5, (minY + maxY) * 0.5, h.color);
            }
        }
    }
}

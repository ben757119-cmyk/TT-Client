package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.render.EspDraw;
import com.ttclient.render.EspProjection;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

/** Montage-style china hat ring above players (screen-space). */
public class ChinaHat extends Module {
    public final ColorSetting color = addSetting(new ColorSetting("Color", "Hat color", 0xFF00E8A0));
    public final NumberSetting radius = addSetting(new NumberSetting("Radius", "Ring radius", 0.55, 0.3, 1.2, 0.05));
    public final NumberSetting height = addSetting(new NumberSetting("Height", "Above head", 0.12, 0.0, 0.6, 0.02));
    public final BoolSetting others = addSetting(new BoolSetting("Others", "Draw on other players", true));
    public final BoolSetting self = addSetting(new BoolSetting("Self", "Draw on you", true));
    public final NumberSetting segments = addSetting(new NumberSetting("Segments", "Circle quality", 40, 16, 64, 1));

    public ChinaHat() {
        super("ChinaHat", "Clean ring above player heads", Category.RENDER);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null) return;

        int col = color.get();
        if (((col >> 24) & 0xFF) == 0) col |= 0xFF000000;

        if (self.get()) drawHat(g, mc.player, col);

        if (others.get()) {
            for (Player pl : mc.level.players()) {
                if (pl == mc.player || !pl.isAlive()) continue;
                if (mc.player.distanceToSqr(pl) > 64 * 64) continue;
                drawHat(g, pl, col);
            }
        }
    }

    private void drawHat(GuiGraphicsExtractor g, Player pl, int col) {
        double x = pl.getX();
        double y = pl.getY() + pl.getBbHeight() + height.get();
        double z = pl.getZ();

        double rad = radius.get();
        int segs = segments.getInt();
        double[] out = new double[2];
        Double prevX = null, prevY = null;
        double firstX = 0, firstY = 0;
        boolean hasFirst = false;

        for (int i = 0; i <= segs; i++) {
            double a = (i / (double) segs) * Math.PI * 2.0;
            double px = x + Math.cos(a) * rad;
            double pz = z + Math.sin(a) * rad;
            if (!EspProjection.worldToScreen(px, y, pz, out)) {
                prevX = null;
                prevY = null;
                continue;
            }
            if (!hasFirst) {
                firstX = out[0];
                firstY = out[1];
                hasFirst = true;
            }
            if (prevX != null) {
                EspDraw.line(g, prevX, prevY, out[0], out[1], col);
            }
            prevX = out[0];
            prevY = out[1];
        }
        if (hasFirst && prevX != null) {
            EspDraw.line(g, prevX, prevY, firstX, firstY, col);
        }

        if (EspProjection.worldToScreen(x, y, z, out)) {
            int cx = (int) out[0];
            int cy = (int) out[1];
            g.fill(cx - 1, cy - 1, cx + 2, cy + 2, col);
        }
    }
}

package com.ttclient.modules.render;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.modules.client.ClickGUIModule;
import com.ttclient.render.EspDraw;
import com.ttclient.render.EspProjection;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

/**
 * Full solid china hat (filled cone) — Vape-style solid brim + peak.
 */
public class ChinaHat extends Module {
    public final ColorSetting color = addSetting(new ColorSetting("Color", "Hat color", 0xFF00E8A0));
    public final BoolSetting useAccent = addSetting(new BoolSetting("UseAccent", "Use ClickGUI accent", true));
    public final NumberSetting radius = addSetting(new NumberSetting("Radius", "Brim radius", 0.7, 0.35, 1.4, 0.05));
    public final NumberSetting height = addSetting(new NumberSetting("Height", "Brim above head", 0.12, 0.0, 0.5, 0.02));
    public final NumberSetting tip = addSetting(new NumberSetting("Tip", "Peak height", 0.4, 0.15, 1.0, 0.05));
    public final BoolSetting others = addSetting(new BoolSetting("Others", "Draw on other players", true));
    public final BoolSetting self = addSetting(new BoolSetting("Self", "Draw on you", true));
    public final NumberSetting segments = addSetting(new NumberSetting("Segments", "Mesh quality", 36, 16, 64, 1));
    public final NumberSetting opacity = addSetting(new NumberSetting("Opacity", "Fill alpha 0-1", 0.55, 0.15, 1.0, 0.05));

    public ChinaHat() {
        super("ChinaHat", "Solid cone hat above heads", Category.RENDER);
    }

    private int hatColor(int alpha) {
        int base = useAccent.get() ? ClickGUIModule.globalAccent() : color.get();
        if (((base >> 24) & 0xFF) == 0) base |= 0xFF000000;
        return (base & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null) return;

        if (self.get()) drawHat(g, mc.player);
        if (others.get()) {
            for (Player pl : mc.level.players()) {
                if (pl == mc.player || !pl.isAlive()) continue;
                if (mc.player.distanceToSqr(pl) > 48 * 48) continue;
                drawHat(g, pl);
            }
        }
    }

    private void drawHat(GuiGraphicsExtractor g, Player pl) {
        double x = pl.getX();
        double baseY = pl.getY() + pl.getBbHeight() + height.get();
        double tipY = baseY + tip.get();
        double z = pl.getZ();
        double rad = radius.get();
        int segs = Math.max(12, segments.getInt());
        int aFill = (int) (opacity.get() * 255);
        int colFill = hatColor(aFill);
        int colEdge = hatColor(Math.min(255, aFill + 80));

        double[] tipScr = new double[2];
        boolean tipOk = EspProjection.worldToScreen(x, tipY, z, tipScr);
        if (!tipOk) return;

        double[] rimX = new double[segs];
        double[] rimY = new double[segs];
        boolean[] ok = new boolean[segs];
        double[] out = new double[2];

        for (int i = 0; i < segs; i++) {
            double a = (i / (double) segs) * Math.PI * 2.0;
            if (EspProjection.worldToScreen(x + Math.cos(a) * rad, baseY, z + Math.sin(a) * rad, out)) {
                rimX[i] = out[0];
                rimY[i] = out[1];
                ok[i] = true;
            }
        }

        // Filled cone: fan of triangles tip → rim[i] → rim[i+1]
        for (int i = 0; i < segs; i++) {
            int j = (i + 1) % segs;
            if (!ok[i] || !ok[j]) continue;
            fillTriangle(g,
                    tipScr[0], tipScr[1],
                    rimX[i], rimY[i],
                    rimX[j], rimY[j],
                    colFill);
        }

        // Solid brim disc (inner rings)
        for (int ring = 4; ring >= 1; ring--) {
            double r = rad * (ring / 4.0);
            Double prevX = null, prevY = null;
            double fX = 0, fY = 0;
            boolean has = false;
            for (int i = 0; i <= segs; i++) {
                double a = (i / (double) segs) * Math.PI * 2.0;
                if (!EspProjection.worldToScreen(x + Math.cos(a) * r, baseY, z + Math.sin(a) * r, out)) {
                    prevX = null;
                    continue;
                }
                if (!has) { fX = out[0]; fY = out[1]; has = true; }
                if (prevX != null) EspDraw.line(g, prevX, prevY, out[0], out[1], colFill);
                prevX = out[0];
                prevY = out[1];
            }
            if (has && prevX != null) EspDraw.line(g, prevX, prevY, fX, fY, colFill);
        }

        // Hard outer rim
        Double prevX = null, prevY = null;
        double firstX = 0, firstY = 0;
        boolean hasFirst = false;
        for (int i = 0; i < segs; i++) {
            if (!ok[i]) { prevX = null; continue; }
            if (!hasFirst) { firstX = rimX[i]; firstY = rimY[i]; hasFirst = true; }
            if (prevX != null) {
                EspDraw.line(g, prevX, prevY, rimX[i], rimY[i], colEdge);
                EspDraw.line(g, prevX + 1, prevY, rimX[i] + 1, rimY[i], colEdge);
            }
            prevX = rimX[i];
            prevY = rimY[i];
        }
        if (hasFirst && prevX != null) EspDraw.line(g, prevX, prevY, firstX, firstY, colEdge);

        // Peak
        int cx = (int) tipScr[0];
        int cy = (int) tipScr[1];
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, colEdge);
    }

    /** Scanline fill for a triangle in screen space. */
    private static void fillTriangle(GuiGraphicsExtractor g,
                                     double x0, double y0,
                                     double x1, double y1,
                                     double x2, double y2,
                                     int argb) {
        // Sort by Y
        if (y0 > y1) { double t; t = y0; y0 = y1; y1 = t; t = x0; x0 = x1; x1 = t; }
        if (y1 > y2) { double t; t = y1; y1 = y2; y2 = t; t = x1; x1 = x2; x2 = t; }
        if (y0 > y1) { double t; t = y0; y0 = y1; y1 = t; t = x0; x0 = x1; x1 = t; }

        int minY = (int) Math.ceil(y0);
        int maxY = (int) Math.floor(y2);
        if (maxY - minY > 120) maxY = minY + 120; // safety

        for (int y = minY; y <= maxY; y++) {
            double xa, xb;
            if (y < y1) {
                xa = interp(y0, x0, y2, x2, y);
                xb = interp(y0, x0, y1, x1, y);
            } else {
                xa = interp(y0, x0, y2, x2, y);
                xb = interp(y1, x1, y2, x2, y);
            }
            if (xa > xb) { double t = xa; xa = xb; xb = t; }
            int xStart = (int) Math.ceil(xa);
            int xEnd = (int) Math.floor(xb);
            if (xEnd >= xStart) {
                g.fill(xStart, y, xEnd + 1, y + 1, argb);
            }
        }
    }

    private static double interp(double y0, double x0, double y1, double x1, double y) {
        if (Math.abs(y1 - y0) < 1e-6) return x0;
        return x0 + (x1 - x0) * ((y - y0) / (y1 - y0));
    }
}

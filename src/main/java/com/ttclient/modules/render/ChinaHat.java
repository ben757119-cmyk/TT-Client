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
 * Vape-style china hat — cone tip + filled rim above the head (screen-space).
 */
public class ChinaHat extends Module {
    public final ColorSetting color = addSetting(new ColorSetting("Color", "Hat color", 0xFF00E8A0));
    public final BoolSetting useAccent = addSetting(new BoolSetting("UseAccent", "Use ClickGUI accent color", true));
    public final NumberSetting radius = addSetting(new NumberSetting("Radius", "Rim radius", 0.65, 0.35, 1.4, 0.05));
    public final NumberSetting height = addSetting(new NumberSetting("Height", "Rim above head", 0.18, 0.0, 0.7, 0.02));
    public final NumberSetting tip = addSetting(new NumberSetting("Tip", "Cone tip above rim", 0.35, 0.05, 0.9, 0.05));
    public final BoolSetting others = addSetting(new BoolSetting("Others", "Draw on other players", true));
    public final BoolSetting self = addSetting(new BoolSetting("Self", "Draw on you", true));
    public final NumberSetting segments = addSetting(new NumberSetting("Segments", "Circle quality", 48, 24, 72, 1));
    public final BoolSetting spokes = addSetting(new BoolSetting("Spokes", "Lines from tip to rim", true));

    public ChinaHat() {
        super("ChinaHat", "Vape-style cone hat above heads", Category.RENDER);
    }

    private int hatColor() {
        if (useAccent.get()) return ClickGUIModule.globalAccent();
        int col = color.get();
        if (((col >> 24) & 0xFF) == 0) col |= 0xFF000000;
        return col;
    }

    @Override
    public void onRender2D(GuiGraphicsExtractor g, float partialTick) {
        Minecraft mc = mc();
        if (mc == null || mc.player == null || mc.level == null) return;

        int col = hatColor();

        if (self.get()) drawHat(g, mc.player, col);

        if (others.get()) {
            for (Player pl : mc.level.players()) {
                if (pl == mc.player || !pl.isAlive()) continue;
                if (mc.player.distanceToSqr(pl) > 48 * 48) continue;
                drawHat(g, pl, col);
            }
        }
    }

    private void drawHat(GuiGraphicsExtractor g, Player pl, int col) {
        double x = pl.getX();
        double baseY = pl.getY() + pl.getBbHeight() + height.get();
        double tipY = baseY + tip.get();
        double z = pl.getZ();
        double rad = radius.get();
        int segs = Math.max(16, segments.getInt());

        double[] out = new double[2];
        double[] tipScr = new double[2];
        boolean tipOk = EspProjection.worldToScreen(x, tipY, z, tipScr);

        // Soft fill under the rim (project a few inner rings)
        int fillCol = (col & 0x00FFFFFF) | 0x28000000;
        for (int ring = 3; ring >= 1; ring--) {
            double r = rad * (ring / 3.0);
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
                if (prevX != null) EspDraw.line(g, prevX, prevY, out[0], out[1], fillCol);
                prevX = out[0];
                prevY = out[1];
            }
            if (has && prevX != null) EspDraw.line(g, prevX, prevY, fX, fY, fillCol);
        }

        // Outer rim (double pass for thickness)
        Double prevX = null, prevY = null;
        double firstX = 0, firstY = 0;
        boolean hasFirst = false;
        double[] rimX = new double[segs + 1];
        double[] rimY = new double[segs + 1];
        boolean[] rimOk = new boolean[segs + 1];

        for (int i = 0; i <= segs; i++) {
            double a = (i / (double) segs) * Math.PI * 2.0;
            double px = x + Math.cos(a) * rad;
            double pz = z + Math.sin(a) * rad;
            if (!EspProjection.worldToScreen(px, baseY, pz, out)) {
                rimOk[i] = false;
                prevX = null;
                continue;
            }
            rimX[i] = out[0];
            rimY[i] = out[1];
            rimOk[i] = true;
            if (!hasFirst) {
                firstX = out[0];
                firstY = out[1];
                hasFirst = true;
            }
            if (prevX != null) {
                EspDraw.line(g, prevX, prevY, out[0], out[1], col);
                EspDraw.line(g, prevX + 1, prevY, out[0] + 1, out[1], col);
            }
            prevX = out[0];
            prevY = out[1];
        }
        if (hasFirst && prevX != null) {
            EspDraw.line(g, prevX, prevY, firstX, firstY, col);
        }

        // Spokes tip → rim (every other segment for cleanliness)
        if (spokes.get() && tipOk) {
            int spokeCol = (col & 0x00FFFFFF) | 0xAA000000;
            for (int i = 0; i < segs; i += 2) {
                if (!rimOk[i]) continue;
                EspDraw.line(g, tipScr[0], tipScr[1], rimX[i], rimY[i], spokeCol);
            }
        }

        // Tip dot
        if (tipOk) {
            int cx = (int) tipScr[0];
            int cy = (int) tipScr[1];
            g.fill(cx - 1, cy - 1, cx + 2, cy + 2, col);
        }
    }
}

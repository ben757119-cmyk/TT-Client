package com.ttclient.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Screen-space ESP primitives (Meteor-ish boxes / labels). */
public final class EspDraw {
    private EspDraw() {}

    public static void box(GuiGraphicsExtractor g, double x1, double y1, double x2, double y2, int argb, int thickness) {
        int a = (argb >> 24) & 0xFF;
        if (a == 0) argb = 0xFF000000 | (argb & 0x00FFFFFF);
        int ix1 = (int) Math.floor(Math.min(x1, x2));
        int iy1 = (int) Math.floor(Math.min(y1, y2));
        int ix2 = (int) Math.ceil(Math.max(x1, x2));
        int iy2 = (int) Math.ceil(Math.max(y1, y2));
        // top / bottom / left / right
        g.fill(ix1, iy1, ix2, iy1 + thickness, argb);
        g.fill(ix1, iy2 - thickness, ix2, iy2, argb);
        g.fill(ix1, iy1, ix1 + thickness, iy2, argb);
        g.fill(ix2 - thickness, iy1, ix2, iy2, argb);
    }

    public static void fillBox(GuiGraphicsExtractor g, double x1, double y1, double x2, double y2, int argb) {
        int ix1 = (int) Math.floor(Math.min(x1, x2));
        int iy1 = (int) Math.floor(Math.min(y1, y2));
        int ix2 = (int) Math.ceil(Math.max(x1, x2));
        int iy2 = (int) Math.ceil(Math.max(y1, y2));
        g.fill(ix1, iy1, ix2, iy2, argb);
    }

    public static void label(GuiGraphicsExtractor g, Font font, String text, double x, double y, int argb) {
        int tw = font.width(text);
        int ix = (int) x - tw / 2;
        int iy = (int) y;
        g.fill(ix - 2, iy - 1, ix + tw + 2, iy + 10, 0x88000000);
        g.text(font, text, ix, iy, argb, false);
    }

    public static void line(GuiGraphicsExtractor g, double x1, double y1, double x2, double y2, int argb) {
        // Bresenham-ish thick line using fills
        int steps = (int) Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps < 1) steps = 1;
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            int x = (int) (x1 + (x2 - x1) * t);
            int y = (int) (y1 + (y2 - y1) * t);
            g.fill(x, y, x + 1, y + 1, argb);
        }
    }
}

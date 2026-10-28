package com.ttclient.gui;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.modules.ModuleManager;
import com.ttclient.settings.Setting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Layout render helpers — soft rounded Classic/Meteor/Wurst + petal Radial. */
final class ClickGUILayouts {
    private ClickGUILayouts() {}

    private static void round(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int r, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        if (r < 1) { g.fill(x1, y1, x2, y2, color); return; }
        r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        g.fill(x1 + r, y1, x2 - r, y2, color);
        g.fill(x1, y1 + r, x1 + r, y2 - r, color);
        g.fill(x2 - r, y1 + r, x2, y2 - r, color);
        for (int i = 0; i < r; i++) {
            int o = r - (int) Math.sqrt(r * r - (r - i) * (r - i) + 0.5);
            g.fill(x1 + o, y1 + i, x1 + r, y1 + i + 1, color);
            g.fill(x2 - r, y1 + i, x2 - o, y1 + i + 1, color);
            g.fill(x1 + o, y2 - i - 1, x1 + r, y2 - i, color);
            g.fill(x2 - r, y2 - i - 1, x2 - o, y2 - i, color);
        }
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF;
        int ra = (int) (aa + (ba - aa) * t);
        int rr = (int) (ar + (br - ar) * t);
        int rg = (int) (ag + (bg - ag) * t);
        int rb = (int) (ab + (bb - ab) * t);
        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }

    private static float animPulse() {
        return 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 420.0);
    }

    /** Classic — soft rounded category panels. */
    static void renderClassic(GuiGraphicsExtractor g, Font font, int mx, int my, int ac,
                              Map<Category, int[]> panels, ModuleManager m, List<String> tips) {
        if (m == null || panels == null) return;
        int panelBg = 0xE8121622;
        int headerBg = 0xF0161A28;
        for (var e : panels.entrySet()) {
            Category cat = e.getKey();
            if (m.getByCategory(cat).isEmpty()) continue;
            int[] p = e.getValue();
            int px = p[0], py = p[1], open = p[2];
            int pw = 118;
            int ph = 24 + (open != 0 ? m.getByCategory(cat).size() * 26 : 0);
            round(g, px, py, px + pw, py + ph, 10, panelBg);
            round(g, px, py, px + pw, py + 24, 10, headerBg);
            g.fill(px + 10, py + 22, px + pw - 10, py + 23, (ac & 0x00FFFFFF) | 0x88000000);
            g.text(font, cat.name, px + 10, py + 8, 0xFFF2F4F8, false);
            g.text(font, open != 0 ? "▾" : "▸", px + pw - 14, py + 8, 0xFF9AA3B5, false);
            if (open != 0) {
                int y = py + 28;
                for (Module mod : m.getByCategory(cat)) {
                    boolean on = mod.isEnabled();
                    boolean hov = mx >= px + 4 && mx < px + pw - 4 && my >= y && my < y + 24;
                    if (hov) round(g, px + 4, y, px + pw - 4, y + 24, 6, 0x28FFFFFF);
                    if (on) g.fill(px + 6, y + 6, px + 8, y + 18, ac);
                    g.text(font, mod.getName(), px + 14, y + 8, on ? 0xFFF2F4F8 : 0xFF9AA3B5, false);
                    if (hov && mod.getDescription() != null) tips.add(mod.getDescription());
                    y += 26;
                }
            }
        }
    }

    /** Meteor — compact stacked columns. */
    static void renderMeteor(GuiGraphicsExtractor g, Font font, int mx, int my, int ac,
                             Map<Category, int[]> panels, ModuleManager m, List<String> tips) {
        if (m == null || panels == null) return;
        for (Category c : Category.values()) {
            if (m.getByCategory(c).isEmpty()) continue;
            int[] p = panels.get(c);
            if (p == null) continue;
            int px = p[0], py = p[1], pw = 108;
            List<Module> mods = m.getByCategory(c);
            int ph = 22 + mods.size() * 15;
            round(g, px, py, px + pw, py + ph, 8, 0xE00C0E14);
            round(g, px, py, px + pw, py + 20, 8, 0xFF141820);
            g.fill(px + 8, py + 18, px + pw - 8, py + 19, ac);
            g.text(font, c.name, px + 8, py + 6, 0xFFE8ECF4, false);
            int y = py + 22;
            for (Module mod : mods) {
                boolean on = mod.isEnabled();
                boolean hov = mx >= px + 2 && mx < px + pw - 2 && my >= y && my < y + 15;
                if (hov) g.fill(px + 2, y, px + pw - 2, y + 15, 0x22FFFFFF);
                g.text(font, mod.getName(), px + 8, y + 3, on ? ac : 0xFFA8B0C0, false);
                if (hov && mod.getDescription() != null) tips.add(mod.getDescription());
                y += 15;
            }
        }
    }

    /** Wurst — light gray tab window. */
    static void renderWurst(GuiGraphicsExtractor g, Font font, int width, int height,
                            Category selected, Module selectedModule, int listScroll,
                            ModuleManager m, List<int[]> catBounds, List<Module> visible) {
        int winW = Math.min(520, width - 40);
        int winH = Math.min(380, height - 50);
        int winX = (width - winW) / 2;
        int winY = (height - winH) / 2 - 8;
        round(g, winX, winY, winX + winW, winY + winH, 8, 0xF0C8CCD4);
        round(g, winX + 2, winY + 2, winX + winW - 2, winY + winH - 2, 6, 0xF0DCE0E8);
        g.fill(winX, winY, winX + winW, winY + 22, 0xFF3A3F4A);
        g.text(font, "TT Client", winX + 10, winY + 7, 0xFFFFFFFF, false);
        int tx = winX + 6, ty = winY + 26;
        if (m != null) {
            for (Category cat : Category.values()) {
                if (m.getByCategory(cat).isEmpty()) continue;
                int tw = font.width(cat.name) + 12;
                boolean sel = cat == selected;
                round(g, tx, ty, tx + tw, ty + 16, 4, sel ? 0xFF6A7388 : 0xFF505868);
                g.text(font, cat.name, tx + 6, ty + 4, 0xFFFFFFFF, false);
                catBounds.add(new int[]{tx, ty, tx + tw, ty + 16, cat.ordinal()});
                tx += tw + 3;
            }
        }
        int lx = winX + 8, ly = winY + 48, lw = winW / 2 - 14, lh = winH - 78;
        round(g, lx, ly, lx + lw, ly + lh, 6, 0xFFF8F9FC);
        int y = ly + 6 - listScroll;
        for (Module mod : visible) {
            if (y + 14 < ly) { y += 14; continue; }
            if (y > ly + lh) break;
            boolean sel = mod == selectedModule;
            boolean on = mod.isEnabled();
            if (sel) round(g, lx + 2, y, lx + lw - 2, y + 14, 3, 0xFF3B82F6);
            g.text(font, mod.getName(), lx + 6, y + 3, on ? 0xFF0A7A3A : 0xFF1A1D24, false);
            y += 14;
        }
        int rx = winX + winW / 2 + 6;
        round(g, rx, ly, winX + winW - 8, ly + lh, 6, 0xFFF8F9FC);
        if (selectedModule != null) {
            g.text(font, selectedModule.getName(), rx + 6, ly + 6, 0xFF111318, false);
            int sy = ly + 22;
            int bind = selectedModule.getKeyBind();
            g.text(font, "Bind: " + (bind > 0 ? ("KEY" + bind) : "None"), rx + 6, sy, 0xFF444A58, false);
            sy += 14;
            for (Setting<?> s : selectedModule.getSettings()) {
                g.text(font, s.getName() + ": " + String.valueOf(s.get()), rx + 6, sy, 0xFF2A3040, false);
                sy += 12;
                if (sy > ly + lh - 10) break;
            }
        } else {
            g.text(font, "Select a module", rx + 6, ly + 6, 0xFF6A7280, false);
        }
    }

    /**
     * Radial — soft petal ring like a pie menu (screenshot reference).
     * Translucent category petals around center, hover expands, open fans modules.
     */
    static void renderRadial(GuiGraphicsExtractor g, Font font, int mx, int my, int ac,
                             int width, int height, Category selected, Category radialOpen,
                             ModuleManager m, List<int[]> catBounds, List<String> tips) {
        int cx = width / 2;
        int cy = height / 2 - 6;

        List<Category> cats = new ArrayList<>();
        if (m != null) {
            for (Category c : Category.values()) {
                if (!m.getByCategory(c).isEmpty()) cats.add(c);
            }
        }
        int n = Math.max(1, cats.size());

        // Soft vignette
        g.fill(0, 0, width, height, 0x66050810);

        float pulse = animPulse();
        int outerR = Math.min(width, height) * 22 / 100;
        int innerR = Math.max(28, outerR * 38 / 100);

        // Accent-tinted petal base (soft teal/green like reference, driven by accent)
        int petalBase = (ac & 0x00FFFFFF) | 0x55000000;
        int petalHover = (ac & 0x00FFFFFF) | 0x99000000;
        int petalSel = (ac & 0x00FFFFFF) | 0xBB000000;

        Category hoverCat = null;
        double bestAngDist = 999;

        // Hit-test angle from center
        double mouseAng = Math.atan2(my - cy, mx - cx);
        double mouseDist = Math.hypot(mx - cx, my - cy);

        for (int i = 0; i < n; i++) {
            Category cat = cats.get(i);
            double a0 = -Math.PI / 2 + (Math.PI * 2 * i / n) - (Math.PI / n) * 0.92;
            double a1 = -Math.PI / 2 + (Math.PI * 2 * (i + 1) / n) + (Math.PI / n) * 0.08;
            // normalize mouse into segment
            double mid = (a0 + a1) * 0.5;
            double half = (a1 - a0) * 0.5;
            double dang = Math.atan2(Math.sin(mouseAng - mid), Math.cos(mouseAng - mid));
            boolean inRing = mouseDist >= innerR * 0.85 && mouseDist <= outerR * 1.15;
            boolean hov = inRing && Math.abs(dang) <= half;
            boolean sel = cat == selected || cat == radialOpen;
            if (hov && Math.abs(dang) < bestAngDist) {
                bestAngDist = Math.abs(dang);
                hoverCat = cat;
            }

            int col = sel ? petalSel : (hov ? petalHover : petalBase);
            // Slight pulse on selected
            if (sel) {
                int extra = (int) (18 * pulse);
                col = (Math.min(255, ((col >>> 24) & 0xFF) + extra) << 24) | (col & 0x00FFFFFF);
            }

            drawPetal(g, cx, cy, innerR, outerR + (hov || sel ? 8 : 0), a0, a1, col);

            // Label at petal mid
            double labelR = (innerR + outerR) * 0.55;
            int lx = cx + (int) (Math.cos(mid) * labelR);
            int ly = cy + (int) (Math.sin(mid) * labelR);
            String name = cat.name;
            int tw = font.width(name);
            int labelCol = (sel || hov) ? 0xFFF8FAFC : 0xE0E8EEF6;
            // Soft label plate
            round(g, lx - tw / 2 - 6, ly - 7, lx + tw / 2 + 6, ly + 8, 6,
                    sel ? 0x66000000 : 0x44000000);
            g.text(font, name, lx - tw / 2, ly - 3, labelCol, false);

            // Approximate bounds for click (arc bbox)
            int bx1 = cx + (int) (Math.cos(a0) * outerR);
            int by1 = cy + (int) (Math.sin(a0) * outerR);
            int bx2 = cx + (int) (Math.cos(a1) * outerR);
            int by2 = cy + (int) (Math.sin(a1) * outerR);
            int mxp = cx + (int) (Math.cos(mid) * ((innerR + outerR) / 2.0));
            int myp = cy + (int) (Math.sin(mid) * ((innerR + outerR) / 2.0));
            int pad = 28;
            catBounds.add(new int[]{
                    Math.min(mxp, Math.min(bx1, bx2)) - pad,
                    Math.min(myp, Math.min(by1, by2)) - pad,
                    Math.max(mxp, Math.max(bx1, bx2)) + pad,
                    Math.max(myp, Math.max(by1, by2)) + pad,
                    cat.ordinal()
            });
        }

        // Center hub — soft glass disc
        int hubR = innerR - 6;
        round(g, cx - hubR, cy - hubR, cx + hubR, cy + hubR, hubR, 0xCC0A0E16);
        round(g, cx - hubR + 3, cy - hubR + 3, cx + hubR - 3, cy + hubR - 3, hubR - 3, 0xEE121826);
        // Accent ring
        int ringA = 0x55FFFFFF & 0xFF000000 | (ac & 0x00FFFFFF);
        // thin ring approx
        for (int t = 0; t < 2; t++) {
            int rr = hubR - 2 - t;
            // sparse points
            for (int k = 0; k < 48; k++) {
                double a = k * Math.PI * 2 / 48.0;
                int px = cx + (int) (Math.cos(a) * rr);
                int py = cy + (int) (Math.sin(a) * rr);
                g.fill(px, py, px + 1, py + 1, (ac & 0x00FFFFFF) | 0xAA000000);
            }
        }
        String hub = "TT";
        g.text(font, hub, cx - font.width(hub) / 2, cy - 4, ac, false);

        // Module fan when a category is open
        Category open = radialOpen != null ? radialOpen : (hoverCat != null ? hoverCat : selected);
        if (open != null && m != null) {
            List<Module> mods = m.getByCategory(open);
            int cols = 1;
            int cardW = 168;
            int cardH = 22;
            int gap = 4;
            int totalH = Math.min(mods.size(), 10) * (cardH + gap) + 28;
            int panelX = cx - cardW / 2;
            int panelY = cy + outerR + 28;
            if (panelY + totalH > height - 40) panelY = cy - outerR - totalH - 16;

            round(g, panelX - 8, panelY - 8, panelX + cardW + 8, panelY + totalH + 4, 12, 0xE010141E);
            round(g, panelX - 6, panelY - 6, panelX + cardW + 6, panelY + totalH + 2, 10, 0xF0161A26);
            g.text(font, open.name, panelX + 6, panelY, ac, false);

            int y = panelY + 16;
            int shown = 0;
            for (Module mod : mods) {
                if (shown >= 10) break;
                boolean on = mod.isEnabled();
                boolean hov = mx >= panelX && mx < panelX + cardW && my >= y && my < y + cardH;
                int bg = hov ? 0xFF222836 : 0xFF1A1E28;
                round(g, panelX, y, panelX + cardW, y + cardH, 7, bg);
                if (on) g.fill(panelX + 4, y + 5, panelX + 6, y + cardH - 5, ac);
                g.text(font, mod.getName(), panelX + 12, y + 7, on ? 0xFFF2F4F8 : 0xFF9AA3B5, false);
                // mini toggle
                int tsx = panelX + cardW - 30;
                round(g, tsx, y + 5, tsx + 22, y + 17, 6, on ? ((ac & 0x00FFFFFF) | 0xCC000000) : 0xFF2A3140);
                int knob = on ? tsx + 11 : tsx + 2;
                round(g, knob, y + 7, knob + 9, y + 15, 4, on ? 0xFFF8FAFC : 0xFF8A93A5);
                if (hov && mod.getDescription() != null) tips.add(mod.getDescription());
                y += cardH + gap;
                shown++;
            }
        }
    }

    /** Filled annular sector approximated with radial slices (smooth petal). */
    private static void drawPetal(GuiGraphicsExtractor g, int cx, int cy,
                                  int innerR, int outerR, double a0, double a1, int color) {
        if (a1 <= a0) return;
        // denser steps for smoother edges
        int steps = Math.max(12, (int) ((a1 - a0) * 28));
        for (int i = 0; i < steps; i++) {
            double t0 = a0 + (a1 - a0) * i / steps;
            double t1 = a0 + (a1 - a0) * (i + 1) / steps;
            // draw a thin trapezoid as stacked horizontal spans along the mid ray
            double mid = (t0 + t1) * 0.5;
            double cos = Math.cos(mid);
            double sin = Math.sin(mid);
            // width of slice at outer
            double slice = (a1 - a0) / steps;
            for (int r = innerR; r < outerR; r += 1) {
                double halfW = Math.max(1.0, r * slice * 0.55);
                // perpendicular
                double px = -sin;
                double py = cos;
                int x1 = cx + (int) (cos * r + px * halfW);
                int y1 = cy + (int) (sin * r + py * halfW);
                int x2 = cx + (int) (cos * r - px * halfW);
                int y2 = cy + (int) (sin * r - py * halfW);
                // small fill blob
                int minx = Math.min(x1, x2);
                int maxx = Math.max(x1, x2);
                int miny = Math.min(y1, y2);
                int maxy = Math.max(y1, y2);
                if (maxx - minx < 1) maxx = minx + 1;
                if (maxy - miny < 1) maxy = miny + 1;
                g.fill(minx, miny, maxx + 1, maxy + 1, color);
            }
        }
    }
}

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

/** Layout render helpers for Classic, Meteor, Wurst, Radial styles. */
final class ClickGUILayouts {
    private ClickGUILayouts() {}

    /** Classic — draggable category panels. panel = int[]{x, y, open(0/1)}. */
    static void renderClassic(GuiGraphicsExtractor g, Font font, int mx, int my, int ac,
                              Map<Category, int[]> panels, ModuleManager m, List<String> tips) {
        if (m == null || panels == null) return;
        for (var e : panels.entrySet()) {
            Category cat = e.getKey();
            if (m.getByCategory(cat).isEmpty()) continue;
            int[] p = e.getValue();
            int px = p[0], py = p[1], open = p[2];
            int pw = 110;
            int ph = 22 + (open != 0 ? m.getByCategory(cat).size() * 28 : 0);
            g.fill(px, py, px + pw, py + 22, 0xFF0E1A16);
            g.fill(px, py + 20, px + pw, py + 22, ac);
            g.text(font, cat.name, px + 8, py + 7, 0xFFF0F2F5, false);
            g.text(font, open != 0 ? "-" : "+", px + pw - 12, py + 7, 0xFF8E96A8, false);
            if (open != 0) {
                g.fill(px, py + 22, px + pw, py + ph, 0xF0121620);
                int y = py + 24;
                for (Module mod : m.getByCategory(cat)) {
                    boolean on = mod.isEnabled();
                    boolean hov = mx >= px && mx < px + pw && my >= y && my < y + 28;
                    if (hov) g.fill(px + 2, y, px + pw - 2, y + 28, 0x18FFFFFF);
                    if (on) g.fill(px + 2, y + 4, px + 4, y + 24, ac);
                    g.text(font, mod.getName(), px + 8, y + 9, on ? 0xFFF0F2F5 : 0xFF8E96A8, false);
                    if (hov && mod.getDescription() != null) tips.add(mod.getDescription());
                    y += 28;
                }
            }
        }
    }

    /** Meteor — compact stacked columns, always expanded. */
    static void renderMeteor(GuiGraphicsExtractor g, Font font, int mx, int my, int ac,
                             Map<Category, int[]> panels, ModuleManager m, List<String> tips) {
        if (m == null || panels == null) return;
        for (Category c : Category.values()) {
            if (m.getByCategory(c).isEmpty()) continue;
            int[] p = panels.get(c);
            if (p == null) continue;
            int px = p[0], py = p[1], pw = 100;
            List<Module> mods = m.getByCategory(c);
            int ph = 20 + mods.size() * 16;
            g.fill(px, py, px + pw, py + ph, 0xE00A0A0A);
            g.fill(px, py, px + pw, py + 18, 0xFF111111);
            g.fill(px, py + 17, px + pw, py + 18, ac);
            g.text(font, c.name, px + 6, py + 5, 0xFFE0E0E0, false);
            int y = py + 20;
            for (Module mod : mods) {
                boolean on = mod.isEnabled();
                boolean hov = mx >= px && mx < px + pw && my >= y && my < y + 16;
                if (hov) g.fill(px + 1, y, px + pw - 1, y + 16, 0x22FFFFFF);
                g.text(font, mod.getName(), px + 6, y + 4, on ? ac : 0xFFAAAAAA, false);
                if (hov && mod.getDescription() != null) tips.add(mod.getDescription());
                y += 16;
            }
        }
    }

    /** Wurst — light gray tab window, left module list, right settings. */
    static void renderWurst(GuiGraphicsExtractor g, Font font, int width, int height,
                            Category selected, Module selectedModule, int listScroll,
                            ModuleManager m, List<int[]> catBounds, List<Module> visible) {
        int winW = Math.min(520, width - 40);
        int winH = Math.min(380, height - 50);
        int winX = (width - winW) / 2;
        int winY = (height - winH) / 2 - 8;
        g.fill(winX, winY, winX + winW, winY + winH, 0xF0C0C0C0);
        g.fill(winX + 2, winY + 2, winX + winW - 2, winY + winH - 2, 0xF0D8D8D8);
        g.fill(winX, winY, winX + winW, winY + 20, 0xFF404040);
        g.text(font, "TT Client", winX + 8, winY + 6, 0xFFFFFFFF, false);
        int tx = winX + 4, ty = winY + 22;
        if (m != null) {
            for (Category cat : Category.values()) {
                if (m.getByCategory(cat).isEmpty()) continue;
                int tw = font.width(cat.name) + 10;
                boolean sel = cat == selected;
                g.fill(tx, ty, tx + tw, ty + 16, sel ? 0xFF808080 : 0xFF606060);
                g.text(font, cat.name, tx + 5, ty + 4, 0xFFFFFFFF, false);
                catBounds.add(new int[]{tx, ty, tx + tw, ty + 16, cat.ordinal()});
                tx += tw + 2;
            }
        }
        int lx = winX + 6, ly = winY + 42, lw = winW / 2 - 10, lh = winH - 70;
        g.fill(lx, ly, lx + lw, ly + lh, 0xFFFFFFFF);
        int y = ly + 4 - listScroll;
        for (Module mod : visible) {
            if (y + 14 < ly) { y += 14; continue; }
            if (y > ly + lh) break;
            boolean sel = mod == selectedModule;
            boolean on = mod.isEnabled();
            if (sel) g.fill(lx + 1, y, lx + lw - 1, y + 14, 0xFF3399FF);
            g.text(font, mod.getName(), lx + 4, y + 3, on ? 0xFF006600 : 0xFF000000, false);
            y += 14;
        }
        int rx = winX + winW / 2 + 4;
        g.fill(rx, ly, winX + winW - 6, ly + lh, 0xFFFFFFFF);
        if (selectedModule != null) {
            g.text(font, selectedModule.getName(), rx + 4, ly + 4, 0xFF000000, false);
            int sy = ly + 20;
            int bind = selectedModule.getKeyBind();
            g.text(font, "Bind: " + (bind > 0 ? ("KEY" + bind) : "None"), rx + 4, sy, 0xFF333333, false);
            sy += 14;
            for (Setting<?> s : selectedModule.getSettings()) {
                g.text(font, s.getName() + ": " + String.valueOf(s.get()), rx + 4, sy, 0xFF222222, false);
                sy += 12;
                if (sy > ly + lh - 10) break;
            }
        } else {
            g.text(font, "Select a module", rx + 4, ly + 4, 0xFF666666, false);
        }
    }

    /** Radial — center hub with category orbit + fan module list. */
    static void renderRadial(GuiGraphicsExtractor g, Font font, int mx, int my, int ac,
                             int width, int height, Category selected, Category radialOpen,
                             ModuleManager m, List<int[]> catBounds, List<String> tips) {
        int cx = width / 2, cy = height / 2 - 10;
        int ringR = Math.min(width, height) / 5;
        g.fill(cx - 36, cy - 36, cx + 36, cy + 36, 0xF010121A);
        g.fill(cx - 30, cy - 30, cx + 30, cy + 30, 0xFF161922);
        g.text(font, "TT", cx - font.width("TT") / 2, cy - 4, ac, false);
        List<Category> cats = new ArrayList<>();
        if (m != null) {
            for (Category c : Category.values()) {
                if (!m.getByCategory(c).isEmpty()) cats.add(c);
            }
        }
        int n = Math.max(1, cats.size());
        for (int i = 0; i < n; i++) {
            Category cat = cats.get(i);
            double ang = -Math.PI / 2 + (Math.PI * 2 * i / n);
            int bx = cx + (int) (Math.cos(ang) * ringR);
            int by = cy + (int) (Math.sin(ang) * ringR);
            boolean sel = cat == selected || cat == radialOpen;
            int bw = font.width(cat.name) + 20;
            int bg = sel ? ((ac & 0x00FFFFFF) | 0x55000000) : 0xFF161922;
            g.fill(bx - bw / 2, by - 11, bx + bw / 2, by + 11, bg);
            g.text(font, cat.name, bx - font.width(cat.name) / 2, by - 4, sel ? 0xFFF0F2F5 : 0xFF8E96A8, false);
            catBounds.add(new int[]{bx - bw / 2, by - 11, bx + bw / 2, by + 11, cat.ordinal()});
        }
        Category open = radialOpen != null ? radialOpen : selected;
        if (open != null && m != null) {
            List<Module> mods = m.getByCategory(open);
            int my0 = cy + ringR + 40;
            int mw = 160, mx0 = cx - mw / 2;
            int mh = Math.min(mods.size() * 22 + 8, 220);
            g.fill(mx0, my0, mx0 + mw, my0 + mh, 0xF010121A);
            g.text(font, open.name, mx0 + 10, my0 + 6, ac, false);
            int y = my0 + 20;
            for (Module mod : mods) {
                if (y + 20 > my0 + mh) break;
                boolean on = mod.isEnabled();
                boolean hov = mx >= mx0 + 4 && mx < mx0 + mw - 4 && my >= y && my < y + 20;
                if (hov) g.fill(mx0 + 4, y, mx0 + mw - 4, y + 20, 0xFF1C2030);
                if (on) g.fill(mx0 + 6, y + 4, mx0 + 8, y + 16, ac);
                g.text(font, mod.getName(), mx0 + 14, y + 6, on ? 0xFFF0F2F5 : 0xFF8E96A8, false);
                if (hov && mod.getDescription() != null) tips.add(mod.getDescription());
                y += 20;
            }
        }
    }
}

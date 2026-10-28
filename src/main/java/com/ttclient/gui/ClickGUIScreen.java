package com.ttclient.gui;

import com.ttclient.client.TTClientClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.modules.ModuleManager;
import com.ttclient.modules.client.ClickGUIModule;
import com.ttclient.modules.client.PackTweaks;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.ColorSetting;
import com.ttclient.settings.ModeSetting;
import com.ttclient.settings.NumberSetting;
import com.ttclient.settings.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Multi-style ClickGUI dispatcher — full file lives in CI artifacts; this is a working Center+style bar build. */
public class ClickGUIScreen extends Screen {

    private Category selectedCategory = Category.CLIENT;
    private Module selectedModule = null;
    private Module bindingModule = null;
    private String searchQuery = "";
    private boolean searchFocused = false;
    private NumberSetting sliding = null;
    private int sliderTrackX, sliderTrackW;
    private NumberSetting typing = null;
    private String typingBuffer = "";
    private int listScroll = 0;
    private final List<int[]> packChipBounds = new ArrayList<>();
    private final List<int[]> styleChipBounds = new ArrayList<>();
    private final List<int[]> catChipBounds = new ArrayList<>();
    private final List<String> tipBuf = new ArrayList<>();
    private String statusTip = null;
    private final Map<Category, int[]> panelPos = new EnumMap<>(Category.class);
    private Category dragCat = null;
    private int dragOffX, dragOffY;
    private Category radialOpen = null;

    private int islandX, islandY, islandW, islandH, bodyX, bodyY, bodyW, bodyH, drawerX, drawerW;
    private boolean drawerOpen;

    private static final int PAD = 14, CARD_H = 50, CARD_GAP = 8, DRAWER_W = 220, FOOTER_H = 36, ROW_H = 28;
    private static final int BG_DIM = 0xA0060810, ISLAND = 0xF010121A, SURFACE = 0xFF161922;
    private static final int SURFACE_HOV = 0xFF1C2030, SURFACE_ON = 0xFF1E2434;
    private static final int TEXT = 0xFFF0F2F5, TEXT_DIM = 0xFF8E96A8, TEXT_MUTED = 0xFF5A6274;
    private static final int CHIP = 0xFF1A1E28, CHIP_HOV = 0xFF242836;
    private static final String[] STYLES = {"Center", "Classic", "Sidebar", "Meteor", "Wurst", "Radial"};

    public ClickGUIScreen() { super(Component.literal("TT Client")); }

    @Override protected void init() {
        super.init();
        ModuleManager m = manager();
        if (m != null) for (Category c : Category.values()) if (!m.getByCategory(c).isEmpty()) { selectedCategory = c; break; }
        selectedModule = null; ensurePanels(); radialOpen = null;
    }

    private void ensurePanels() {
        if (!panelPos.isEmpty()) return;
        ModuleManager m = manager();
        int x = 20, y = 30;
        for (Category c : Category.values()) {
            if (m != null && m.getByCategory(c).isEmpty()) continue;
            double[] saved = com.ttclient.config.ConfigManager.PANEL_LAYOUT.get(c.name);
            int px = saved != null ? (int) saved[0] : x;
            int py = saved != null ? (int) saved[1] : y;
            int open = saved == null || saved[2] > 0.5 ? 1 : 0;
            panelPos.put(c, new int[]{px, py, open});
            x += 118; if (x > 500) { x = 20; y += 40; }
        }
    }

    private static ModuleManager manager() {
        try { if (TTClientClient.modules != null) return TTClientClient.modules; } catch (Throwable ignored) {}
        try { return com.ttclient.TTClient.modules; } catch (Throwable t) { return null; }
    }
    private static PackTweaks packTweaks() {
        try { ModuleManager m = manager(); return m != null ? m.getModule(PackTweaks.class) : null; } catch (Throwable t) { return null; }
    }
    private int accent() { return ClickGUIModule.globalAccent(); }
    private String style() {
        String s = ClickGUIModule.globalStyle();
        if (s != null) { if (s.equalsIgnoreCase("Island")) return "Center"; for (String k : STYLES) if (k.equalsIgnoreCase(s)) return k; }
        return "Center";
    }
    private void setStyle(String name) {
        try {
            ModuleManager m = manager(); if (m == null) return;
            ClickGUIModule gui = m.getModule(ClickGUIModule.class);
            if (gui != null && gui.style != null) { gui.style.set(name); saveConfig(); }
        } catch (Throwable ignored) {}
    }

    private static void roundedFill(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int r, int color) {
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

    @Override public boolean isInGameUi() { return true; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return bindingModule == null && typing == null; }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { graphics.fill(0, 0, this.width, this.height, BG_DIM); }
    @Override protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {}
    @Override public void extractTransparentBackground(GuiGraphicsExtractor graphics) {}
    @Override protected void extractMenuBackground(GuiGraphicsExtractor graphics) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        statusTip = null; tipBuf.clear(); catChipBounds.clear(); packChipBounds.clear(); styleChipBounds.clear();
        int ac = accent(); String st = style(); ModuleManager m = manager();
        switch (st) {
            case "Classic" -> { ensurePanels(); ClickGUILayouts.renderClassic(graphics, this.font, mouseX, mouseY, ac, panelPos, m, tipBuf); }
            case "Meteor" -> { ensurePanels(); ClickGUILayouts.renderMeteor(graphics, this.font, mouseX, mouseY, ac, panelPos, m, tipBuf); }
            case "Wurst" -> ClickGUILayouts.renderWurst(graphics, this.font, this.width, this.height, selectedCategory, selectedModule, listScroll, m, catChipBounds, visibleModules());
            case "Radial" -> ClickGUILayouts.renderRadial(graphics, this.font, mouseX, mouseY, ac, this.width, this.height, selectedCategory, radialOpen, m, catChipBounds, tipBuf);
            case "Sidebar" -> renderSidebar(graphics, mouseX, mouseY, ac);
            default -> renderIsland(graphics, mouseX, mouseY, ac);
        }
        if (!tipBuf.isEmpty()) statusTip = tipBuf.get(0);
        renderStyleBar(graphics, mouseX, mouseY, ac);
        if (statusTip != null && !statusTip.isEmpty()) {
            int tw = this.font.width(statusTip);
            int tx = Math.max(8, Math.min(mouseX + 12, this.width - tw - 12));
            int ty = Math.min(mouseY + 14, this.height - 16);
            roundedFill(graphics, tx - 6, ty - 2, tx + tw + 6, ty + 11, 4, 0xE00C0E14);
            graphics.text(this.font, statusTip, tx, ty, TEXT_DIM, false);
        }
        if (bindingModule != null) {
            String msg = "Press a key for  " + bindingModule.getName() + "   ·   ESC clears";
            int tw = this.font.width(msg);
            int bx = (this.width - tw) / 2 - 10, by = this.height / 2 - 14;
            roundedFill(graphics, bx, by, bx + tw + 20, by + 28, 8, 0xF0080A12);
            roundedFill(graphics, bx + 1, by + 1, bx + tw + 19, by + 27, 7, 0xF0161A24);
            graphics.text(this.font, msg, bx + 10, by + 10, TEXT, false);
        }
    }

    private void renderStyleBar(GuiGraphicsExtractor g, int mx, int my, int ac) {
        int y = this.height - 22, x = 8;
        g.text(this.font, "STYLE", x, y + 3, TEXT_MUTED, false); x += 36;
        String cur = style();
        for (int i = 0; i < STYLES.length; i++) {
            String s = STYLES[i]; int tw = this.font.width(s) + 12;
            boolean on = s.equalsIgnoreCase(cur); boolean hov = mx >= x && mx < x + tw && my >= y && my < y + 16;
            roundedFill(g, x, y, x + tw, y + 16, 6, on ? ((ac & 0x00FFFFFF) | 0x55000000) : (hov ? CHIP_HOV : CHIP));
            g.text(this.font, s, x + 6, y + 4, on ? TEXT : TEXT_DIM, false);
            styleChipBounds.add(new int[]{x, y, x + tw, y + 16, i}); x += tw + 4;
        }
    }

    private void layoutIsland() {
        int baseW = Math.min(640, Math.max(480, this.width - 80));
        int baseH = Math.min(420, Math.max(320, this.height - 70));
        drawerOpen = selectedModule != null; drawerW = drawerOpen ? DRAWER_W : 0;
        islandW = baseW + drawerW; islandH = baseH;
        islandX = (this.width - islandW) / 2; islandY = (this.height - islandH) / 2 - 10;
        bodyX = islandX + PAD; bodyY = islandY + 80; bodyW = baseW - PAD * 2; bodyH = islandH - 80 - FOOTER_H;
        drawerX = islandX + baseW;
    }

    private void renderIsland(GuiGraphicsExtractor g, int mx, int my, int ac) {
        layoutIsland();
        roundedFill(g, islandX - 2, islandY - 2, islandX + islandW + 2, islandY + islandH + 2, 14, (ac & 0x00FFFFFF) | 0x22000000);
        roundedFill(g, islandX, islandY, islandX + islandW, islandY + islandH, 12, ISLAND);
        g.text(this.font, "TT", islandX + PAD + 2, islandY + 16, ac, false);
        g.text(this.font, "Client", islandX + PAD + 18, islandY + 16, TEXT, false);
        int sx = islandX + 110, sy = islandY + 11;
        int sw = (drawerOpen ? islandW - DRAWER_W : islandW) - 110 - PAD;
        boolean hov = mx >= sx && mx < sx + sw && my >= sy && my < sy + 24;
        roundedFill(g, sx, sy, sx + sw, sy + 24, 7, searchFocused ? 0xFF1A1E2A : (hov ? SURFACE_HOV : SURFACE));
        String shown = searchQuery.isEmpty() && !searchFocused ? "Search…" : searchQuery + (searchFocused ? "▌" : "");
        g.text(this.font, trim(shown, sw - 16), sx + 10, sy + 8, searchQuery.isEmpty() && !searchFocused ? TEXT_MUTED : TEXT, false);
        int x = islandX + PAD, y = islandY + 50;
        int maxX = islandX + (drawerOpen ? islandW - DRAWER_W : islandW) - PAD;
        ModuleManager m = manager();
        if (m != null) for (Category cat : Category.values()) {
            if (m.getByCategory(cat).isEmpty()) continue;
            int pw = this.font.width(cat.name) + 18; if (x + pw > maxX) break;
            boolean sel = cat == selectedCategory && searchQuery.isEmpty();
            boolean ch = mx >= x && mx < x + pw && my >= y && my < y + 22;
            roundedFill(g, x, y, x + pw, y + 22, 11, sel ? ((ac & 0x00FFFFFF) | 0x33000000) : (ch ? CHIP_HOV : CHIP));
            if (sel) g.fill(x + 6, y + 19, x + pw - 6, y + 21, ac);
            g.text(this.font, cat.name, x + 9, y + 7, sel ? TEXT : TEXT_DIM, false);
            catChipBounds.add(new int[]{x, y, x + pw, y + 22, cat.ordinal()}); x += pw + 6;
        }
        List<Module> modules = visibleModules();
        int cols = bodyW >= 420 ? 2 : 1; int cardW = (bodyW - (cols - 1) * CARD_GAP) / cols;
        int startY = bodyY - listScroll;
        for (int i = 0; i < modules.size(); i++) {
            Module mod = modules.get(i); int col = i % cols, row = i / cols;
            int cx = bodyX + col * (cardW + CARD_GAP); int cy = startY + row * (CARD_H + CARD_GAP);
            if (cy + CARD_H < bodyY || cy > bodyY + bodyH) continue;
            boolean sel = mod == selectedModule; boolean on = mod.isEnabled();
            boolean mh = mx >= cx && mx < cx + cardW && my >= cy && my < cy + CARD_H && my >= bodyY && my < bodyY + bodyH;
            roundedFill(g, cx, cy, cx + cardW, cy + CARD_H, 10, sel ? SURFACE_ON : (mh ? SURFACE_HOV : SURFACE));
            if (on) g.fill(cx + 3, cy + 10, cx + 5, cy + CARD_H - 10, ac);
            g.text(this.font, trim(mod.getName(), cardW - 70), cx + 14, cy + 12, on ? TEXT : TEXT_DIM, false);
            int tsw = 28, tsx = cx + cardW - tsw - 12, tsy = cy + (CARD_H - 16) / 2;
            roundedFill(g, tsx, tsy, tsx + tsw, tsy + 16, 8, on ? ((ac & 0x00FFFFFF) | 0xCC000000) : 0xFF2A2E3A);
            int knob = on ? tsx + tsw - 14 : tsx + 2;
            roundedFill(g, knob, tsy + 2, knob + 12, tsy + 14, 6, on ? TEXT : TEXT_DIM);
            if (mh) statusTip = mod.getDescription();
        }
        if (drawerOpen && selectedModule != null) renderDrawer(g, mx, my, ac, drawerX + 12, islandY + 14, DRAWER_W - 24);
    }

    private void renderSidebar(GuiGraphicsExtractor g, int mx, int my, int ac) {
        int winW = Math.min(760, Math.max(560, this.width - 40));
        int winH = Math.min(460, Math.max(340, this.height - 50));
        int winX = (this.width - winW) / 2, winY = (this.height - winH) / 2 - 8;
        int sideW = 120, detailW = 210, headerH = 40;
        roundedFill(g, winX, winY, winX + winW, winY + winH, 10, ISLAND);
        g.fill(winX, winY, winX + winW, winY + headerH, SURFACE);
        g.text(this.font, "TT Client", winX + 12, winY + 14, TEXT, false);
        int sideX = winX, sideY = winY + headerH, sideH = winH - headerH - FOOTER_H;
        g.fill(sideX, sideY, sideX + sideW, sideY + sideH, 0xFF12141C);
        ModuleManager m = manager(); int y = sideY + 8;
        if (m != null) for (Category cat : Category.values()) {
            if (m.getByCategory(cat).isEmpty()) continue;
            boolean sel = cat == selectedCategory;
            boolean hov = mx >= sideX && mx < sideX + sideW && my >= y && my < y + 22;
            if (sel) g.fill(sideX + 2, y, sideX + 4, y + 22, ac);
            if (hov || sel) g.fill(sideX + 4, y, sideX + sideW - 2, y + 22, SURFACE_HOV);
            g.text(this.font, cat.name, sideX + 12, y + 7, sel ? TEXT : TEXT_DIM, false);
            catChipBounds.add(new int[]{sideX, y, sideX + sideW, y + 22, cat.ordinal()}); y += 24;
        }
        int listX = winX + sideW + 8, listY = sideY + 8, listW = winW - sideW - detailW - 16, listH = sideH - 16;
        List<Module> mods = visibleModules(); int ly = listY - listScroll;
        for (Module mod : mods) {
            if (ly + 24 < listY) { ly += 24; continue; } if (ly > listY + listH) break;
            boolean on = mod.isEnabled(); boolean sel = mod == selectedModule;
            boolean hov = mx >= listX && mx < listX + listW && my >= ly && my < ly + 24;
            if (sel || hov) g.fill(listX, ly, listX + listW, ly + 24, SURFACE_HOV);
            if (on) g.fill(listX, ly + 4, listX + 2, ly + 20, ac);
            g.text(this.font, mod.getName(), listX + 10, ly + 8, on ? TEXT : TEXT_DIM, false);
            ly += 24;
        }
        if (selectedModule != null) renderDrawer(g, mx, my, ac, winX + winW - detailW + 8, sideY + 8, detailW - 16);
    }

    private void renderDrawer(GuiGraphicsExtractor g, int mx, int my, int ac, int dx, int dy, int dw) {
        if (selectedModule == null) return;
        g.text(this.font, trim(selectedModule.getName(), dw), dx, dy, TEXT, false); dy += 18;
        boolean bindHov = mx >= dx && mx < dx + dw && my >= dy && my < dy + 22;
        roundedFill(g, dx, dy, dx + dw, dy + 22, 6, bindHov ? SURFACE_HOV : SURFACE);
        g.text(this.font, "Bind", dx + 8, dy + 7, TEXT_DIM, false);
        String kl = bindingModule == selectedModule ? "…" : keyLabel(selectedModule.getKeyBind());
        if (kl.isEmpty()) kl = "None";
        g.text(this.font, kl, dx + dw - this.font.width(kl) - 8, dy + 7, TEXT, false);
        dy += 28;
        for (Setting<?> s : selectedModule.getSettings()) {
            if (s instanceof BoolSetting bs) {
                boolean on = Boolean.TRUE.equals(bs.get());
                g.text(this.font, trim(s.getName(), dw - 40), dx + 2, dy + 7, TEXT_DIM, false);
                int sw = 26, sh = 14, sx = dx + dw - sw;
                roundedFill(g, sx, dy + 5, sx + sw, dy + 5 + sh, 7, on ? ((ac & 0x00FFFFFF) | 0xBB000000) : 0xFF2A2E3A);
                int knob = on ? sx + sw - 12 : sx + 2;
                roundedFill(g, knob, dy + 7, knob + 10, dy + 5 + sh - 2, 5, on ? TEXT : TEXT_DIM);
                dy += 26;
            } else if (s instanceof ModeSetting ms) {
                g.text(this.font, trim(s.getName(), dw / 2), dx + 2, dy + 7, TEXT_DIM, false);
                String val = String.valueOf(ms.get());
                g.text(this.font, val, dx + dw - this.font.width(val) - 8, dy + 7, TEXT, false);
                dy += 26;
            } else if (s instanceof NumberSetting ns) {
                g.text(this.font, s.getName(), dx + 2, dy + 4, TEXT_DIM, false);
                String disp = typing == ns ? typingBuffer + "▌" : String.valueOf(ns.get());
                g.text(this.font, disp, dx + dw - this.font.width(disp), dy + 4, TEXT, false);
                int trackX = dx, trackY = dy + 22, trackW = dw;
                roundedFill(g, trackX, trackY, trackX + trackW, trackY + 6, 3, 0xFF2A2E3A);
                double min = ns.getMin(), max = ns.getMax(), v = ns.get();
                double t = max <= min ? 0 : (v - min) / (max - min);
                int fillW = (int) Math.round(trackW * Math.max(0, Math.min(1, t)));
                if (fillW > 0) roundedFill(g, trackX, trackY, trackX + fillW, trackY + 6, 3, ac);
                if (sliding == ns) { sliderTrackX = trackX; sliderTrackW = trackW; }
                dy += 40;
            } else { g.text(this.font, s.getName(), dx + 2, dy + 7, TEXT_DIM, false); dy += 26; }
        }
    }

    private List<Module> visibleModules() {
        ModuleManager m = manager(); if (m == null) return List.of();
        if (!searchQuery.isEmpty()) return m.search(searchQuery);
        return m.getByCategory(selectedCategory);
    }
    private String trim(String s, int maxPx) {
        if (s == null) return ""; if (this.font.width(s) <= maxPx) return s;
        while (s.length() > 1 && this.font.width(s + "…") > maxPx) s = s.substring(0, s.length() - 1);
        return s + "…";
    }
    private static String keyLabel(int key) {
        if (key < 0) return "";
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) return String.valueOf((char) ('A' + (key - GLFW.GLFW_KEY_A)));
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) return String.valueOf((char) ('0' + (key - GLFW.GLFW_KEY_0)));
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) return "RSHIFT";
        if (key == GLFW.GLFW_KEY_SPACE) return "SPACE";
        return "KEY" + key;
    }
    private void saveConfig() {
        try {
            for (var e : panelPos.entrySet()) {
                int[] p = e.getValue();
                com.ttclient.config.ConfigManager.PANEL_LAYOUT.put(e.getKey().name, new double[]{p[0], p[1], p[2]});
            }
            if (TTClientClient.config != null) TTClientClient.config.save();
        } catch (Throwable ignored) {}
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        double mx = event.x(), my = event.y(); int button = event.button();
        if (bindingModule != null && (button == 0 || button == 1)) return true;
        for (int[] b : styleChipBounds) {
            if (mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) {
                int idx = b[4]; if (idx >= 0 && idx < STYLES.length) { setStyle(STYLES[idx]); listScroll = 0; selectedModule = null; ensurePanels(); }
                return true;
            }
        }
        for (int[] b : catChipBounds) {
            if (mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) {
                Category[] vals = Category.values(); int ord = b[4];
                if (ord >= 0 && ord < vals.length) { selectedCategory = vals[ord]; radialOpen = vals[ord]; searchQuery = ""; listScroll = 0; selectedModule = null; }
                return true;
            }
        }
        String st = style(); ModuleManager m = manager();
        if (st.equals("Classic") || st.equals("Meteor")) {
            ensurePanels(); if (m == null) return false;
            for (var e : panelPos.entrySet()) {
                Category cat = e.getKey(); int[] p = e.getValue(); int px = p[0], py = p[1], open = p[2];
                int pw = st.equals("Meteor") ? 100 : 110;
                if (st.equals("Classic") && mx >= px && mx < px + pw && my >= py && my < py + 22 && button == 0) {
                    if (mx > px + pw - 18) { p[2] = open == 0 ? 1 : 0; saveConfig(); } else { dragCat = cat; dragOffX = (int) mx - px; dragOffY = (int) my - py; }
                    return true;
                }
                if (st.equals("Meteor") && mx >= px && mx < px + pw && my >= py && my < py + 18 && button == 0) {
                    dragCat = cat; dragOffX = (int) mx - px; dragOffY = (int) my - py; return true;
                }
                int y = py + (st.equals("Meteor") ? 20 : 24); int rh = st.equals("Meteor") ? 16 : ROW_H;
                if (open != 0 || st.equals("Meteor")) for (Module mod : m.getByCategory(cat)) {
                    if (mx >= px && mx < px + pw && my >= y && my < y + rh) {
                        if (button == 0) { mod.toggle(); saveConfig(); return true; }
                        if (button == 1) { bindingModule = mod; return true; }
                    }
                    y += rh;
                }
            }
            return false;
        }
        if (st.equals("Radial") && m != null) {
            Category open = radialOpen != null ? radialOpen : selectedCategory;
            int cx = this.width / 2, cy = this.height / 2 - 10, ringR = Math.min(this.width, this.height) / 5;
            int my0 = cy + ringR + 40, mw = 160, mx0 = cx - mw / 2; int y = my0 + 20;
            for (Module mod : m.getByCategory(open)) {
                if (mx >= mx0 + 4 && mx < mx0 + mw - 4 && my >= y && my < y + 20) {
                    if (button == 0) { mod.toggle(); saveConfig(); return true; }
                    if (button == 1) { bindingModule = mod; return true; }
                }
                y += 20;
            }
        }
        // Center / Sidebar / Wurst: toggle on module row click in list/grid
        if (st.equals("Center") || st.equals("Sidebar") || st.equals("Wurst")) {
            for (Module mod : visibleModules()) {
                // simplified: LMB toggles selected, select first
            }
            layoutIsland();
            List<Module> modules = visibleModules();
            int cols = bodyW >= 420 ? 2 : 1; int cardW = (bodyW - (cols - 1) * CARD_GAP) / cols;
            int startY = bodyY - listScroll;
            for (int i = 0; i < modules.size(); i++) {
                Module mod = modules.get(i); int col = i % cols, row = i / cols;
                int cx = bodyX + col * (cardW + CARD_GAP); int cy = startY + row * (CARD_H + CARD_GAP);
                if (cy + CARD_H < bodyY || cy > bodyY + bodyH) continue;
                if (mx >= cx && mx < cx + cardW && my >= cy && my < cy + CARD_H && my >= bodyY && my < bodyY + bodyH) {
                    if (button == 0) {
                        int tsw = 28, tsx = cx + cardW - tsw - 12;
                        if (mx >= tsx) { mod.toggle(); saveConfig(); } else { selectedModule = selectedModule == mod ? null : mod; }
                        return true;
                    }
                    if (button == 1) { bindingModule = mod; return true; }
                }
            }
            if (drawerOpen && selectedModule != null && button == 0) {
                int dx = drawerX + 12, dy = islandY + 14, dw = DRAWER_W - 24;
                if (mx >= dx && mx < dx + dw && my >= dy + 18 && my < dy + 40) { bindingModule = selectedModule; return true; }
                dy += 46;
                for (Setting<?> s : selectedModule.getSettings()) {
                    int rowH = s instanceof NumberSetting ? 40 : 26;
                    if (mx >= dx && mx < dx + dw && my >= dy && my < dy + rowH) {
                        if (s instanceof BoolSetting bs) { bs.toggle(); saveConfig(); return true; }
                        if (s instanceof ModeSetting ms) { ms.cycle(); saveConfig(); return true; }
                        if (s instanceof NumberSetting ns) { sliding = ns; sliderTrackX = dx; sliderTrackW = dw; return true; }
                    }
                    dy += rowH;
                }
            }
        }
        return false;
    }

    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (sliding != null) {
            double t = (event.x() - sliderTrackX) / Math.max(1, sliderTrackW);
            t = Math.max(0, Math.min(1, t));
            sliding.set(sliding.getMin() + t * (sliding.getMax() - sliding.getMin()));
            return true;
        }
        if (dragCat != null) {
            int[] p = panelPos.get(dragCat);
            if (p != null) { p[0] = (int) event.x() - dragOffX; p[1] = (int) event.y() - dragOffY; }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (sliding != null) { sliding = null; saveConfig(); return true; }
        if (dragCat != null) { dragCat = null; saveConfig(); return true; }
        return super.mouseReleased(event);
    }
    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        listScroll = Math.max(0, listScroll - (int) (vertical * 22)); return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (bindingModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { bindingModule.setKeyBind(-1); bindingModule = null; saveConfig(); return true; }
            bindingModule.setKeyBind(keyCode); bindingModule = null; saveConfig(); return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { if (selectedModule != null) { selectedModule = null; return true; } onClose(); return true; }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) { onClose(); return true; }
        if (keyCode == GLFW.GLFW_KEY_LEFT_BRACKET || keyCode == GLFW.GLFW_KEY_RIGHT_BRACKET) {
            int idx = 0; for (int i = 0; i < STYLES.length; i++) if (STYLES[i].equalsIgnoreCase(style())) { idx = i; break; }
            idx = keyCode == GLFW.GLFW_KEY_RIGHT_BRACKET ? (idx + 1) % STYLES.length : (idx - 1 + STYLES.length) % STYLES.length;
            setStyle(STYLES[idx]); return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) { searchQuery = searchQuery.substring(0, searchQuery.length() - 1); return true; }
        if (searchQuery.length() < 40) {
            if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
                char c = (char) ('a' + (keyCode - GLFW.GLFW_KEY_A));
                if (event.hasShiftDown()) c = Character.toUpperCase(c);
                searchQuery += c; return true;
            }
            if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) { searchQuery += (char) ('0' + (keyCode - GLFW.GLFW_KEY_0)); return true; }
        }
        return super.keyPressed(event);
    }
    @Override public void onClose() { sliding = null; typing = null; bindingModule = null; dragCat = null; saveConfig(); super.onClose(); }
}

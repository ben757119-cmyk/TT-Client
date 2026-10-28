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
import java.util.List;

/** TT Control Island — category pills, module card grid, on-demand settings drawer. */
public class ClickGUIScreen extends Screen {

    private int islandX, islandY, islandW, islandH;
    private int bodyX, bodyY, bodyW, bodyH;
    private int drawerX, drawerW;
    private boolean drawerOpen;

    private static final int HEADER_H = 46;
    private static final int CAT_H = 34;
    private static final int FOOTER_H = 38;
    private static final int PAD = 14;
    private static final int CARD_H = 52;
    private static final int CARD_GAP = 8;
    private static final int DRAWER_W = 228;

    private Category selectedCategory = Category.CLIENT;
    private Module selectedModule = null;
    private Module bindingModule = null;
    private boolean suppressBindClick = false;

    private String searchQuery = "";
    private boolean searchFocused = false;

    private NumberSetting sliding = null;
    private int sliderTrackX, sliderTrackW;
    private NumberSetting typing = null;
    private String typingBuffer = "";

    private int gridScroll = 0;
    private int drawerScroll = 0;

    private final List<int[]> packChipBounds = new ArrayList<>();
    private final List<int[]> catChipBounds = new ArrayList<>();
    private String statusTip = null;

    private static final int BG_DIM = 0xA0060810;
    private static final int ISLAND = 0xF010121A;
    private static final int SURFACE = 0xFF161922;
    private static final int SURFACE_HOV = 0xFF1C2030;
    private static final int SURFACE_ON = 0xFF1E2434;
    private static final int TEXT = 0xFFF0F2F5;
    private static final int TEXT_DIM = 0xFF8E96A8;
    private static final int TEXT_MUTED = 0xFF5A6274;
    private static final int CHIP = 0xFF1A1E28;
    private static final int CHIP_HOV = 0xFF242836;

    public ClickGUIScreen() { super(Component.literal("TT Client")); }

    @Override
    protected void init() {
        super.init();
        ModuleManager m = manager();
        if (m != null) {
            for (Category c : Category.values()) {
                if (!m.getByCategory(c).isEmpty()) { selectedCategory = c; break; }
            }
        }
        selectedModule = null;
        drawerOpen = false;
    }

    private static ModuleManager manager() {
        try { if (TTClientClient.modules != null) return TTClientClient.modules; } catch (Throwable ignored) {}
        try { return com.ttclient.TTClient.modules; } catch (Throwable t) { return null; }
    }

    private static PackTweaks packTweaks() {
        try {
            ModuleManager m = manager();
            return m != null ? m.getModule(PackTweaks.class) : null;
        } catch (Throwable t) { return null; }
    }

    private int accent() { return ClickGUIModule.globalAccent(); }

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

    private void layout() {
        int baseW = Math.min(640, Math.max(480, this.width - 80));
        int baseH = Math.min(420, Math.max(320, this.height - 60));
        drawerOpen = selectedModule != null;
        drawerW = drawerOpen ? DRAWER_W : 0;
        islandW = baseW + drawerW;
        islandH = baseH;
        islandX = (this.width - islandW) / 2;
        islandY = (this.height - islandH) / 2 - 6;
        bodyX = islandX + PAD;
        bodyY = islandY + HEADER_H + CAT_H;
        bodyW = baseW - PAD * 2;
        bodyH = islandH - HEADER_H - CAT_H - FOOTER_H;
        drawerX = islandX + baseW;
    }

    @Override public boolean isInGameUi() { return true; }
    @Override public boolean isPauseScreen() { return false; }

    /** Don't auto-close while listening for a keybind — ESC must clear the bind instead. */
    @Override
    public boolean shouldCloseOnEsc() {
        return bindingModule == null && typing == null;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, BG_DIM);
    }
    @Override protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {}
    @Override public void extractTransparentBackground(GuiGraphicsExtractor graphics) {}
    @Override protected void extractMenuBackground(GuiGraphicsExtractor graphics) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        layout();
        statusTip = null;
        catChipBounds.clear();
        packChipBounds.clear();
        int ac = accent();
        roundedFill(graphics, islandX - 2, islandY - 2, islandX + islandW + 2, islandY + islandH + 2, 14, (ac & 0x00FFFFFF) | 0x22000000);
        roundedFill(graphics, islandX, islandY, islandX + islandW, islandY + islandH, 12, ISLAND);
        renderHeader(graphics, mouseX, mouseY, ac);
        renderCategoryPills(graphics, mouseX, mouseY, ac);
        renderModuleGrid(graphics, mouseX, mouseY, ac);
        if (drawerOpen) renderDrawer(graphics, mouseX, mouseY, ac);
        renderFooter(graphics, mouseX, mouseY, ac);
        if (statusTip != null && !statusTip.isEmpty()) {
            int tw = this.font.width(statusTip);
            int tx = islandX + (islandW - tw) / 2;
            int ty = islandY + islandH + 8;
            roundedFill(graphics, tx - 8, ty - 2, tx + tw + 8, ty + 11, 4, 0xE00C0E14);
            graphics.text(this.font, statusTip, tx, ty, TEXT_DIM, false);
        }
        if (bindingModule != null) {
            String msg = "Press a key for  " + bindingModule.getName() + "   ·   ESC clears";
            int tw = this.font.width(msg);
            int bx = islandX + (islandW - tw) / 2 - 10;
            int by = islandY + islandH / 2 - 14;
            roundedFill(graphics, bx, by, bx + tw + 20, by + 28, 8, 0xF0080A12);
            roundedFill(graphics, bx + 1, by + 1, bx + tw + 19, by + 27, 7, 0xF0161A24);
            graphics.text(this.font, msg, bx + 10, by + 10, TEXT, false);
        }
    }

    private void renderHeader(GuiGraphicsExtractor g, int mx, int my, int ac) {
        g.text(this.font, "TT", islandX + PAD + 2, islandY + 16, ac, false);
        g.text(this.font, "Client", islandX + PAD + 18, islandY + 16, TEXT, false);
        int sx = islandX + 110;
        int sy = islandY + 11;
        int sw = (drawerOpen ? islandW - DRAWER_W : islandW) - 110 - PAD;
        int sh = 24;
        boolean hov = mx >= sx && mx < sx + sw && my >= sy && my < sy + sh;
        roundedFill(g, sx, sy, sx + sw, sy + sh, 7, searchFocused ? 0xFF1A1E2A : (hov ? SURFACE_HOV : SURFACE));
        if (searchFocused) g.fill(sx + 1, sy + 1, sx + 3, sy + sh - 1, ac);
        String shown = searchQuery.isEmpty() && !searchFocused ? "Search anything…" : searchQuery + (searchFocused ? "▌" : "");
        int col = searchQuery.isEmpty() && !searchFocused ? TEXT_MUTED : TEXT;
        g.text(this.font, trim(shown, sw - 16), sx + 10, sy + 8, col, false);
    }

    private void renderCategoryPills(GuiGraphicsExtractor g, int mx, int my, int ac) {
        ModuleManager m = manager();
        if (m == null) return;
        int x = islandX + PAD;
        int y = islandY + HEADER_H + 4;
        int maxX = islandX + (drawerOpen ? islandW - DRAWER_W : islandW) - PAD;
        for (Category cat : Category.values()) {
            if (m.getByCategory(cat).isEmpty()) continue;
            String label = cat.name;
            int tw = this.font.width(label);
            int pw = tw + 18;
            if (x + pw > maxX) break;
            boolean sel = cat == selectedCategory && searchQuery.isEmpty();
            boolean hov = mx >= x && mx < x + pw && my >= y && my < y + 22;
            int bg = sel ? ((ac & 0x00FFFFFF) | 0x33000000) : (hov ? CHIP_HOV : CHIP);
            roundedFill(g, x, y, x + pw, y + 22, 11, bg);
            if (sel) g.fill(x + 6, y + 19, x + pw - 6, y + 21, ac);
            g.text(this.font, label, x + 9, y + 7, sel ? TEXT : TEXT_DIM, false);
            catChipBounds.add(new int[]{x, y, x + pw, y + 22, cat.ordinal()});
            x += pw + 6;
        }
    }

    private List<Module> visibleModules() {
        ModuleManager m = manager();
        if (m == null) return List.of();
        if (!searchQuery.isEmpty()) return m.search(searchQuery);
        return m.getByCategory(selectedCategory);
    }

    private void renderModuleGrid(GuiGraphicsExtractor g, int mx, int my, int ac) {
        List<Module> modules = visibleModules();
        int cols = bodyW >= 420 ? 2 : 1;
        int cardW = (bodyW - (cols - 1) * CARD_GAP) / cols;
        int rows = (modules.size() + cols - 1) / Math.max(1, cols);
        int contentH = rows * (CARD_H + CARD_GAP);
        int maxScroll = Math.max(0, contentH - bodyH);
        gridScroll = Math.max(0, Math.min(gridScroll, maxScroll));
        int startY = bodyY - gridScroll;
        for (int i = 0; i < modules.size(); i++) {
            Module mod = modules.get(i);
            int col = i % cols;
            int row = i / cols;
            int cx = bodyX + col * (cardW + CARD_GAP);
            int cy = startY + row * (CARD_H + CARD_GAP);
            if (cy + CARD_H < bodyY || cy > bodyY + bodyH) continue;
            boolean sel = mod == selectedModule;
            boolean on = mod.isEnabled();
            boolean hov = mx >= cx && mx < cx + cardW && my >= cy && my < cy + CARD_H && my >= bodyY && my < bodyY + bodyH;
            int bg = sel ? SURFACE_ON : (hov ? SURFACE_HOV : SURFACE);
            roundedFill(g, cx, cy, cx + cardW, cy + CARD_H, 10, bg);
            if (on) g.fill(cx + 3, cy + 10, cx + 5, cy + CARD_H - 10, ac);
            g.text(this.font, trim(mod.getName(), cardW - 70), cx + 14, cy + 12, on ? TEXT : TEXT_DIM, false);
            String bind = keyLabel(mod.getKeyBind());
            if (!bind.isEmpty()) {
                int bw = this.font.width(bind) + 10;
                roundedFill(g, cx + 14, cy + 30, cx + 14 + bw, cy + 42, 4, 0xFF0E1018);
                g.text(this.font, bind, cx + 19, cy + 32, TEXT_MUTED, false);
            }
            int sw = 28, sh = 16;
            int sx = cx + cardW - sw - 12;
            int sy = cy + (CARD_H - sh) / 2;
            roundedFill(g, sx, sy, sx + sw, sy + sh, 8, on ? ((ac & 0x00FFFFFF) | 0xCC000000) : 0xFF2A2E3A);
            int knob = on ? sx + sw - 14 : sx + 2;
            roundedFill(g, knob, sy + 2, knob + 12, sy + sh - 2, 6, on ? TEXT : TEXT_DIM);
            if (hov && my >= bodyY && my < bodyY + bodyH) statusTip = mod.getDescription();
        }
        if (maxScroll > 0) {
            int trackH = bodyH - 8;
            int thumbH = Math.max(16, trackH * bodyH / Math.max(1, contentH));
            int thumbY = bodyY + 4 + (trackH - thumbH) * gridScroll / Math.max(1, maxScroll);
            g.fill(bodyX + bodyW + 2, bodyY + 4, bodyX + bodyW + 4, bodyY + bodyH - 4, 0x18FFFFFF);
            g.fill(bodyX + bodyW + 2, thumbY, bodyX + bodyW + 4, thumbY + thumbH, (ac & 0x00FFFFFF) | 0x66000000);
        }
    }

    private void renderDrawer(GuiGraphicsExtractor g, int mx, int my, int ac) {
        if (selectedModule == null) return;
        g.fill(drawerX, islandY + 12, drawerX + 1, islandY + islandH - 12, 0x14FFFFFF);
        int dx = drawerX + 12;
        int dy = islandY + 14;
        int dw = DRAWER_W - 24;
        g.text(this.font, trim(selectedModule.getName(), dw - 20), dx, dy, TEXT, false);
        g.text(this.font, "esc", dx + dw - this.font.width("esc"), dy, TEXT_MUTED, false);
        dy += 16;
        g.text(this.font, trim(selectedModule.getDescription(), dw), dx, dy, TEXT_MUTED, false);
        dy += 18;
        boolean bindHov = mx >= dx && mx < dx + dw && my >= dy && my < dy + 22;
        roundedFill(g, dx, dy, dx + dw, dy + 22, 6, bindHov ? SURFACE_HOV : SURFACE);
        g.text(this.font, "Bind", dx + 8, dy + 7, TEXT_DIM, false);
        String kl = bindingModule == selectedModule ? "…" : keyLabel(selectedModule.getKeyBind());
        if (kl.isEmpty()) kl = "None";
        g.text(this.font, kl, dx + dw - this.font.width(kl) - 8, dy + 7, TEXT, false);
        dy += 28;
        List<Setting<?>> settings = selectedModule.getSettings();
        int contentTop = dy;
        int available = islandY + islandH - FOOTER_H - 8 - contentTop;
        int totalH = 0;
        for (Setting<?> s : settings) totalH += (s instanceof NumberSetting) ? 40 : 26;
        int maxScroll = Math.max(0, totalH - available);
        drawerScroll = Math.max(0, Math.min(drawerScroll, maxScroll));
        int y = contentTop - drawerScroll;
        for (Setting<?> s : settings) {
            int rowH = (s instanceof NumberSetting) ? 40 : 26;
            if (y + rowH < contentTop || y > contentTop + available) { y += rowH; continue; }
            if (s instanceof BoolSetting bs) {
                boolean on = Boolean.TRUE.equals(bs.get());
                boolean hov = mx >= dx && mx < dx + dw && my >= y && my < y + 24;
                if (hov) statusTip = s.getDescription();
                g.text(this.font, trim(s.getName(), dw - 40), dx + 2, y + 7, hov ? TEXT : TEXT_DIM, false);
                int sw = 26, sh = 14, sx = dx + dw - sw, sy = y + 5;
                roundedFill(g, sx, sy, sx + sw, sy + sh, 7, on ? ((ac & 0x00FFFFFF) | 0xBB000000) : 0xFF2A2E3A);
                int knob = on ? sx + sw - 12 : sx + 2;
                roundedFill(g, knob, sy + 2, knob + 10, sy + sh - 2, 5, on ? TEXT : TEXT_DIM);
            } else if (s instanceof ModeSetting ms) {
                boolean hov = mx >= dx && mx < dx + dw && my >= y && my < y + 24;
                if (hov) statusTip = s.getDescription();
                g.text(this.font, trim(s.getName(), dw / 2), dx + 2, y + 7, TEXT_DIM, false);
                String val = String.valueOf(ms.get());
                roundedFill(g, dx + dw - this.font.width(val) - 16, y + 3, dx + dw, y + 21, 5, hov ? SURFACE_HOV : SURFACE);
                g.text(this.font, val, dx + dw - this.font.width(val) - 8, y + 7, TEXT, false);
            } else if (s instanceof NumberSetting ns) {
                boolean hov = mx >= dx && mx < dx + dw && my >= y && my < y + 38;
                if (hov) statusTip = s.getDescription();
                g.text(this.font, s.getName(), dx + 2, y + 4, TEXT_DIM, false);
                String disp = typing == ns ? typingBuffer + "▌" : ns.getDisplay();
                g.text(this.font, disp, dx + dw - this.font.width(disp), y + 4, TEXT, false);
                int trackX = dx, trackY = y + 22, trackW = dw, trackH = 6;
                roundedFill(g, trackX, trackY, trackX + trackW, trackY + trackH, 3, 0xFF2A2E3A);
                double min = ns.getMin(), max = ns.getMax(), v = ns.get();
                double t = max <= min ? 0 : (v - min) / (max - min);
                int fillW = (int) Math.round(trackW * Math.max(0, Math.min(1, t)));
                if (fillW > 0) roundedFill(g, trackX, trackY, trackX + fillW, trackY + trackH, 3, ac);
                int knobX = trackX + fillW - 4;
                roundedFill(g, knobX, trackY - 3, knobX + 8, trackY + trackH + 3, 4, TEXT);
                if (sliding == ns) { sliderTrackX = trackX; sliderTrackW = trackW; }
            } else if (s instanceof ColorSetting cs) {
                boolean hov = mx >= dx && mx < dx + dw && my >= y && my < y + 24;
                if (hov) statusTip = s.getDescription();
                g.text(this.font, s.getName(), dx + 2, y + 7, TEXT_DIM, false);
                roundedFill(g, dx + dw - 28, y + 4, dx + dw, y + 20, 4, cs.get() | 0xFF000000);
            } else {
                g.text(this.font, s.getName(), dx + 2, y + 7, TEXT_DIM, false);
            }
            y += rowH;
        }
    }

    private void renderFooter(GuiGraphicsExtractor g, int mx, int my, int ac) {
        PackTweaks pt = packTweaks();
        int y = islandY + islandH - FOOTER_H + 8;
        int x = islandX + PAD;
        int maxX = islandX + (drawerOpen ? islandW - DRAWER_W : islandW) - PAD;
        g.text(this.font, "PACK", x, y + 4, TEXT_MUTED, false);
        x += 32;
        if (pt == null) { g.text(this.font, "—", x, y + 4, TEXT_MUTED, false); return; }
        BoolSetting[] chips = { pt.lowFire, pt.smallTotems, pt.noPumpkin, pt.rainbowTools, pt.rainbowBlocks };
        for (BoolSetting bs : chips) {
            if (bs == null) continue;
            String label = shortName(bs.getName());
            int tw = this.font.width(label) + 14;
            if (x + tw > maxX) break;
            boolean on = Boolean.TRUE.equals(bs.get());
            boolean hov = mx >= x && mx < x + tw && my >= y && my < y + 18;
            int bg = on ? ((ac & 0x00FFFFFF) | 0x44000000) : (hov ? CHIP_HOV : CHIP);
            roundedFill(g, x, y, x + tw, y + 18, 9, bg);
            g.text(this.font, label, x + 7, y + 5, on ? TEXT : TEXT_DIM, false);
            packChipBounds.add(new int[]{x, y, x + tw, y + 18, System.identityHashCode(bs)});
            x += tw + 5;
        }
    }

    private static String shortName(String n) {
        if (n == null) return "";
        if (n.equalsIgnoreCase("LowFire")) return "Fire";
        if (n.equalsIgnoreCase("SmallTotems")) return "Totem";
        if (n.equalsIgnoreCase("NoPumpkin")) return "Pumpkin";
        if (n.equalsIgnoreCase("RainbowTools")) return "R.Tools";
        if (n.equalsIgnoreCase("RainbowBlocks")) return "R.Blocks";
        return n.length() > 8 ? n.substring(0, 8) : n;
    }

    private String trim(String s, int maxPx) {
        if (s == null) return "";
        if (this.font.width(s) <= maxPx) return s;
        while (s.length() > 1 && this.font.width(s + "…") > maxPx) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    private static String keyLabel(int key) {
        if (key < 0) return "";
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) return String.valueOf((char) ('A' + (key - GLFW.GLFW_KEY_A)));
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) return String.valueOf((char) ('0' + (key - GLFW.GLFW_KEY_0)));
        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS";
            case GLFW.GLFW_KEY_INSERT -> "INS";
            case GLFW.GLFW_KEY_DELETE -> "DEL";
            case GLFW.GLFW_KEY_HOME -> "HOME";
            case GLFW.GLFW_KEY_END -> "END";
            case GLFW.GLFW_KEY_PAGE_UP -> "PGUP";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PGDN";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            case GLFW.GLFW_KEY_F1 -> "F1"; case GLFW.GLFW_KEY_F2 -> "F2"; case GLFW.GLFW_KEY_F3 -> "F3";
            case GLFW.GLFW_KEY_F4 -> "F4"; case GLFW.GLFW_KEY_F5 -> "F5"; case GLFW.GLFW_KEY_F6 -> "F6";
            case GLFW.GLFW_KEY_F7 -> "F7"; case GLFW.GLFW_KEY_F8 -> "F8"; case GLFW.GLFW_KEY_F9 -> "F9";
            case GLFW.GLFW_KEY_F10 -> "F10"; case GLFW.GLFW_KEY_F11 -> "F11"; case GLFW.GLFW_KEY_F12 -> "F12";
            default -> "KEY" + key;
        };
    }

    private void saveConfig() {
        try { if (TTClientClient.config != null) TTClientClient.config.save(); } catch (Throwable ignored) {}
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        if (bindingModule != null && (button == 0 || button == 1)) return true;

        for (int[] b : packChipBounds) {
            if (mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) {
                PackTweaks pt = packTweaks();
                if (pt != null) {
                    for (BoolSetting bs : List.of(pt.lowFire, pt.smallTotems, pt.noPumpkin, pt.rainbowTools, pt.rainbowBlocks)) {
                        if (bs != null && System.identityHashCode(bs) == b[4]) {
                            bs.toggle(); saveConfig(); return true;
                        }
                    }
                }
                return true;
            }
        }

        for (int[] b : catChipBounds) {
            if (mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) {
                Category[] vals = Category.values();
                int ord = b[4];
                if (ord >= 0 && ord < vals.length) {
                    selectedCategory = vals[ord];
                    searchQuery = ""; searchFocused = false; gridScroll = 0;
                    if (selectedModule != null && selectedModule.getCategory() != selectedCategory) selectedModule = null;
                }
                return true;
            }
        }

        int sx = islandX + 110, sy = islandY + 11;
        int sw = (drawerOpen ? islandW - DRAWER_W : islandW) - 110 - PAD;
        if (mx >= sx && mx < sx + sw && my >= sy && my < sy + 24) { searchFocused = true; return true; }
        searchFocused = false;

        List<Module> modules = visibleModules();
        int cols = bodyW >= 420 ? 2 : 1;
        int cardW = (bodyW - (cols - 1) * CARD_GAP) / cols;
        int startY = bodyY - gridScroll;
        for (int i = 0; i < modules.size(); i++) {
            Module mod = modules.get(i);
            int col = i % cols, row = i / cols;
            int cx = bodyX + col * (cardW + CARD_GAP);
            int cy = startY + row * (CARD_H + CARD_GAP);
            if (cy + CARD_H < bodyY || cy > bodyY + bodyH) continue;
            if (mx < cx || mx >= cx + cardW || my < cy || my >= cy + CARD_H) continue;
            if (my < bodyY || my >= bodyY + bodyH) continue;
            int swx = cx + cardW - 28 - 12, swy = cy + (CARD_H - 16) / 2;
            if (button == 0 && mx >= swx && mx < swx + 28 && my >= swy && my < swy + 16) {
                mod.toggle(); saveConfig(); return true;
            }
            if (button == 1) { bindingModule = mod; suppressBindClick = true; return true; }
            if (button == 0) {
                if (selectedModule == mod) { mod.toggle(); saveConfig(); }
                else { selectedModule = mod; drawerScroll = 0; }
                return true;
            }
        }

        if (drawerOpen && selectedModule != null && mx >= drawerX) {
            int dx = drawerX + 12;
            int dy = islandY + 14 + 16 + 18;
            int dw = DRAWER_W - 24;
            if (button == 0 && mx >= dx && mx < dx + dw && my >= dy && my < dy + 22) {
                if (!suppressBindClick) bindingModule = selectedModule;
                suppressBindClick = false;
                return true;
            }
            dy += 28;
            List<Setting<?>> settings = selectedModule.getSettings();
            int contentTop = dy;
            int y = contentTop - drawerScroll;
            for (Setting<?> s : settings) {
                int rowH = (s instanceof NumberSetting) ? 40 : 26;
                if (my >= y && my < y + rowH && mx >= dx && mx < dx + dw) {
                    if (s instanceof BoolSetting bs && button == 0) { bs.toggle(); saveConfig(); return true; }
                    if (s instanceof ModeSetting ms && button == 0) { ms.cycle(); saveConfig(); return true; }
                    if (s instanceof NumberSetting ns && button == 0) {
                        int trackY = y + 22;
                        if (my >= trackY - 4 && my <= trackY + 12) {
                            sliding = ns; sliderTrackX = dx; sliderTrackW = dw; applySlider(ns, mx);
                        } else {
                            typing = ns; typingBuffer = ns.getDisplay();
                        }
                        return true;
                    }
                }
                y += rowH;
            }
            return true;
        }

        if (button == 0 && (mx < islandX || mx >= islandX + islandW || my < islandY || my >= islandY + islandH)) {
            if (selectedModule != null) { selectedModule = null; return true; }
        }
        return super.mouseClicked(event, bl);
    }

    private void applySlider(NumberSetting ns, double mx) {
        double t = (mx - sliderTrackX) / Math.max(1, sliderTrackW);
        t = Math.max(0, Math.min(1, t));
        ns.set(ns.getMin() + t * (ns.getMax() - ns.getMin()));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (sliding != null) { applySlider(sliding, event.x()); return true; }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (sliding != null) { sliding = null; saveConfig(); return true; }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (drawerOpen && mx >= drawerX) {
            drawerScroll = Math.max(0, drawerScroll - (int) (vertical * 18));
            return true;
        }
        if (mx >= bodyX && mx < bodyX + bodyW && my >= bodyY && my < bodyY + bodyH) {
            gridScroll = Math.max(0, gridScroll - (int) (vertical * 24));
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();

        if (typing != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                try { typing.set(Double.parseDouble(typingBuffer)); } catch (Exception ignored) {}
                typing = null; typingBuffer = ""; saveConfig(); return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { typing = null; typingBuffer = ""; return true; }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !typingBuffer.isEmpty()) {
                typingBuffer = typingBuffer.substring(0, typingBuffer.length() - 1); return true;
            }
            if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
                typingBuffer += (char) ('0' + (keyCode - GLFW.GLFW_KEY_0)); return true;
            }
            if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
                typingBuffer += (char) ('0' + (keyCode - GLFW.GLFW_KEY_KP_0)); return true;
            }
            if (keyCode == GLFW.GLFW_KEY_PERIOD || keyCode == GLFW.GLFW_KEY_KP_DECIMAL) {
                if (!typingBuffer.contains(".")) typingBuffer += '.'; return true;
            }
            if ((keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) && typingBuffer.isEmpty()) {
                typingBuffer = "-"; return true;
            }
            return true;
        }

        if (bindingModule != null) {
            int k = keyCode;
            if (k <= 0) { try { k = event.input(); } catch (Throwable ignored) {} }
            if (k == GLFW.GLFW_KEY_ESCAPE || k == 256) bindingModule.setKeyBind(-1);
            else if (k > 0) bindingModule.setKeyBind(k);
            bindingModule = null;
            saveConfig();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (selectedModule != null) { selectedModule = null; return true; }
            onClose(); return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) { onClose(); return true; }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            searchFocused = true; gridScroll = 0; return true;
        }
        if (searchQuery.length() < 40) {
            if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
                char c = (char) ('a' + (keyCode - GLFW.GLFW_KEY_A));
                if (event.hasShiftDown()) c = Character.toUpperCase(c);
                searchQuery += c; searchFocused = true; gridScroll = 0; return true;
            }
            if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
                searchQuery += (char) ('0' + (keyCode - GLFW.GLFW_KEY_0));
                searchFocused = true; gridScroll = 0; return true;
            }
            if (keyCode == GLFW.GLFW_KEY_SPACE) { searchQuery += ' '; searchFocused = true; return true; }
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        sliding = null; typing = null; bindingModule = null;
        saveConfig();
        super.onClose();
    }
}

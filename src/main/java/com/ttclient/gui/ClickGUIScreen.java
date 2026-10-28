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
import java.util.Locale;

/**
 * Modern hub ClickGUI — single window, clear hierarchy, no panel clutter.
 * <p>
 * Layout: sidebar categories · searchable module list · detail/settings panel · pack footer.
 * Interactive toggles, large hit targets, soft surfaces, accent only for state.
 */
public class ClickGUIScreen extends Screen {
    // Layout (recomputed each frame)
    private int winX, winY, winW, winH;
    private int sideW = 128;
    private int detailW = 220;
    private int headerH = 42;
    private int footerH = 40;
    private int listX, listY, listW, listH;
    private int detailX, detailY, detailH;
    private int sideX, sideY, sideH;

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

    private int listScroll = 0;
    private int detailScroll = 0;

    private final List<int[]> packChipBounds = new ArrayList<>();
    private String statusTip = null;

    // Soft theme — dark, low contrast chrome, accent for state only
    private static final int BG_DIM = 0xB0080A10;
    private static final int WIN_BG = 0xF0101218;
    private static final int SIDE_BG = 0xF00C0E14;
    private static final int HEADER_BG = 0xF0141620;
    private static final int CARD = 0xFF181B24;
    private static final int CARD_HOV = 0xFF1E2230;
    private static final int CARD_SEL = 0xFF222838;
    private static final int TEXT = 0xFFE8EAED;
    private static final int TEXT_DIM = 0xFF8B93A7;
    private static final int TEXT_MUTED = 0xFF5C6578;
    private static final int DIVIDER = 0x18FFFFFF;
    private static final int ROW_H = 30;
    private static final int SET_H = 24;
    private static final int NUM_H = 38;

    public ClickGUIScreen() {
        super(Component.literal("TT Client"));
    }

    @Override
    protected void init() {
        super.init();
        ModuleManager m = manager();
        if (m != null) {
            for (Category c : Category.values()) {
                if (!m.getByCategory(c).isEmpty()) {
                    selectedCategory = c;
                    List<Module> list = m.getByCategory(c);
                    if (!list.isEmpty()) selectedModule = list.get(0);
                    break;
                }
            }
        }
    }

    private static ModuleManager manager() {
        try {
            if (TTClientClient.modules != null) return TTClientClient.modules;
        } catch (Throwable ignored) {}
        try {
            return com.ttclient.TTClient.modules;
        } catch (Throwable t) {
            return null;
        }
    }

    private static PackTweaks packTweaks() {
        try {
            ModuleManager m = manager();
            return m != null ? m.getModule(PackTweaks.class) : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private int accent() {
        return ClickGUIModule.globalAccent();
    }

    private static void roundedFill(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int r, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        if (r < 1) {
            g.fill(x1, y1, x2, y2, color);
            return;
        }
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
        winW = Math.min(780, Math.max(580, this.width - 36));
        winH = Math.min(480, Math.max(360, this.height - 32));
        winX = (this.width - winW) / 2;
        winY = (this.height - winH) / 2;

        sideX = winX;
        sideY = winY + headerH;
        sideH = winH - headerH - footerH;

        detailX = winX + winW - detailW;
        detailY = sideY;
        detailH = sideH;

        listX = winX + sideW;
        listY = sideY;
        listW = winW - sideW - detailW;
        listH = sideH;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, BG_DIM);
    }

    @Override
    protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {}

    @Override
    public void extractTransparentBackground(GuiGraphicsExtractor graphics) {}

    @Override
    protected void extractMenuBackground(GuiGraphicsExtractor graphics) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        layout();
        statusTip = null;
        int ac = accent();

        // Soft outer glow + window
        roundedFill(graphics, winX - 1, winY - 1, winX + winW + 1, winY + winH + 1, 12, (ac & 0x00FFFFFF) | 0x28000000);
        roundedFill(graphics, winX, winY, winX + winW, winY + winH, 11, WIN_BG);

        renderHeader(graphics, mouseX, mouseY, ac);
        renderSidebar(graphics, mouseX, mouseY, ac);
        renderModuleList(graphics, mouseX, mouseY, ac);
        renderDetail(graphics, mouseX, mouseY, ac);
        renderFooter(graphics, mouseX, mouseY, ac);

        // Status tip (hover descriptions)
        if (statusTip != null && !statusTip.isEmpty()) {
            int tw = this.font.width(statusTip);
            int tx = Math.max(winX + 8, Math.min(mouseX + 12, winX + winW - tw - 16));
            int ty = Math.min(mouseY + 14, winY + winH - 20);
            roundedFill(graphics, tx - 6, ty - 3, tx + tw + 6, ty + 11, 4, 0xF00C0E14);
            graphics.text(this.font, statusTip, tx, ty, TEXT_DIM, false);
        }

        // Bind overlay
        if (bindingModule != null) {
            String msg = "Press a key for  " + bindingModule.getName() + "   ·   ESC to clear";
            int tw = this.font.width(msg);
            int bx = winX + (winW - tw) / 2 - 14;
            int by = winY + winH / 2 - 14;
            roundedFill(graphics, bx, by, bx + tw + 28, by + 28, 8, 0xF0080A10);
            roundedFill(graphics, bx + 1, by + 1, bx + tw + 27, by + 27, 7, 0xE0141620);
            graphics.text(this.font, msg, bx + 14, by + 10, TEXT, false);
        }
    }

    // ── Header ──────────────────────────────────────────────────────────────

    private void renderHeader(GuiGraphicsExtractor g, int mx, int my, int ac) {
        g.fill(winX + 10, winY, winX + winW - 10, winY + headerH, HEADER_BG);
        g.fill(winX + 10, winY + headerH - 1, winX + winW - 10, winY + headerH, DIVIDER);

        g.text(this.font, "TT Client", winX + 18, winY + 14, ac, false);

        int sx = winX + 110;
        int sw = winW - 130 - 24;
        int sy = winY + 10;
        int sh = 22;
        boolean hov = mx >= sx && mx < sx + sw && my >= sy && my < sy + sh;
        roundedFill(g, sx, sy, sx + sw, sy + sh, 6, searchFocused ? 0xFF1A1E2A : (hov ? 0xFF161A24 : 0xFF12151E));
        if (searchFocused) {
            g.fill(sx + 1, sy + sh - 2, sx + sw - 1, sy + sh - 1, ac);
        }
        String shown = searchQuery.isEmpty() && !searchFocused ? "Search modules…" : searchQuery + (searchFocused ? "▌" : "");
        int col = searchQuery.isEmpty() && !searchFocused ? TEXT_MUTED : TEXT;
        g.text(this.font, shown, sx + 10, sy + 7, col, false);
    }

    // ── Sidebar ─────────────────────────────────────────────────────────────

    private void renderSidebar(GuiGraphicsExtractor g, int mx, int my, int ac) {
        g.fill(sideX, sideY, sideX + sideW, sideY + sideH, SIDE_BG);
        g.fill(sideX + sideW - 1, sideY, sideX + sideW, sideY + sideH, DIVIDER);

        ModuleManager m = manager();
        int y = sideY + 10;
        for (Category cat : Category.values()) {
            if (m != null && m.getByCategory(cat).isEmpty()) continue;
            boolean sel = cat == selectedCategory;
            boolean hov = mx >= sideX + 8 && mx < sideX + sideW - 8 && my >= y && my < y + 26;
            if (sel) {
                roundedFill(g, sideX + 8, y, sideX + sideW - 8, y + 26, 6, (ac & 0x00FFFFFF) | 0x28000000);
                g.fill(sideX + 8, y + 4, sideX + 11, y + 22, ac);
            } else if (hov) {
                roundedFill(g, sideX + 8, y, sideX + sideW - 8, y + 26, 6, 0x14FFFFFF);
            }
            int col = sel ? TEXT : (hov ? TEXT_DIM : TEXT_MUTED);
            g.text(this.font, cat.name, sideX + 20, y + 9, col, false);
            y += 30;
        }
    }

    // ── Module list ─────────────────────────────────────────────────────────

    private List<Module> filteredModules() {
        ModuleManager m = manager();
        if (m == null) return List.of();
        List<Module> base;
        if (!searchQuery.isEmpty()) {
            base = m.search(searchQuery);
        } else {
            base = m.getByCategory(selectedCategory);
        }
        return base;
    }

    private void renderModuleList(GuiGraphicsExtractor g, int mx, int my, int ac) {
        g.fill(listX, listY, listX + listW, listY + listH, WIN_BG);

        List<Module> mods = filteredModules();
        int pad = 10;
        int y = listY + pad - listScroll;
        int maxScroll = Math.max(0, mods.size() * (ROW_H + 4) + pad * 2 - listH);

        for (Module mod : mods) {
            if (y + ROW_H < listY) {
                y += ROW_H + 4;
                continue;
            }
            if (y > listY + listH) break;

            boolean sel = mod == selectedModule;
            boolean hov = mx >= listX + pad && mx < listX + listW - pad && my >= y && my < y + ROW_H
                    && my >= listY && my < listY + listH;
            boolean on = mod.isEnabled();

            int bg = sel ? CARD_SEL : (hov ? CARD_HOV : CARD);
            roundedFill(g, listX + pad, y, listX + listW - pad, y + ROW_H, 7, bg);
            if (sel) {
                g.fill(listX + pad, y + 6, listX + pad + 2, y + ROW_H - 6, ac);
            }

            int nameCol = on ? TEXT : TEXT_DIM;
            g.text(this.font, mod.getName(), listX + pad + 12, y + 10, nameCol, false);

            int bind = mod.getKeyBind();
            if (bind > 0) {
                String k = keyLabel(bind);
                int kw = this.font.width(k);
                int kx = listX + listW - pad - 52 - kw;
                roundedFill(g, kx - 4, y + 7, kx + kw + 4, y + 21, 4, 0x22FFFFFF);
                g.text(this.font, k, kx, y + 10, TEXT_MUTED, false);
            }

            int swX = listX + listW - pad - 36;
            int swY = y + 8;
            int swW = 28;
            int swH = 14;
            roundedFill(g, swX, swY, swX + swW, swY + swH, 7, on ? ((ac & 0x00FFFFFF) | 0xCC000000) : 0xFF2A2E3A);
            int knobX = on ? swX + swW - 12 : swX + 2;
            roundedFill(g, knobX, swY + 2, knobX + 10, swY + swH - 2, 5, on ? 0xFFFFFFFF : 0xFF8B93A7);

            if (hov && mod.getDescription() != null && !mod.getDescription().isEmpty()) {
                statusTip = mod.getDescription();
            }

            y += ROW_H + 4;
        }

        if (maxScroll > 0) {
            int trackH = listH - 16;
            int thumbH = Math.max(16, (int) (trackH * (listH / (float) (listH + maxScroll))));
            int thumbY = listY + 8 + (int) ((trackH - thumbH) * (listScroll / (float) maxScroll));
            g.fill(listX + listW - 4, listY + 8, listX + listW - 2, listY + listH - 8, 0x18FFFFFF);
            g.fill(listX + listW - 4, thumbY, listX + listW - 2, thumbY + thumbH, 0x44FFFFFF);
        }
    }

    // ── Detail panel ────────────────────────────────────────────────────────

    private void renderDetail(GuiGraphicsExtractor g, int mx, int my, int ac) {
        g.fill(detailX, detailY, detailX + detailW, detailY + detailH, SIDE_BG);
        g.fill(detailX, detailY, detailX + 1, detailY + detailH, DIVIDER);

        if (selectedModule == null) {
            g.text(this.font, "Select a module", detailX + 16, detailY + 20, TEXT_MUTED, false);
            return;
        }

        Module mod = selectedModule;
        int pad = 14;
        int y = detailY + 14 - detailScroll;

        g.text(this.font, mod.getName(), detailX + pad, y, TEXT, false);
        y += 14;
        String desc = mod.getDescription();
        if (desc != null && !desc.isEmpty()) {
            int maxW = detailW - pad * 2;
            StringBuilder line = new StringBuilder();
            for (String word : desc.split(" ")) {
                String test = line.isEmpty() ? word : line + " " + word;
                if (this.font.width(test) > maxW) {
                    if (!line.isEmpty()) {
                        g.text(this.font, line.toString(), detailX + pad, y, TEXT_MUTED, false);
                        y += 11;
                        line = new StringBuilder(word);
                    } else {
                        g.text(this.font, word, detailX + pad, y, TEXT_MUTED, false);
                        y += 11;
                    }
                } else {
                    line = new StringBuilder(test);
                }
            }
            if (!line.isEmpty()) {
                g.text(this.font, line.toString(), detailX + pad, y, TEXT_MUTED, false);
                y += 11;
            }
        }
        y += 8;
        g.fill(detailX + pad, y, detailX + detailW - pad, y + 1, DIVIDER);
        y += 10;

        boolean bindHov = mx >= detailX + pad && mx < detailX + detailW - pad && my >= y && my < y + SET_H
                && my >= detailY && my < detailY + detailH;
        if (bindHov) roundedFill(g, detailX + pad - 2, y - 1, detailX + detailW - pad + 2, y + SET_H, 5, 0x14FFFFFF);
        g.text(this.font, "Bind", detailX + pad, y + 6, TEXT_DIM, false);
        String bindTxt = bindingModule == mod ? "..." : (mod.getKeyBind() > 0 ? keyLabel(mod.getKeyBind()) : "None");
        int bw = this.font.width(bindTxt);
        roundedFill(g, detailX + detailW - pad - bw - 12, y + 2, detailX + detailW - pad, y + SET_H - 2, 5,
                bindingModule == mod ? ((ac & 0x00FFFFFF) | 0x44000000) : 0x22FFFFFF);
        g.text(this.font, bindTxt, detailX + detailW - pad - bw - 6, y + 6, TEXT, false);
        y += SET_H + 6;

        List<Setting<?>> settings = mod.getSettings();
        if (settings == null || settings.isEmpty()) {
            g.text(this.font, "No settings", detailX + pad, y + 4, TEXT_MUTED, false);
            return;
        }

        for (Setting<?> s : settings) {
            if (y > detailY + detailH) break;
            if (y + SET_H < detailY) {
                y += settingHeight(s);
                continue;
            }

            if (s instanceof BoolSetting bs) {
                boolean hov = mx >= detailX + pad && mx < detailX + detailW - pad && my >= y && my < y + SET_H
                        && my >= detailY && my < detailY + detailH;
                if (hov) {
                    roundedFill(g, detailX + pad - 2, y - 1, detailX + detailW - pad + 2, y + SET_H, 5, 0x14FFFFFF);
                    if (bs.getDescription() != null) statusTip = bs.getDescription();
                }
                g.text(this.font, bs.getName(), detailX + pad, y + 6, TEXT_DIM, false);
                int swX = detailX + detailW - pad - 28;
                int swY = y + 5;
                boolean on = Boolean.TRUE.equals(bs.get());
                roundedFill(g, swX, swY, swX + 26, swY + 13, 6, on ? ((ac & 0x00FFFFFF) | 0xCC000000) : 0xFF2A2E3A);
                int knobX = on ? swX + 14 : swX + 2;
                roundedFill(g, knobX, swY + 2, knobX + 10, swY + 11, 4, on ? 0xFFFFFFFF : 0xFF8B93A7);
                y += SET_H + 2;
            } else if (s instanceof ModeSetting ms) {
                boolean hov = mx >= detailX + pad && mx < detailX + detailW - pad && my >= y && my < y + SET_H
                        && my >= detailY && my < detailY + detailH;
                if (hov) {
                    roundedFill(g, detailX + pad - 2, y - 1, detailX + detailW - pad + 2, y + SET_H, 5, 0x14FFFFFF);
                    if (ms.getDescription() != null) statusTip = ms.getDescription();
                }
                g.text(this.font, ms.getName(), detailX + pad, y + 6, TEXT_DIM, false);
                String val = String.valueOf(ms.get());
                int vw = this.font.width(val);
                roundedFill(g, detailX + detailW - pad - vw - 12, y + 2, detailX + detailW - pad, y + SET_H - 2, 5, 0x22FFFFFF);
                g.text(this.font, val, detailX + detailW - pad - vw - 6, y + 6, TEXT, false);
                y += SET_H + 2;
            } else if (s instanceof NumberSetting ns) {
                boolean hov = mx >= detailX + pad && mx < detailX + detailW - pad && my >= y && my < y + NUM_H
                        && my >= detailY && my < detailY + detailH;
                if (hov && ns.getDescription() != null) statusTip = ns.getDescription();
                g.text(this.font, ns.getName(), detailX + pad, y + 4, TEXT_DIM, false);
                String disp = typing == ns ? typingBuffer + "▌" : ns.getDisplay();
                int dw = this.font.width(disp);
                g.text(this.font, disp, detailX + detailW - pad - dw, y + 4, TEXT, false);

                int trackX = detailX + pad;
                int trackW = detailW - pad * 2;
                int trackY = y + 20;
                g.fill(trackX, trackY + 3, trackX + trackW, trackY + 5, 0x33FFFFFF);
                double t = (ns.get() - ns.getMin()) / (ns.getMax() - ns.getMin());
                int fillW = (int) (trackW * Math.max(0, Math.min(1, t)));
                g.fill(trackX, trackY + 3, trackX + fillW, trackY + 5, ac);
                int knobX = trackX + fillW - 4;
                roundedFill(g, knobX, trackY, knobX + 8, trackY + 8, 4, 0xFFFFFFFF);
                y += NUM_H + 2;
            } else if (s instanceof ColorSetting cs) {
                g.text(this.font, cs.getName(), detailX + pad, y + 6, TEXT_DIM, false);
                int cx = detailX + detailW - pad - 18;
                roundedFill(g, cx, y + 4, cx + 16, y + SET_H - 4, 4, cs.get() | 0xFF000000);
                y += SET_H + 2;
            } else {
                g.text(this.font, s.getName(), detailX + pad, y + 6, TEXT_DIM, false);
                y += SET_H + 2;
            }
        }
    }

    private int settingHeight(Setting<?> s) {
        if (s instanceof NumberSetting) return NUM_H + 2;
        return SET_H + 2;
    }

    // ── Footer (Pack chips) ─────────────────────────────────────────────────

    private void renderFooter(GuiGraphicsExtractor g, int mx, int my, int ac) {
        int fy = winY + winH - footerH;
        g.fill(winX + 10, fy, winX + winW - 10, fy + 1, DIVIDER);
        g.fill(winX + 10, fy + 1, winX + winW - 10, winY + winH - 8, HEADER_BG);

        packChipBounds.clear();
        PackTweaks pt = packTweaks();
        if (pt == null) {
            g.text(this.font, "Pack", winX + 18, fy + 14, TEXT_MUTED, false);
            return;
        }

        g.text(this.font, "Pack", winX + 16, fy + 14, TEXT_MUTED, false);

        BoolSetting[] chips = {
                pt.lowFire, pt.smallTotems, pt.noPumpkin, pt.rainbowTools, pt.rainbowBlocks, pt.forceGlint
        };
        int x = winX + 48;
        int cy = fy + 10;
        for (BoolSetting chip : chips) {
            if (chip == null) continue;
            String label = chip.getName();
            int lw = this.font.width(label);
            int cw = lw + 16;
            if (x + cw > winX + winW - 16) break;
            boolean on = Boolean.TRUE.equals(chip.get());
            boolean hov = mx >= x && mx < x + cw && my >= cy && my < cy + 20;
            int bg = on ? ((ac & 0x00FFFFFF) | 0x44000000) : (hov ? 0x22FFFFFF : 0x14FFFFFF);
            roundedFill(g, x, cy, x + cw, cy + 20, 6, bg);
            if (on) g.fill(x + 1, cy + 18, x + cw - 1, cy + 19, ac);
            g.text(this.font, label, x + 8, cy + 6, on ? TEXT : TEXT_DIM, false);
            packChipBounds.add(new int[]{x, cy, x + cw, cy + 20, System.identityHashCode(chip)});
            x += cw + 6;
        }
    }

    // ── Input ───────────────────────────────────────────────────────────────

    private void saveConfig() {
        try {
            if (TTClientClient.config != null) TTClientClient.config.save();
        } catch (Throwable ignored) {}
    }

    private static String keyLabel(int key) {
        if (key <= 0) return "None";
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
            return String.valueOf((char) ('A' + (key - GLFW.GLFW_KEY_A)));
        }
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) ('0' + (key - GLFW.GLFW_KEY_0)));
        }
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
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_BACKSPACE -> "BKSP";
            case GLFW.GLFW_KEY_DELETE -> "DEL";
            case GLFW.GLFW_KEY_INSERT -> "INS";
            case GLFW.GLFW_KEY_HOME -> "HOME";
            case GLFW.GLFW_KEY_END -> "END";
            case GLFW.GLFW_KEY_PAGE_UP -> "PGUP";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PGDN";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            case GLFW.GLFW_KEY_F1 -> "F1";
            case GLFW.GLFW_KEY_F2 -> "F2";
            case GLFW.GLFW_KEY_F3 -> "F3";
            case GLFW.GLFW_KEY_F4 -> "F4";
            case GLFW.GLFW_KEY_F5 -> "F5";
            case GLFW.GLFW_KEY_F6 -> "F6";
            case GLFW.GLFW_KEY_F7 -> "F7";
            case GLFW.GLFW_KEY_F8 -> "F8";
            case GLFW.GLFW_KEY_F9 -> "F9";
            case GLFW.GLFW_KEY_F10 -> "F10";
            case GLFW.GLFW_KEY_F11 -> "F11";
            case GLFW.GLFW_KEY_F12 -> "F12";
            default -> "K" + key;
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (handled) return true;
        int mx = (int) event.x();
        int my = (int) event.y();
        int button = event.button();
        layout();

        int sx = winX + 110;
        int sw = winW - 130 - 24;
        int sy = winY + 10;
        if (mx >= sx && mx < sx + sw && my >= sy && my < sy + 22) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;

        ModuleManager m = manager();
        int y = sideY + 10;
        if (m != null) {
            for (Category cat : Category.values()) {
                if (m.getByCategory(cat).isEmpty()) continue;
                if (mx >= sideX + 8 && mx < sideX + sideW - 8 && my >= y && my < y + 26) {
                    selectedCategory = cat;
                    searchQuery = "";
                    listScroll = 0;
                    List<Module> list = m.getByCategory(cat);
                    selectedModule = list.isEmpty() ? null : list.get(0);
                    detailScroll = 0;
                    return true;
                }
                y += 30;
            }
        }

        List<Module> mods = filteredModules();
        int pad = 10;
        y = listY + pad - listScroll;
        for (Module mod : mods) {
            if (y + ROW_H < listY) {
                y += ROW_H + 4;
                continue;
            }
            if (y > listY + listH) break;

            if (mx >= listX + pad && mx < listX + listW - pad && my >= y && my < y + ROW_H
                    && my >= listY && my < listY + listH) {
                int swX = listX + listW - pad - 36;
                if (button == 0 && mx >= swX && mx < swX + 28) {
                    mod.toggle();
                    saveConfig();
                    return true;
                }
                if (button == 1) {
                    bindingModule = mod;
                    suppressBindClick = true;
                    return true;
                }
                if (button == 0) {
                    if (selectedModule == mod) {
                        mod.toggle();
                        saveConfig();
                    }
                    selectedModule = mod;
                    detailScroll = 0;
                    return true;
                }
            }
            y += ROW_H + 4;
        }

        if (selectedModule != null && mx >= detailX && mx < detailX + detailW && my >= detailY && my < detailY + detailH) {
            Module mod = selectedModule;
            y = detailY + 14 - detailScroll;

            y += 14;
            String desc = mod.getDescription();
            if (desc != null && !desc.isEmpty()) {
                int maxW = detailW - 28;
                StringBuilder line = new StringBuilder();
                int lines = 0;
                for (String word : desc.split(" ")) {
                    String test = line.isEmpty() ? word : line + " " + word;
                    if (this.font.width(test) > maxW) {
                        lines++;
                        line = new StringBuilder(word);
                    } else {
                        line = new StringBuilder(test);
                    }
                }
                if (!line.isEmpty()) lines++;
                y += lines * 11;
            }
            y += 18;

            if (my >= y && my < y + SET_H && button == 0) {
                bindingModule = mod;
                suppressBindClick = true;
                return true;
            }
            y += SET_H + 6;

            for (Setting<?> s : mod.getSettings()) {
                if (s instanceof BoolSetting bs) {
                    if (my >= y && my < y + SET_H && button == 0) {
                        bs.toggle();
                        saveConfig();
                        return true;
                    }
                    y += SET_H + 2;
                } else if (s instanceof ModeSetting ms) {
                    if (my >= y && my < y + SET_H && button == 0) {
                        ms.cycle();
                        saveConfig();
                        return true;
                    }
                    y += SET_H + 2;
                } else if (s instanceof NumberSetting ns) {
                    int trackX = detailX + 14;
                    int trackW = detailW - 28;
                    int trackY = y + 20;
                    if (my >= y && my < y + NUM_H && button == 0) {
                        if (my >= trackY - 4 && my <= trackY + 12) {
                            sliding = ns;
                            sliderTrackX = trackX;
                            sliderTrackW = trackW;
                            applySlider(ns, mx);
                            return true;
                        }
                        typing = ns;
                        typingBuffer = ns.getDisplay();
                        return true;
                    }
                    y += NUM_H + 2;
                } else {
                    y += SET_H + 2;
                }
            }
        }

        if (button == 0) {
            PackTweaks pt = packTweaks();
            if (pt != null) {
                for (int[] b : packChipBounds) {
                    if (mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) {
                        BoolSetting[] chips = {
                                pt.lowFire, pt.smallTotems, pt.noPumpkin,
                                pt.rainbowTools, pt.rainbowBlocks, pt.forceGlint
                        };
                        for (BoolSetting chip : chips) {
                            if (chip != null && System.identityHashCode(chip) == b[4]) {
                                chip.toggle();
                                saveConfig();
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    private void applySlider(NumberSetting ns, int mx) {
        double t = (mx - sliderTrackX) / (double) sliderTrackW;
        t = Math.max(0, Math.min(1, t));
        double v = ns.getMin() + t * (ns.getMax() - ns.getMin());
        ns.set(v);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (sliding != null) {
            applySlider(sliding, (int) event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (sliding != null) {
            sliding = null;
            saveConfig();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        layout();
        int amount = (int) (-scrollY * 18);
        if (mx >= listX && mx < listX + listW && my >= listY && my < listY + listH) {
            List<Module> mods = filteredModules();
            int maxScroll = Math.max(0, mods.size() * (ROW_H + 4) + 20 - listH);
            listScroll = Math.max(0, Math.min(maxScroll, listScroll + amount));
            return true;
        }
        if (mx >= detailX && mx < detailX + detailW && my >= detailY && my < detailY + detailH) {
            detailScroll = Math.max(0, detailScroll + amount);
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();

        if (typing != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                try {
                    typing.set(Double.parseDouble(typingBuffer));
                    saveConfig();
                } catch (Exception ignored) {}
                typing = null;
                typingBuffer = "";
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                typing = null;
                typingBuffer = "";
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !typingBuffer.isEmpty()) {
                typingBuffer = typingBuffer.substring(0, typingBuffer.length() - 1);
                return true;
            }
            if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
                typingBuffer += (char) ('0' + (keyCode - GLFW.GLFW_KEY_0));
                return true;
            }
            if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
                typingBuffer += (char) ('0' + (keyCode - GLFW.GLFW_KEY_KP_0));
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_PERIOD || keyCode == GLFW.GLFW_KEY_KP_DECIMAL) {
                if (!typingBuffer.contains(".")) typingBuffer += '.';
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_MINUS || keyCode == GLFW.GLFW_KEY_KP_SUBTRACT) {
                if (typingBuffer.isEmpty()) typingBuffer = "-";
                return true;
            }
            return true;
        }

        if (bindingModule != null) {
            if (suppressBindClick) {
                suppressBindClick = false;
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    bindingModule = null;
                    return true;
                }
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                bindingModule.setKeyBind(-1);
            } else if (keyCode > 0 && keyCode != GLFW.GLFW_KEY_RIGHT_SHIFT) {
                bindingModule.setKeyBind(keyCode);
            }
            bindingModule = null;
            saveConfig();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            searchFocused = true;
            listScroll = 0;
            return true;
        }
        if (searchQuery.length() < 40) {
            if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
                char c = (char) ('a' + (keyCode - GLFW.GLFW_KEY_A));
                searchQuery += c;
                searchFocused = true;
                listScroll = 0;
                return true;
            }
            if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
                searchQuery += (char) ('0' + (keyCode - GLFW.GLFW_KEY_0));
                searchFocused = true;
                listScroll = 0;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_SPACE) {
                searchQuery += ' ';
                searchFocused = true;
                return true;
            }
        }

        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        try {
            ModuleManager m = manager();
            if (m != null) {
                ClickGUIModule gui = m.getModule(ClickGUIModule.class);
                if (gui != null && gui.isEnabled()) gui.setEnabled(false);
            }
        } catch (Throwable ignored) {}
        saveConfig();
        super.onClose();
    }
}

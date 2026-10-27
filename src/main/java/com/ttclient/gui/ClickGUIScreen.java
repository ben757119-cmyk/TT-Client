package com.ttclient.gui;

import com.ttclient.TTClient;
import com.ttclient.config.ConfigManager;
import com.ttclient.client.TTClientClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.modules.client.ClickGUIModule;
import com.ttclient.modules.client.PackTweaks;
import com.ttclient.modules.render.BlockESP;
import com.ttclient.settings.BoolSetting;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ClickGUIScreen extends Screen {
    private final Map<Category, Panel> panels = new HashMap<>();
    private final List<Panel> panelList = new ArrayList<>();
    private Module bindingModule = null;
    private boolean suppressBindClick = false;
    private Module hoveredModule = null;
    private String searchQuery = "";
    private boolean searchFocused = true;
    private NumberSetting sliding = null;
    private int sliderTrackX, sliderTrackW;
    private NumberSetting typing = null;
    private String typingBuffer = "";

    private static final int PANEL_BG = 0xF8161A24, HEADER_BG = 0xFF0C0E14, TEXT_DIM = 0xFF6B7280, ROW_OFF = 0xFF9CA3AF;
    private static final int SLIDER_BG = 0xFF1A1E28, SEARCH_BG = 0xF80C0E14, ROW_ON_BG = 0x2800E8A0;
    private static final int ROW_H = 14, SETTING_H = 12, NUMBER_H = 26;
    private static final int SEARCH_X = 10, SEARCH_Y = 8, SEARCH_W = 160, SEARCH_H = 16;
    private static final int PANEL_W = 100, PANEL_GAP = 6, PANEL_START_Y = 8;
    private static final int PACK_STRIP_H = 50;
    private static final int PACK_CHIP_H = 16;
    private final List<int[]> packChipBounds = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();

    private static void roundedFill(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int r, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        if (r < 1) { g.fill(x1, y1, x2, y2, color); return; }
        r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        g.fill(x1 + r, y1, x2 - r, y2, color);
        g.fill(x1, y1 + r, x1 + r, y2 - r, color);
        g.fill(x2 - r, y1 + r, x2, y2 - r, color);
        for (int i = 0; i < r; i++) {
            int off = r - 1 - (int) Math.sqrt((double) (r * r - (r - 1 - i) * (r - 1 - i)));
            g.fill(x1 + off, y1 + i, x1 + r, y1 + i + 1, color);
            g.fill(x2 - r, y1 + i, x2 - off, y1 + i + 1, color);
            g.fill(x1 + off, y2 - 1 - i, x1 + r, y2 - i, color);
            g.fill(x2 - r, y2 - 1 - i, x2 - off, y2 - i, color);
        }
    }

    private static final class Particle {
        float x, y, vy, size; int alpha;
        Particle(float x, float y) {
            this.x = x; this.y = y;
            this.vy = 0.18f + (float) Math.random() * 1.15f;
            this.size = 1f + (float) Math.random() * 4.5f;
            this.alpha = 0x28 + (int) (Math.random() * 0x50);
        }
    }

    private int accent() {
        return ClickGUIModule.globalAccent();
    }

    private static com.ttclient.modules.ModuleManager manager() {
        return TTClientClient.modules != null ? TTClientClient.modules : TTClient.modules;
    }

    public ClickGUIScreen() {
        super(Component.literal("TT Client"));
        int x = SEARCH_X + SEARCH_W + 18;
        for (Category cat : Category.values()) {
            if (cat == Category.PACK) continue;
            if (manager() != null && manager().getByCategory(cat).isEmpty()) continue;
            double px = x, py = PANEL_START_Y; boolean isOpen = true;
            double[] saved = ConfigManager.PANEL_LAYOUT.get(cat.name);
            if (saved != null && saved.length >= 2) {
                px = saved[0]; py = saved[1];
                if (saved.length >= 3) isOpen = saved[2] >= 0.5;
            }
            Panel p = new Panel(cat, px, py); p.open = isOpen;
            panels.put(cat, p); panelList.add(p);
            x += PANEL_W + PANEL_GAP;
        }
    }

    private void savePanelLayout() {
        java.util.HashMap<String, double[]> layout = new java.util.HashMap<>();
        for (Panel p : panelList) layout.put(p.category.name, new double[]{p.x, p.y, p.open ? 1.0 : 0.0});
        ConfigManager.PANEL_LAYOUT.clear();
        ConfigManager.PANEL_LAYOUT.putAll(layout);
        if (TTClientClient.config != null) TTClientClient.config.save();
    }

    @Override public boolean isInGameUi() { return true; }
    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xE002040A);
        graphics.fill(0, 0, this.width, this.height, 0x66000000);
        if (particles.isEmpty() && this.width > 0) {
            for (int i = 0; i < 110; i++)
                particles.add(new Particle((float) (Math.random() * this.width), (float) (Math.random() * this.height)));
        }
        int acRgb = accent() & 0x00FFFFFF;
        for (Particle p : particles) {
            p.y += p.vy;
            if (p.y > this.height) {
                p.y = -4;
                p.x = (float) (Math.random() * this.width);
            }
            int s = Math.max(1, (int) p.size);
            graphics.fill((int) p.x, (int) p.y, (int) p.x + s, (int) p.y + s, (p.alpha << 24) | acRgb);
        }
    }

    @Override protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {}
    @Override public void extractTransparentBackground(GuiGraphicsExtractor graphics) {}
    @Override protected void extractMenuBackground(GuiGraphicsExtractor graphics) {}

    private int settingRowHeight(Setting<?> s) {
        return s instanceof NumberSetting ? NUMBER_H : SETTING_H;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        String shown = searchQuery.isEmpty() ? (searchFocused ? "Search..." : "Search") : searchQuery;
        roundedFill(graphics, SEARCH_X, SEARCH_Y, SEARCH_X + SEARCH_W, SEARCH_Y + SEARCH_H, 5, SEARCH_BG);
        graphics.fill(SEARCH_X, SEARCH_Y + 3, SEARCH_X + 2, SEARCH_Y + SEARCH_H - 3, accent());
        graphics.text(this.font, shown, SEARCH_X + 8, SEARCH_Y + 4, searchQuery.isEmpty() ? TEXT_DIM : 0xFFE8EAED, false);
        graphics.text(this.font, "tt " + TTClient.VERSION, SEARCH_X + 4, SEARCH_Y + SEARCH_H + 4, accent(), false);

        for (Panel panel : panelList) panel.render(graphics, mouseX, mouseY);
        renderPackStrip(graphics, mouseX, mouseY);

        if (hoveredModule != null) {
            String tip = hoveredModule.getDescription();
            if (tip != null && !tip.isEmpty()) {
                int tw = this.font.width(tip) + 10;
                int tx = Math.min(mouseX + 12, this.width - tw - 4);
                int ty = mouseY + 12;
                roundedFill(graphics, tx, ty, tx + tw, ty + 14, 3, 0xF00C0E14);
                graphics.text(this.font, tip, tx + 5, ty + 3, 0xFFE8EAED, false);
            }
        }
    }

    @Override public void onClose() {
        commitTyping(true); sliding = null;
        for (Panel p : panelList) if (p.expanded instanceof BlockESP be) be.uiSearchFocused = false;
        savePanelLayout();
        super.onClose();
    }

    private void applySlider(NumberSetting ns, double mouseX, int trackX, int trackW) {
        double t = Math.max(0, Math.min(1, (mouseX - trackX) / (double) trackW));
        ns.set(ns.getMin() + t * (ns.getMax() - ns.getMin()));
    }

    private void startTyping(NumberSetting ns) {
        commitTyping(true); typing = ns; typingBuffer = ns.getDisplay(); sliding = null; searchFocused = false;
    }

    private void commitTyping(boolean apply) {
        if (typing == null) return;
        if (apply) {
            try { typing.set(Double.parseDouble(typingBuffer)); } catch (Exception ignored) {}
        }
        typing = null; typingBuffer = "";
    }

    private PackTweaks packTweaks() {
        try {
            return manager() != null ? manager().getModule(PackTweaks.class) : null;
        } catch (Throwable t) { return null; }
    }

    private void renderPackStrip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        packChipBounds.clear();
        PackTweaks pt = packTweaks();
        if (pt == null) return;
        int ac = accent();
        int y0 = this.height - PACK_STRIP_H - 6;
        int x0 = 10;
        int w0 = this.width - 20;
        roundedFill(g, x0, y0, x0 + w0, y0 + PACK_STRIP_H, 6, 0xF80C0E14);
        g.fill(x0, y0, x0 + 3, y0 + PACK_STRIP_H, ac);
        g.text(this.font, "RESOURCE PACK", x0 + 10, y0 + 5, ac, false);
        boolean on = pt.isEnabled();
        String master = on ? "ON" : "OFF";
        int mw = this.font.width(master) + 12;
        int mx = x0 + w0 - mw - 8;
        int my = y0 + 3;
        roundedFill(g, mx, my, mx + mw, my + 12, 3, on ? 0x4000E8A0 : 0x28FFFFFF);
        g.text(this.font, master, mx + 6, my + 2, on ? ac : TEXT_DIM, false);
        packChipBounds.add(new int[]{mx, my, mw, 12, -1});

        BoolSetting[] chips = {
                pt.lowFire, pt.smallTotems, pt.lowShield, pt.noPumpkin,
                pt.cleanScoreboard, pt.rainbowTools, pt.rainbowBlocks, pt.forceGlint
        };
        int cx = x0 + 10;
        int cy = y0 + 22;
        for (int i = 0; i < chips.length; i++) {
            BoolSetting s = chips[i];
            String lab = s.getName();
            int cw = this.font.width(lab) + 14;
            if (cx + cw > x0 + w0 - 8) break;
            boolean son = Boolean.TRUE.equals(s.get());
            boolean hov = mouseX >= cx && mouseX <= cx + cw && mouseY >= cy && mouseY <= cy + PACK_CHIP_H;
            int bg = son ? 0x4000E8A0 : (hov ? 0x28FFFFFF : 0x18FFFFFF);
            roundedFill(g, cx, cy, cx + cw, cy + PACK_CHIP_H, 4, bg);
            if (son) g.fill(cx, cy, cx + 2, cy + PACK_CHIP_H, ac);
            g.text(this.font, lab, cx + 7, cy + 4, son ? 0xFFE8EAED : TEXT_DIM, false);
            packChipBounds.add(new int[]{cx, cy, cw, PACK_CHIP_H, i});
            cx += cw + 5;
        }
    }

    private boolean clickPackStrip(double mouseX, double mouseY) {
        PackTweaks pt = packTweaks();
        if (pt == null) return false;
        BoolSetting[] chips = {
                pt.lowFire, pt.smallTotems, pt.lowShield, pt.noPumpkin,
                pt.cleanScoreboard, pt.rainbowTools, pt.rainbowBlocks, pt.forceGlint
        };
        for (int[] b : packChipBounds) {
            if (mouseX >= b[0] && mouseX <= b[0] + b[2] && mouseY >= b[1] && mouseY <= b[1] + b[3]) {
                int idx = b[4];
                if (idx == -1) pt.toggle();
                else if (idx >= 0 && idx < chips.length) {
                    chips[idx].set(!Boolean.TRUE.equals(chips[idx].get()));
                    if (!pt.isEnabled()) pt.setEnabled(true);
                }
                if (TTClientClient.config != null) TTClientClient.config.save();
                return true;
            }
        }
        return false;
    }

    public class Panel {
        private final Category category;
        double x, y;
        boolean open = true;
        boolean dragging;
        double dragOffX, dragOffY;
        Module expanded;

        public Panel(Category category, double x, double y) { this.category = category; this.x = x; this.y = y; }

        private List<Module> modules() {
            List<Module> base = manager() != null ? manager().getByCategory(category) : List.of();
            if (searchQuery.isEmpty()) return base;
            String q = searchQuery.toLowerCase(Locale.ROOT);
            List<Module> out = new ArrayList<>();
            for (Module m : base) {
                if (m.getName().toLowerCase(Locale.ROOT).contains(q)) out.add(m);
            }
            return out;
        }

        public void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
            int px = (int) x, py = (int) y;
            int ac = accent();
            List<Module> mods = modules();
            int h = ROW_H + 4;
            if (open) {
                for (Module m : mods) {
                    h += ROW_H;
                    if (expanded == m) for (Setting<?> s : m.getSettings()) h += settingRowHeight(s);
                }
            }
            roundedFill(g, px, py, px + PANEL_W, py + h, 5, PANEL_BG);
            roundedFill(g, px, py, px + PANEL_W, py + ROW_H, 5, HEADER_BG);
            g.fill(px, py, px + 2, py + ROW_H, ac);
            g.text(font, category.name, px + 6, py + 3, ac, false);
            g.text(font, open ? "-" : "+", px + PANEL_W - 10, py + 3, TEXT_DIM, false);
            if (!open) return;
            int rowY = py + ROW_H + 2;
            for (Module m : mods) {
                boolean hov = mouseX >= px && mouseX <= px + PANEL_W && mouseY >= rowY && mouseY <= rowY + ROW_H;
                if (hov) hoveredModule = m;
                if (m.isEnabled()) roundedFill(g, px + 2, rowY, px + PANEL_W - 2, rowY + ROW_H, 3, ROW_ON_BG);
                g.text(font, m.getName(), px + 6, rowY + 3, m.isEnabled() ? 0xFFE8EAED : ROW_OFF, false);
                rowY += ROW_H;
                if (expanded == m) {
                    for (Setting<?> s : m.getSettings()) {
                        if (s instanceof BoolSetting bs) {
                            g.text(font, bs.getName(), px + 10, rowY + 2, TEXT_DIM, false);
                            String v = Boolean.TRUE.equals(bs.get()) ? "ON" : "OFF";
                            g.text(font, v, px + PANEL_W - 8 - font.width(v), rowY + 2, Boolean.TRUE.equals(bs.get()) ? ac : ROW_OFF, false);
                            rowY += SETTING_H;
                        } else if (s instanceof ModeSetting ms) {
                            g.text(font, ms.getName(), px + 10, rowY + 2, TEXT_DIM, false);
                            String v = String.valueOf(ms.get());
                            g.text(font, v, px + PANEL_W - 8 - font.width(v), rowY + 2, ac, false);
                            rowY += SETTING_H;
                        } else if (s instanceof NumberSetting ns) {
                            g.text(font, ns.getName(), px + 10, rowY + 2, TEXT_DIM, false);
                            String disp = typing == ns ? typingBuffer + "_" : ns.getDisplay();
                            g.text(font, disp, px + PANEL_W - 8 - font.width(disp), rowY + 2, ac, false);
                            int trackX = px + 10, trackW = PANEL_W - 20, trackY = rowY + 14;
                            g.fill(trackX, trackY, trackX + trackW, trackY + 3, SLIDER_BG);
                            double t = (ns.get() - ns.getMin()) / (ns.getMax() - ns.getMin());
                            int fill = (int) (trackW * Math.max(0, Math.min(1, t)));
                            g.fill(trackX, trackY, trackX + fill, trackY + 3, ac);
                            rowY += NUMBER_H;
                        } else {
                            rowY += SETTING_H;
                        }
                    }
                }
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int px = (int) x, py = (int) y;
            if (mouseX < px || mouseX > px + PANEL_W) return false;
            if (mouseY >= py && mouseY <= py + ROW_H) {
                if (button == 1 || button == 0) {
                    if (mouseX > px + PANEL_W - 16) { open = !open; savePanelLayout(); return true; }
                    if (button == 0) { dragging = true; dragOffX = mouseX - x; dragOffY = mouseY - y; return true; }
                    open = !open; savePanelLayout(); return true;
                }
            }
            if (!open) return false;
            int rowY = py + ROW_H + 2;
            for (Module m : modules()) {
                if (mouseY >= rowY && mouseY <= rowY + ROW_H) {
                    if (button == 0) { m.toggle(); if (TTClientClient.config != null) TTClientClient.config.save(); return true; }
                    if (button == 1) { expanded = expanded == m ? null : m; return true; }
                    return true;
                }
                rowY += ROW_H;
                if (expanded == m) {
                    for (Setting<?> s : m.getSettings()) {
                        int rh = settingRowHeight(s);
                        if (mouseY >= rowY && mouseY <= rowY + rh) {
                            if (s instanceof BoolSetting bs && button == 0) {
                                bs.set(!Boolean.TRUE.equals(bs.get()));
                                if (TTClientClient.config != null) TTClientClient.config.save();
                                return true;
                            }
                            if (s instanceof ModeSetting ms && button == 0) {
                                ms.cycle();
                                if (TTClientClient.config != null) TTClientClient.config.save();
                                return true;
                            }
                            if (s instanceof NumberSetting ns) {
                                if (button == 0) {
                                    sliding = ns; sliderTrackX = px + 10; sliderTrackW = PANEL_W - 20;
                                    applySlider(ns, mouseX, sliderTrackX, sliderTrackW);
                                    return true;
                                }
                                if (button == 1) { startTyping(ns); return true; }
                            }
                            return true;
                        }
                        rowY += rh;
                    }
                }
            }
            return false;
        }

        public boolean mouseDragged(double mouseX, double mouseY, int button) {
            if (dragging && button == 0) {
                x = mouseX - dragOffX; y = mouseY - dragOffY;
                return true;
            }
            return false;
        }

        public void mouseReleased(int button) {
            if (button == 0 && dragging) { dragging = false; savePanelLayout(); }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        hoveredModule = null;
        if (bindingModule != null && button >= 0) {
            if (suppressBindClick) { suppressBindClick = false; return true; }
            bindingModule.setKeyBind(2000 + button);
            bindingModule = null;
            if (TTClientClient.config != null) TTClientClient.config.save();
            return true;
        }
        if (button == 0 && mouseX >= SEARCH_X && mouseX <= SEARCH_X + SEARCH_W && mouseY >= SEARCH_Y && mouseY <= SEARCH_Y + SEARCH_H) {
            searchFocused = true; commitTyping(true); return true;
        }
        if (button == 0 && clickPackStrip(mouseX, mouseY)) return true;
        for (int i = panelList.size() - 1; i >= 0; i--) {
            if (panelList.get(i).mouseClicked(mouseX, mouseY, button)) return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x(), mouseY = event.y();
        if (sliding != null) { applySlider(sliding, mouseX, sliderTrackX, sliderTrackW); return true; }
        for (Panel panel : panelList) if (panel.mouseDragged(mouseX, mouseY, event.button())) return true;
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        sliding = null;
        for (Panel panel : panelList) panel.mouseReleased(event.button());
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (typing != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { commitTyping(true); return true; }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { commitTyping(false); return true; }
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
                if (!typingBuffer.contains(".")) typingBuffer += '.';
                return true;
            }
            return true;
        }
        if (bindingModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) bindingModule.setKeyBind(-1);
            else if (keyCode > 0) bindingModule.setKeyBind(keyCode);
            bindingModule = null;
            if (TTClientClient.config != null) TTClientClient.config.save();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) { onClose(); return true; }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1); searchFocused = true; return true;
        }
        if (searchQuery.length() < 32) {
            if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
                searchQuery += (char) ('a' + (keyCode - GLFW.GLFW_KEY_A)); searchFocused = true; return true;
            }
            if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
                searchQuery += (char) ('0' + (keyCode - GLFW.GLFW_KEY_0)); searchFocused = true; return true;
            }
            if (keyCode == GLFW.GLFW_KEY_SPACE) { searchQuery += ' '; searchFocused = true; return true; }
        }
        return super.keyPressed(event);
    }
}

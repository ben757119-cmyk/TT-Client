package com.ttclient.gui;

import com.ttclient.TTClient;
import com.ttclient.client.TTClientClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.modules.client.ClickGUIModule;
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

/**
 * ClickGUI — panels start to the right of the search bar, fully rounded,
 * full settings (bool / number slider / mode cycle / bind).
 */
public class ClickGUIScreen extends Screen {
    private final Map<Category, Panel> panels = new HashMap<>();
    private final List<Panel> panelList = new ArrayList<>();
    private Module bindingModule = null;
    private Module hoveredModule = null;
    private String searchQuery = "";
    private boolean searchFocused = true;
    private NumberSetting sliding = null;
    private int sliderTrackX, sliderTrackW;
    private NumberSetting typing = null;
    private String typingBuffer = "";

    // High contrast: near-black dim + solid opaque panels so rounded edges read clearly
    private static final int PANEL_BG = 0xF8161A24, HEADER_BG = 0xFF0C0E14, TEXT_DIM = 0xFF6B7280, ROW_OFF = 0xFF9CA3AF;
    private static final int SLIDER_BG = 0xFF1A1E28, SEARCH_BG = 0xF80C0E14, ROW_ON_BG = 0x2800E8A0;
    private static final int ROW_H = 14, SETTING_H = 12, NUMBER_H = 26;
    private static final int SEARCH_X = 10, SEARCH_Y = 8, SEARCH_W = 160, SEARCH_H = 16;
    private static final int PANEL_W = 100, PANEL_GAP = 6, PANEL_START_Y = 8;
    private final List<Particle> particles = new ArrayList<>();

    /**
     * Fast rounded rect — center + side strips + stepped corners (no O(r²) per-pixel fills).
     * This is the main lag fix for the ClickGUI.
     */
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
        float x, y, vy, size;
        int alpha;
        Particle(float x, float y) {
            this.x = x; this.y = y;
            this.vy = 0.18f + (float) Math.random() * 1.15f;
            this.size = 1f + (float) Math.random() * 4.5f;
            this.alpha = 0x28 + (int) (Math.random() * 0x50);
        }
    }

    private int accent() {
        ClickGUIModule guiMod = TTClientClient.modules != null ? TTClientClient.modules.getModule(ClickGUIModule.class) : null;
        return guiMod != null ? guiMod.accent.get() : 0xFF00E8A0;
    }

    private static com.ttclient.modules.ModuleManager manager() {
        return TTClientClient.modules != null ? TTClientClient.modules : TTClient.modules;
    }

    public ClickGUIScreen() {
        super(Component.literal("TT Client"));
        int x = SEARCH_X + SEARCH_W + 18;
        for (Category cat : Category.values()) {
            if (manager() != null && manager().getModulesByCategory(cat).isEmpty()) continue;
            Panel p = new Panel(cat, x, PANEL_START_Y);
            panels.put(cat, p);
            panelList.add(p);
            x += PANEL_W + PANEL_GAP;
        }
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
        for (Particle p : particles) {
            p.y += p.vy;
            if (p.y > this.height) {
                p.y = -6;
                p.x = (float) (Math.random() * Math.max(1, this.width));
                p.vy = 0.18f + (float) Math.random() * 1.15f;
                p.size = 1f + (float) Math.random() * 4.5f;
                p.alpha = 0x28 + (int) (Math.random() * 0x50);
            }
            int s = Math.max(1, (int) p.size);
            int col = (p.alpha << 24) | 0x00E8A0;
            graphics.fill((int) p.x, (int) p.y, (int) p.x + s, (int) p.y + s, col);
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
        hoveredModule = null;
        roundedFill(graphics, SEARCH_X, SEARCH_Y, SEARCH_X + SEARCH_W, SEARCH_Y + SEARCH_H, 5, SEARCH_BG);
        graphics.fill(SEARCH_X, SEARCH_Y + 3, SEARCH_X + 2, SEARCH_Y + SEARCH_H - 3, accent());
        String shown = searchQuery.isEmpty()
                ? (searchFocused ? "search..." : "search modules")
                : searchQuery + (searchFocused ? "_" : "");
        graphics.text(this.font, shown, SEARCH_X + 8, SEARCH_Y + 4,
                searchQuery.isEmpty() ? TEXT_DIM : 0xFFE8EAED, false);
        graphics.text(this.font, "tt " + TTClient.VERSION, SEARCH_X + 4, SEARCH_Y + SEARCH_H + 4, accent(), false);

        if (searchQuery != null && !searchQuery.isBlank() && manager() != null) {
            String q = searchQuery.toLowerCase(Locale.ROOT);
            int sy = SEARCH_Y + SEARCH_H + 16;
            int shownN = 0;
            for (Module m : manager().getModules()) {
                if (!m.getName().toLowerCase(Locale.ROOT).contains(q)
                        && !m.getDescription().toLowerCase(Locale.ROOT).contains(q)) continue;
                roundedFill(graphics, SEARCH_X, sy, SEARCH_X + SEARCH_W, sy + 14, 4, 0xF010141C);
                graphics.fill(SEARCH_X, sy + 2, SEARCH_X + 2, sy + 12, m.isEnabled() ? accent() : 0xFF334155);
                graphics.text(this.font, m.getName(), SEARCH_X + 8, sy + 3,
                        m.isEnabled() ? accent() : 0xFFE8EAED, false);
                sy += 15;
                if (++shownN >= 12) break;
            }
            if (shownN == 0) {
                roundedFill(graphics, SEARCH_X, sy, SEARCH_X + SEARCH_W, sy + 14, 4, 0xF010141C);
                graphics.text(this.font, "no modules", SEARCH_X + 8, sy + 3, TEXT_DIM, false);
            }
        }

        for (Panel panel : panelList) panel.render(graphics, mouseX, mouseY);

        if (hoveredModule != null && typing == null) {
            String title = hoveredModule.getName();
            String desc = hoveredModule.getDescription();
            int pad = 8, tw = Math.max(font.width(title), font.width(desc)), boxW = tw + pad * 2, boxH = 28;
            int tx = Math.min(mouseX + 12, this.width - boxW - 4);
            int ty = Math.min(mouseY + 12, this.height - boxH - 4);
            roundedFill(graphics, tx, ty, tx + boxW, ty + boxH, 5, 0xF0080A0E);
            graphics.fill(tx, ty + 3, tx + 2, ty + boxH - 3, accent());
            graphics.text(font, title, tx + pad, ty + 5, accent(), false);
            graphics.text(font, desc, tx + pad, ty + 15, TEXT_DIM, false);
        }
        if (bindingModule != null) {
            String msg = "bind  " + bindingModule.getName() + "  ·  esc clear";
            int tw = this.font.width(msg);
            roundedFill(graphics, this.width / 2 - tw / 2 - 12, this.height - 30,
                    this.width / 2 + tw / 2 + 12, this.height - 12, 5, 0xF0080A0E);
            graphics.text(this.font, msg, this.width / 2 - tw / 2, this.height - 24, accent(), false);
        }
    }

    @Override public void onClose() {
        commitTyping(true); sliding = null;
        for (Panel p : panelList) if (p.expanded instanceof BlockESP be) be.uiSearchFocused = false;
        super.onClose();
        if (TTClientClient.config != null) TTClientClient.config.save();
    }

    private void applySlider(NumberSetting ns, double mouseX, int trackX, int trackW) {
        double t = Math.max(0, Math.min(1, (mouseX - trackX) / (double) trackW));
        ns.set(ns.getMin() + t * (ns.getMax() - ns.getMin()));
    }

    private void startTyping(NumberSetting ns) {
        commitTyping(true);
        typing = ns;
        typingBuffer = ns.getDisplay();
        sliding = null;
        searchFocused = false;
    }

    private void commitTyping(boolean apply) {
        if (typing == null) return;
        if (apply) {
            try {
                String raw = typingBuffer.trim().replace(',', '.');
                if (!raw.isEmpty()) typing.set(Double.parseDouble(raw));
            } catch (NumberFormatException ignored) {}
        }
        typing = null;
        typingBuffer = "";
    }

    public void startBinding(Module module) {
        commitTyping(true);
        this.bindingModule = module;
        searchFocused = false;
    }

    private static String keyName(int key) {
        if (key <= 0) return "NONE";
        if (key >= 2000 && key < 2016) {
            int b = key - 2000;
            if (b == GLFW.GLFW_MOUSE_BUTTON_4) return "MOUSE4";
            if (b == GLFW.GLFW_MOUSE_BUTTON_5) return "MOUSE5";
            if (b == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) return "MOUSE3";
            return "MOUSE" + (b + 1);
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) return "RSHIFT";
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z)
            return String.valueOf((char) ('A' + (key - GLFW.GLFW_KEY_A)));
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9)
            return String.valueOf((char) ('0' + (key - GLFW.GLFW_KEY_0)));
        return "KEY" + key;
    }

    public class Panel {
        private final Category category;
        private double x, y;
        private boolean open = true, dragging = false;
        private double dragOffsetX, dragOffsetY;
        private final int width = PANEL_W, headerHeight = 16;
        Module expanded = null;

        public Panel(Category category, double x, double y) {
            this.category = category;
            this.x = x;
            this.y = y;
        }

        private List<Module> filteredModules() {
            List<Module> base = manager() != null ? manager().getModulesByCategory(category) : List.of();
            if (searchQuery == null || searchQuery.isBlank()) return base;
            String q = searchQuery.toLowerCase(Locale.ROOT);
            List<Module> out = new ArrayList<>();
            for (Module m : base)
                if (m.getName().toLowerCase(Locale.ROOT).contains(q)
                        || m.getDescription().toLowerCase(Locale.ROOT).contains(q)) out.add(m);
            return out;
        }

        private int bodyHeight(List<Module> mods) {
            int bodyH = headerHeight;
            if (!open) return bodyH;
            for (Module mod : mods) {
                bodyH += ROW_H;
                if (expanded == mod) {
                    for (Setting<?> s : mod.getSettings()) bodyH += settingRowHeight(s);
                    bodyH += SETTING_H;
                    if (mod instanceof BlockESP be) {
                        bodyH += SETTING_H * 2;
                        bodyH += Math.min(5, be.searchRegistry(be.uiSearch, 5).size()) * SETTING_H;
                        bodyH += SETTING_H;
                        bodyH += Math.min(6, be.getSelected().size()) * SETTING_H;
                    }
                }
            }
            return bodyH;
        }

        public void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
            int ix = (int) x, iy = (int) y;
            List<Module> mods = filteredModules();
            int bodyH = bodyHeight(mods);
            roundedFill(g, ix + 3, iy + 3, ix + width + 3, iy + bodyH + 3, 8, 0x88000000);
            roundedFill(g, ix, iy, ix + width, iy + bodyH, 8, PANEL_BG);
            roundedFill(g, ix, iy, ix + width, iy + headerHeight, 8, HEADER_BG);
            g.fill(ix, iy + headerHeight - 6, ix + width, iy + headerHeight, HEADER_BG);
            g.fill(ix, iy + headerHeight - 1, ix + width, iy + headerHeight, 0x3300E8A0);
            g.fill(ix + 1, iy + 6, ix + 3, iy + bodyH - 6, accent());
            g.text(font, category.name.toUpperCase(Locale.ROOT), ix + 8, iy + 4, accent(), false);
            g.text(font, open ? "-" : "+", ix + width - 12, iy + 4, TEXT_DIM, false);
            if (!open) return;

            int my = iy + headerHeight;
            for (Module mod : mods) {
                boolean on = mod.isEnabled();
                int color = on ? accent() : ROW_OFF;
                if (mouseX >= ix && mouseX <= ix + width && mouseY >= my && mouseY <= my + ROW_H) {
                    g.fill(ix + 4, my, ix + width - 4, my + ROW_H, 0x14FFFFFF);
                    hoveredModule = mod;
                }
                if (on) g.fill(ix + 4, my, ix + width - 4, my + ROW_H, ROW_ON_BG);
                g.text(font, mod.getName(), ix + 8, my + 3, color, false);
                if (on) g.fill(ix + width - 10, my + 4, ix + width - 5, my + 9, accent());
                my += ROW_H;

                if (expanded == mod) {
                    for (Setting<?> s : mod.getSettings()) {
                        if (s instanceof NumberSetting ns) {
                            g.text(font, ns.getName(), ix + 8, my + 2, TEXT_DIM, false);
                            String valText = (typing == ns) ? typingBuffer + "_" : ns.getDisplay();
                            int vw = font.width(valText);
                            g.text(font, valText, ix + width - 8 - vw, my + 2,
                                    typing == ns ? accent() : 0xFFE8EAED, false);
                            int trackX = ix + 8, trackW = width - 16, trackY = my + 14;
                            g.fill(trackX, trackY, trackX + trackW, trackY + 3, SLIDER_BG);
                            double t = (ns.get() - ns.getMin()) / Math.max(1e-9, ns.getMax() - ns.getMin());
                            int fill = (int) (trackW * Math.max(0, Math.min(1, t)));
                            g.fill(trackX, trackY, trackX + fill, trackY + 3, accent());
                            g.fill(trackX + fill - 1, trackY - 1, trackX + fill + 2, trackY + 4, 0xFFE8EAED);
                            my += NUMBER_H;
                        } else if (s instanceof BoolSetting bs) {
                            g.text(font, bs.getName(), ix + 8, my + 2, TEXT_DIM, false);
                            String v = bs.get() ? "ON" : "OFF";
                            int vw = font.width(v);
                            g.text(font, v, ix + width - 8 - vw, my + 2, bs.get() ? accent() : ROW_OFF, false);
                            my += SETTING_H;
                        } else if (s instanceof ModeSetting ms) {
                            g.text(font, ms.getName(), ix + 8, my + 2, TEXT_DIM, false);
                            String v = String.valueOf(ms.get());
                            int vw = font.width(v);
                            g.text(font, v, ix + width - 8 - vw, my + 2, accent(), false);
                            my += SETTING_H;
                        } else {
                            g.text(font, s.getName(), ix + 8, my + 2, TEXT_DIM, false);
                            my += SETTING_H;
                        }
                    }
                    String bind = "bind " + keyName(mod.getKeyBind());
                    g.text(font, bind, ix + 8, my + 2, TEXT_DIM, false);
                    my += SETTING_H;
                    if (mod instanceof BlockESP be) {
                        g.text(font, "search: " + (be.uiSearch == null ? "" : be.uiSearch) + (be.uiSearchFocused ? "_" : ""),
                                ix + 8, my + 2, TEXT_DIM, false);
                        my += SETTING_H;
                    }
                }
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int ix = (int) x, iy = (int) y;
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight) {
                if (button == 0) {
                    dragging = true;
                    dragOffsetX = mouseX - x;
                    dragOffsetY = mouseY - y;
                    return true;
                }
                if (button == 1) { open = !open; return true; }
            }
            if (!open) return false;
            List<Module> mods = filteredModules();
            int my = iy + headerHeight;
            for (Module mod : mods) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + ROW_H) {
                    if (button == 0) { mod.toggle(); return true; }
                    if (button == 1) { expanded = expanded == mod ? null : mod; return true; }
                    if (button == 2) { startBinding(mod); return true; }
                }
                my += ROW_H;
                if (expanded == mod) {
                    for (Setting<?> s : mod.getSettings()) {
                        int h = settingRowHeight(s);
                        if (mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + h) {
                            if (s instanceof BoolSetting bs && button == 0) { bs.toggle(); return true; }
                            if (s instanceof ModeSetting ms && button == 0) { ms.cycle(); return true; }
                            if (s instanceof NumberSetting ns) {
                                if (button == 0) {
                                    int trackY = my + 14;
                                    if (mouseY >= trackY - 2 && mouseY <= trackY + 6) {
                                        sliding = ns;
                                        sliderTrackX = ix + 8;
                                        sliderTrackW = width - 16;
                                        applySlider(ns, mouseX, sliderTrackX, sliderTrackW);
                                        return true;
                                    }
                                    startTyping(ns);
                                    return true;
                                }
                            }
                            return true;
                        }
                        my += h;
                    }
                    if (mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + SETTING_H) {
                        if (button == 0) { startBinding(mod); return true; }
                    }
                    my += SETTING_H;
                }
            }
            return false;
        }

        public boolean mouseDragged(double mouseX, double mouseY, int button) {
            if (dragging && button == 0) {
                x = mouseX - dragOffsetX;
                y = mouseY - dragOffsetY;
                return true;
            }
            return false;
        }

        public void mouseReleased(int button) {
            if (button == 0) dragging = false;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
        if (bindingModule != null && button >= 0) {
            bindingModule.setKeyBind(2000 + button);
            bindingModule = null;
            return true;
        }
        if (button == 0 && mouseX >= SEARCH_X && mouseX <= SEARCH_X + SEARCH_W
                && mouseY >= SEARCH_Y && mouseY <= SEARCH_Y + SEARCH_H) {
            searchFocused = true;
            commitTyping(true);
            return true;
        }
        if (button == 0 && searchQuery != null && !searchQuery.isBlank() && manager() != null
                && mouseX >= SEARCH_X && mouseX <= SEARCH_X + SEARCH_W && mouseY > SEARCH_Y + SEARCH_H + 14) {
            String q = searchQuery.toLowerCase(Locale.ROOT);
            int sy = SEARCH_Y + SEARCH_H + 16;
            for (Module m : manager().getModules()) {
                if (!m.getName().toLowerCase(Locale.ROOT).contains(q)
                        && !m.getDescription().toLowerCase(Locale.ROOT).contains(q)) continue;
                if (mouseY >= sy && mouseY <= sy + 14) { m.toggle(); return true; }
                sy += 15;
                if (sy > SEARCH_Y + SEARCH_H + 16 + 15 * 12) break;
            }
        }
        searchFocused = false;
        if (sliding != null) return true;
        for (int i = panelList.size() - 1; i >= 0; i--) {
            if (panelList.get(i).mouseClicked(mouseX, mouseY, button)) return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x(), mouseY = event.y();
        if (sliding != null) {
            applySlider(sliding, mouseX, sliderTrackX, sliderTrackW);
            return true;
        }
        for (Panel panel : panelList) {
            if (panel.mouseDragged(mouseX, mouseY, event.button())) return true;
        }
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
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                commitTyping(true); return true;
            }
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
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose(); return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            searchFocused = true;
            return true;
        }
        if (searchQuery.length() < 32) {
            if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
                searchQuery += (char) ('a' + (keyCode - GLFW.GLFW_KEY_A));
                searchFocused = true; return true;
            }
            if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
                searchQuery += (char) ('0' + (keyCode - GLFW.GLFW_KEY_0));
                searchFocused = true; return true;
            }
            if (keyCode == GLFW.GLFW_KEY_SPACE) {
                searchQuery += ' ';
                searchFocused = true; return true;
            }
        }
        return super.keyPressed(event);
    }
}

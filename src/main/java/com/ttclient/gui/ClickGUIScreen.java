package com.ttclient.gui;

import com.ttclient.TTClient;
import com.ttclient.client.TTClientClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.modules.client.ClickGUIModule;
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
 * Right Shift GUI. Number settings: drag slider + click value to type.
 */
public class ClickGUIScreen extends Screen {
    private final Map<Category, Panel> panels = new HashMap<>();
    private final List<Panel> panelList = new ArrayList<>();
    private Module bindingModule = null;
    private Module hoveredModule = null;
    private String searchQuery = "";

    /** Active number slider drag. */
    private NumberSetting sliding = null;
    private int sliderTrackX;
    private int sliderTrackW;

    /** Active number text edit. */
    private NumberSetting typing = null;
    private String typingBuffer = "";

    private static final int PANEL_BG = 0xF0121620;
    private static final int HEADER_BG = 0xFF0E1A16;
    private static final int TEXT_DIM = 0xFF8B95A8;
    private static final int ROW_OFF = 0xFF9AA3B5;
    private static final int SLIDER_BG = 0xFF1A2030;
    private static final int SLIDER_FILL = 0xFF0A3D32;

    private static final int ROW_H = 15;
    private static final int SETTING_H = 13;
    private static final int NUMBER_H = 28;

    private int accent() {
        ClickGUIModule guiMod = TTClientClient.modules != null
                ? TTClientClient.modules.getModule(ClickGUIModule.class) : null;
        if (guiMod != null) return guiMod.accent.get();
        return 0xFF00E8A0;
    }

    private static com.ttclient.modules.ModuleManager manager() {
        if (TTClientClient.modules != null) return TTClientClient.modules;
        return TTClient.modules;
    }

    public ClickGUIScreen() {
        super(Component.literal("TT Client"));
        int x = 14;
        for (Category cat : Category.values()) {
            if (manager() != null && manager().getModulesByCategory(cat).isEmpty()) continue;
            Panel p = new Panel(cat, x, 36);
            panels.put(cat, p);
            panelList.add(p);
            x += 138;
        }
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
        ClickGUIModule guiMod = TTClientClient.modules != null
                ? TTClientClient.modules.getModule(ClickGUIModule.class) : null;
        if (guiMod != null && guiMod.dim.get()) {
            graphics.fill(0, 0, this.width, this.height, 0x660A0B0F);
        }
    }

    @Override
    protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {
    }

    @Override
    public void extractTransparentBackground(GuiGraphicsExtractor graphics) {
    }

    @Override
    protected void extractMenuBackground(GuiGraphicsExtractor graphics) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        hoveredModule = null;
        String searchLabel = searchQuery.isEmpty() ? "type to search" : "search: " + searchQuery;
        String tip = typing != null
                ? "Typing " + typing.getName() + ": " + typingBuffer + "_  (Enter apply · Esc cancel)"
                : "LMB toggle · RMB settings · drag sliders · click number to type · " + searchLabel;
        graphics.text(this.font, "TT Client v" + TTClient.VERSION + "  ·  " + tip, 14, 10, accent(), false);

        for (Panel panel : panelList) {
            panel.render(graphics, mouseX, mouseY);
        }

        if (hoveredModule != null && typing == null) {
            String title = hoveredModule.getName();
            String desc = hoveredModule.getDescription();
            int pad = 8;
            int tw = Math.max(font.width(title), font.width(desc));
            int boxW = tw + pad * 2;
            int boxH = 28;
            int tx = Math.min(mouseX + 12, this.width - boxW - 4);
            int ty = Math.min(mouseY + 12, this.height - boxH - 4);
            graphics.fill(tx, ty, tx + boxW, ty + boxH, 0xF0000000);
            graphics.fill(tx, ty, tx + 2, ty + boxH, accent());
            graphics.text(font, title, tx + pad, ty + 4, accent(), false);
            graphics.text(font, desc, tx + pad, ty + 15, TEXT_DIM, false);
        }
        if (bindingModule != null) {
            String msg = "Bind: " + bindingModule.getName() + "  (ESC clear)";
            int tw = this.font.width(msg);
            graphics.fill(this.width / 2 - tw / 2 - 10, this.height - 30, this.width / 2 + tw / 2 + 10, this.height - 12, 0xEE000000);
            graphics.text(this.font, msg, this.width / 2 - tw / 2, this.height - 25, accent(), false);
        }
    }

    @Override
    public void onClose() {
        commitTyping(true);
        sliding = null;
        super.onClose();
        if (TTClientClient.config != null) TTClientClient.config.save();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        // Click outside finishes typing
        if (typing != null && button == 0) {
            boolean onPanel = false;
            for (Panel p : panelList) {
                if (p.contains(mouseX, mouseY)) { onPanel = true; break; }
            }
            if (!onPanel) {
                commitTyping(true);
            }
        }

        for (int i = panelList.size() - 1; i >= 0; i--) {
            if (panelList.get(i).mouseClicked(mouseX, mouseY, button)) return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        sliding = null;
        for (Panel panel : panelList) panel.mouseReleased();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (sliding != null && sliderTrackW > 0) {
            applySlider(sliding, mouseX, sliderTrackX, sliderTrackW);
            return true;
        }

        for (Panel panel : panelList) {
            if (panel.mouseDragged(mouseX, mouseY)) return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    private void applySlider(NumberSetting ns, double mouseX, int trackX, int trackW) {
        double t = (mouseX - trackX) / (double) trackW;
        t = Math.max(0, Math.min(1, t));
        double val = ns.getMin() + t * (ns.getMax() - ns.getMin());
        ns.set(val);
    }

    private void startTyping(NumberSetting ns) {
        commitTyping(true);
        typing = ns;
        typingBuffer = ns.getDisplay();
        sliding = null;
    }

    private void commitTyping(boolean apply) {
        if (typing == null) return;
        if (apply) {
            try {
                String raw = typingBuffer.trim().replace(',', '.');
                if (!raw.isEmpty()) {
                    double v = Double.parseDouble(raw);
                    typing.set(v);
                }
            } catch (NumberFormatException ignored) {
                // keep previous value
            }
        }
        typing = null;
        typingBuffer = "";
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();

        if (typing != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                commitTyping(true);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                commitTyping(false);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!typingBuffer.isEmpty()) {
                    typingBuffer = typingBuffer.substring(0, typingBuffer.length() - 1);
                }
                return true;
            }
            // digits / decimal / minus
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
            return true; // swallow other keys while typing
        }

        if (bindingModule != null) {
            if (keyCode == 256) bindingModule.setKeyBind(-1);
            else bindingModule.setKeyBind(keyCode);
            bindingModule = null;
            return true;
        }
        if (keyCode == 256 || keyCode == 344) {
            onClose();
            return true;
        }
        if (keyCode == 259 && !searchQuery.isEmpty()) {
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            return true;
        }
        if (searchQuery.length() < 24) {
            if (keyCode >= 65 && keyCode <= 90) {
                searchQuery += (char) ('a' + (keyCode - 65));
                return true;
            }
            if (keyCode >= 48 && keyCode <= 57) {
                searchQuery += (char) ('0' + (keyCode - 48));
                return true;
            }
            if (keyCode == 32) {
                searchQuery += ' ';
                return true;
            }
        }
        return super.keyPressed(event);
    }

    public void startBinding(Module module) {
        commitTyping(true);
        this.bindingModule = module;
    }

    private static String keyName(int key) {
        if (key < 0) return "NONE";
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) return "RSHIFT";
        if (key == GLFW.GLFW_KEY_LEFT_SHIFT) return "LSHIFT";
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
            return String.valueOf((char) ('A' + (key - GLFW.GLFW_KEY_A)));
        }
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) ('0' + (key - GLFW.GLFW_KEY_0)));
        }
        return "KEY" + key;
    }

    private int settingRowHeight(Setting<?> s) {
        return s instanceof NumberSetting ? NUMBER_H : SETTING_H;
    }

    public class Panel {
        private final Category category;
        private double x, y;
        private boolean open = true;
        private boolean dragging = false;
        private double dragOffsetX, dragOffsetY;
        private final int width = 130;
        private final int headerHeight = 18;
        private Module expanded = null;

        public Panel(Category category, double x, double y) {
            this.category = category;
            this.x = x;
            this.y = y;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + width && my >= y && my <= y + 400;
        }

        private List<Module> filteredModules() {
            List<Module> base = manager() != null
                    ? manager().getModulesByCategory(category)
                    : List.of();
            if (searchQuery == null || searchQuery.isBlank()) return base;
            String q = searchQuery.toLowerCase(Locale.ROOT);
            List<Module> out = new ArrayList<>();
            for (Module m : base) {
                if (m.getName().toLowerCase(Locale.ROOT).contains(q)
                        || m.getDescription().toLowerCase(Locale.ROOT).contains(q)) {
                    out.add(m);
                }
            }
            return out;
        }

        private int bodyHeight(List<Module> mods) {
            int bodyH = headerHeight;
            if (!open) return bodyH;
            for (Module mod : mods) {
                bodyH += ROW_H;
                if (expanded == mod) {
                    for (Setting<?> s : mod.getSettings()) {
                        bodyH += settingRowHeight(s);
                    }
                    bodyH += SETTING_H; // bind
                }
            }
            return bodyH;
        }

        public void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
            int ix = (int) x;
            int iy = (int) y;
            List<Module> mods = filteredModules();
            int bodyH = bodyHeight(mods);

            g.fill(ix + 2, iy + 2, ix + width + 2, iy + bodyH + 2, 0x44000000);
            g.fill(ix, iy, ix + width, iy + bodyH, PANEL_BG);
            g.fill(ix, iy, ix + width, iy + headerHeight, HEADER_BG);
            g.fill(ix, iy, ix + 2, iy + bodyH, accent());
            g.text(font, category.name, ix + 8, iy + 5, accent(), false);
            if (!open) return;

            int my = iy + headerHeight;
            for (Module mod : mods) {
                boolean on = mod.isEnabled();
                int color = on ? accent() : ROW_OFF;
                boolean hover = mouseX >= ix && mouseX <= ix + width && mouseY >= my && mouseY <= my + ROW_H;
                if (hover) {
                    g.fill(ix + 2, my, ix + width, my + ROW_H, 0x18FFFFFF);
                    hoveredModule = mod;
                }
                g.text(font, mod.getName(), ix + 8, my + 4, color, false);
                if (on) g.fill(ix + width - 11, my + 5, ix + width - 5, my + 11, accent());
                my += ROW_H;

                if (expanded == mod) {
                    for (Setting<?> s : mod.getSettings()) {
                        if (s instanceof NumberSetting ns) {
                            renderNumberSetting(g, ns, ix, my, mouseX, mouseY);
                            my += NUMBER_H;
                        } else {
                            String label = s.getName() + ": " + String.valueOf(s.get());
                            g.text(font, label, ix + 10, my + 2, TEXT_DIM, false);
                            my += SETTING_H;
                        }
                    }
                    String bindLabel = "Bind: " + keyName(mod.getKeyBind()) + "  (click)";
                    g.text(font, bindLabel, ix + 10, my + 2, accent(), false);
                    my += SETTING_H;
                }
            }
        }

        private void renderNumberSetting(GuiGraphicsExtractor g, NumberSetting ns, int ix, int my, int mouseX, int mouseY) {
            // Name
            g.text(font, ns.getName(), ix + 10, my + 2, TEXT_DIM, false);

            // Value (right) — highlight if typing this one
            String valText = (typing == ns) ? typingBuffer + "_" : ns.getDisplay();
            int vw = font.width(valText);
            int valueColor = (typing == ns) ? accent() : 0xFFE8EAED;
            g.text(font, valText, ix + width - 8 - vw, my + 2, valueColor, false);

            // Slider track
            int trackX = ix + 10;
            int trackW = width - 20;
            int trackY = my + 15;
            int trackH = 6;
            g.fill(trackX, trackY, trackX + trackW, trackY + trackH, SLIDER_BG);

            double range = ns.getMax() - ns.getMin();
            double t = range <= 0 ? 0 : (ns.get() - ns.getMin()) / range;
            t = Math.max(0, Math.min(1, t));
            int fillW = (int) Math.round(trackW * t);
            if (fillW > 0) {
                g.fill(trackX, trackY, trackX + fillW, trackY + trackH, SLIDER_FILL);
                g.fill(trackX, trackY, trackX + fillW, trackY + trackH, (accent() & 0x00FFFFFF) | 0x99000000);
            }
            // Knob
            int knobX = trackX + fillW - 2;
            g.fill(knobX, trackY - 2, knobX + 4, trackY + trackH + 2, accent());

            // Min / max hints (tiny)
            // skipped to keep clean
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight) {
                if (button == 0) {
                    dragging = true;
                    dragOffsetX = mouseX - x;
                    dragOffsetY = mouseY - y;
                    return true;
                } else if (button == 1) {
                    open = !open;
                    return true;
                }
            }
            if (!open || manager() == null) return false;

            List<Module> mods = filteredModules();
            int my = (int) y + headerHeight;
            int ix = (int) x;

            for (Module mod : mods) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + ROW_H) {
                    if (button == 0) {
                        commitTyping(true);
                        mod.toggle();
                        return true;
                    }
                    if (button == 1) {
                        commitTyping(true);
                        expanded = (expanded == mod) ? null : mod;
                        return true;
                    }
                    if (button == 2) {
                        startBinding(mod);
                        return true;
                    }
                }
                my += ROW_H;

                if (expanded == mod) {
                    for (Setting<?> s : mod.getSettings()) {
                        int h = settingRowHeight(s);
                        if (mouseX >= x + 2 && mouseX <= x + width - 2 && mouseY >= my && mouseY <= my + h) {
                            if (s instanceof BoolSetting bs) {
                                commitTyping(true);
                                bs.toggle();
                                return true;
                            }
                            if (s instanceof ModeSetting ms) {
                                commitTyping(true);
                                ms.cycle();
                                return true;
                            }
                            if (s instanceof NumberSetting ns && button == 0) {
                                int trackX = ix + 10;
                                int trackW = width - 20;
                                int trackY = my + 15;
                                // Click on value text area (top-right) → type
                                String val = ns.getDisplay();
                                int vw = font.width(val) + 8;
                                boolean onValue = mouseY < my + 14 && mouseX >= x + width - vw - 10;
                                if (onValue) {
                                    startTyping(ns);
                                    return true;
                                }
                                // Otherwise start slider drag (also click sets value)
                                commitTyping(true);
                                sliding = ns;
                                sliderTrackX = trackX;
                                sliderTrackW = trackW;
                                applySlider(ns, mouseX, trackX, trackW);
                                return true;
                            }
                        }
                        my += h;
                    }
                    if (mouseX >= x + 2 && mouseX <= x + width - 2 && mouseY >= my && mouseY <= my + SETTING_H) {
                        startBinding(mod);
                        return true;
                    }
                    my += SETTING_H;
                }
            }
            return false;
        }

        public void mouseReleased() {
            dragging = false;
        }

        public boolean mouseDragged(double mouseX, double mouseY) {
            if (dragging) {
                x = mouseX - dragOffsetX;
                y = mouseY - dragOffsetY;
                return true;
            }
            return false;
        }
    }
}

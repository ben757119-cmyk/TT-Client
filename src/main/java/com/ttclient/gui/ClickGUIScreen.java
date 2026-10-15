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

    private static final int PANEL_BG = 0xF0121620, HEADER_BG = 0xFF0E1A16, TEXT_DIM = 0xFF8B95A8, ROW_OFF = 0xFF9AA3B5;
    private static final int SLIDER_BG = 0xFF1A2030, SEARCH_BG = 0xF00A0E14;
    private static final int ROW_H = 15, SETTING_H = 13, NUMBER_H = 28;
    private static final int SEARCH_X = 14, SEARCH_Y = 8, SEARCH_W = 220, SEARCH_H = 16;

    private int accent() {
        ClickGUIModule guiMod = TTClientClient.modules != null ? TTClientClient.modules.getModule(ClickGUIModule.class) : null;
        return guiMod != null ? guiMod.accent.get() : 0xFF00E8A0;
    }
    private static com.ttclient.modules.ModuleManager manager() {
        return TTClientClient.modules != null ? TTClientClient.modules : TTClient.modules;
    }

    public ClickGUIScreen() {
        super(Component.literal("TT Client"));
        int x = 14;
        for (Category cat : Category.values()) {
            if (manager() != null && manager().getModulesByCategory(cat).isEmpty()) continue;
            Panel p = new Panel(cat, x, 48);
            panels.put(cat, p);
            panelList.add(p);
            x += 138;
        }
    }

    @Override public boolean isInGameUi() { return true; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ClickGUIModule guiMod = TTClientClient.modules != null ? TTClientClient.modules.getModule(ClickGUIModule.class) : null;
        if (guiMod != null && guiMod.dim.get()) graphics.fill(0, 0, this.width, this.height, 0x660A0B0F);
    }
    @Override protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {}
    @Override public void extractTransparentBackground(GuiGraphicsExtractor graphics) {}
    @Override protected void extractMenuBackground(GuiGraphicsExtractor graphics) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        hoveredModule = null;
        graphics.fill(SEARCH_X, SEARCH_Y, SEARCH_X + SEARCH_W, SEARCH_Y + SEARCH_H, SEARCH_BG);
        graphics.fill(SEARCH_X, SEARCH_Y, SEARCH_X + 2, SEARCH_Y + SEARCH_H, accent());
        String shown = searchQuery.isEmpty() ? (searchFocused ? "Search modules…" : "Click to search") : searchQuery + (searchFocused ? "_" : "");
        graphics.text(this.font, shown, SEARCH_X + 8, SEARCH_Y + 4, searchQuery.isEmpty() ? TEXT_DIM : 0xFFE8EAED, false);
        graphics.text(this.font, "TT Client v" + TTClient.VERSION + "  ·  BlockESP: ttblock add <id>", SEARCH_X + SEARCH_W + 12, SEARCH_Y + 4, accent(), false);
        for (Panel panel : panelList) panel.render(graphics, mouseX, mouseY);
        if (hoveredModule != null && typing == null) {
            String title = hoveredModule.getName();
            String desc = hoveredModule.getDescription();
            int pad = 8, tw = Math.max(font.width(title), font.width(desc)), boxW = tw + pad * 2, boxH = 28;
            int tx = Math.min(mouseX + 12, this.width - boxW - 4), ty = Math.min(mouseY + 12, this.height - boxH - 4);
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

    @Override public void onClose() {
        commitTyping(true); sliding = null; super.onClose();
        if (TTClientClient.config != null) TTClientClient.config.save();
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y(); int button = event.button();
        if (button == 0 && mouseX >= SEARCH_X && mouseX <= SEARCH_X + SEARCH_W && mouseY >= SEARCH_Y && mouseY <= SEARCH_Y + SEARCH_H) {
            searchFocused = true; commitTyping(true); return true;
        }
        if (button == 0) searchFocused = false;
        for (int i = panelList.size() - 1; i >= 0; i--) if (panelList.get(i).mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(event, doubleClick);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        sliding = null; for (Panel panel : panelList) panel.mouseReleased(); return super.mouseReleased(event);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (sliding != null && sliderTrackW > 0) { applySlider(sliding, event.x(), sliderTrackX, sliderTrackW); return true; }
        for (Panel panel : panelList) if (panel.mouseDragged(event.x(), event.y())) return true;
        return super.mouseDragged(event, dragX, dragY);
    }

    private void applySlider(NumberSetting ns, double mouseX, int trackX, int trackW) {
        double t = Math.max(0, Math.min(1, (mouseX - trackX) / (double) trackW));
        ns.set(ns.getMin() + t * (ns.getMax() - ns.getMin()));
    }
    private void startTyping(NumberSetting ns) { commitTyping(true); typing = ns; typingBuffer = ns.getDisplay(); sliding = null; searchFocused = false; }
    private void commitTyping(boolean apply) {
        if (typing == null) return;
        if (apply) { try { String raw = typingBuffer.trim().replace(',', '.'); if (!raw.isEmpty()) typing.set(Double.parseDouble(raw)); } catch (NumberFormatException ignored) {} }
        typing = null; typingBuffer = "";
    }

    @Override public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        // BlockESP list search when expanded
        if (typing == null && bindingModule == null) {
            for (Panel panel : panelList) {
                if (panel.expanded instanceof BlockESP be) {
                    if (keyCode == GLFW.GLFW_KEY_BACKSPACE && be.uiSearch != null && !be.uiSearch.isEmpty()) {
                        be.uiSearch = be.uiSearch.substring(0, be.uiSearch.length() - 1); return true;
                    }
                    if (be.uiSearch == null) be.uiSearch = "";
                    if (be.uiSearch.length() < 40) {
                        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) { be.uiSearch += (char)('a'+(keyCode-GLFW.GLFW_KEY_A)); return true; }
                        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) { be.uiSearch += (char)('0'+(keyCode-GLFW.GLFW_KEY_0)); return true; }
                        if (keyCode == GLFW.GLFW_KEY_MINUS) { be.uiSearch += '_'; return true; }
                    }
                }
            }
        }
        if (typing != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { commitTyping(true); return true; }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { commitTyping(false); return true; }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !typingBuffer.isEmpty()) { typingBuffer = typingBuffer.substring(0, typingBuffer.length()-1); return true; }
            if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) { typingBuffer += (char)('0'+(keyCode-GLFW.GLFW_KEY_0)); return true; }
            if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) { typingBuffer += (char)('0'+(keyCode-GLFW.GLFW_KEY_KP_0)); return true; }
            if (keyCode == GLFW.GLFW_KEY_PERIOD || keyCode == GLFW.GLFW_KEY_KP_DECIMAL) { if (!typingBuffer.contains(".")) typingBuffer += '.'; return true; }
            return true;
        }
        if (bindingModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) bindingModule.setKeyBind(-1);
            else if (keyCode > 0) bindingModule.setKeyBind(keyCode);
            bindingModule = null; return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) { onClose(); return true; }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) { searchQuery = searchQuery.substring(0, searchQuery.length()-1); searchFocused = true; return true; }
        if (searchQuery.length() < 32) {
            if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) { searchQuery += (char)('a'+(keyCode-GLFW.GLFW_KEY_A)); searchFocused = true; return true; }
            if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) { searchQuery += (char)('0'+(keyCode-GLFW.GLFW_KEY_0)); searchFocused = true; return true; }
            if (keyCode == GLFW.GLFW_KEY_SPACE) { searchQuery += ' '; searchFocused = true; return true; }
        }
        return super.keyPressed(event);
    }

    public void startBinding(Module module) { commitTyping(true); this.bindingModule = module; searchFocused = false; }
    private static String keyName(int key) {
        if (key <= 0) return "NONE";
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) return "RSHIFT";
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) return String.valueOf((char)('A'+(key-GLFW.GLFW_KEY_A)));
        if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) return String.valueOf((char)('0'+(key-GLFW.GLFW_KEY_0)));
        return "KEY"+key;
    }
    private int settingRowHeight(Setting<?> s) { return s instanceof NumberSetting ? NUMBER_H : SETTING_H; }

    public class Panel {
        private final Category category;
        private double x, y;
        private boolean open = true, dragging = false;
        private double dragOffsetX, dragOffsetY;
        private final int width = 130, headerHeight = 18;
        Module expanded = null;

        public Panel(Category category, double x, double y) { this.category = category; this.x = x; this.y = y; }

        private List<Module> filteredModules() {
            List<Module> base = manager() != null ? manager().getModulesByCategory(category) : List.of();
            if (searchQuery == null || searchQuery.isBlank()) return base;
            String q = searchQuery.toLowerCase(Locale.ROOT);
            List<Module> out = new ArrayList<>();
            for (Module m : base) if (m.getName().toLowerCase(Locale.ROOT).contains(q) || m.getDescription().toLowerCase(Locale.ROOT).contains(q)) out.add(m);
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
                        bodyH += SETTING_H;
                        bodyH += Math.min(6, be.searchRegistry(be.uiSearch, 6).size()) * SETTING_H;
                        bodyH += Math.min(8, be.getSelected().size()) * SETTING_H;
                    }
                }
            }
            return bodyH;
        }

        public void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
            int ix = (int) x, iy = (int) y;
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
                if (mouseX >= ix && mouseX <= ix + width && mouseY >= my && mouseY <= my + ROW_H) {
                    g.fill(ix + 2, my, ix + width, my + ROW_H, 0x18FFFFFF);
                    hoveredModule = mod;
                }
                g.text(font, mod.getName(), ix + 8, my + 4, color, false);
                if (on) g.fill(ix + width - 11, my + 5, ix + width - 5, my + 11, accent());
                my += ROW_H;
                if (expanded == mod) {
                    for (Setting<?> s : mod.getSettings()) {
                        if (s instanceof NumberSetting ns) {
                            g.text(font, ns.getName(), ix + 10, my + 2, TEXT_DIM, false);
                            String valText = (typing == ns) ? typingBuffer + "_" : ns.getDisplay();
                            int vw = font.width(valText);
                            g.text(font, valText, ix + width - 8 - vw, my + 2, typing == ns ? accent() : 0xFFE8EAED, false);
                            int trackX = ix + 10, trackW = width - 20, trackY = my + 15;
                            g.fill(trackX, trackY, trackX + trackW, trackY + 6, SLIDER_BG);
                            double range = ns.getMax() - ns.getMin();
                            double t = range <= 0 ? 0 : (ns.get() - ns.getMin()) / range;
                            int fillW = (int) Math.round(trackW * Math.max(0, Math.min(1, t)));
                            if (fillW > 0) g.fill(trackX, trackY, trackX + fillW, trackY + 6, 0xFF0A3D32);
                            g.fill(trackX + fillW - 2, trackY - 2, trackX + fillW + 2, trackY + 8, accent());
                            my += NUMBER_H;
                        } else {
                            g.text(font, s.getName() + ": " + String.valueOf(s.get()), ix + 10, my + 2, TEXT_DIM, false);
                            my += SETTING_H;
                        }
                    }
                    g.text(font, "Bind: " + keyName(mod.getKeyBind()), ix + 10, my + 2, accent(), false);
                    my += SETTING_H;
                    if (mod instanceof BlockESP be) {
                        String q = be.uiSearch == null ? "" : be.uiSearch;
                        g.text(font, "Search: " + (q.isEmpty() ? "type id..." : q + "_"), ix + 10, my + 2, accent(), false);
                        my += SETTING_H;
                        for (String id : be.searchRegistry(q, 6)) {
                            String shortId = id.startsWith("minecraft:") ? id.substring(10) : id;
                            g.text(font, "+ " + shortId, ix + 10, my + 2, 0xFF8BFFB0, false);
                            my += SETTING_H;
                        }
                        for (String id : be.getSelected()) {
                            String shortId = id.startsWith("minecraft:") ? id.substring(10) : id;
                            if (shortId.length() > 16) shortId = shortId.substring(0, 14) + "..";
                            g.text(font, "- " + shortId, ix + 10, my + 2, 0xFFFF8888, false);
                            my += SETTING_H;
                        }
                    }
                }
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight) {
                if (button == 0) { dragging = true; dragOffsetX = mouseX - x; dragOffsetY = mouseY - y; return true; }
                if (button == 1) { open = !open; return true; }
            }
            if (!open || manager() == null) return false;
            List<Module> mods = filteredModules();
            int my = (int) y + headerHeight, ix = (int) x;
            for (Module mod : mods) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + ROW_H) {
                    if (button == 0) { commitTyping(true); mod.toggle(); return true; }
                    if (button == 1) { commitTyping(true); expanded = (expanded == mod) ? null : mod; return true; }
                    if (button == 2) { startBinding(mod); return true; }
                }
                my += ROW_H;
                if (expanded == mod) {
                    for (Setting<?> s : mod.getSettings()) {
                        int h = settingRowHeight(s);
                        if (mouseX >= x + 2 && mouseX <= x + width - 2 && mouseY >= my && mouseY <= my + h) {
                            if (s instanceof BoolSetting bs) { commitTyping(true); bs.toggle(); return true; }
                            if (s instanceof ModeSetting ms) { commitTyping(true); ms.cycle(); return true; }
                            if (s instanceof NumberSetting ns && button == 0) {
                                int trackX = ix + 10, trackW = width - 20;
                                String val = ns.getDisplay(); int vw = font.width(val) + 8;
                                if (mouseY < my + 14 && mouseX >= x + width - vw - 10) { startTyping(ns); return true; }
                                commitTyping(true); sliding = ns; sliderTrackX = trackX; sliderTrackW = trackW;
                                applySlider(ns, mouseX, trackX, trackW); return true;
                            }
                        }
                        my += h;
                    }
                    if (mouseX >= x + 2 && mouseX <= x + width - 2 && mouseY >= my && mouseY <= my + SETTING_H) { startBinding(mod); return true; }
                    my += SETTING_H;
                    if (mod instanceof BlockESP be && button == 0) {
                        my += SETTING_H; // skip search label row
                        for (String id : be.searchRegistry(be.uiSearch, 6)) {
                            if (mouseX >= x + 2 && mouseX <= x + width - 2 && mouseY >= my && mouseY <= my + SETTING_H) {
                                be.addBlock(id); if (TTClientClient.config != null) TTClientClient.config.save(); return true;
                            }
                            my += SETTING_H;
                        }
                        for (String id : new ArrayList<>(be.getSelected())) {
                            if (mouseX >= x + 2 && mouseX <= x + width - 2 && mouseY >= my && mouseY <= my + SETTING_H) {
                                be.removeBlock(id); if (TTClientClient.config != null) TTClientClient.config.save(); return true;
                            }
                            my += SETTING_H;
                        }
                    }
                }
            }
            return false;
        }
        public void mouseReleased() { dragging = false; }
        public boolean mouseDragged(double mouseX, double mouseY) {
            if (dragging) { x = mouseX - dragOffsetX; y = mouseY - dragOffsetY; return true; }
            return false;
        }
    }
}

package com.ttclient.gui;

import com.ttclient.TTClient;
import com.ttclient.client.TTClientClient;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.modules.client.ClickGUIModule;
import com.ttclient.modules.render.BlockESP;
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

    private static final int PANEL_BG = 0xF00B0D12, HEADER_BG = 0xFF080A0E, TEXT_DIM = 0xFF6B7280, ROW_OFF = 0xFF9CA3AF;
    private static final int SLIDER_BG = 0xFF141820, SEARCH_BG = 0xF0080A0E, ROW_ON_BG = 0x2200E8A0;
    private static final int ROW_H = 13, SETTING_H = 11, NUMBER_H = 24;
    private static final int SEARCH_X = 8, SEARCH_Y = 5, SEARCH_W = 150, SEARCH_H = 14;
    private final List<Particle> particles = new ArrayList<>();

    private static void roundedFill(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int r, int color) {
        if (r < 1 || x2 <= x1 || y2 <= y1) { g.fill(x1, y1, x2, y2, color); return; }
        r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
        g.fill(x1 + r, y1, x2 - r, y2, color);
        g.fill(x1, y1 + r, x1 + r, y2 - r, color);
        g.fill(x2 - r, y1 + r, x2, y2 - r, color);
        for (int i = 0; i < r; i++) {
            int t = r - i;
            g.fill(x1 + i, y1 + t - 1, x1 + i + 1, y1 + r, color);
            g.fill(x2 - i - 1, y1 + t - 1, x2 - i, y1 + r, color);
            g.fill(x1 + i, y2 - r, x1 + i + 1, y2 - t + 1, color);
            g.fill(x2 - i - 1, y2 - r, x2 - i, y2 - t + 1, color);
        }
    }

    private static final class Particle {
        float x, y, vy, size;
        Particle(float x, float y) {
            this.x = x; this.y = y;
            this.vy = 0.25f + (float) Math.random() * 0.85f;
            this.size = 1 + (float) Math.random() * 2;
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
        int x = 10;
        for (Category cat : Category.values()) {
            if (manager() != null && manager().getModulesByCategory(cat).isEmpty()) continue;
            Panel p = new Panel(cat, x, 36);
            panels.put(cat, p);
            panelList.add(p);
            x += 94;
        }
    }

    @Override public boolean isInGameUi() { return true; }
    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x99050A12);
        graphics.fill(0, 0, this.width, this.height, 0x44060C18);
        if (particles.isEmpty() && this.width > 0) {
            for (int i = 0; i < 40; i++)
                particles.add(new Particle((float) (Math.random() * this.width), (float) (Math.random() * this.height)));
        }
        for (Particle p : particles) {
            p.y += p.vy;
            if (p.y > this.height) { p.y = -4; p.x = (float) (Math.random() * Math.max(1, this.width)); }
            int s = Math.max(1, (int) p.size);
            graphics.fill((int) p.x, (int) p.y, (int) p.x + s, (int) p.y + s, 0x5500E8A0);
        }
    }

    @Override protected void extractBlurredBackground(GuiGraphicsExtractor graphics) {
        try { super.extractBlurredBackground(graphics); } catch (Throwable ignored) {}
    }
    @Override public void extractTransparentBackground(GuiGraphicsExtractor graphics) {}
    @Override protected void extractMenuBackground(GuiGraphicsExtractor graphics) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        hoveredModule = null;
        roundedFill(graphics, SEARCH_X, SEARCH_Y, SEARCH_X + SEARCH_W, SEARCH_Y + SEARCH_H, 4, SEARCH_BG);
        graphics.fill(SEARCH_X, SEARCH_Y, SEARCH_X + 2, SEARCH_Y + SEARCH_H, accent());
        String shown = searchQuery.isEmpty() ? (searchFocused ? "search..." : "search modules") : searchQuery + (searchFocused ? "_" : "");
        graphics.text(this.font, shown, SEARCH_X + 8, SEARCH_Y + 4, searchQuery.isEmpty() ? TEXT_DIM : 0xFFE8EAED, false);
        graphics.text(this.font, "tt  " + TTClient.VERSION, SEARCH_X + SEARCH_W + 10, SEARCH_Y + 4, accent(), false);
        if (searchQuery != null && !searchQuery.isBlank() && manager() != null) {
            String q = searchQuery.toLowerCase(Locale.ROOT);
            int sy = SEARCH_Y + SEARCH_H + 2;
            int shownN = 0;
            for (Module m : manager().getModules()) {
                if (!m.getName().toLowerCase(Locale.ROOT).contains(q) && !m.getDescription().toLowerCase(Locale.ROOT).contains(q)) continue;
                roundedFill(graphics, SEARCH_X, sy, SEARCH_X + SEARCH_W, sy + 13, 3, 0xF010141C);
                graphics.fill(SEARCH_X, sy, SEARCH_X + 2, sy + 13, m.isEnabled() ? accent() : 0xFF334155);
                graphics.text(this.font, m.getName(), SEARCH_X + 8, sy + 3, m.isEnabled() ? accent() : 0xFFE8EAED, false);
                sy += 13;
                if (++shownN >= 10) break;
            }
        }
        for (Panel panel : panelList) panel.render(graphics, mouseX, mouseY);
        if (hoveredModule != null && typing == null) {
            String title = hoveredModule.getName();
            String desc = hoveredModule.getDescription();
            int pad = 8, tw = Math.max(font.width(title), font.width(desc)), boxW = tw + pad * 2, boxH = 26;
            int tx = Math.min(mouseX + 12, this.width - boxW - 4), ty = Math.min(mouseY + 12, this.height - boxH - 4);
            roundedFill(graphics, tx, ty, tx + boxW, ty + boxH, 4, 0xF0080A0E);
            graphics.fill(tx, ty, tx + 2, ty + boxH, accent());
            graphics.text(font, title, tx + pad, ty + 4, accent(), false);
            graphics.text(font, desc, tx + pad, ty + 14, TEXT_DIM, false);
        }
        if (bindingModule != null) {
            String msg = "bind  " + bindingModule.getName() + "  ·  esc clear";
            int tw = this.font.width(msg);
            roundedFill(graphics, this.width / 2 - tw / 2 - 12, this.height - 30, this.width / 2 + tw / 2 + 12, this.height - 12, 4, 0xF0080A0E);
            graphics.text(this.font, msg, this.width / 2 - tw / 2, this.height - 24, accent(), false);
        }
    }

    @Override public void onClose() {
        commitTyping(true); sliding = null;
        for (Panel p : panelList) if (p.expanded instanceof BlockESP be) be.uiSearchFocused = false;
        super.onClose();
        if (TTClientClient.config != null) TTClientClient.config.save();
    }

    private void commitTyping(boolean apply) {
        if (typing == null) return;
        if (apply) { try { String raw = typingBuffer.trim().replace(',', '.'); if (!raw.isEmpty()) typing.set(Double.parseDouble(raw)); } catch (NumberFormatException ignored) {} }
        typing = null; typingBuffer = "";
    }

    public void startBinding(Module module) { commitTyping(true); this.bindingModule = module; searchFocused = false; }

    public class Panel {
        private final Category category;
        private double x, y;
        private boolean open = true, dragging = false;
        private double dragOffsetX, dragOffsetY;
        private final int width = 88, headerHeight = 15;
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

        public void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
            int ix = (int) x, iy = (int) y;
            List<Module> mods = filteredModules();
            int bodyH = headerHeight + (open ? mods.size() * ROW_H : 0);
            if (open && expanded != null) {
                for (Module mod : mods) if (expanded == mod) bodyH += mod.getSettings().size() * SETTING_H + SETTING_H;
            }
            roundedFill(g, ix + 2, iy + 2, ix + width + 2, iy + bodyH + 2, 7, 0x44000000);
            roundedFill(g, ix, iy, ix + width, iy + bodyH, 7, PANEL_BG);
            roundedFill(g, ix, iy, ix + width, iy + headerHeight, 7, HEADER_BG);
            g.fill(ix + 1, iy + 5, ix + 3, iy + Math.max(headerHeight + 1, bodyH - 5), accent());
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
                        g.text(font, s.getName(), ix + 8, my + 2, TEXT_DIM, false);
                        my += SETTING_H;
                    }
                    g.text(font, "bind: click middle", ix + 8, my + 2, TEXT_DIM, false);
                    my += SETTING_H;
                }
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int ix = (int) x, iy = (int) y;
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight) {
                if (button == 0) { dragging = true; dragOffsetX = mouseX - x; dragOffsetY = mouseY - y; return true; }
                if (button == 1) { open = !open; return true; }
            }
            if (!open) return false;
            List<Module> mods = filteredModules();
            int my = iy + headerHeight;
            for (Module mod : mods) {
                if (mouseX >= x && mouseX <= x + width && mouseY >= my && mouseY <= my + ROW_H) {
                    if (button == 0) { mod.toggle(); return true; }
                    if (button == 1) { expanded = expanded == mod ? null : mod; return true; }
                    if (button == 2) { bindingModule = mod; return true; }
                }
                my += ROW_H;
                if (expanded == mod) {
                    my += mod.getSettings().size() * SETTING_H + SETTING_H;
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

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y(); int button = event.button();
        if (bindingModule != null && button >= 0) {
            bindingModule.setKeyBind(2000 + button);
            bindingModule = null;
            return true;
        }
        if (button == 0 && mouseX >= SEARCH_X && mouseX <= SEARCH_X + SEARCH_W && mouseY >= SEARCH_Y && mouseY <= SEARCH_Y + SEARCH_H) {
            searchFocused = true; commitTyping(true);
            return true;
        }
        if (button == 0 && searchQuery != null && !searchQuery.isBlank() && manager() != null
                && mouseX >= SEARCH_X && mouseX <= SEARCH_X + SEARCH_W && mouseY > SEARCH_Y + SEARCH_H) {
            String q = searchQuery.toLowerCase(Locale.ROOT);
            int sy = SEARCH_Y + SEARCH_H + 2;
            for (Module m : manager().getModules()) {
                if (!m.getName().toLowerCase(Locale.ROOT).contains(q) && !m.getDescription().toLowerCase(Locale.ROOT).contains(q)) continue;
                if (mouseY >= sy && mouseY <= sy + 13) { m.toggle(); return true; }
                sy += 13;
                if (sy > SEARCH_Y + SEARCH_H + 2 + 13 * 10) break;
            }
        }
        if (button == 0) searchFocused = false;
        for (int i = panelList.size() - 1; i >= 0; i--) if (panelList.get(i).mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(event, doubleClick);
    }

    @Override public boolean mouseReleased(MouseButtonEvent event) {
        sliding = null; for (Panel panel : panelList) panel.mouseReleased(); return super.mouseReleased(event);
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        for (Panel panel : panelList) if (panel.mouseDragged(event.x(), event.y())) return true;
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
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
}
